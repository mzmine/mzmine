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
package net.sf.mzmine.methods.rawdata;
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.obsoletedistributionframework.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;
import net.sf.mzmine.util.*;


import java.text.NumberFormat;

public class CropFilter implements Filter {

	private CropFilterParameters myParameters;

	private final String[] fieldNames = {
											"Minimum M/Z value",
											"Maximum M/Z value",
											"Minimum RT value",
											"Maximum RT value"
										};


	public CropFilterParameters askParameters(MainWindow mainWin, CropFilterParameters currentValues) {

		// Create filter parameter object
		CropFilterParameters tmpParameters;
		if (currentValues == null) {
			tmpParameters = new CropFilterParameters();
		} else {
			tmpParameters = currentValues;
		}


		// Show parameter setup dialog
		double[] paramValues = new double[4];
		paramValues[0] = tmpParameters.minMZ;
		paramValues[1] = tmpParameters.maxMZ;
		paramValues[2] = tmpParameters.minRT;
		paramValues[3] = tmpParameters.maxRT;

		NumberFormat[] numberFormats = new NumberFormat[4];
		numberFormats[0] = NumberFormat.getNumberInstance(); numberFormats[0].setMinimumFractionDigits(3);
		numberFormats[1] = NumberFormat.getNumberInstance(); numberFormats[1].setMinimumFractionDigits(3);
		numberFormats[2] = NumberFormat.getNumberInstance(); numberFormats[2].setMinimumFractionDigits(1);
		numberFormats[3] = NumberFormat.getNumberInstance(); numberFormats[3].setMinimumFractionDigits(1);

		ParameterSetupDialog psd = new ParameterSetupDialog(mainWin, "Please check the parameter values", fieldNames, paramValues, numberFormats);
		psd.showModal(mainWin.getDesktop());
		/*
		psd.setLocationRelativeTo(mainWin);
		psd.setVisible(true);
		*/

		// Check if user clicked Cancel-button
		if (psd.getExitCode()==-1) {
			return null;
		}

		// Read parameter values
		double d;

		// minMZ
		d = psd.getFieldValue(0);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect minimum M/Z value!");
			return null;
		}
		tmpParameters.minMZ = d;

		// maxMZ
		d = psd.getFieldValue(1);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect maximum M/Z value!");
			return null;
		}
		tmpParameters.maxMZ = d;

		// minRT
		d = psd.getFieldValue(2);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect minimum RT value!");
			return null;
		}
		tmpParameters.minRT = d;

		// maxRT
		d = psd.getFieldValue(3);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect maximum RT value!");
			return null;
		}
		tmpParameters.maxRT = d;

		return tmpParameters;

	}


	public int doFiltering(NodeServer nodeServer, RawDataAtNode rawData, FilterParameters _filterParameters) {

		CropFilterParameters filterParameters = (CropFilterParameters)_filterParameters;

		Scan sc;
		int ret;

		int maxScan = rawData.getNumberOfScans();

		double[] scanTimes = rawData.getScanTimes();
		int numOfIncludeScans = 0;
		for (double rt : scanTimes) {
			// Is this within the RT range?
			if (rt < filterParameters.minRT) { continue; }
			if (rt > filterParameters.maxRT) { break; }
			numOfIncludeScans++;
		}

		ret = rawData.initializeForWriting(-1, numOfIncludeScans);
		if (ret != 1) { return ret; }

		rawData.initializeScanBrowser(0, maxScan-1);

		for (int scani=0; scani<maxScan; scani++) {

			nodeServer.updateJobCompletionRate((double)scani/(double)(maxScan-1));

			sc = rawData.getNextScan();


			// Process the scan

			// Is this within the RT range?
			if (rawData.getScanTime(sc.getScanNumber()) < filterParameters.minRT) { continue; }
			if (rawData.getScanTime(sc.getScanNumber()) > filterParameters.maxRT) { break; }

			// Pickup datapoints inside the M/Z range
			double originalMassValues[] = sc.getMZValues();
			double originalIntensityValues[] = sc.getIntensityValues();

			int numSmallerThanMin = 0;
			for (int ind=0; ind<originalMassValues.length; ind++) {
				if (originalMassValues[ind]>=filterParameters.minMZ) { break; }
				numSmallerThanMin++;
			}

			int numBiggerThanMax = 0;
			for (int ind=(originalMassValues.length-1); ind>=0; ind--) {
				if (originalMassValues[ind]<=filterParameters.maxMZ) { break; }
				numBiggerThanMax++;
			}

			double newMassValues[] = new double[originalMassValues.length-numSmallerThanMin-numBiggerThanMax];
			double newIntensityValues[] = new double[originalMassValues.length-numSmallerThanMin-numBiggerThanMax];

			int newInd = 0;
			for (int ind=numSmallerThanMin; ind<(originalMassValues.length-numBiggerThanMax); ind++) {
				newMassValues[newInd] = originalMassValues[ind];
				newIntensityValues[newInd] = originalIntensityValues[ind];
				newInd++;
			}

			// Set net datapoints
			sc.setMZValues(newMassValues);
			sc.setIntensityValues(newIntensityValues);


			// Store scan
			ret = rawData.setScan(sc);

			if (ret != 1) {
				rawData.finalizeScanBrowser();
				rawData.finalizeAfterWriting();
				return ret;
			}

		}

		rawData.finalizeScanBrowser();

		ret = rawData.finalizeAfterWriting();

		return ret;

	}


}