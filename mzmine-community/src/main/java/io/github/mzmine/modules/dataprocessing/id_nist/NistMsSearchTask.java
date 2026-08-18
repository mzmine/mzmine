/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 * SPDX-License-Identifier: MIT
 */
package io.github.mzmine.modules.dataprocessing.id_nist;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.dataprocessing.id_spectral_match_sort.SortSpectralMatchesTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Runs NIST MSPepSearch without opening the NIST graphical interface. */
public class NistMsSearchTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(NistMsSearchModule.class.getName());
  private static final ReentrantLock SEARCH_LOCK = new ReentrantLock(true);
  private static final String SEARCH_METHOD = "NIST MSPepSearch (EI)";
  private static final long SEARCH_TIMEOUT_SECONDS = 120;
  /** Column titles that must all be present for a line to be treated as the /OUTTAB header. */
  static final List<String> REQUIRED_HEADER_COLUMNS = List.of("Rank", "MF", "Name");

  private final FeatureList featureList;
  private final FeatureListRow selectedRow;
  private final Scan explicitQueryScan;
  private final Feature preferredFeature;
  private final ParameterSet parameterSet;
  private final File executable;
  private final File libraryDirectory;
  private final NistLibrarySelection libraries;
  private final int minMatchFactor;
  private final int maxHits;
  private final boolean limitRawApexRetries;
  private final double minimumPercentAboveBaseline;
  private int progress;
  private int progressMax;
  private int addedHitCount;
  private int rawApexRetryCount;
  private int rawApexSkipCount;
  private int failedRowCount;

  public NistMsSearchTask(final FeatureList list, final ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    this(null, list, parameters, moduleCallDate, null, null);
  }

  public NistMsSearchTask(@Nullable final FeatureListRow row, final FeatureList list,
      final ParameterSet parameters, @NotNull Instant moduleCallDate) {
    this(row, list, parameters, moduleCallDate, null, null);
  }

  static NistMsSearchTask forSelectedFeature(@Nullable final FeatureListRow row,
      final FeatureList list, final ParameterSet parameters, @NotNull Instant moduleCallDate,
      @Nullable Feature preferredFeature) {
    return new NistMsSearchTask(row, list, parameters, moduleCallDate, preferredFeature, null);
  }

  public NistMsSearchTask(@Nullable final FeatureListRow row, final FeatureList list,
      final ParameterSet parameters, @NotNull Instant moduleCallDate,
      @Nullable Scan explicitQueryScan) {
    this(row, list, parameters, moduleCallDate, null, explicitQueryScan);
  }

  private NistMsSearchTask(@Nullable final FeatureListRow row, final FeatureList list,
      final ParameterSet parameters, @NotNull Instant moduleCallDate,
      @Nullable Feature preferredFeature, @Nullable Scan explicitQueryScan) {
    super(null, moduleCallDate);
    featureList = list;
    selectedRow = row;
    this.preferredFeature = preferredFeature;
    this.explicitQueryScan = explicitQueryScan;
    parameterSet = parameters;
    NistMsSearchParameters nistParameters = (NistMsSearchParameters) parameters;
    executable = nistParameters.getMsPepSearchExecutable();
    libraryDirectory = nistParameters.getNistLibraryDirectory();
    libraries = parameters.getValue(NistMsSearchParameters.LIBRARIES);
    minMatchFactor = parameters.getValue(NistMsSearchParameters.MIN_MATCH_FACTOR);
    maxHits = parameters.getValue(NistMsSearchParameters.MAX_HITS);
    var retryLimit = nistParameters.getParameter(
        NistMsSearchParameters.LIMIT_RAW_APEX_RETRIES);
    limitRawApexRetries = retryLimit.getValue();
    minimumPercentAboveBaseline = retryLimit.getEmbeddedParameter().getValue();
  }

  @Override
  public String getTaskDescription() {
    return explicitQueryScan == null ? "Searching NIST EI libraries for " + featureList
        : "Explicit NIST search at RT %.3f min".formatted(explicitQueryScan.getRetentionTime());
  }

  public int getAddedHitCount() {
    return addedHitCount;
  }

  public int getMinimumMatchFactor() {
    return minMatchFactor;
  }

  @Override
  public double getFinishedPercentage() {
    return progressMax == 0 ? 0d : (double) progress / progressMax;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.WAITING);
    try {
      if (isCanceled()) {
        return;
      }
      setStatus(TaskStatus.PROCESSING);
      // State which executable and libraries this run actually used. Without this, a search that
      // silently fell back to a bundled copy looks identical to one using the configured folder.
      logger.info(() -> "NIST search using MSPepSearch %s and libraries %s from %s".formatted(
          executable == null ? "<not found>" : executable.getAbsolutePath(), libraries,
          libraryDirectory));
      List<FeatureListRow> rows = selectedRow == null ? featureList.getRows()
          : List.of(selectedRow);
      progressMax = rows.size();
      for (FeatureListRow row : rows) {
        if (isCanceled()) {
          return;
        }
        SEARCH_LOCK.lockInterruptibly();
        try {
          searchSingleRow(row);
        } catch (IOException rowError) {
          // A single unreadable row, a timed-out MSPepSearch call or a transient library lock must
          // not discard the hits already stored for every other row. Record it and keep going.
          failedRowCount++;
          logger.log(Level.WARNING,
              "NIST search failed for row %d; continuing with the remaining rows".formatted(
                  row.getID()), rowError);
        } finally {
          SEARCH_LOCK.unlock();
        }
        progress++;
      }
      if (!isCanceled()) {
        logger.info(() -> "NIST search summary: list=%s, rows=%d, stored hits=%d, raw-apex retries=%d, retries skipped=%d, failed rows=%d"
            .formatted(featureList.getName(), progressMax, addedHitCount, rawApexRetryCount,
                rawApexSkipCount, failedRowCount));
        featureList.getAppliedMethods().add(
            new SimpleFeatureListAppliedMethod(NistMsSearchModule.class, parameterSet,
                getModuleCallDate()));
        setStatus(TaskStatus.FINISHED);
      }
    } catch (InterruptedException cancelled) {
      // Raised by lockInterruptibly when the user cancels while this row waits for the search lock.
      Thread.currentThread().interrupt();
      setStatus(TaskStatus.CANCELED);
    } catch (Throwable error) {
      logger.log(Level.SEVERE, "NIST MSPepSearch error", error);
      setErrorMessage(ExceptionUtils.exceptionToString(error));
      setStatus(TaskStatus.ERROR);
    }
  }

  /** Runs one row's search and stores any qualifying hits. Caller holds {@link #SEARCH_LOCK}. */
  private void searchSingleRow(FeatureListRow row) throws IOException, InterruptedException {
    Scan queryScan = findQueryScan(row);
    if (queryScan == null) {
      return;
    }
    List<SpectralDBAnnotation> hits = searchRow(row, queryScan);
    logger.info("NIST row search: list=%s, row=%d, row RT=%s, query RT=%.3f, query=%s, hits=%d"
        .formatted(featureList.getName(), row.getID(), row.getAverageRT(),
            queryScan.getRetentionTime(), queryType(queryScan), hits.size()));
    if (hits.isEmpty() && explicitQueryScan == null && queryScan instanceof PseudoSpectrum
        && shouldRetryRawApex(row)) {
      final Scan rawApexScan = findRawApexScan(row);
      if (rawApexScan != null && rawApexScan != queryScan) {
        rawApexRetryCount++;
        logger.info(() -> "No qualifying NIST hit for deconvoluted row %d at RT %s; retrying raw apex scan %d at RT %.3f"
            .formatted(row.getID(), row.getAverageRT(), rawApexScan.getScanNumber(),
                rawApexScan.getRetentionTime()));
        hits = searchRow(row, rawApexScan);
        final int fallbackHitCount = hits.size();
        logger.info(() -> "NIST raw-apex fallback: list=%s, row=%d, row RT=%s, query RT=%.3f, hits=%d"
            .formatted(featureList.getName(), row.getID(), row.getAverageRT(),
                rawApexScan.getRetentionTime(), fallbackHitCount));
      }
    } else if (hits.isEmpty() && explicitQueryScan == null && queryScan instanceof PseudoSpectrum
        && limitRawApexRetries) {
      rawApexSkipCount++;
      logger.fine(() -> "Skipping NIST raw-apex retry below local-baseline threshold: list=%s, row=%d, RT=%s"
          .formatted(featureList.getName(), row.getID(), row.getAverageRT()));
    }
    if (!hits.isEmpty()) {
      row.addSpectralLibraryMatches(hits);
      SortSpectralMatchesTask.sortIdentities(row);
      addedHitCount += hits.size();
    }
  }

  private boolean shouldRetryRawApex(FeatureListRow row) {
    if (!limitRawApexRetries) {
      return true;
    }
    final Feature feature = row.getBestFeature();
    if (feature == null || feature.getHeight() == null) {
      return true;
    }
    try {
      final var series = feature.getFeatureData();
      final int size = series.getNumberOfValues();
      if (size < 2) {
        return true;
      }
      final int edgeCount = Math.max(1, size / 5);
      final double[] edges = new double[edgeCount * 2];
      for (int i = 0; i < edgeCount; i++) {
        edges[i] = series.getIntensity(i);
        edges[edgeCount + i] = series.getIntensity(size - edgeCount + i);
      }
      Arrays.sort(edges);
      final double baseline = edges.length % 2 == 0
          ? (edges[edges.length / 2 - 1] + edges[edges.length / 2]) / 2d
          : edges[edges.length / 2];
      return isAboveLocalBaseline(feature.getHeight(), baseline, minimumPercentAboveBaseline);
    } catch (UnsupportedOperationException ignored) {
      // Older feature implementations may not expose their trace. Preserve the fallback in that case.
      return true;
    }
  }

  static boolean isAboveLocalBaseline(double apex, double baseline,
      double minimumPercentAboveBaseline) {
    return apex > 0d && (baseline <= 0d
        || apex >= baseline * (1d + minimumPercentAboveBaseline / 100d));
  }

  /** Prefer the deconvoluted GC-EI spectrum, then use the raw apex scan for an ordinary row. */
  private @Nullable Scan findQueryScan(FeatureListRow row) {
    if (explicitQueryScan != null && row == selectedRow) {
      return explicitQueryScan;
    }
    if (preferredFeature != null && row == selectedRow) {
      for (Scan scan : preferredFeature.getAllMS2FragmentScans()) {
        if (scan instanceof PseudoSpectrum pseudo
            && pseudo.getPseudoSpectrumType() == PseudoSpectrumType.GC_EI) {
          return scan;
        }
      }
      if (preferredFeature.getRepresentativeScan() != null) {
        return preferredFeature.getRepresentativeScan();
      }
    }
    List<Scan> fragments = row.getAllFragmentScans();
    for (Scan scan : fragments) {
      if (scan instanceof PseudoSpectrum pseudo
          && pseudo.getPseudoSpectrumType() == PseudoSpectrumType.GC_EI) {
        return scan;
      }
    }
    if (row.getBestFeature() != null && row.getBestFeature().getRepresentativeScan() != null) {
      return row.getBestFeature().getRepresentativeScan();
    }
    return fragments.isEmpty() ? null : fragments.getFirst();
  }

  private @Nullable Scan findRawApexScan(FeatureListRow row) {
    final var feature = preferredFeature != null && row == selectedRow ? preferredFeature
        : row.getBestFeature();
    if (feature == null) {
      return null;
    }
    final Scan representative = feature.getRepresentativeScan();
    if (representative != null && !(representative instanceof PseudoSpectrum)) {
      return representative;
    }
    if (feature.getRawDataFile() != null && feature.getRT() != null) {
      return feature.getRawDataFile().binarySearchClosestScan(feature.getRT(), 1);
    }
    return null;
  }

  private static String queryType(Scan scan) {
    return scan instanceof PseudoSpectrum pseudo
        ? "pseudo-spectrum " + pseudo.getPseudoSpectrumType() : "raw apex scan";
  }

  private List<SpectralDBAnnotation> searchRow(FeatureListRow row, Scan queryScan)
      throws IOException, InterruptedException {
    DataPoint[] points = ScanUtils.extractDataPoints(queryScan, true);
    if (points.length == 0) {
      return List.of();
    }

    Path workDir = Files.createTempDirectory("mzmine_mspep_");
    Path queryFile = workDir.resolve("query.msp");
    Path resultFile = workDir.resolve("hits.tsv");
    Path logFile = workDir.resolve("mspepsearch.log");
    try {
      writeQuery(queryFile, row, points);
      List<String> command = buildCommand(queryFile, resultFile);
      logger.fine(() -> "Executing headless NIST search: " + command);
      Process process = new ProcessBuilder(command).directory(executable.getParentFile())
          .redirectErrorStream(true).redirectOutput(logFile.toFile()).start();

      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(SEARCH_TIMEOUT_SECONDS);
      while (process.isAlive() && !isCanceled() && System.nanoTime() < deadline) {
        process.waitFor(200, TimeUnit.MILLISECONDS);
      }
      if (isCanceled()) {
        process.destroyForcibly();
        return List.of();
      }
      if (process.isAlive()) {
        process.destroyForcibly();
        throw new IOException("MSPepSearch timed out after " + SEARCH_TIMEOUT_SECONDS + " seconds");
      }
      if (process.exitValue() != 0 && !Files.exists(resultFile)) {
        String output = Files.exists(logFile) ? Files.readString(logFile) : "";
        throw new IOException("MSPepSearch failed (exit " + process.exitValue() + "): "
            + output.strip());
      }
      return parseResults(resultFile, row, queryScan);
    } finally {
      deleteWorkDirQuietly(workDir);
    }
  }

  /**
   * Removes the temporary search directory and everything MSPepSearch left in it. Cleanup never
   * throws: an exception raised here would replace the real result or mask the original failure.
   */
  private static void deleteWorkDirQuietly(Path workDir) {
    try (var entries = Files.walk(workDir)) {
      entries.sorted(Comparator.reverseOrder()).forEach(path -> {
        try {
          Files.deleteIfExists(path);
        } catch (IOException ignored) {
          // Leave the leftover behind; the OS temp directory is cleaned up eventually.
        }
      });
    } catch (IOException ignored) {
      // Nothing to clean up, or the directory is unreadable. Not worth failing the search over.
    }
  }

  private List<String> buildCommand(Path queryFile, Path resultFile) {
    List<String> command = new ArrayList<>();
    command.add(executable.getAbsolutePath());
    command.add("I");
    if (libraries.usesMain()) {
      command.add("/MAIN");
      command.add(new File(libraryDirectory, "mainlib").getAbsolutePath());
    }
    if (libraries.usesReplicate()) {
      command.add("/REPL");
      command.add(new File(libraryDirectory, "replib").getAbsolutePath());
    }
    command.addAll(List.of("/INP", queryFile.toAbsolutePath().toString(), "/OUTTAB",
        resultFile.toAbsolutePath().toString(), "/HITS", Integer.toString(maxHits), "/MinMF",
        Integer.toString(minMatchFactor), "/MaxNumSpec", "1", "/OutChemForm", "/OutCASrn",
        "/OutNISTrn", "/OutMW", "/OutNumMP", "v"));
    return command;
  }

  private void writeQuery(Path queryFile, FeatureListRow row, DataPoint[] points)
      throws IOException {
    long exportedPoints = java.util.Arrays.stream(points)
        .filter(point -> point.getMZ() > 0 && point.getIntensity() > 0).count();
    try (BufferedWriter writer = Files.newBufferedWriter(queryFile, StandardCharsets.UTF_8)) {
      writer.write("Name: MZmine row " + row.getID() + " RT " + row.getAverageRT());
      writer.newLine();
      writer.write("Comments: " + queryScanDescription(row));
      writer.newLine();
      writer.write("Num Peaks: " + exportedPoints);
      writer.newLine();
      for (DataPoint point : points) {
        if (point.getMZ() > 0 && point.getIntensity() > 0) {
          writer.write(Double.toString(point.getMZ()));
          writer.write(' ');
          writer.write(Double.toString(point.getIntensity()));
          writer.newLine();
        }
      }
    }
  }

  private String queryScanDescription(FeatureListRow row) {
    return "Feature list " + featureList.getName() + ", row " + row.getID();
  }

  private List<SpectralDBAnnotation> parseResults(Path resultFile, FeatureListRow row,
      Scan queryScan) throws IOException {
    if (!Files.exists(resultFile)) {
      return List.of();
    }
    List<SpectralDBAnnotation> matches = new ArrayList<>();
    List<String> header = null;
    int skippedBeforeHeader = 0;
    try (BufferedReader reader = Files.newBufferedReader(resultFile, StandardCharsets.UTF_8)) {
      for (String line; (line = reader.readLine()) != null; ) {
        if (line.isBlank() || line.startsWith(">")) {
          continue;
        }
        List<String> cells = parseTabLine(line);
        if (header == null) {
          if (cells.containsAll(REQUIRED_HEADER_COLUMNS)) {
            header = cells;
          } else {
            skippedBeforeHeader++;
          }
          continue;
        }
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
          values.put(header.get(i), i < cells.size() ? cells.get(i) : "");
        }
        int matchFactor = parseInt(values.get("MF"));
        if (matchFactor < minMatchFactor) {
          continue;
        }
        int reverseMatch = parseInt(values.get("R.Match"));
        int commonPeaks = parseInt(values.get("NumMP"));
        double probability = parseDouble(values.get("Prob(%)"));
        double molecularWeight = parseDouble(
            firstNonBlank(values.get("Lib MW"), values.get("Mass")));

        Map<DBEntryField, Object> fields = new EnumMap<>(DBEntryField.class);
        fields.put(DBEntryField.NAME, values.getOrDefault("Name", "Unknown"));
        fields.put(DBEntryField.ENTRY_ID,
            firstNonBlank(values.get("NIST r.n."), values.get("Id")));
        putIfNotBlank(fields, DBEntryField.FORMULA, values.get("Formula"));
        putIfNotBlank(fields, DBEntryField.CAS, values.get("CAS"));
        if (molecularWeight > 0) {
          fields.put(DBEntryField.MOLWEIGHT, molecularWeight);
        }
        fields.put(DBEntryField.SOFTWARE, SEARCH_METHOD);
        fields.put(DBEntryField.COMMENT,
            "Library: %s; rank: %s; MF: %d; reverse MF: %d; probability: %.1f%%; matched peaks: %d; library spectrum not available (MSPepSearch reports scores only)"
                .formatted(values.getOrDefault("Library", "NIST"),
                    values.getOrDefault("Rank", ""), matchFactor, reverseMatch, probability,
                    commonPeaks));

        // MSPepSearch's tabular output carries scores but no peak list, so there is no library
        // spectrum to store. Consumers that mirror-plot or re-score an annotation will find an
        // empty library side; the comment above records why. Fabricating peaks from the query
        // spectrum would manufacture a perfect self-match, so the arrays stay genuinely empty.
        SpectralLibraryEntry entry = new SpectralDBEntry(null, new double[0], new double[0],
            fields);
        SpectralSimilarity similarity = new SpectralSimilarity("NIST match factor",
            matchFactor / 1000d, commonPeaks, Double.NaN);
        matches.add(new SpectralDBAnnotation(entry, similarity, queryScan, null, null,
            row.getAverageRT(), null));
      }
    }
    if (header == null && skippedBeforeHeader > 0) {
      // Without a recognized header every data row is dropped, which is indistinguishable from
      // "nothing scored above MinMF". Say so loudly instead of reporting a silent zero-hit search.
      final int inspectedLines = skippedBeforeHeader;
      logger.warning(() ->
          "MSPepSearch produced %d non-empty line(s) but no header containing %s. No hits could be parsed for row %d - the installed MSPepSearch build may use different /OUTTAB column titles.".formatted(
              inspectedLines, REQUIRED_HEADER_COLUMNS, row.getID()));
    }
    return matches;
  }

  static List<String> parseTabLine(String line) {
    List<String> cells = new ArrayList<>();
    StringBuilder cell = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') {
        if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          cell.append('"');
          i++;
        } else {
          quoted = !quoted;
        }
      } else if (c == '\t' && !quoted) {
        cells.add(cell.toString());
        cell.setLength(0);
      } else {
        cell.append(c);
      }
    }
    cells.add(cell.toString());
    return cells;
  }

  private static void putIfNotBlank(Map<DBEntryField, Object> fields, DBEntryField key,
      @Nullable String value) {
    if (value != null && !value.isBlank()) {
      fields.put(key, value);
    }
  }

  private static String firstNonBlank(@Nullable String first, @Nullable String second) {
    return first != null && !first.isBlank() ? first : second == null ? "" : second;
  }

  private static int parseInt(@Nullable String value) {
    try {
      return (int) Math.round(Double.parseDouble(value));
    } catch (Exception ignored) {
      return 0;
    }
  }

  private static double parseDouble(@Nullable String value) {
    try {
      return Double.parseDouble(value);
    } catch (Exception ignored) {
      return 0d;
    }
  }
}
