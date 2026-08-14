/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_nist;

import static io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchParameters.DOT_PRODUCT;
import static io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchParameters.IMPORT_PARAMETER;
import static io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchParameters.INTEGER_MZ;
import static io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchParameters.NIST_MS_SEARCH_DIR;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectModuleOptions;
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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Performs NIST MS Search.
 *
 * @author $Author$
 * @version $Revision$
 */
public class NistMsSearchTask extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(NistMsSearchModule.class.getName());

  // Command-line arguments passed to executable. Kept as a list so that an installation path
  // containing spaces is passed as a single argument instead of being re-tokenized by the shell.
  private static final List<String> COMMAND_LINE_ARGS = List.of("/par=2", "/instrument");

  // Give up waiting for MS Search after this long instead of polling forever.
  private static final long SEARCH_TIMEOUT_MS = 5 * 60 * 1000L;

  // The locator file names.
  private static final String PRIMARY_LOCATOR_FILE_NAME = "AUTOIMP.MSD";
  private static final String SECONDARY_LOCATOR_FILE_NAME = "MZMINE2.MSD";

  // Spectra file prefix and suffix.
  private static final String SPECTRA_FILE_PREFIX = "MZM2NIST";
  private static final String SPECTRA_FILE_SUFFIX = ".MSP";

  // Spectrum name prefix and maximum length.
  private static final String SPECTRUM_NAME_PREFIX = "Row ";
  private static final int SPECTRUM_NAME_MAX_LENGTH = 511;

  // The search results file and polling file.
  private static final String SEARCH_POLL_FILE_NAME = "SRCREADY.TXT";
  private static final String SEARCH_RESULTS_FILE_NAME = "SRCRESLT.TXT";

  // Search method.
  private static final String SEARCH_METHOD = "NIST MS Search";

  // Regular expressions for matching header and hit lines in results.
  private static final Pattern SEARCH_REGEX = Pattern.compile(
      "^Unknown:\\s*" + SPECTRUM_NAME_PREFIX + "(\\d+).*");
  private static final Pattern RI_REGEX = Pattern.compile("RI:\\s*(\\d+)");
  private static final Pattern MF_REGEX = Pattern.compile("MF:\\s*(\\d+)");
  private static final Pattern RMF_REGEX = Pattern.compile("RMF:\\s*(\\d+)");
  //private static final Pattern ION_REGEX = Pattern.compile("  \\[.*?)\\]");
  private static final Pattern ION_REGEX = Pattern.compile("  (\\[.*?\\].*? )");
  private static final Pattern CAS_REGEX = Pattern.compile("CAS:\\s*([^;]*);");
  private static final Pattern MW_REGEX = Pattern.compile("Mw:\\s*(\\d+)");
  private static final Pattern ID_REGEX = Pattern.compile("Id:\\s*(\\d+)");
  private static final Pattern CMP_REGEX = Pattern.compile("^Hit.* : <<(.*?)>>");
  private static final Pattern FML_REGEX = Pattern.compile(";<<(.*?)>>");
  private static final Pattern LIB_REGEX = Pattern.compile("Lib: <<(.*?)>>");

  // Used to ensure that MS Search operations are synchronized.
  private static final Object SEMAPHORE = new Object();

  // Polling period for the search results file.
  private static final long POLL_RESULTS = 50L;
  // Import Options.
  private static ImportOption importOption;
  // The mass-list and peak-list.
  private final FeatureList peakList;
  // The feature list row to search for (null => all).
  private final FeatureListRow peakListRow;
  // Dot Product cut-offs.
  private final Double minDotProduct;
  // Optional params.
  private final IntegerMode integerMZ;
  // NIST MS Search directory and executable.
  private final File nistMsSearchDir;
  private final File nistMsSearchExe;
  private final ParameterSet parameterSet;
  // Progress counters.
  private int progress;
  private int progressMax;
  private FragmentScanSelection fragmentScanSelection;
  // Aggregated problems, reported once when the search finishes instead of failing the task.
  private int emptySpectra;
  private int missingMassLists;
  private int unmatchedRows;

  /**
   * Create the task.
   *
   * @param list   the feature list to search.
   * @param params search parameters.
   */
  public NistMsSearchTask(final FeatureList list, final ParameterSet params,
      @NotNull Instant moduleCallDate) {

    this(null, list, params, moduleCallDate);
  }

  /**
   * Create the task.
   *
   * @param row    the feature list row to search for.
   * @param list   the feature list to search.
   * @param params search parameters.
   */
  public NistMsSearchTask(final FeatureListRow row, final FeatureList list,
      final ParameterSet params, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    // Initialize.
    peakList = list;
    peakListRow = row;
    progress = 0;
    progressMax = 0;

    // Parameters.
    minDotProduct = params.getParameter(DOT_PRODUCT).getValue();
    nistMsSearchDir = params.getParameter(NIST_MS_SEARCH_DIR).getValue();
    nistMsSearchExe = ((NistMsSearchParameters) params).getNistMsSearchExecutable();
    importOption = params.getParameter(IMPORT_PARAMETER).getValue();
    final SpectraMergeSelectModuleOptions value = params.getValue(
        NistMsSearchParameters.spectraMergeSelect);
    fragmentScanSelection = value.createFragmentScanSelection(getMemoryMapStorage(),
        value.getModuleParameters());

    // Optional parameters.
    if (params.getParameter(INTEGER_MZ).getValue()) {
      integerMZ = params.getParameter(INTEGER_MZ).getEmbeddedParameter().getValue();
    } else {
      integerMZ = null;
    }

    this.parameterSet = params;
  }

  /**
   * Writes the secondary locator file.
   *
   * @param locatorFile the locator file.
   * @param spectraFile the spectra file.
   * @throws IOException if an i/o problem occurs.
   */
  private static void writeSecondaryLocatorFile(final File locatorFile, final File spectraFile)
      throws IOException {
    // Write the spectra file name to the secondary locator file.
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(locatorFile))) {
      writer.write(spectraFile.getCanonicalPath() + " " + importOption.toString());
      writer.newLine();
    }
  }

  @Override
  public String getTaskDescription() {

    return "Running NIST MS Search for " + peakList;
  }

  @Override
  public double getFinishedPercentage() {

    return progressMax == 0 ? 0.0 : (double) progress / (double) progressMax;
  }

  @Override
  public void run() {

    try {

      // Run the search.
      nistSearch();

      if (!isCanceled()) {

        // Finished.
        peakList.getAppliedMethods().add(
            new SimpleFeatureListAppliedMethod(NistMsSearchModule.class, parameterSet,
                getModuleCallDate()));
        setStatus(TaskStatus.FINISHED);
        logger.info("NIST MS Search completed");
      }

    } catch (Throwable t) {

      logger.log(Level.SEVERE, "NIST MS Search error", t);
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
    }


  }

  /**
   * Run the NIST search.
   *
   * @throws IOException if there are i/o problems.
   */
  private void nistSearch() throws IOException {

    // Waiting to get the SEMAPHORE: only one instance of NIST MS Search can
    // run at a time.
    setStatus(TaskStatus.WAITING);
    synchronized (SEMAPHORE) {

      File locatorFile2 = null;
      try {
        if (isCanceled()) {
          return;
        }

        setStatus(TaskStatus.PROCESSING);

        // Configure locator files. Resolved before the search loop so that a cancel can never
        // leave locatorFile2 null while it is still being used below.
        final File locatorFile1 = new File(nistMsSearchDir, PRIMARY_LOCATOR_FILE_NAME);
        locatorFile2 = getSecondLocatorFile(locatorFile1);

        if (locatorFile2 == null) {
          throw new IOException("Primary locator file " + locatorFile1
              + " doesn't contain the name of a valid file.");
        }

        // Is MS Search already running?
        if (locatorFile2.exists()) {
          throw new IllegalStateException(
              "NIST MS Search appears to be busy - please wait until it finishes its current task and then try again.  Alternatively, try manually deleting the file "
                  + locatorFile2);
        }

        List<FeatureListRow> rows = new ArrayList<>();

        // Searching FeatureList or FeatureListRow?
        if (peakListRow == null) {
          rows = peakList.getRows();
        } else {
          rows.add(peakListRow);
        }

        // Perform searches for each feature list row.
        progress = 0;
        progressMax = rows.size();
        for (final FeatureListRow row : rows) {

          if (!row.hasMs2Fragmentation()) {
            progress++;
            continue;
          }

          // Merge multiple MSn fragment spectra.
          final List<Scan> msMsScans = fragmentScanSelection.getAllFragmentSpectra(row);
          for (Scan scan : msMsScans) {
            if (isCanceled()) {
              return;
            }

            final DataPoint[] dataPoints = extractSearchSpectrum(scan);
            if (dataPoints == null || dataPoints.length == 0) {
              // MS Search silently refuses a spectrum without peaks: no search runs, no new
              // results file is written, and the previous row's results would be read instead.
              if (dataPoints != null) {
                emptySpectra++;
              }
              continue;
            }

            // Write spectra file.
            final File spectraFile = writeSpectraFile(row, dataPoints, scan.getScanDefinition());

            // Run the search. This clears the previous results before handing MS Search the
            // locator file, so the results we then read can only be from this search.
            runNistMsSearch(locatorFile2, spectraFile, dataPoints.length);

            // Read the search results file and store the results.
            List<SpectralDBAnnotation> identities = readSearchResults(row, scan);

            if (identities != null && !identities.isEmpty()) {
              addIdentities(row, identities);
              SortSpectralMatchesTask.sortIdentities(row);
            }
          }
          progress++;
        }

        logSkippedSpectra();
      } finally {

        // Clean up.
        if (locatorFile2 != null) {
          locatorFile2.delete();
        }
      }
    }
  }

  /**
   * Extracts the search spectrum of a scan, applying the optional unit-mass rounding.
   *
   * @param scan the scan.
   * @return the data points, an empty array if the mass list is empty, or null if the scan has no
   * mass list at all.
   */
  private @Nullable DataPoint[] extractSearchSpectrum(final Scan scan) {

    final DataPoint[] dataPoints;
    try {
      dataPoints = ScanUtils.extractDataPoints(scan, true);
    } catch (MissingMassListException e) {
      missingMassLists++;
      return null;
    }

    // Round high-res to low-res.
    return integerMZ != null ? ScanUtils.integerDataPoints(dataPoints, integerMZ) : dataPoints;
  }

  /**
   * Reports the spectra that were not searched, once, rather than failing the whole task.
   */
  private void logSkippedSpectra() {

    if (missingMassLists > 0) {
      logger.warning(() -> missingMassLists
          + " spectra were skipped because they have no mass list - run mass detection first.");
    }
    if (emptySpectra > 0) {
      logger.warning(() -> emptySpectra
          + " spectra were skipped because their mass list is empty - check the noise level of your mass detection.");
    }
    if (unmatchedRows > 0) {
      logger.warning(() -> "NIST MS Search returned no results block for " + unmatchedRows
          + " searched spectra.");
    }
  }

  /**
   * Reads the search results file for a given feature list row.
   * <p>
   * The results file holds one block per spectrum in the MS Search spec list, so blocks belonging
   * to other rows are expected - with {@link ImportOption#APPEND} every previously searched
   * spectrum is reported again - and are skipped rather than treated as an error.
   *
   * @param row the row.
   * @return the identities for this row, or null if the file holds no block for it.
   * @throws IOException if and i/o problem occurs.
   */
  private List<SpectralDBAnnotation> readSearchResults(final FeatureListRow row,
      @NotNull final Scan queryScan) throws IOException {

    final File resultsFile = new File(nistMsSearchDir, SEARCH_RESULTS_FILE_NAME);
    if (!resultsFile.exists()) {
      unmatchedRows++;
      logger.warning(
          () -> "NIST MS Search wrote no results file " + resultsFile + " for row " + row.getID());
      return null;
    }

    final int rowID = row.getID();

    // Read the results file.
    final List<String> hitLines;
    try (BufferedReader reader = new BufferedReader(new FileReader(resultsFile))) {
      hitLines = readHitLinesForRow(reader, rowID);
    }

    if (hitLines == null) {
      unmatchedRows++;
      logger.warning(() -> "No results block for row " + rowID + " in " + SEARCH_RESULTS_FILE_NAME);
      return null;
    }

    final List<SpectralDBAnnotation> ids = new ArrayList<>();
    for (final String hitLine : hitLines) {

      final Matcher cmpMatcher = CMP_REGEX.matcher(hitLine);
      if (cmpMatcher.find()) {

        final SpectralDBAnnotation match = parseHitLine(hitLine, cmpMatcher, row, queryScan);
        if (match != null) {
          ids.add(match);
        }
      }
    }

    return ids;
  }

  /**
   * Splits a results file into its {@code Unknown: Row <n>} blocks and returns the hit lines of the
   * block that belongs to the given row.
   * <p>
   * Blocks of other rows and unrecognised text are logged and skipped - never treated as an error.
   *
   * @param reader the results file reader.
   * @param rowID  the row whose block is wanted.
   * @return the hit lines of the row's block, or null if the file holds no block for it.
   * @throws IOException if an i/o problem occurs.
   */
  static @Nullable List<String> readHitLinesForRow(final BufferedReader reader, final int rowID)
      throws IOException {

    final List<String> hitLines = new ArrayList<>();
    boolean insideOwnBlock = false;
    boolean foundOwnBlock = false;

    int lineCount = 1;
    String line = reader.readLine();
    while (line != null) {

      final String currentLine = line;
      final int currentLineCount = lineCount;

      // Match the line.
      final Matcher scanMatcher = SEARCH_REGEX.matcher(line);

      // Is this the start of a result block?
      if (scanMatcher.matches()) {

        final int hitID = Integer.parseInt(scanMatcher.group(1));
        insideOwnBlock = hitID == rowID;
        foundOwnBlock |= insideOwnBlock;

        if (!insideOwnBlock) {
          logger.finest(() -> "Skipping results block of row " + hitID + " at line "
              + currentLineCount + " while searching row " + rowID);
        }
      } else if (CMP_REGEX.matcher(line).find()) {

        if (insideOwnBlock) {
          hitLines.add(currentLine);
        }
      } else if (!line.isBlank()) {
        logger.finest(() -> "Ignoring unrecognised text at line " + currentLineCount + " of "
            + SEARCH_RESULTS_FILE_NAME + ": " + StringUtils.abbreviate(currentLine, 120));
      }

      // Read the next line.
      line = reader.readLine();
      lineCount++;
    }

    return foundOwnBlock ? hitLines : null;
  }

  /**
   * Parses a single hit line into an annotation.
   *
   * @return the annotation, or null if the hit is below the dot product cut-off.
   */
  private @Nullable SpectralDBAnnotation parseHitLine(final String line, final Matcher cmpMatcher,
      final FeatureListRow row, @NotNull final Scan queryScan) {

    final Matcher mfMatcher = MF_REGEX.matcher(line);
    final Matcher rmfMatcher = RMF_REGEX.matcher(line);

    /*
      Known bug in NIST MS Search v. <= 2.5. For MS/MS-based searches, Dot Product is
      reported in RMF field. Must conditionally assign dot product based one whether
      EI or MS/MS spectrum search type. Only EI-based searches report RI.
     */
    double dotProduct;
    if (RI_REGEX.matcher(line).find()) {
      dotProduct = mfMatcher.find() ? Double.parseDouble(mfMatcher.group(1)) : Double.NaN;
    } else {
      dotProduct = rmfMatcher.find() ? Double.parseDouble(rmfMatcher.group(1)) : Double.NaN;
    }

    // NIST cosine similarity scores range between 0 and 1000. Make compatible with MZmine.
    dotProduct = dotProduct / 1000;

    // Parse compound meta data and make SprectralDBAnnotation.
    if (!(dotProduct >= minDotProduct)) {
      return null;
    }

    String name = cmpMatcher.group(1);

    final Matcher fmlMatcher = FML_REGEX.matcher(line);
    final Matcher casMatcher = CAS_REGEX.matcher(line);
    final Matcher mwMatcher = MW_REGEX.matcher(line);
    final Matcher idMatcher = ID_REGEX.matcher(line);
    final Matcher libMatcher = LIB_REGEX.matcher(line);

    String formula = "";
    String ion = "";
    String molWeight = "";
    String casNumber = "";
    String id = "";
    String lib = "";

    if (fmlMatcher.find()) {
      formula = fmlMatcher.group(1);
    }
    if (mwMatcher.find()) {
      molWeight = mwMatcher.group(1);
    }
    if (casMatcher.find()) {
      casNumber = casMatcher.group(1);
    }
    if (idMatcher.find()) {
      id = idMatcher.group(1);
    }
    if (libMatcher.find()) {
      lib = "Library: " + libMatcher.group(1) + "\n"
          + "NIST results only viewable in NIST MS Search";
    }

    // Compound ion_type is combined with name field for LC-MS/MS field.
    final Matcher ionMatcher = ION_REGEX.matcher(name);
    if (ionMatcher.find()) {
      name = StringUtils.substringBefore(name, "  [");
      ion = ionMatcher.group(1);
    }

    Map<DBEntryField, Object> map = Map.of(DBEntryField.ENTRY_ID, id, DBEntryField.NAME, name,
        DBEntryField.FORMULA, formula, DBEntryField.ION_TYPE, ion, DBEntryField.CAS, casNumber,
        DBEntryField.MOLWEIGHT, molWeight, DBEntryField.COMMENT, lib, DBEntryField.SOFTWARE,
        SEARCH_METHOD);

    // Use empty spectrum for now as NIST search does not provide the spectrum
    SpectralLibraryEntry entry = new SpectralDBEntry(null, new double[0], new double[0], map);

    SpectralSimilarity similarity = new SpectralSimilarity("Cosine Dot Product", dotProduct, 100,
        Double.NaN);

    return new SpectralDBAnnotation(entry, similarity, queryScan, null, row.getAverageMZ(),
        row.getAverageRT(), null);
  }

  /**
   * Executes the NIST MS Search for a single spectra file.
   *
   * @param locatorFile the secondary locator file telling MS Search what to import.
   * @param spectraFile the spectra file to search.
   * @param numSignals  number of signals submitted, used in the timeout message.
   * @throws IOException if there are i/o problems, or if MS Search produced no results in time.
   */
  private void runNistMsSearch(final File locatorFile, final File spectraFile, final int numSignals)
      throws IOException {

    final File srcReady = new File(nistMsSearchDir, SEARCH_POLL_FILE_NAME);
    final File srcResult = new File(nistMsSearchDir, SEARCH_RESULTS_FILE_NAME);

    // Remove the previous results *before* the locator file is written. MS Search polls for the
    // locator file on its own schedule, so it may pick it up and finish before we get here -
    // deleting the polling file afterwards would destroy the very signal we then wait for.
    // Once both files are gone, their reappearance can only be the result of this search.
    deleteResultFile(srcReady);
    deleteResultFile(srcResult);

    // Tell MS Search which file to import and search.
    writeSecondaryLocatorFile(locatorFile, spectraFile);

    // Execute NIST MS Search. Passed as an argument list so that an installation directory
    // containing spaces is not split into separate arguments.
    final List<String> command = new ArrayList<>();
    command.add(nistMsSearchExe.getAbsolutePath());
    command.addAll(COMMAND_LINE_ARGS);
    logger.finest(() -> "Executing " + String.join(" ", command));
    new ProcessBuilder(command).directory(nistMsSearchDir).start();

    // Wait for the search to finish by polling the results file.
    final long deadline = System.currentTimeMillis() + SEARCH_TIMEOUT_MS;
    while (!srcReady.exists() && !isCanceled()) {

      if (System.currentTimeMillis() > deadline) {
        throw new IOException("NIST MS Search produced no results within %d s. Check that the "
            .formatted(SEARCH_TIMEOUT_MS / 1000)
            + "\"Automation\" check box is enabled (Options -> Library search options -> Other "
            + "options -> Automation), that MS Search is not waiting on a dialog, and that the "
            + "submitted spectrum is valid (%d signals were submitted).".formatted(numSignals));
      }

      try {
        Thread.sleep(POLL_RESULTS);
      } catch (InterruptedException ignore) {
        // uninterruptible.
      }
    }
  }

  /**
   * Deletes one of the MS Search result files, failing with an actionable message.
   */
  private static void deleteResultFile(final File file) throws IOException {

    if (file.exists() && !file.delete()) {
      throw new IOException("Couldn't delete the previous search results file " + file
          + ".  Close NIST MS Search and delete it manually.");
    }
  }

  /**
   * Writes a search spectrum file for the given row and data points.
   *
   * @param peakRow   the row.
   * @param dataPoint the chosen spectral results.
   * @param comment   details of scan or merging stats.
   * @return the file.
   * @throws IOException if an i/o problem occurs.
   */
  private File writeSpectraFile(final FeatureListRow peakRow, @NotNull final DataPoint[] dataPoint,
      final String comment) throws IOException {

    final File spectraFile = FileAndPathUtil.createTempFile(SPECTRA_FILE_PREFIX,
        SPECTRA_FILE_SUFFIX);
    spectraFile.deleteOnExit();
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(spectraFile))) {
      logger.finest("Writing spectra to file " + spectraFile);

      // Write header.
      final FeatureIdentity identity = peakRow.getPreferredFeatureIdentity();
      final String name =
          SPECTRUM_NAME_PREFIX + peakRow.getID() + (identity == null ? "" : " (" + identity + ')')
              + " of " + peakList.getName();
      writer.write("Name: " + name.substring(0, Math.min(SPECTRUM_NAME_MAX_LENGTH, name.length())));
      writer.newLine();
      writer.write("PrecursorMZ: " + peakRow.getAverageMZ());
      writer.newLine();
      writer.write("Comments: " + comment);
      writer.newLine();

      // Write clustered spectra or MSn spectra.
      writer.write("Num Peaks: " + dataPoint.length);
      writer.newLine();

      for (final DataPoint dp : dataPoint) {

        writer.write(dp.getMZ() + "\t" + dp.getIntensity());
        writer.newLine();
      }
    }
    return spectraFile;
  }

  /**
   * Gets the second locator file by reading it's path from the primary locator file.
   *
   * @param primaryLocatorFile the primary locator file.
   * @return the secondary locator file or null if the primary locator file couldn't be read.
   * @throws IOException if there are i/o problems.
   */
  private File getSecondLocatorFile(final File primaryLocatorFile) throws IOException {

    // Check for the primary locator file.
    if (!primaryLocatorFile.exists()) {
      logger.warning("Primary locator file not found - writing new " + primaryLocatorFile);
      // Write the primary locator file.
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(primaryLocatorFile))) {
        writer.write(new File(nistMsSearchDir, SECONDARY_LOCATOR_FILE_NAME).getCanonicalPath());
        writer.newLine();
      }
    }

    // Read the secondary locator file.
    File locatorFile2 = null;
    try (BufferedReader reader = new BufferedReader(new FileReader(primaryLocatorFile))) {
      final String line = reader.readLine();
      if (line != null) {
        locatorFile2 = new File(line);
      }
    }

    return locatorFile2;
  }

  protected void addIdentities(FeatureListRow row, List<SpectralDBAnnotation> matches) {
    // add new identity to the row
    if (row != null) {
      row.addSpectralLibraryMatches(matches);
    }
  }
}


