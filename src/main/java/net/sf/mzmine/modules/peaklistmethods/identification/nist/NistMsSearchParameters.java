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

import java.io.File;
import java.util.Collection;

import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

/**
 * Holds NIST MS Search parameters.
 *
 * @author $Author$
 * @version $Revision$
 */
public class NistMsSearchParameters extends SimpleParameterSet {

    /**
     * Peak lists to operate on.
     */
    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    /**
     * NIST MS Search path.
     */
    public static final DirectoryParameter NIST_MS_SEARCH_DIR = new DirectoryParameter(
	    "NIST MS Search directory",
	    "Full path of the directory containing the NIST MS Search executable (nistms$.exe)");

    /**
     * Ionization method.
     */
    public static final ComboParameter<IonizationType> IONIZATION_METHOD = new ComboParameter<IonizationType>(
	    "Ionization method",
	    "Type of ion used to calculate the neutral mass",
	    IonizationType.values());

    /**
     * Spectrum RT width.
     */
    public static final RTToleranceParameter SPECTRUM_RT_WIDTH = new RTToleranceParameter(
	    "Spectrum RT tolerance",
	    "The RT tolerance (>= 0) to use when forming search spectra; include all other"
	    	+"\ndetected peaks whose RT is within the specified tolerance of a given peak");

    /**
     * Maximum number of peaks per spectrum.
     */
    public static final IntegerParameter MAX_NUM_PEAKS = new IntegerParameter(
	    "Max. peaks per spectrum",
	    "The maximum number of peaks to include in a spectrum (0 -> unlimited)",
	    10, 0, null);

    /**
     * Must have same identities.
     */

    public static final BooleanParameter SAME_IDENTITIES = new BooleanParameter(
	    "Must have same identities",
	    "Two peaks will only be included in the same spectrum if they have the same identity (for use with CAMERA pseudo-spectra)",
	    true);

    /**
     * Match factor cut-off.
     */
    public static final IntegerParameter MIN_MATCH_FACTOR = new IntegerParameter(
	    "Min. match factor",
	    "The minimum match factor (0 .. 1000) that search hits must have",
	    800, 0, 1000);

    /**
     * Match factor cut-off.
     */
    public static final IntegerParameter MIN_REVERSE_MATCH_FACTOR = new IntegerParameter(
	    "Min. reverse match factor",
	    "The minimum reverse match factor (0 .. 1000) that search hits must have",
	    800, 0, 1000);

    // NIST MS Search executable.
    private static final String NIST_MS_SEARCH_EXE = "nistms$.exe";

    /**
     * Construct the parameter set.
     */
    public NistMsSearchParameters() {
	super(new Parameter[] { PEAK_LISTS, NIST_MS_SEARCH_DIR,
		IONIZATION_METHOD, SPECTRUM_RT_WIDTH, MAX_NUM_PEAKS,
		SAME_IDENTITIES, MIN_MATCH_FACTOR, MIN_REVERSE_MATCH_FACTOR });
    }

    @Override
    public boolean checkParameterValues(final Collection<String> errorMessages) {

	// Unsupported OS.
	if (!isWindows()) {
	    errorMessages
		    .add("NIST MS Search is only supported on the Windows operating system.");
	    return false;
	}

	boolean result = super.checkParameterValues(errorMessages);

	// NIST MS Search home directory and executable.
	final File executable = getNistMsSearchExecutable();

	// Executable missing.
	if (executable == null || !executable.exists()) {

	    errorMessages
		    .add("NIST MS Search executable ("
			    + NIST_MS_SEARCH_EXE
			    + ") not found.  Please set the to the full path of the directory containing the NIST MS Search executable.");
	    result = false;
	}

	return result;
    }

    /**
     * Gets the full path to the NIST MS Search executable.
     *
     * @return the path.
     */
    public File getNistMsSearchExecutable() {

	final File dir = getParameter(NIST_MS_SEARCH_DIR).getValue();
	return dir == null ? null : new File(dir, NIST_MS_SEARCH_EXE);
    }

    /**
     * Is this a Windows OS?
     *
     * @return true/false if the os.name property does/doesn't contain
     *         "Windows".
     */
    private static boolean isWindows() {

	return System.getProperty("os.name").toUpperCase().contains("WINDOWS");
    }
}
