/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_nist;

import io.github.mzmine.datamodel.DataPoint;
import static io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchParameters.MIN_MATCH_FACTOR;
import static io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchParameters.MIN_REVERSE_MATCH_FACTOR;
import static io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchParameters.NIST_MS_SEARCH_DIR;
import static io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchParameters.MS_LEVEL;
import static io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchParameters.MASS_LIST;
import static io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchParameters.MERGE_PARAMETER;
import static io.github.mzmine.modules.dataprocessing.id_nist.NistMsSearchParameters.INTEGER_MZ;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PeakIdentity;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimplePeakIdentity;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.msmsspectramerge.MergedSpectrum;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeModule;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.Hashtable;

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
  private static final Pattern SEARCH_REGEX =
      Pattern.compile("^Unknown:\\s*" + SPECTRUM_NAME_PREFIX + "(\\d+).*");
  private static final Pattern HIT_REGEX = Pattern.compile(
      "^Hit.*<<(.*)>>.*<<(.*)>>.*MF:\\s*(\\d+).*RMF:\\s*(\\d+).*CAS:\\s*([^;]*);.*Mw:\\s*(\\d+).*Id:\\s*(\\d+).*");

  // Used to ensure that MS Search operations are synchronized.
  private static final Object SEMAPHORE = new Object();

  // Polling period for the search results file.
  private static final long POLL_RESULTS = 1000L;

  // Additional peak identity properties.
  private static final String MATCH_FACTOR_PROPERTY = "Match factor";
  private static final String REVERSE_MATCH_FACTOR_PROPERTY = "Reverse match factor";
  private static final String CAS_PROPERTY = "CAS number";
  private static final String MOLECULAR_WEIGHT_PROPERTY = "Molecular weight";

  // The mass-list and peak-list.
  private final PeakList peakList;
  private final String massList;

  // The feature list row to search for (null => all).
  private final PeakListRow peakListRow;

  // Progress counters.
  private int progress;
  private int progressMax;

  // Match factor cut-offs.
  private final int minMatchFactor;
  private final int minReverseMatchFactor;

  // MS Level.
  private final int msLevel;

  // Optional params.
  private final MsMsSpectraMergeParameters mergeParameters;
  private final String integerMZ;

  // NIST MS Search directory and executable.
  private final File nistMsSearchDir;
  private final File nistMsSearchExe;

  /**
   * Create the task.
   *
   * @param list   the feature list to search.
   * @param params search parameters.
   */
  public NistMsSearchTask(final PeakList list, final ParameterSet params) {

    this(null, list, params);
  }

  /**
   * Create the task.
   *
   * @param row    the feature list row to search for.
   * @param list   the feature list to search.
   * @param params search parameters.
   */
  public NistMsSearchTask(final PeakListRow row, final PeakList list, final ParameterSet params) {

    // Initialize.
    peakList = list;
    peakListRow = row;
    progress = 0;
    progressMax = 0;

    // Parameters.
    minMatchFactor = params.getParameter(MIN_MATCH_FACTOR).getValue();
    minReverseMatchFactor = params.getParameter(MIN_REVERSE_MATCH_FACTOR).getValue();
    msLevel = params.getParameter(MS_LEVEL).getValue();
    nistMsSearchDir = params.getParameter(NIST_MS_SEARCH_DIR).getValue();
    nistMsSearchExe = ((NistMsSearchParameters) params).getNistMsSearchExecutable();
    massList = params.getParameter(MASS_LIST).getValue();

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

        List<PeakListRow> peakRow = new ArrayList();

        // Searching FeatureList or FeatureListRow?
        if (peakListRow == null) {
          peakRow = peakList.getRows();
        } else {
          peakRow.add(peakListRow);
        }

        // Perform searches for each feature list row.
        progress = 0;
        progressMax = peakList.getNumberOfRows();
        for (final PeakListRow row : peakRow) {

          DataPoint[] dataPoints = null;
          String comment = null;

          // Get MS level data points.
          if (msLevel == 1) {

            // Clustered Spectra.
            IsotopePattern ip = row.getBestIsotopePattern();
            if (ip != null) {
              dataPoints = ip.getDataPoints();
              comment = "Clustered spectra at RT= " + row.getAverageRT();
            }
          } else {

            // Merge multiple MSn fragment spectra.
            if (mergeParameters != null) {
              MsMsSpectraMergeModule merger =
                  MZmineCore.getModuleInstance(MsMsSpectraMergeModule.class);
              MergedSpectrum spectrum =
                  merger.getBestMergedSpectrum(mergeParameters, row, massList);
              if (spectrum != null) {
                dataPoints = spectrum.data;
                comment = "MERGED_STATS= " + spectrum.getMergeStatsDescription();
              }
            } else {

              // Get best fragment scan.
              Scan scan = row.getBestFragmentation();
              dataPoints = scan.getDataPoints();
              comment =
                  "DATA_FILE = " + scan.getDataFile().getName() + " SCAN = " + scan.getScanNumber();
            }
          }

          // Round high-res to low-res.
          if (integerMZ != null & dataPoints != null) {
            dataPoints = integerDataPoints(dataPoints, integerMZ);
          }

          if (!isCanceled()) {

            // Write spectra file.
            final File spectraFile = writeSpectraFile(row, dataPoints, comment);

            // Write locator file.
            writeSecondaryLocatorFile(locatorFile2, spectraFile);

            // Run the search.
            runNistMsSearch(command);

            // Read the search results file and store the results.
            List<PeakIdentity> identities = readSearchResults(row);

            if (identities != null) {

              // Add (copy of) identities to peak row.
              int maxMatchFactor = -1;

              for (final PeakIdentity identity : identities) {
                // Copy the identity.
                final PeakIdentity id =
                    new SimplePeakIdentity((Hashtable<String, String>) identity.getAllProperties());

                // Best match factor?
                final boolean isPreferred;
                final int matchFactor =
                    Integer.parseInt(id.getPropertyValue(MATCH_FACTOR_PROPERTY));
                if (matchFactor > maxMatchFactor) {

                  maxMatchFactor = matchFactor;
                  isPreferred = true;

                } else {

                  isPreferred = false;
                }

                // Add peak identity.
                row.addPeakIdentity(id, isPreferred);
              }
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
  private List<PeakIdentity> readSearchResults(final PeakListRow row) throws IOException {

    // Search results.
    List<PeakIdentity> hitList = null;

    // Read the results file.
    final BufferedReader reader =
        new BufferedReader(new FileReader(new File(nistMsSearchDir, SEARCH_RESULTS_FILE_NAME)));
    try {

      // Read results.
      int lineCount = 1;
      String line = reader.readLine();
      while (line != null) {

        // Match the line.
        final Matcher scanMatcher = SEARCH_REGEX.matcher(line);
        final Matcher hitMatcher = HIT_REGEX.matcher(line);

        // Is this the start of a result block?
        if (scanMatcher.matches()) {

          // Is the row ID correct?
          final int rowID = row.getID();
          final int hitID = Integer.parseInt(scanMatcher.group(1));
          if (rowID == hitID) {

            // Create a new list for the hits.
            hitList = new ArrayList<PeakIdentity>(1);

          } else {

            // Search results are for the wrong peak.
            throw new IllegalArgumentException(
                "Search results are for a different peak.  Expected peak: " + rowID + " but found: "
                    + hitID);
          }
        } else if (hitMatcher.matches()) {

          if (hitList != null) {

            // Do hit match factors exceed thresholds?
            final String matchFactor = hitMatcher.group(3);
            final String reverseMatchFactor = hitMatcher.group(4);
            if (Integer.parseInt(matchFactor) >= minMatchFactor
                && Integer.parseInt(reverseMatchFactor) >= minReverseMatchFactor) {

              // Extract identity from hit information.
              final SimplePeakIdentity id = new SimplePeakIdentity(hitMatcher.group(1),
                  hitMatcher.group(2), SEARCH_METHOD, hitMatcher.group(7), null);
              id.setPropertyValue(MATCH_FACTOR_PROPERTY, matchFactor);
              id.setPropertyValue(REVERSE_MATCH_FACTOR_PROPERTY, reverseMatchFactor);
              id.setPropertyValue(CAS_PROPERTY, hitMatcher.group(5));
              id.setPropertyValue(MOLECULAR_WEIGHT_PROPERTY, hitMatcher.group(6));
              hitList.add(id);
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

    return hitList;
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
  private File writeSpectraFile(final PeakListRow peakRow, final DataPoint[] dataPoint,
      final String comment) throws IOException {

    final File spectraFile = File.createTempFile(SPECTRA_FILE_PREFIX, SPECTRA_FILE_SUFFIX);
    spectraFile.deleteOnExit();
    final BufferedWriter writer = new BufferedWriter(new FileWriter(spectraFile));
    try {
      logger.finest("Writing spectra to file " + spectraFile);

      // Write header.
      final PeakIdentity identity = peakRow.getPreferredPeakIdentity();
      final String name = SPECTRUM_NAME_PREFIX + peakRow.getID()
          + (identity == null ? "" : " (" + identity + ')') + " of " + peakList.getName();
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

      writer.write(spectraFile.getCanonicalPath() + " Append");
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

  /**
   * Converts DataPoint ions mz to int.
   *
   * Function adapted from module: adap.mspexport.
   *
   * @param dataPoints spectra to convert.
   * @param mode       conversion method.
   * @return DataPoint array converted to integers.
   * @throws IOException if there are i/o problems.
   */
  private DataPoint[] integerDataPoints(final DataPoint[] dataPoints, final String mode) {

    int size = dataPoints.length;

    Map<Double, Double> integerDataPoints = new HashMap<>();

    for (int i = 0; i < size; ++i) {
      double mz = (double) Math.round(dataPoints[i].getMZ());
      double intensity = dataPoints[i].getIntensity();
      Double prevIntensity = integerDataPoints.get(mz);
      if (prevIntensity == null) {
        prevIntensity = 0.0;
      }

      if ("Merging mode: Sum".equals(mode)) {
        integerDataPoints.put(mz, prevIntensity + intensity);
      } else {
        integerDataPoints.put(mz, Math.max(prevIntensity, intensity));
      }
    }

    DataPoint[] result = new DataPoint[integerDataPoints.size()];
    int count = 0;
    for (Entry<Double, Double> e : integerDataPoints.entrySet()) {
      result[count++] = new SimpleDataPoint(e.getKey(), e.getValue());
    }

    return result;
  }
}
