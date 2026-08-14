/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectModuleOptions;
import io.github.mzmine.modules.dataprocessing.id_nist_mspepsearch.NistEiSearchParameters.NistRetentionIndexParameters;
import io.github.mzmine.modules.dataprocessing.id_spectral_match_sort.SortSpectralMatchesTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.RITolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.RIColumn;
import io.github.mzmine.util.RIRecord;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Searches feature list rows against NIST libraries with NIST's command line program MSPepSearch.
 * <p>
 * All query spectra go into a single MSP file and are searched in one run of the executable, which
 * is what MSPepSearch is designed for: it reports one block of hits per submitted spectrum. Results
 * are mapped back to the query by the 1-based ordinal MSPepSearch echoes in its {@code Num} column,
 * falling back to the {@code Name:} of the MSP entry.
 */
public class NistPepSearchTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(NistPepSearchTask.class.getName());

  private static final String SEARCH_METHOD = "NIST MSPepSearch";

  /**
   * Prefix of the generated query spectrum names. Kept short and free of characters that MSP or the
   * tab delimited output would treat specially, and placed at the very start of the name so that it
   * survives any truncation NIST applies when echoing the name back.
   */
  private static final String QUERY_NAME_PREFIX = "mzmine_";

  private final FeatureList featureList;
  private final FeatureListRow singleRow;
  private final ParameterSet parameters;
  private final NistSearchMode mode;
  private final File executable;
  private final FragmentScanSelection fragmentScanSelection;
  private final IntegerMode integerMz;
  private final RITolerance riTolerance;
  private final long timeoutMs;

  private Process process;

  private int progressMax;
  private int progress;
  private int annotatedRows;
  private int totalHits;
  private int emptySpectra;
  private int missingMassLists;
  private int unmappedHits;

  public NistPepSearchTask(final FeatureList featureList, final ParameterSet parameters,
      @NotNull final Instant moduleCallDate) {
    this(null, featureList, parameters, moduleCallDate);
  }

  public NistPepSearchTask(@Nullable final FeatureListRow row, final FeatureList featureList,
      final ParameterSet parameters, @NotNull final Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.featureList = featureList;
    this.singleRow = row;
    this.parameters = parameters;
    this.mode = parameters.getValue(NistPepSearchParameters.searchMode);
    this.executable = ((NistPepSearchParameters) parameters).getExecutable();

    final SpectraMergeSelectModuleOptions mergeSelect = parameters.getValue(
        NistPepSearchParameters.spectraMergeSelect);
    this.fragmentScanSelection = mergeSelect.createFragmentScanSelection(getMemoryMapStorage(),
        mergeSelect.getModuleParameters());

    final ParameterSet ei = parameters.getParameter(NistPepSearchParameters.eiParameters)
        .getEmbeddedParameters();
    this.integerMz = !mode.isHighResolution() && ei.getValue(NistEiSearchParameters.integerMz)
        ? ei.getParameter(NistEiSearchParameters.integerMz).getEmbeddedParameter().getValue() : null;
    this.riTolerance = createRiTolerance(ei);

    final ParameterSet advanced = parameters.getParameter(NistPepSearchParameters.advanced)
        .getEmbeddedParameters();
    this.timeoutMs =
        advanced.getValue(NistPepSearchAdvancedParameters.timeout) * 60L * 1000L;
  }

  /**
   * The tolerance used to compute the delta RI of an annotation. Only set when the EI search is
   * configured to use retention indices.
   */
  private static @Nullable RITolerance createRiTolerance(final ParameterSet ei) {

    if (!ei.getValue(NistEiSearchParameters.retentionIndex)) {
      return null;
    }

    final ParameterSet ri = ei.getParameter(NistEiSearchParameters.retentionIndex)
        .getEmbeddedParameters();
    return new RITolerance(ri.getValue(NistRetentionIndexParameters.tolerance),
        ri.getValue(NistRetentionIndexParameters.column), false);
  }

  @Override
  public String getTaskDescription() {
    return "Running NIST MSPepSearch for " + featureList;
  }

  @Override
  public double getFinishedPercentage() {
    return progressMax == 0 ? 0.0 : (double) progress / (double) progressMax;
  }

  @Override
  public void cancel() {
    super.cancel();

    // MSPepSearch does not react to anything but being killed
    final Process running = process;
    if (running != null) {
      running.destroy();
    }
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    try {
      search();

      if (isCanceled()) {
        return;
      }

      featureList.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(NistPepSearchModule.class, parameters,
              getModuleCallDate()));
      setStatus(TaskStatus.FINISHED);

      logger.info(
          () -> "NIST MSPepSearch completed: %d hits on %d rows".formatted(totalHits, annotatedRows));

    } catch (Throwable t) {
      logger.log(Level.SEVERE, "NIST MSPepSearch error", t);
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
    }
  }

  private void search() throws IOException, InterruptedException {

    final List<QuerySpectrum> queries = collectQueries();
    logSkippedSpectra();

    if (queries.isEmpty()) {
      logger.warning("No spectra to search - nothing was submitted to MSPepSearch.");
      return;
    }

    featureList.addRowType(DataTypes.get(SpectralLibraryMatchesType.class));

    // The hybrid search needs the nominal molecular weight of the unknown, and MSPepSearch only
    // accepts it as a single global /MwForLoss value, so those queries have to be grouped by
    // molecular weight and searched one group at a time. Every other mode is a single run.
    progress = 0;
    progressMax = queries.size();

    for (final Map.Entry<Integer, List<QuerySpectrum>> group : groupQueries(queries).entrySet()) {

      if (isCanceled()) {
        return;
      }

      runSearch(group.getValue(), group.getKey());
      progress += group.getValue().size();
    }

    if (unmappedHits > 0) {
      logger.warning(() -> unmappedHits
          + " hits could not be mapped back to a query spectrum and were ignored.");
    }
  }

  /**
   * Groups the queries for the {@code /MwForLoss} constraint of the hybrid search. Every other mode
   * returns a single group with a null key.
   */
  private Map<Integer, List<QuerySpectrum>> groupQueries(final List<QuerySpectrum> queries) {

    if (!mode.needsMolecularWeight()) {
      final Map<Integer, List<QuerySpectrum>> single = new LinkedHashMap<>();
      single.put(null, queries);
      return single;
    }

    final Map<Integer, List<QuerySpectrum>> grouped = new LinkedHashMap<>();
    int withoutMw = 0;

    for (final QuerySpectrum query : queries) {
      if (query.nominalMw() == null) {
        withoutMw++;
        continue;
      }
      grouped.computeIfAbsent(query.nominalMw(), _ -> new ArrayList<>()).add(query);
    }

    if (withoutMw > 0) {
      final int skipped = withoutMw;
      logger.warning(() -> skipped
          + " spectra were skipped: the hybrid search needs the molecular weight of the unknown, "
          + "which requires an annotation or a known neutral mass on the row.");
    }

    return grouped;
  }

  /**
   * Writes one query file, runs MSPepSearch once and applies the hits.
   */
  private void runSearch(final List<QuerySpectrum> queries, @Nullable final Integer mwForLoss)
      throws IOException, InterruptedException {

    final File queryFile = FileAndPathUtil.createTempFile("mzmine_mspepsearch_", ".msp");
    final File outputFile = FileAndPathUtil.createTempFile("mzmine_mspepsearch_", ".tsv");
    queryFile.deleteOnExit();
    outputFile.deleteOnExit();

    try {
      writeQueryFile(queryFile, queries);

      final List<String> command = MsPepSearchCommand.build(parameters, executable, queryFile,
          outputFile, FileAndPathUtil.getTempDir(), mwForLoss);

      execute(command);

      if (isCanceled()) {
        return;
      }

      final NistPepSearchResult result = MsPepSearchOutputParser.parse(outputFile);
      result.warnings().forEach(warning -> logger.warning(() -> "MSPepSearch output: " + warning));

      applyHits(queries, result);

    } finally {
      queryFile.delete();
      outputFile.delete();
    }
  }

  /**
   * Runs MSPepSearch and waits for it, honouring cancellation.
   */
  private void execute(final List<String> command) throws IOException, InterruptedException {

    logger.finest(() -> "Running MSPepSearch: " + String.join(" ", command));

    // The working directory must be the MSPepSearch folder: the executable loads nistdl64a.dll,
    // dForm64.dll and its other siblings from there.
    final ProcessBuilder builder = new ProcessBuilder(command).directory(
        executable.getParentFile());

    process = builder.start();
    final Process running = process;

    // Drain both streams on their own threads. Reading them from one thread risks blocking on a
    // full pipe buffer, and stderr carries the /PROGRESSNS messages and any error text.
    final StringBuilder stdout = new StringBuilder();
    final StringBuilder stderr = new StringBuilder();
    final Thread outDrain = drain(running.getInputStream(), stdout);
    final Thread errDrain = drain(running.getErrorStream(), stderr);

    final boolean finished = running.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

    if (!finished) {
      running.destroy();
      throw new IOException(
          "MSPepSearch did not finish within %d minutes. Increase the timeout in the advanced parameters, or reduce the number of spectra or libraries.".formatted(
              timeoutMs / 60_000));
    }

    outDrain.join(1_000);
    errDrain.join(1_000);

    final int exitCode = running.exitValue();
    process = null;

    if (isCanceled()) {
      return;
    }

    if (exitCode != 0) {
      throw new IOException(
          "MSPepSearch failed with exit code %d.%n%s%n%s%nCommand: %s".formatted(exitCode,
              tail(stderr), tail(stdout), String.join(" ", command)));
    }

    logger.finest(() -> "MSPepSearch finished: " + tail(stderr));
  }

  private static Thread drain(final InputStream stream, final StringBuilder target) {

    final Thread thread = new Thread(() -> {
      try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        final char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) >= 0) {
          synchronized (target) {
            target.append(buffer, 0, read);
          }
        }
      } catch (IOException e) {
        // the process was killed, nothing to add
      }
    }, "MSPepSearch output reader");

    thread.setDaemon(true);
    thread.start();
    return thread;
  }

  /**
   * The last part of a captured stream, so that an error message stays readable.
   */
  private static String tail(final StringBuilder text) {

    synchronized (text) {
      final String string = text.toString().strip();
      return string.length() <= 2000 ? string : "..." + string.substring(string.length() - 2000);
    }
  }

  /**
   * Collects the spectra to search, in submission order.
   */
  private List<QuerySpectrum> collectQueries() {

    final List<FeatureListRow> rows =
        singleRow == null ? featureList.getRows() : List.of(singleRow);

    final List<QuerySpectrum> queries = new ArrayList<>();
    for (final FeatureListRow row : rows) {

      if (!row.hasMs2Fragmentation()) {
        continue;
      }

      int queryIndex = 0;
      for (final Scan scan : fragmentScanSelection.getAllFragmentSpectra(row)) {

        final DataPoint[] dataPoints = extractSearchSpectrum(scan);
        if (dataPoints == null) {
          continue;
        }
        if (dataPoints.length == 0) {
          emptySpectra++;
          continue;
        }

        queries.add(
            new QuerySpectrum(queries.size() + 1, queryName(row, queryIndex++), row, scan,
                dataPoints, nominalMw(row, dataPoints)));
      }
    }

    return queries;
  }

  /**
   * The {@code Name:} written to the query file. MSPepSearch echoes it verbatim in the
   * {@code Unknown} column, which makes it the fallback mapping key.
   */
  private static String queryName(final FeatureListRow row, final int queryIndex) {
    return QUERY_NAME_PREFIX + row.getID() + "_" + queryIndex;
  }

  /**
   * The nominal molecular weight of the unknown, needed by the hybrid search.
   * <p>
   * Taken from an existing annotation when there is one. Hybrid searches are run precisely on the
   * rows that have no annotation yet, so the fallback is the highest m/z of the spectrum, i.e. the
   * usual assumption that the heaviest signal of an EI spectrum is the molecular ion.
   */
  private static @Nullable Integer nominalMw(final FeatureListRow row,
      final DataPoint[] dataPoints) {

    final FeatureAnnotation annotation = row.getPreferredAnnotation();
    if (annotation != null && annotation.getPrecursorMZ() != null) {
      return (int) Math.round(annotation.getPrecursorMZ());
    }

    double highestMz = 0;
    for (final DataPoint dataPoint : dataPoints) {
      highestMz = Math.max(highestMz, dataPoint.getMZ());
    }

    return highestMz > 0 ? (int) Math.round(highestMz) : null;
  }

  private @Nullable DataPoint[] extractSearchSpectrum(final Scan scan) {

    final DataPoint[] dataPoints;
    try {
      dataPoints = ScanUtils.extractDataPoints(scan, true);
    } catch (MissingMassListException e) {
      missingMassLists++;
      return null;
    }

    return integerMz != null ? ScanUtils.integerDataPoints(dataPoints, integerMz) : dataPoints;
  }

  /**
   * Writes the query spectra as one MSP file.
   */
  private void writeQueryFile(final File file, final List<QuerySpectrum> queries)
      throws IOException {

    try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {

      for (final QuerySpectrum query : queries) {

        writer.write("Name: " + query.name());
        writer.newLine();

        // A precursor m/z only means something for the accurate mass MS/MS searches; the EI
        // searches ignore it.
        if (mode.isHighResolution()) {
          final Double precursorMz = query.scan().getPrecursorMz() != null
              ? query.scan().getPrecursorMz().doubleValue() : query.row().getAverageMZ();
          if (precursorMz != null) {
            writer.write("PrecursorMZ: " + precursorMz);
            writer.newLine();
          }
        }

        if (query.nominalMw() != null) {
          writer.write("MW: " + query.nominalMw());
          writer.newLine();
        }

        final Float rt = query.row().getAverageRT();
        if (rt != null) {
          writer.write("RT: " + rt);
          writer.newLine();
        }

        writer.write("Num Peaks: " + query.dataPoints().length);
        writer.newLine();

        for (final DataPoint dataPoint : query.dataPoints()) {
          writer.write(dataPoint.getMZ() + "\t" + dataPoint.getIntensity());
          writer.newLine();
        }

        writer.newLine();
      }
    }
  }

  /**
   * Maps the hits back to their query spectra and adds them to the rows.
   */
  private void applyHits(final List<QuerySpectrum> queries, final NistPepSearchResult result) {

    final Map<Integer, QuerySpectrum> bySpecNum = new HashMap<>();
    final Map<String, QuerySpectrum> byName = new HashMap<>();
    for (final QuerySpectrum query : queries) {
      bySpecNum.put(query.specNum(), query);
      byName.put(query.name().toLowerCase(Locale.ROOT), query);
    }

    // collect per row so that all hits of a row are added and sorted in one go
    final Map<FeatureListRow, List<SpectralDBAnnotation>> matchesByRow = new LinkedHashMap<>();

    for (final NistHit hit : result.hits()) {

      QuerySpectrum query = bySpecNum.get(hit.specNum());
      if (query == null && hit.unknownName() != null) {
        query = byName.get(hit.unknownName().toLowerCase(Locale.ROOT));
      }

      if (query == null) {
        unmappedHits++;
        continue;
      }

      matchesByRow.computeIfAbsent(query.row(), _ -> new ArrayList<>())
          .add(toAnnotation(hit, query));
    }

    matchesByRow.forEach((row, matches) -> {
      row.addSpectralLibraryMatches(matches);
      SortSpectralMatchesTask.sortIdentities(row);
      annotatedRows++;
      totalHits += matches.size();
    });
  }

  /**
   * Builds an annotation from one MSPepSearch hit.
   */
  private SpectralDBAnnotation toAnnotation(final NistHit hit, final QuerySpectrum query) {

    // A HashMap, not Map.of: most of these fields are absent depending on the search mode.
    final Map<DBEntryField, Object> fields = new HashMap<>();
    putIfNotNull(fields, DBEntryField.NAME, hit.name());
    putIfNotNull(fields, DBEntryField.ENTRY_ID, hit.entryId());
    putIfNotNull(fields, DBEntryField.FORMULA, hit.formula());
    putIfNotNull(fields, DBEntryField.CAS, hit.cas());
    putIfNotNull(fields, DBEntryField.INCHIKEY, hit.inChIKey());
    putIfNotNull(fields, DBEntryField.EXACT_MASS, hit.exactMass());
    putIfNotNull(fields, DBEntryField.MOLWEIGHT,
        hit.nominalMw() == null ? null : hit.nominalMw().doubleValue());
    putIfNotNull(fields, DBEntryField.PRECURSOR_MZ, hit.precursorMz());
    putIfNotNull(fields, DBEntryField.ION_TYPE, hit.precursorType());
    putIfNotNull(fields, DBEntryField.CHARGE, hit.charge());
    putIfNotNull(fields, DBEntryField.INSTRUMENT_TYPE, hit.instrumentType());
    putIfNotNull(fields, DBEntryField.NUM_PEAKS, hit.numPeaks());
    fields.put(DBEntryField.SOFTWARE, SEARCH_METHOD);

    final RIRecord libraryRi = parseRetentionIndex(hit.retentionIndex());
    putIfNotNull(fields, DBEntryField.RETENTION_INDEX, libraryRi);

    // COLLISION_ENERGY is a numeric field but NIST reports free text such as "NCE=65% 34eV", so it
    // only goes in when it actually parses and is kept in the comment otherwise.
    final Object collisionEnergy = hit.collisionEnergy() == null ? null
        : DBEntryField.COLLISION_ENERGY.tryConvertValue(hit.collisionEnergy());
    putIfNotNull(fields, DBEntryField.COLLISION_ENERGY, collisionEnergy);

    fields.put(DBEntryField.COMMENT, buildComment(hit, collisionEnergy == null));

    // MSPepSearch does not return the library spectrum, so the entry carries no peaks and the
    // mirror plot of the match stays empty.
    final SpectralLibraryEntry entry = new SpectralDBEntry(null, new double[0], new double[0],
        fields);

    final SpectralSimilarity similarity = new SpectralSimilarity(scoreName(),
        hit.score0to1(), hit.numMatchedPeaks() == null ? 0 : hit.numMatchedPeaks(), Double.NaN);

    final Float riDiff = computeRiDiff(query.row(), libraryRi);

    return new SpectralDBAnnotation(entry, similarity, query.scan(), null,
        query.row().getAverageMZ(), query.row().getAverageRT(), riDiff);
  }

  /**
   * The name of the score, so that the feature table shows which NIST measure it came from.
   */
  private String scoreName() {
    return mode.isHighResolution() ? "NIST score" : "NIST match factor";
  }

  /**
   * Everything worth keeping that has no dedicated field.
   */
  private String buildComment(final NistHit hit, final boolean includeCollisionEnergy) {

    final List<String> parts = new ArrayList<>();
    if (hit.libraryName() != null) {
      parts.add("Library: " + hit.libraryName());
    }
    if (hit.nistNumber() != null) {
      parts.add("NIST r.n.: " + hit.nistNumber());
    }
    if (hit.revMatchFactor() != null) {
      parts.add("Reverse match: " + hit.revMatchFactor());
    }
    if (hit.dotProduct() != null) {
      parts.add("Dot product: " + hit.dotProduct());
    }
    if (hit.probability() != null) {
      parts.add("Probability: " + hit.probability() + "%");
    }
    if (includeCollisionEnergy && hit.collisionEnergy() != null) {
      parts.add("Collision energy: " + hit.collisionEnergy());
    }
    parts.add("Searched with " + SEARCH_METHOD + " (" + mode + ")");

    return String.join("; ", parts);
  }

  /**
   * Converts an MSPepSearch retention index into an {@link RIRecord}.
   * <p>
   * MSPepSearch formats the RI column as value and column class, e.g. {@code 2480-S}, which
   * {@link RIRecord#fromString(String)} does not understand - it expects the {@code s=}, {@code n=},
   * {@code p=} or {@code a=} prefixes of the spectral library RI field.
   */
  static @Nullable RIRecord parseRetentionIndex(@Nullable final String value) {

    if (value == null || value.isBlank()) {
      return null;
    }

    final int separator = value.lastIndexOf('-');
    if (separator < 0) {
      // a plain number is the default column
      return RIRecord.fromString(value.trim());
    }

    final String number = value.substring(0, separator).trim();
    final String columnClass = value.substring(separator + 1).trim().toUpperCase(Locale.ROOT);

    final RIColumn column = switch (columnClass) {
      case "S" -> RIColumn.SEMIPOLAR;
      case "N" -> RIColumn.NONPOLAR;
      case "P" -> RIColumn.POLAR;
      // A = any, U = unspecified, V = AI predicted
      default -> RIColumn.DEFAULT;
    };

    return RIRecord.fromString(column.getShortDefinition() + "=" + number);
  }

  /**
   * The difference between the measured and the library retention index, or null if either is
   * unknown or retention indices are not in use.
   */
  private @Nullable Float computeRiDiff(final FeatureListRow row,
      @Nullable final RIRecord libraryRi) {

    if (riTolerance == null || libraryRi == null || row.getAverageRI() == null) {
      return null;
    }
    return riTolerance.getRiDifference(row.getAverageRI(), libraryRi);
  }

  private static void putIfNotNull(final Map<DBEntryField, Object> fields, final DBEntryField field,
      @Nullable final Object value) {

    if (value != null) {
      fields.put(field, value);
    }
  }

  private void logSkippedSpectra() {

    if (missingMassLists > 0) {
      logger.warning(() -> missingMassLists
          + " spectra were skipped because they have no mass list - run mass detection first.");
    }
    if (emptySpectra > 0) {
      logger.warning(() -> emptySpectra
          + " spectra were skipped because their mass list is empty - check the noise level of your mass detection.");
    }
  }

  /**
   * One spectrum submitted to MSPepSearch.
   *
   * @param specNum    1-based submission ordinal, which MSPepSearch echoes in its {@code Num}
   *                   column.
   * @param name       the {@code Name:} written to the query file, the fallback mapping key.
   * @param nominalMw  nominal molecular weight, only needed by the hybrid search.
   */
  private record QuerySpectrum(int specNum, String name, FeatureListRow row, Scan scan,
                               DataPoint[] dataPoints, @Nullable Integer nominalMw) {

  }
}
