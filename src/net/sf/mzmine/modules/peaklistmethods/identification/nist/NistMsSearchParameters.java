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

import java.util.Collection;

import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;

/**
 * Holds NIST MS Search parameters.
 * 
 * @author $Author: cpudney $
 * @version $Revision: 2369 $
 */
public class NistMsSearchParameters extends SimpleParameterSet {

	public static final PeakListsParameter peakLists = new PeakListsParameter();

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
	public static final DoubleParameter SPECTRUM_RT_WIDTH = new DoubleParameter(
			"Spectrum RT tolerance",
			"The RT tolerance (>= 0) to use when forming search spectra; include all other detected peaks whose RT is within the specified tolerance of a given peak.",
			MZmineCore.getRTFormat(),
			3.0, 0.0, null);

	/**
	 * Match factor cut-off.
	 */
	public static final IntegerParameter MIN_MATCH_FACTOR = new IntegerParameter(
			"Min. match factor",
			"The minimum match factor (0 .. 1000) that search hits must have.",
			800, 0, 1000);

	/**
	 * Match factor cut-off.
	 */
	public static final IntegerParameter MIN_REVERSE_MATCH_FACTOR = new IntegerParameter(
			"Min. reverse match factor",
			"The minimum reverse match factor (0 .. 1000) that search hits must have.",
			800, 0, 1000);

	/**
	 * Construct the parameter set.
	 */
	public NistMsSearchParameters() {
		super(new Parameter[] { peakLists, IONIZATION_METHOD,
				SPECTRUM_RT_WIDTH, MIN_MATCH_FACTOR, MIN_REVERSE_MATCH_FACTOR });
	}

	@Override
	public boolean checkUserParameterValues(Collection<String> errorMessages) {

		boolean result = super.checkUserParameterValues(errorMessages);

		// Unsupported OS
		if (!isWindows()) {
			errorMessages
					.add("NIST MS Search is only supported on the Windows operating system.");
			return false;
		}
		;

		// Property not defined
		if (NistMsSearchModule.NIST_MS_SEARCH_DIR == null) {
			errorMessages.add("The "
					+ NistMsSearchModule.NIST_MS_SEARCH_PATH_PROPERTY
					+ " system property is not set.");
			result = false;
		}

		// Executable missing
		if (!NistMsSearchModule.NIST_MS_SEARCH_EXE.exists()) {

			errorMessages
					.add(NistMsSearchModule.NIST_MS_SEARCH_EXE
							+ " not found.  Please set the "
							+ NistMsSearchModule.NIST_MS_SEARCH_PATH_PROPERTY
							+ " system property to the full path of the directory containing the NIST MS Search executable.");
			result = false;

		}

		return result;
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
