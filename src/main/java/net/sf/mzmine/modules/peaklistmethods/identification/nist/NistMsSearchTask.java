/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

import static net.sf.mzmine.modules.peaklistmethods.identification.nist.NistMsSearchParameters.IONIZATION_METHOD;
import static net.sf.mzmine.modules.peaklistmethods.identification.nist.NistMsSearchParameters.MAX_NUM_PEAKS;
import static net.sf.mzmine.modules.peaklistmethods.identification.nist.NistMsSearchParameters.MIN_MATCH_FACTOR;
import static net.sf.mzmine.modules.peaklistmethods.identification.nist.NistMsSearchParameters.MIN_REVERSE_MATCH_FACTOR;
import static net.sf.mzmine.modules.peaklistmethods.identification.nist.NistMsSearchParameters.NIST_MS_SEARCH_DIR;
import static net.sf.mzmine.modules.peaklistmethods.identification.nist.NistMsSearchParameters.SAME_IDENTITIES;
import static net.sf.mzmine.modules.peaklistmethods.identification.nist.NistMsSearchParameters.SPECTRUM_RT_WIDTH;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * Performs NIST MS Search.
 *
 * @author $Author$
 * @version $Revision$
 */
public class NistMsSearchTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger.getLogger(NistMsSearchModule.class
	    .getName());

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
    private static final Pattern SEARCH_REGEX = Pattern.compile("^Unknown:\\s*"
	    + SPECTRUM_NAME_PREFIX + "(\\d+).*");
    private static final Pattern HIT_REGEX = Pattern
	    .compile("^Hit.*<<(.*)>>.*<<(.*)>>.*MF:\\s*(\\d+).*RMF:\\s*(\\d+).*CAS:\\s*([^;]*);.*Mw:\\s*(\\d+).*Id:\\s*(\\d+).*");

    // Used to ensure that MS Search operations are synchronized.
    private static final Object SEMAPHORE = new Object();

    // Polling period for the search results file.
    private static final long POLL_RESULTS = 1000L;

    // Additional peak identity properties.
    private static final String MATCH_FACTOR_PROPERTY = "Match factor";
    private static final String REVERSE_MATCH_FACTOR_PROPERTY = "Reverse match factor";
    private static final String CAS_PROPERTY = "CAS number";
    private static final String MOLECULAR_WEIGHT_PROPERTY = "Molecular weight";

    // Initial neighbourhood size.
    private static final int INITIAL_NEIGHBOURHOOD_SIZE = 4;

    // The peak-list.
    private final PeakList peakList;

    // The peak list row to search for (null => all).
    private final PeakListRow peakListRow;

    // Progress counters.
    private int progress;
    private int progressMax;

    // Ion type parameter.
    private final IonizationType ionType;

    // Match factor cut-offs.
    private final int minMatchFactor;
    private final int minReverseMatchFactor;

    // Peak matching parameters.
    private final int maxPeaks;
    private final RTTolerance rtTolerance;
    private final Boolean sameIds;

    // NIST MS Search directory and executable.
    private final File nistMsSearchDir;
    private final File nistMsSearchExe;

    /**
     * Create the task.
     *
     * @param list
     *            the peak list to search.
     * @param params
     *            search parameters.
     */
    public NistMsSearchTask(final PeakList list, final ParameterSet params) {

	this(null, list, params);
    }

    /**
     * Create the task.
     *
     * @param row
     *            the peak list row to search for.
     * @param list
     *            the peak list to search.
     * @param params
     *            search parameters.
     */
    public NistMsSearchTask(final PeakListRow row, final PeakList list,
	    final ParameterSet params) {

	// Initialize.
	peakList = list;
	peakListRow = row;
	progress = 0;
	progressMax = 0;

	// Parameters.
	ionType = params.getParameter(IONIZATION_METHOD).getValue();
	minMatchFactor = params.getParameter(MIN_MATCH_FACTOR).getValue();
	minReverseMatchFactor = params.getParameter(MIN_REVERSE_MATCH_FACTOR)
		.getValue();
	rtTolerance = params.getParameter(SPECTRUM_RT_WIDTH).getValue();
	maxPeaks = params.getParameter(MAX_NUM_PEAKS).getValue();
	sameIds = params.getParameter(SAME_IDENTITIES).getValue();
	nistMsSearchDir = params.getParameter(NIST_MS_SEARCH_DIR).getValue();
	nistMsSearchExe = ((NistMsSearchParameters) params)
		.getNistMsSearchExecutable();
    }

    @Override
    public String getTaskDescription() {

	return "Running NIST MS Search for " + peakList;
    }

    @Override
    public double getFinishedPercentage() {

	return progressMax == 0 ? 0.0 : (double) progress
		/ (double) progressMax;
    }

    @Override
    public void run() {

	try {

	    // Run the search.
	    nistSearch();

	    if (!isCanceled()) {

		// Finished.
		setStatus(TaskStatus.FINISHED);
		LOG.info("NIST MS Search completed");
	    }

            // Repaint the window to reflect the change in the peak list
            Desktop desktop = MZmineCore.getDesktop();
            if (!(desktop instanceof HeadLessDesktop))
                desktop.getMainWindow().repaint();
            
	} catch (Throwable t) {

	    LOG.log(Level.SEVERE, "NIST MS Search error", t);
	    setErrorMessage(t.getMessage());
	    setStatus(TaskStatus.ERROR);
	}
    }

    /**
     * Run the NIST search.
     *
     * @throws IOException
     *             if there are i/o problems.
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
		    final File locatorFile1 = new File(nistMsSearchDir,
			    PRIMARY_LOCATOR_FILE_NAME);
		    locatorFile2 = getSecondLocatorFile(locatorFile1);
		    if (locatorFile2 == null) {

			throw new IOException("Primary locator file "
				+ locatorFile1
				+ " doesn't contain the name of a valid file.");
		    }

		    // Is MS Search already running?
		    if (locatorFile2.exists()) {

			throw new IllegalStateException(
				"NIST MS Search appears to be busy - please wait until it finishes its current task and then try again.  Alternatively, try manually deleting the file "
					+ locatorFile2);
		    }
		}

		// Single or multiple row search?
		final PeakListRow[] peakListRows;
		final Map<PeakListRow, Set<PeakListRow>> rowHoods;
		if (peakListRow == null) {

		    peakListRows = peakList.getRows();
		    rowHoods = groupPeakRows();

		} else {

		    peakListRows = new PeakListRow[] { peakListRow };
		    rowHoods = new HashMap<PeakListRow, Set<PeakListRow>>(1);
		    rowHoods.put(peakListRow, findPeakRowGroup());
		}

		// Reduce neighbourhoods to maximum number of peaks.
		trimNeighbours(rowHoods);

		// Store search results for each neighbourhood - to avoid repeat
		// searches.
		final int numRows = peakListRows.length;
		final Map<Set<PeakListRow>, List<PeakIdentity>> rowIdentities = new HashMap<Set<PeakListRow>, List<PeakIdentity>>(
			numRows);

		// Search command string.
		final String command = nistMsSearchExe.getAbsolutePath() + ' '
			+ COMMAND_LINE_ARGS;

		// Perform searches for each peak list row..
		progress = 0;
		progressMax = numRows;
		for (final PeakListRow row : peakListRows) {

		    // Get the row's neighbours.
		    final Set<PeakListRow> neighbours = rowHoods.get(row);

		    // Has this neighbourhood's search been run already?
		    if (!rowIdentities.containsKey(neighbours)) {

			if (!isCanceled()) {

			    // Write spectra file.
			    final File spectraFile = writeSpectraFile(row,
				    neighbours);

			    // Write locator file.
			    writeSecondaryLocatorFile(locatorFile2, spectraFile);

			    // Run the search.
			    runNistMsSearch(command);

			    // Read the search results file and store the
			    // results.
			    rowIdentities.put(neighbours,
				    readSearchResults(row));
			}
		    }

		    // Get the search results.
		    final List<PeakIdentity> identities = rowIdentities
			    .get(neighbours);
		    if (identities != null) {

			// Add (copy of) identities to peak row.
			int maxMatchFactor = -1;
			for (final PeakIdentity identity : identities) {

			    // Copy the identity.
			    final PeakIdentity id = new SimplePeakIdentity(
				    (Hashtable<String, String>) identity
					    .getAllProperties());

			    // Best match factor?
			    final boolean isPreferred;
			    final int matchFactor = Integer.parseInt(id
				    .getPropertyValue(MATCH_FACTOR_PROPERTY));
			    if (matchFactor > maxMatchFactor) {

				maxMatchFactor = matchFactor;
				isPreferred = true;

			    } else {

				isPreferred = false;
			    }

			    // Add peak identity.
			    row.addPeakIdentity(id, isPreferred);
			}

			// Notify the GUI about the change in the project
			MZmineCore.getProjectManager().getCurrentProject()
				.notifyObjectChanged(row, false);
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
     * Trims the row neighbourhoods to the specified size.
     *
     * @param neighbourhoods
     *            map from each peak list row to its neighbours.
     */
    private void trimNeighbours(
	    final Map<PeakListRow, Set<PeakListRow>> neighbourhoods) {

	if (maxPeaks > 0) {

	    // Process each row's neighbour list.
	    for (final Entry<PeakListRow, Set<PeakListRow>> entry : neighbourhoods
		    .entrySet()) {

		final PeakListRow row = entry.getKey();
		final Set<PeakListRow> neighbours = entry.getValue();

		// Need trimming?
		if (neighbours.size() > maxPeaks) {

		    final List<PeakListRow> keepers = new ArrayList<PeakListRow>(
			    neighbours);

		    // Exclude current row from sorting.
		    keepers.remove(row);

		    // Sort on RT difference (ascending) then intensity
		    // (descending)
		    final double rt = row.getAverageRT();
		    Collections.sort(keepers, new Comparator<PeakListRow>() {

			@Override
			public int compare(final PeakListRow o1,
				final PeakListRow o2) {

			    // Compare on RT difference (ascending).
			    final int compare = Double.compare(
				    Math.abs(rt - o1.getAverageRT()),
				    Math.abs(rt - o2.getAverageRT()));

			    // Compare on intensity (descending) if equal RT
			    // difference.
			    return compare == 0 ? Double.compare(
				    o2.getAverageHeight(),
				    o1.getAverageHeight()) : compare;
			}
		    });

		    // Add the current row and keepers up to maxPeaks.
		    neighbours.clear();
		    neighbours.add(row);
		    neighbours.addAll(keepers.subList(0, maxPeaks - 1));
		}
	    }
	}
    }

    /**
     * Determines the contemporaneity between all pairs of non-identical peak
     * rows.
     *
     * @return a map holding pairs of adjacent (non-identical) peak rows. (x,y)
     *         <=> (y,x)
     */
    private Set<PeakListRow> findPeakRowGroup() {

	// Create neighbourhood.
	final Set<PeakListRow> neighbours = new HashSet<PeakListRow>(
		INITIAL_NEIGHBOURHOOD_SIZE);

	// Contemporaneous with self.
	neighbours.add(peakListRow);

	// Find neighbours.
	final double rt = peakListRow.getAverageRT();
	for (final PeakListRow row2 : peakList.getRows()) {

	    // Are peak rows contemporaneous?
	    if (!peakListRow.equals(row2)
		    && rtTolerance
			    .checkWithinTolerance(rt, row2.getAverageRT())
		    && (!sameIds || checkSameIds(peakListRow, row2))) {

		neighbours.add(row2);
	    }
	}

	return neighbours;
    }

    /**
     * Determines the contemporaneity between all pairs of non-identical peak
     * rows.
     *
     * @return a map holding pairs of adjacent (non-identical) peak rows. (x,y)
     *         <=> (y,x)
     */
    private Map<PeakListRow, Set<PeakListRow>> groupPeakRows() {

	// Determine contemporaneity.
	final int numRows = peakList.getNumberOfRows();
	final Map<PeakListRow, Set<PeakListRow>> rowHoods = new HashMap<PeakListRow, Set<PeakListRow>>(
		numRows);
	for (int i = 0; i < numRows; i++) {

	    // Get this row's neighbours list - create it if necessary.
	    final PeakListRow row1 = peakList.getRow(i);
	    if (!rowHoods.containsKey(row1)) {

		rowHoods.put(row1, new HashSet<PeakListRow>(4));
	    }

	    // Holds neighbours.
	    final Set<PeakListRow> neighbours = rowHoods.get(row1);

	    // Contemporaneous with self.
	    neighbours.add(row1);

	    // Find contemporaneous peaks.
	    final double rt = row1.getAverageRT();
	    for (int j = i + 1; j < numRows; j++) {

		// Are peak rows contemporaneous?
		final PeakListRow row2 = peakList.getRow(j);
		if (rtTolerance.checkWithinTolerance(rt, row2.getAverageRT())
			&& (!sameIds || checkSameIds(row1, row2))) {

		    // Add rows to each others' neighbours lists.
		    neighbours.add(row2);
		    if (!rowHoods.containsKey(row2)) {

			rowHoods.put(row2, new HashSet<PeakListRow>(4));
		    }
		    rowHoods.get(row2).add(row1);
		}
	    }
	}
	return rowHoods;
    }

    /**
     * Check whether peak row identities are the same.
     *
     * @param row1
     *            first row to compare.
     * @param row2
     *            second row to compare.
     * @return true if the rows have identities with the same name or both have
     *         no identity.
     */
    private static boolean checkSameIds(final PeakListRow row1,
	    final PeakListRow row2) {

	final PeakIdentity id1 = row1.getPreferredPeakIdentity();
	final PeakIdentity id2 = row2.getPreferredPeakIdentity();
	return id1 == id2 // Use == rather than .equals() to handle nulls.
		|| id1 != null
		&& id2 != null
		&& id1.getName().equalsIgnoreCase(id2.getName());
    }

    /**
     * Reads the search results file for a given peak list row.
     *
     * @param row
     *            the row.
     * @return a list of identities corresponding to the search results, or null
     *         if none is found.
     * @throws IOException
     *             if and i/o problem occurs.
     */
    private List<PeakIdentity> readSearchResults(final PeakListRow row)
	    throws IOException {

	// Search results.
	List<PeakIdentity> hitList = null;

	// Read the results file.
	final BufferedReader reader = new BufferedReader(new FileReader(
		new File(nistMsSearchDir, SEARCH_RESULTS_FILE_NAME)));
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
				"Search results are for a different peak.  Expected peak: "
					+ rowID + " but found: " + hitID);
		    }
		} else if (hitMatcher.matches()) {

		    if (hitList != null) {

			// Do hit match factors exceed thresholds?
			final String matchFactor = hitMatcher.group(3);
			final String reverseMatchFactor = hitMatcher.group(4);
			if (Integer.parseInt(matchFactor) >= minMatchFactor
				&& Integer.parseInt(reverseMatchFactor) >= minReverseMatchFactor) {

			    // Extract identity from hit information.
			    final SimplePeakIdentity id = new SimplePeakIdentity(
				    hitMatcher.group(1), hitMatcher.group(2),
				    SEARCH_METHOD, hitMatcher.group(7), null);
			    id.setPropertyValue(MATCH_FACTOR_PROPERTY,
				    matchFactor);
			    id.setPropertyValue(REVERSE_MATCH_FACTOR_PROPERTY,
				    reverseMatchFactor);
			    id.setPropertyValue(CAS_PROPERTY,
				    hitMatcher.group(5));
			    id.setPropertyValue(MOLECULAR_WEIGHT_PROPERTY,
				    hitMatcher.group(6));
			    hitList.add(id);
			}
		    } else {

			throw new IOException(
				"Didn't find start of results block before listing hits at line "
					+ lineCount);
		    }
		} else {
		    throw new IOException(
			    "Unrecognised results file text at line "
				    + lineCount);
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
     * @param command
     *            the search command-line string.
     * @throws IOException
     *             if there are i/o problems.
     */
    private void runNistMsSearch(final String command) throws IOException {

	// Remove the results polling file.
	final File srcReady = new File(nistMsSearchDir, SEARCH_POLL_FILE_NAME);
	if (srcReady.exists() && !srcReady.delete()) {
	    throw new IOException(
		    "Couldn't delete the search results polling file "
			    + srcReady + ".  Please delete it manually.");
	}

	// Execute NIS MS Search.
	LOG.finest("Executing " + command);
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
     * Writes a search spectrum file for the given row and its neighbours.
     *
     * @param peakRow
     *            the row.
     * @param neighbourRows
     *            its neighbouring rows.
     * @return the file.
     * @throws IOException
     *             if an i/o problem occurs.
     */
    private File writeSpectraFile(final PeakListRow peakRow,
	    final Collection<PeakListRow> neighbourRows) throws IOException {

	final File spectraFile = File.createTempFile(SPECTRA_FILE_PREFIX,
		SPECTRA_FILE_SUFFIX);
	spectraFile.deleteOnExit();
	final BufferedWriter writer = new BufferedWriter(new FileWriter(
		spectraFile));
	try {
	    LOG.finest("Writing spectra to file " + spectraFile);

	    // Write header.
	    final PeakIdentity identity = peakRow.getPreferredPeakIdentity();
	    final String name = SPECTRUM_NAME_PREFIX + peakRow.getID()
		    + (identity == null ? "" : " (" + identity + ')') + " of "
		    + peakList.getName();
	    writer.write("Name: "
		    + name.substring(0,
			    Math.min(SPECTRUM_NAME_MAX_LENGTH, name.length())));
	    writer.newLine();
	    writer.write("Num Peaks: " + neighbourRows.size());
	    writer.newLine();

	    for (final PeakListRow row : neighbourRows) {
		final Feature peak = row.getBestPeak();
		final int charge = peak.getCharge();
		final double mass = (peak.getMZ() - ionType.getAddedMass())
			* (charge == 0 ? 1.0 : (double) charge);
		writer.write(mass + "\t" + peak.getHeight());
		writer.newLine();
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
     * @param locatorFile
     *            the locator file.
     * @param spectraFile
     *            the spectra file.
     * @throws IOException
     *             if an i/o problem occurs.
     */
    private static void writeSecondaryLocatorFile(final File locatorFile,
	    final File spectraFile) throws IOException {

	// Write the spectra file name to the secondary locator file.
	final BufferedWriter writer = new BufferedWriter(new FileWriter(
		locatorFile));
	try {

	    writer.write(spectraFile.getCanonicalPath() + " Append");
	    writer.newLine();
	} finally {

	    writer.close();
	}
    }

    /**
     * Gets the second locator file by reading it's path from the primary
     * locator file.
     *
     * @param primaryLocatorFile
     *            the primary locator file.
     * @return the secondary locator file or null if the primary locator file
     *         couldn't be read.
     * @throws IOException
     *             if there are i/o problems.
     */
    private File getSecondLocatorFile(final File primaryLocatorFile)
	    throws IOException {

	// Check for the primary locator file.
	if (!primaryLocatorFile.exists()) {
	    LOG.warning("Primary locator file not found - writing new "
		    + primaryLocatorFile);

	    // Write the primary locator file.
	    final BufferedWriter writer = new BufferedWriter(new FileWriter(
		    primaryLocatorFile));
	    try {
		writer.write(new File(nistMsSearchDir,
			SECONDARY_LOCATOR_FILE_NAME).getCanonicalPath());
		writer.newLine();
	    } finally {
		writer.close();
	    }
	}

	// Read the secondary locator file.
	File locatorFile2 = null;
	final BufferedReader reader = new BufferedReader(new FileReader(
		primaryLocatorFile));
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
}