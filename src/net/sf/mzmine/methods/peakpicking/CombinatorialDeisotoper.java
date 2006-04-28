/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package net.sf.mzmine.methods.peakpicking;
import java.util.Vector;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;


/**
 *	You can use this class for building the combinatorial deisotoping method
 */
public class CombinatorialDeisotoper implements PeakListProcessor {


	/**
	 * This method shows a dialog box to the user for manipulating default/previous parameter values
	 *
	 * @param	mainWin			MZmine main window (required for showing a modal dialog box)
	 * @param	currentValues	Previously used parameter values (null if there are no previous values)
	 */
	public CombinatorialDeisotoperParameters askParameters(MainWindow mainWin, CombinatorialDeisotoperParameters currentValues) {

		// If method's caller didn't give any previous parameter values, then initialize new parameters
		CombinatorialDeisotoperParameters myParameters;
		if (currentValues==null) {
			myParameters = new CombinatorialDeisotoperParameters();
		} else {
			myParameters = currentValues;
		}

		// Since this method is only a stub, it directly returns the default parameters value object without
		// showing a dialog box to the user. When implementing & testing a new method, you can define
		// the required parameters as constants in class CombinatorialDeisotoperParameters. Later
		// we can add user-interface for manipulating those parameters

		return myParameters;

	}


	/**
	 * This method processes a peak list and returns a new peak list.
	 *
	 * @param	nodeServer		MZmine node (required for updating user dialog)
	 * @param	theData			Raw data corresponding to the peak list
	 * @param	peakList		Peak list to be processed
	 * @param	_parameters		Parameter values
	 * @return					New peak list
	 */
	public PeakList processPeakList(RawDataFile theData, PeakList peakList, PeakListProcessorParameters _parameters) {

		// Take the parameters
		CombinatorialDeisotoperParameters parameters = (CombinatorialDeisotoperParameters)_parameters;

		// Initialize a new peak list
		PeakList modifiedPeakList = new PeakList();

		// Loop through the peaks of original peak list
		Vector<Peak> originalPeaks = peakList.getPeaks();
		int numberOfPeaks = originalPeaks.size();
		int currentPeakNumber = 0;
		for (Peak p : originalPeaks) {

			// Update wait dialog
			// updateJobCompletionRate((double)currentPeakNumber/(double)numberOfPeaks);

			// Give each peak a unique isotope pattern ID (put every peak to different pattern)
			p.setIsotopePatternID(currentPeakNumber);
			currentPeakNumber++;

			// Make every peak monoisotopic peak
			p.setIsotopePeakNumber(0);

			// Put the peak to new peak list
			modifiedPeakList.addPeakKeepOldID(p);

		}

		// Return new peak list
		return modifiedPeakList;

	}

}

