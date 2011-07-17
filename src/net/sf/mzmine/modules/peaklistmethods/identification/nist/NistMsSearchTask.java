/*
 * Copyright 2006-2011 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.identification.nist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakIdentity;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * Performs NIST MS Search.
 *
 * @author $Author: cpudney $
 * @version $Revision: 2369 $
 */
public class NistMsSearchTask
        extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger.getLogger(NistMsSearchModule.class.getName());

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
    private static final Pattern SEARCH_REGEX = Pattern.compile("^Unknown:\\s*" + SPECTRUM_NAME_PREFIX + "(\\d+).*");
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

    // The peak-list to search.
    private final PeakList peakList;

    // Progress counters.
    private int progress;
    private int progressMax;

    // NIST MS Search home directory and command-line.
    private final String msSearchDir;
    private final String msSearchCommand;

    // Ion type parameter.
    private final IonizationType ionType;

    // Match factor cut-offs.
    private final int minMatchFactor;
    private final int minReverseMatchFactor;
    private final double rtWindow;

    /**
     * Create the task.
     *
     * @param aPeakList     the peak list to search.
     * @param searchDir     NIST MS Search directory path.
     * @param searchCommand Command-line string to execute search.
     * @param params        search parameters.
     */
    public NistMsSearchTask(final PeakList aPeakList,
                            final String searchDir,
                            final String searchCommand,
                            final ParameterSet params) {

        // Initialize.
        peakList = aPeakList;
        progress = 0;
        progressMax = 0;
        msSearchDir = searchDir;
        msSearchCommand = searchCommand;

        // Parameters.
        ionType = params.getParameter(NistMsSearchParameters.IONIZATION_METHOD).getValue();
        minMatchFactor = params.getParameter(NistMsSearchParameters.MIN_MATCH_FACTOR).getInt();
        minReverseMatchFactor = params.getParameter(NistMsSearchParameters.MIN_REVERSE_MATCH_FACTOR).getInt();
        rtWindow = params.getParameter(NistMsSearchParameters.SPECTRUM_RT_WIDTH).getDouble();
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

        File locatorFile2 = null;

        try {

            // Waiting to get the SEMAPHORE: only one instance of NIST MS Search can run at a time.
            setStatus(TaskStatus.WAITING);
            synchronized (SEMAPHORE) {

                if (!isCanceled()) {
                    setStatus(TaskStatus.PROCESSING);

                    // Configure locator files.
                    final File locatorFile1 = new File(msSearchDir, PRIMARY_LOCATOR_FILE_NAME);
                    locatorFile2 = getSecondLocatorFile(locatorFile1);
                    if (locatorFile2 == null) {

                        throw new IOException(
                                "Primary locator file " + locatorFile1 + " doesn't contain the name of a valid file.");
                    }

                    // Is MS Search already running?
                    if (locatorFile2.exists()) {

                        throw new IllegalStateException(
                                "NIST MS Search appears to be busy - please wait until it finishes its current task and then try again.  Alternatively, try manually deleting the file " +
                                locatorFile2);
                    }
                }

                // Determine the RT neighbourhoods of each row.
                final Map<PeakListRow, Set<PeakListRow>> rowHoods = groupContemporaneousRows();

                // Store search results for each neighbourhood - to avoid repeat searches.
                final int numRows = peakList.getNumberOfRows();
                final Map<Set<PeakListRow>, List<PeakIdentity>> rowIdentities =
                        new HashMap<Set<PeakListRow>, List<PeakIdentity>>(numRows);

                // Perform searches for each raw data file represented in the peak list.
                progress = 0;
                progressMax = numRows;
                for (final PeakListRow row : peakList.getRows()) {

                    // Get the row's neighbours.
                    final Set<PeakListRow> neighbours = rowHoods.get(row);

                    // Has this neighbourhood's search been run already?
                    if (!rowIdentities.containsKey(neighbours)) {

                        if (!isCanceled()) {

                            // Write spectra file.
                            final File spectraFile = writeSpectraFile(row, neighbours);

                            // Write locator file.
                            writeSecondaryLocatorFile(locatorFile2, spectraFile);

                            // Run the search.
                            runNistMsSearch(msSearchCommand);

                            // Read the search results file and store the results.
                            rowIdentities.put(neighbours, readSearchResults(row));
                        }
                    }

                    // Get the search results.
                    final List<PeakIdentity> identities = rowIdentities.get(neighbours);
                    if (identities != null) {

                        // Add (copy of) identities to peak row.
                        for (final PeakIdentity identity : identities) {

                            row.addPeakIdentity(
                                    new SimplePeakIdentity((Hashtable<String, String>) identity.getAllProperties()),
                                    false);
                        }

                        // Notify the GUI about the change in the project
                		MZmineCore.getCurrentProject().notifyObjectChanged(row, false);

                    }
                    progress++;
                }
            }

            if (!isCanceled()) {

                // Finished.
                setStatus(TaskStatus.FINISHED);
                LOG.info("NIST MS Search completed");
            }
        }
        catch (Throwable t) {

            LOG.log(Level.SEVERE, "NIST MS Search error", t);
            errorMessage = t.getMessage();
            setStatus(TaskStatus.ERROR);
        }
        finally {

            // Clean up.
            if (locatorFile2 != null) {
                locatorFile2.delete();
            }
        }
    }

    /**
     * Determines the contemporaneity between all pairs of non-identical peak rows.
     *
     * @return a map holding pairs of adjacent (non-identical) peak rows.  (x,y) <=> (y,x)
     */
    private Map<PeakListRow, Set<PeakListRow>> groupContemporaneousRows() {

        // Ordinarily you'd sort by RT for some efficiency gains *but* we can't because we could be dealing with
        // aligned peak lists, i.e. several different peaks with different RTs per row.

        // Determine contemporaneity.
        final PeakListRow[] rows = peakList.getRows();
        final int numRows = rows.length;
        final Map<PeakListRow, Set<PeakListRow>> rowHoods = new HashMap<PeakListRow, Set<PeakListRow>>(numRows);
        for (int i = 0;
             i < numRows;
             i++) {

            // Get this row's neighbours list - create it if necessary.
            final PeakListRow row1 = rows[i];
            if (!rowHoods.containsKey(row1)) {
                rowHoods.put(row1, new HashSet<PeakListRow>());
            }
            final Set<PeakListRow> neighbours = rowHoods.get(row1);
            neighbours.add(row1);

            for (int j = i + 1; j < numRows; j++) {

                // Are peak rows contemporaneous?
                final PeakListRow row2 = rows[j];
                if (isContemporaneousPair(row1, row2)) {

                    // Add rows to each others' neighbours lists.
                    neighbours.add(row2);
                    if (!rowHoods.containsKey(row2)) {

                        rowHoods.put(row2, new HashSet<PeakListRow>());
                    }
                    rowHoods.get(row2).add(row1);
                }
            }
        }
        return rowHoods;
    }

    /**
     * Determines whether two peak rows are within RT window.
     *
     * @param row1 first row to compare.
     * @param row2 second row to compare.
     * @return true if any of the rows corresponding pairs of peaks (one pair per raw data file) have RTs within the
     *         specified window, false otherwise.
     */
    private boolean isContemporaneousPair(final PeakListRow row1, final PeakListRow row2) {

        boolean adjacent = false;
        final RawDataFile[] files = row1.getRawDataFiles();
        for (int i = 0; !adjacent && i < files.length; i++) {

            // Compare peaks for the given file.
            final RawDataFile file = files[i];
            final ChromatographicPeak peak1 = row1.getPeak(file);
            final ChromatographicPeak peak2 = row2.getPeak(file);
            adjacent = peak1 != null && peak2 != null && Math.abs(peak1.getRT() - peak2.getRT()) <= rtWindow;
        }
        return adjacent;
    }

    /**
     * Reads the search results file for a given peak list row.
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
                new BufferedReader(new FileReader(new File(msSearchDir, SEARCH_RESULTS_FILE_NAME)));
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
                        throw new IllegalArgumentException("Search results are for a different peak.  Expected peak: " +
                                                           rowID + " but found: " + hitID);
                    }
                } else if (hitMatcher.matches()) {

                    if (hitList != null) {

                        // Do hit match factors exceed thresholds?
                        final String matchFactor = hitMatcher.group(3);
                        final String reverseMatchFactor = hitMatcher.group(4);
                        if (Integer.parseInt(matchFactor) >= minMatchFactor &&
                            Integer.parseInt(reverseMatchFactor) >= minReverseMatchFactor) {

                            // Extract identity from hit information.
                            final SimplePeakIdentity id = new SimplePeakIdentity(hitMatcher.group(1),
                                                                                 hitMatcher.group(2),
                                                                                 SEARCH_METHOD,
                                                                                 hitMatcher.group(7),
                                                                                 null);
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
        }
        finally {
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
        final File srcReady = new File(msSearchDir, SEARCH_POLL_FILE_NAME);
        if (srcReady.exists() && !srcReady.delete()) {
            throw new IOException(
                    "Couldn't delete the search results polling file " + srcReady + ".  Please delete it manually.");
        }

        // Execute NIS MS Search.
        LOG.finest("Executing " + command);
        Runtime.getRuntime().exec(command);

        // Wait for the search to finish by polling the results file.
        while (!srcReady.exists() && !isCanceled()) {
            try {

                Thread.sleep(POLL_RESULTS);
            }
            catch (InterruptedException ignore) {

                // uninterruptible.
            }
        }
    }

    /**
     * Writes a search spectrum file for the given row and its neighbours.
     *
     * @param peakRow       the row.
     * @param neighbourRows its neighbouring rows.
     * @return the file.
     * @throws IOException if an i/o problem occurs.
     */
    private File writeSpectraFile(final PeakListRow peakRow, final Collection<PeakListRow> neighbourRows)
            throws IOException {

        final File spectraFile = File.createTempFile(SPECTRA_FILE_PREFIX, SPECTRA_FILE_SUFFIX);
        spectraFile.deleteOnExit();
        final BufferedWriter writer = new BufferedWriter(new FileWriter(spectraFile));
        try {
            LOG.finest("Writing spectra to file " + spectraFile);

            // Write header.
            final String name = SPECTRUM_NAME_PREFIX + peakRow.getID() + " of " + peakList.getName();
            writer.write("Name: " + name.substring(0, Math.min(SPECTRUM_NAME_MAX_LENGTH, name.length())));
            writer.newLine();
            writer.write("Num Peaks: " + neighbourRows.size());
            writer.newLine();

            for (final PeakListRow row : neighbourRows) {
                final ChromatographicPeak peak = row.getBestPeak();
                final int charge = peak.getCharge();
                final double mass = (peak.getMZ() - ionType.getAddedMass()) *
                                    (charge == 0 ? 1.0 : (double) charge);
                writer.write(mass + "\t" + peak.getHeight());
                writer.newLine();
            }
        }
        finally {

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
    private static void writeSecondaryLocatorFile(final File locatorFile, final File spectraFile) throws IOException {

        // Write the spectra file name to the secondary locator file.
        final BufferedWriter writer = new BufferedWriter(new FileWriter(locatorFile));
        try {

            writer.write(spectraFile.getCanonicalPath() + " Append");
            writer.newLine();
        }
        finally {

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
            LOG.warning("Primary locator file not found - writing new " + primaryLocatorFile);

            // Write the primary locator file.
            final BufferedWriter writer = new BufferedWriter(new FileWriter(primaryLocatorFile));
            try {
                writer.write(new File(msSearchDir, SECONDARY_LOCATOR_FILE_NAME).getCanonicalPath());
                writer.newLine();
            }
            finally {
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
        }
        finally {
            reader.close();
        }

        return locatorFile2;
    }
}