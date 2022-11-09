/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
import static io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchParameters.MERGE_PARAMETER;
import static io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchParameters.MS_LEVEL;
import static io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchParameters.NIST_MS_SEARCH_DIR;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_spectral_match_sort.SortSpectralMatchesTask;
import io.github.mzmine.modules.tools.msmsspectramerge.MergedSpectrum;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeModule;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
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

/**
 * Performs NIST MS Search.
 *
 * @author $Author$
 * @version $Revision$
 */
public class NistMsSearchTask extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(NistMsSearchModule.class.getName());

  // Command-line arguments passed to executable.
  private static final String COMMAND_LINE_ARGS = "/par=2 /instrument";

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
  private static final long POLL_RESULTS = 1000L;

  // The mass-list and peak-list.
  private final FeatureList peakList;

  // The feature list row to search for (null => all).
  private final FeatureListRow peakListRow;

  // Progress counters.
  private int progress;
  private int progressMax;

  // Dot Product cut-offs.
  private final Double minDotProduct;

  // MS Level.
  private final int msLevel;

  // Optional params.
  private final MsMsSpectraMergeParameters mergeParameters;
  private final IntegerMode integerMZ;

  // Import Options.
  private static ImportOption importOption;

  // NIST MS Search directory and executable.
  private final File nistMsSearchDir;
  private final File nistMsSearchExe;

  private final ParameterSet parameterSet;

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
    msLevel = params.getParameter(MS_LEVEL).getValue();
    nistMsSearchDir = params.getParameter(NIST_MS_SEARCH_DIR).getValue();
    nistMsSearchExe = ((NistMsSearchParameters) params).getNistMsSearchExecutable();
    importOption = params.getParameter(IMPORT_PARAMETER).getValue();

    // Optional parameters.
    if (params.getParameter(MERGE_PARAMETER).getValue()) {
      mergeParameters = params.getParameter(MERGE_PARAMETER).getEmbeddedParameters();
    } else {
      mergeParameters = null;
    }
    if (params.getParameter(INTEGER_MZ).getValue()) {
      integerMZ = params.getParameter(INTEGER_MZ).getEmbeddedParameter().getValue();
    } else {
      integerMZ = null;
    }

    this.parameterSet = params;
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
        if (!isCanceled()) {

          setStatus(TaskStatus.PROCESSING);

          // Configure locator files.
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
        }

        // Search command string.
        final String command = nistMsSearchExe.getAbsolutePath() + ' ' + COMMAND_LINE_ARGS;

        List<FeatureListRow> peakRow = new ArrayList<>();

        // Searching FeatureList or FeatureListRow?
        if (peakListRow == null) {
          peakRow = peakList.getRows();
        } else {
          peakRow.add(peakListRow);
        }

        // Perform searches for each feature list row.
        progress = 0;
        progressMax = peakList.getNumberOfRows();
        for (final FeatureListRow row : peakRow) {

          DataPoint[] dataPoints = null;
          String comment = null;

          // Get MS level data points.
          if (msLevel > 1) {
            if (!row.hasMs2Fragmentation()) {
              progress++;
              continue;
            }
            // Merge multiple MSn fragment spectra.
            if (mergeParameters != null) {
              MsMsSpectraMergeModule merger = MZmineCore.getModuleInstance(
                  MsMsSpectraMergeModule.class);
              assert merger != null;
              MergedSpectrum spectrum = merger.getBestMergedSpectrum(mergeParameters, row);
              if (spectrum != null) {
                dataPoints = spectrum.data;
                comment = "MERGED_STATS= " + spectrum.getMergeStatsDescription();
              }
            } else {

              // Get best fragment scan.
              Scan scan = row.getMostIntenseFragmentScan();
              dataPoints = ScanUtils.extractDataPoints(scan);
              comment =
                  "DATA_FILE = " + scan.getDataFile().getName() + " SCAN = " + scan.getScanNumber();
            }
          } else {

            // Clustered Spectra.
            IsotopePattern ip = row.getBestIsotopePattern();
            if (ip != null) {
              dataPoints = ScanUtils.extractDataPoints(ip);
              comment = "Clustered spectra at RT= " + row.getAverageRT();
            }
          }

          // Round high-res to low-res.
          if (integerMZ != null & dataPoints != null) {
            dataPoints = ScanUtils.integerDataPoints(dataPoints, integerMZ);
          }

          if (!isCanceled()) {

            // Write spectra file.
            final File spectraFile = writeSpectraFile(row, dataPoints, comment);

            // Write locator file.
            writeSecondaryLocatorFile(locatorFile2, spectraFile);

            // Run the search.
            runNistMsSearch(command);

            // Read the search results file and store the results.
            List<SpectralDBAnnotation> identities = readSearchResults(row);

            if (identities != null) {
              addIdentities(row, identities);
              SortSpectralMatchesTask.sortIdentities(row);
            }
          }

          progress++;
        }
      } finally {

        // Clean up.
        if (locatorFile2 != null) {
          locatorFile2.delete();
        }
      }
    }
  }

  /**
   * Reads the search results file for a given feature list row.
   *
   * @param row the row.
   * @return a list of identities corresponding to the search results, or null if none is found.
   * @throws IOException if and i/o problem occurs.
   */
  private List<SpectralDBAnnotation> readSearchResults(final FeatureListRow row)
      throws IOException {

    // Search results.
    List<SpectralDBAnnotation> ids = null;

    // Read the results file.
    final BufferedReader reader = new BufferedReader(
        new FileReader(new File(nistMsSearchDir, SEARCH_RESULTS_FILE_NAME)));
    try {

      // Read results.
      int lineCount = 1;
      String line = reader.readLine();
      while (line != null) {

        // Match the line.
        final Matcher scanMatcher = SEARCH_REGEX.matcher(line);
        final Matcher cmpMatcher = CMP_REGEX.matcher(line);

        // Is this the start of a result block?
        if (scanMatcher.matches()) {

          // Is the row ID correct?
          final int rowID = row.getID();
          final int hitID = Integer.parseInt(scanMatcher.group(1));
          if (rowID == hitID) {

            // Create a new list for the hits.
            ids = new ArrayList<>(1);

          } else {

            // Search results are for the wrong peak.
            throw new IllegalArgumentException(
                "Search results are for a different peak.  Expected peak: " + rowID + " but found: "
                    + hitID);
          }
        } else if (cmpMatcher.find()) {

          if (ids != null) {

            Matcher mfMatcher = MF_REGEX.matcher(line);
            Matcher rmfMatcher = RMF_REGEX.matcher(line);

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
            if (dotProduct >= minDotProduct) {

              String name = cmpMatcher.group(1);

              Matcher fmlMatcher = FML_REGEX.matcher(line);
              Matcher casMatcher = CAS_REGEX.matcher(line);
              Matcher mwMatcher = MW_REGEX.matcher(line);
              Matcher idMatcher = ID_REGEX.matcher(line);
              Matcher libMatcher = LIB_REGEX.matcher(line);

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
              Matcher ionMatcher = ION_REGEX.matcher(name);
              if (ionMatcher.find()) {
                name = StringUtils.substringBefore(name, "  [");
                ion = ionMatcher.group(1);
              }

              Map<DBEntryField, Object> map = Map.of(DBEntryField.ENTRY_ID, id, DBEntryField.NAME,
                  name, DBEntryField.FORMULA, formula, DBEntryField.ION_TYPE, ion, DBEntryField.CAS,
                  casNumber, DBEntryField.MOLWEIGHT, molWeight, DBEntryField.COMMENT, lib,
                  DBEntryField.SOFTWARE, SEARCH_METHOD);

              // Use empty spectrum for now as NIST search does not provide the spectrum
              SpectralLibraryEntry entry = new SpectralDBEntry(null, new double[0], new double[0],
                  map);

              SpectralSimilarity similarity = new SpectralSimilarity("Cosine Dot Product",
                  dotProduct, 100, Double.NaN);

              final SpectralDBAnnotation libraryID = new SpectralDBAnnotation(entry, similarity,
                  null, null);

              ids.add(libraryID);
            }
          } else {

            throw new IOException(
                "Didn't find start of results block before listing hits at line " + lineCount);
          }
        } else {
          throw new IOException("Unrecognised results file text at line " + lineCount);
        }

        // Read the next line.
        line = reader.readLine();
        lineCount++;
      }
    } finally {
      reader.close();
    }

    return ids;
  }

  /**
   * Executes the NIST MS Search.
   *
   * @param command the search command-line string.
   * @throws IOException if there are i/o problems.
   */
  private void runNistMsSearch(final String command) throws IOException {

    // Remove the results polling file.
    final File srcReady = new File(nistMsSearchDir, SEARCH_POLL_FILE_NAME);
    if (srcReady.exists() && !srcReady.delete()) {
      throw new IOException("Couldn't delete the search results polling file " + srcReady
          + ".  Please delete it manually.");
    }

    // Execute NIS MS Search.
    logger.finest("Executing " + command);
    Runtime.getRuntime().exec(command);

    // Wait for the search to finish by polling the results file.
    while (!srcReady.exists() && !isCanceled()) {
      try {

        Thread.sleep(POLL_RESULTS);
      } catch (InterruptedException ignore) {

        // uninterruptible.
      }
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
  private File writeSpectraFile(final FeatureListRow peakRow, final DataPoint[] dataPoint,
      final String comment) throws IOException {

    final File spectraFile = File.createTempFile(SPECTRA_FILE_PREFIX, SPECTRA_FILE_SUFFIX);
    spectraFile.deleteOnExit();
    final BufferedWriter writer = new BufferedWriter(new FileWriter(spectraFile));
    try {
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

      // Write ions.
      if (dataPoint == null) {

        // Write precursor MZ if no clustered spectra or MSn spectra.
        writer.write("Num Peaks: 1");
        writer.newLine();

        double mz = peakRow.getAverageMZ();

        // If integer, round.
        if (integerMZ != null) {
          mz = (int) Math.round(mz);
        }

        writer.write(mz + "\t" + peakRow.getAverageHeight());

      } else {

        // Write clustered spectra or MSn spectra.
        writer.write("Num Peaks: " + dataPoint.length);
        writer.newLine();

        for (final DataPoint dp : dataPoint) {

          writer.write(dp.getMZ() + "\t" + dp.getIntensity());
          writer.newLine();
        }
      }
    } finally {

      // Close the open file.
      writer.close();
    }
    return spectraFile;
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
    final BufferedWriter writer = new BufferedWriter(new FileWriter(locatorFile));
    try {

      writer.write(spectraFile.getCanonicalPath() + " " + importOption.toString());
      writer.newLine();
    } finally {

      writer.close();
    }
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
      final BufferedWriter writer = new BufferedWriter(new FileWriter(primaryLocatorFile));
      try {
        writer.write(new File(nistMsSearchDir, SECONDARY_LOCATOR_FILE_NAME).getCanonicalPath());
        writer.newLine();
      } finally {
        writer.close();
      }
    }

    // Read the secondary locator file.
    File locatorFile2 = null;
    final BufferedReader reader = new BufferedReader(new FileReader(primaryLocatorFile));
    try {
      final String line = reader.readLine();
      if (line != null) {
        locatorFile2 = new File(line);
      }
    } finally {
      reader.close();
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


