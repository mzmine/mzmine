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
package net.sf.mzmine.peaklistmethods;
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.distributionframework.*;
import net.sf.mzmine.miscellaneous.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;


// Java packages

import java.util.*;
import java.text.NumberFormat;


/**
 * This class implements a peak picker based on searching for local maximums in each spectra
 */
public class IncompleteIsotopePatternFilter implements PeakListProcessor {

	// Labels for parameters
	private final String[] fieldNames = {	"Minimum number of peaks" };


	/**
	 * Method asks parameter values from user
	 */
	public IncompleteIsotopePatternFilterParameters askParameters(MainWindow mainWin, IncompleteIsotopePatternFilterParameters currentValues) {

		// Initialize parameters
		IncompleteIsotopePatternFilterParameters myParameters;
		if (currentValues==null) {
			myParameters = new IncompleteIsotopePatternFilterParameters();
		} else {
			myParameters = currentValues;
		}

		// Show parameter setup dialog
		double[] paramValues = new double[1];
		paramValues[0] = myParameters.minimumNumberOfPeaks;

		// Define number formats for displaying each parameter
		NumberFormat[] numberFormats = new NumberFormat[1];
		numberFormats[0] = NumberFormat.getNumberInstance(); numberFormats[0].setMinimumFractionDigits(0);


		ParameterSetupDialog psd = new ParameterSetupDialog(mainWin, "Please check the parameter values", fieldNames, paramValues, numberFormats);
		psd.showModal(mainWin.getDesktop());


		// Check if user clicked Cancel-button
		if (psd.getExitCode()==-1) {
			return null;
		}


		// Read parameter values
		int d;

		d = (int)psd.getFieldValue(0);
		if (d<1) {
			mainWin.displayErrorMessage("Incorrect minimum number of peaks!");
			return null;
		}
		myParameters.minimumNumberOfPeaks = d;

		return myParameters;
	}



	/**
	 * This method does the processing
	 */
	public PeakList processPeakList(NodeServer nodeServer, RawDataAtNode theData, PeakList peakList, PeakListProcessorParameters _parameters) {

		IncompleteIsotopePatternFilterParameters parameters = (IncompleteIsotopePatternFilterParameters)_parameters;

		// Start a new peak list
		PeakList modifiedPeakList = new PeakList();


		// Assign all peaks to a TreeSet for sorting them
		// At the same time, clear all isotope pattern information
		TreeSet<Peak> peakTree = new TreeSet<Peak>(new PeakOrderer());

		Vector<Peak> allPeaks = peakList.getPeaks();
		for (Peak p : allPeaks) {
			peakTree.add(p);
		}

		Iterator<Peak> peakIterator = peakTree.iterator();
		if (peakIterator.hasNext()) {
			Peak nextPeak = peakIterator.next();
			while (peakIterator.hasNext()) {

				// Mark down the isotope pattern ID of current peak
				int isotopePatternID = nextPeak.getIsotopePatternID();

				// Collect to a vector all peaks belonging to the same isotope pattern
				Vector<Peak> allPeaksInThisPattern = new Vector<Peak>();
				while (nextPeak.getIsotopePatternID()==isotopePatternID) {

					allPeaksInThisPattern.add(nextPeak);
					if (peakIterator.hasNext()) { nextPeak = peakIterator.next(); } else {
						break;
					}
				}

				// Are there enough peaks in this isotope pattern
				if (allPeaksInThisPattern.size()>=parameters.minimumNumberOfPeaks) {
					// Yes, add them all to modified peak list
					for (Peak aPeak : allPeaksInThisPattern) { modifiedPeakList.addPeakKeepOldID(aPeak); }
					allPeaksInThisPattern.clear(); allPeaksInThisPattern = null;
				} else {
					// Throw away all peaks in this pattern
					allPeaksInThisPattern.clear(); allPeaksInThisPattern = null;
				}

			}
		}

		nodeServer.updateJobCompletionRate(1);
		//return modifiedPeakList;

		return modifiedPeakList;

	}


	private class PeakOrderer implements Comparator<Peak> {
		public int compare(Peak p1, Peak p2) {
			if (p1.getIsotopePatternID()<=p2.getIsotopePatternID()) {
				return -1;
			} else { return 1;}
/*
			if (p1.getIsotopePeakNumber()<=p2.getIsotopePeakNumber()) {
				return -1;
			}
			return 1;
*/
		}

		public boolean equals(Object obj) { return false; }
	}



}

