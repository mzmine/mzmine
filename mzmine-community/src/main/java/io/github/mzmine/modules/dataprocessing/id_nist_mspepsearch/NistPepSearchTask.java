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
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.modules.dataprocessing.id_spectral_match_sort.SortSpectralMatchesTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
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
public final class NistPepSearchTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(NistPepSearchTask.class.getName());

  private static final String SEARCH_METHOD = "NIST MSPepSearch";

  /**
   * Prefix of the generated query spectrum names. Kept short and free of characters that MSP or the
   * tab delimited output would treat specially, and placed at the very start of the name so that it
   * survives any truncation NIST applies when echoing the name back.
   */
  private static final String QUERY_NAME_PREFIX = "mzmine_";

  /**
   * MSPepSearch is killed if it has not finished within this time. Generous enough for a whole
   * feature list against several libraries, short enough that a hung process does not block the
   * task list forever.
   */
  private static final long TIMEOUT_MS = 60L * 60L * 1000L;

  private final FeatureList featureList;
  private final FeatureListRow singleRow;
  private final NistSearchConfig config;
  private final Class<? extends MZmineModule> appliedModule;
  private final ParameterSet appliedParameters;
  private final NistSearchMode mode;
  private final File executable;
  private final FragmentScanSelection fragmentScanSelection;
  private final IntegerMode integerMz;

  private Process process;

  private int progressMax;
  private int progress;
  private int annotatedRows;
  private int totalHits;
  private int emptySpectra;
  private int missingMassLists;
  private int unmappedHits;

  /**
   * @param config            what to search and how.
   * @param featureList       the feature list to annotate.
   * @param row               a single row to search, or null for the whole feature list.
   * @param mergeSelect       how the fragment spectra of a row are merged and selected.
   * @param appliedModule     the module recorded in the applied methods of the feature list.
   * @param appliedParameters the parameters recorded there. These are the parameters of
   *                          {@code appliedModule}, not the config above, so that reprocessing a
   *                          feature list reproduces the search.
   */
  public NistPepSearchTask(@NotNull final NistSearchConfig config,
      @NotNull final FeatureList featureList, @Nullable final FeatureListRow row,
      @NotNull final SpectraMergeSelectParameter mergeSelect,
      @NotNull final Class<? extends MZmineModule> appliedModule,
      @NotNull final ParameterSet appliedParameters, @NotNull final Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.featureList = featureList;
    this.singleRow = row;
    this.config = config;
    this.appliedModule = appliedModule;
    this.appliedParameters = appliedParameters;
    this.mode = config.mode();
    this.executable = config.executable();

    this.fragmentScanSelection = mergeSelect.createFragmentScanSelection(getMemoryMapStorage());

    // the NIST EI libraries are unit mass, so rounding only ever applies to those searches
    this.integerMz = mode.isHighResolution() ? null : config.integerMz();
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
          new SimpleFeatureListAppliedMethod(appliedModule, appliedParameters,
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

    // A batch can point at an installation that has been moved or uninstalled since it was saved,
    // in which case the setup dialog never had a chance to complain about it.
    final List<String> problems = config.validate();
    if (!problems.isEmpty()) {
      throw new IOException(String.join(" ", problems));
    }

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

      final List<String> command = MsPepSearchCommand.build(config, executable, queryFile,
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

    final boolean finished = running.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS);

    if (!finished) {
      running.destroy();
      throw new IOException(
          "MSPepSearch did not finish within %d minutes. Reduce the number of spectra or libraries.".formatted(
              TIMEOUT_MS / 60_000));
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

    // MSPepSearch does not return the library spectrum, so the entry carries no peaks and the
    // mirror plot of the match stays empty. Which fields a hit has depends on the search mode, so
    // they all go in through putIfNotNull.
    final SpectralLibraryEntry entry = new SpectralDBEntry(null, new double[0], new double[0]);
    entry.putIfNotNull(DBEntryField.NAME, hit.name());
    entry.putIfNotNull(DBEntryField.ENTRY_ID, hit.entryId());
    entry.putIfNotNull(DBEntryField.FORMULA, hit.formula());
    entry.putIfNotNull(DBEntryField.CAS, hit.cas());
    entry.putIfNotNull(DBEntryField.INCHIKEY, hit.inChIKey());
    entry.putIfNotNull(DBEntryField.EXACT_MASS, hit.exactMass());
    entry.putIfNotNull(DBEntryField.MOLWEIGHT,
        hit.nominalMw() == null ? null : hit.nominalMw().doubleValue());
    entry.putIfNotNull(DBEntryField.PRECURSOR_MZ, hit.precursorMz());
    entry.putIfNotNull(DBEntryField.ION_TYPE, hit.precursorType());
    entry.putIfNotNull(DBEntryField.CHARGE, hit.charge());
    entry.putIfNotNull(DBEntryField.INSTRUMENT_TYPE, hit.instrumentType());
    entry.putIfNotNull(DBEntryField.NUM_PEAKS, hit.numPeaks());
    entry.putIfNotNull(DBEntryField.SOFTWARE, SEARCH_METHOD);

    // COLLISION_ENERGY is a numeric field but NIST reports free text such as "NCE=65% 34eV", so it
    // only goes in when it actually parses and is kept in the comment otherwise.
    final Object collisionEnergy = hit.collisionEnergy() == null ? null
        : DBEntryField.COLLISION_ENERGY.tryConvertValue(hit.collisionEnergy());
    entry.putIfNotNull(DBEntryField.COLLISION_ENERGY, collisionEnergy);

    entry.putIfNotNull(DBEntryField.COMMENT, buildComment(hit, collisionEnergy == null));

    final SpectralSimilarity similarity = new SpectralSimilarity(scoreName(),
        hit.score0to1(), hit.numMatchedPeaks() == null ? 0 : hit.numMatchedPeaks(), Double.NaN);

    return new SpectralDBAnnotation(entry, similarity, query.scan(), null,
        query.row().getAverageMZ(), query.row().getAverageRT(), null);
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
