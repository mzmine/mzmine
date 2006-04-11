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


// Java packages
import java.util.Vector;
import java.util.TreeSet;
import java.util.Arrays;

import java.text.NumberFormat;

import java.awt.Cursor;
import javax.swing.JOptionPane;



public class MeanFilter implements Filter {

	private MeanFilterParameters parameters;

	// Labels for parameters
	private final String[] fieldNames = { "Give m/z window length (one-sided)" };


	public MeanFilterParameters askParameters(MainWindow mainWin, MeanFilterParameters currentValues) {

		// Create filter parameter object
		MeanFilterParameters tmpParameters;
		if (currentValues == null) {
			tmpParameters = new MeanFilterParameters();
		} else {
			tmpParameters = currentValues;
		}


		// Show parameter setup dialog
		double[] paramValues = new double[1];
		paramValues[0] = tmpParameters.windowLength;

		NumberFormat[] numberFormats = new NumberFormat[1];
		numberFormats[0] = NumberFormat.getNumberInstance(); numberFormats[0].setMinimumFractionDigits(3);

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

		d = psd.getFieldValue(0);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect M/Z window length value!");
			return null;
		}
		tmpParameters.windowLength = d;

		return tmpParameters;

	}


	public int doFiltering(NodeServer nodeServer, RawDataAtNode rawData, FilterParameters _filterParameters) {

		MeanFilterParameters filterParameters = (MeanFilterParameters)_filterParameters;

		Scan sc;
		int ret;

		int numberOfDatapoints = rawData.getNumberOfDatapoints();
		int maxScan = rawData.getNumberOfScans();


		ret = rawData.initializeForWriting(numberOfDatapoints, maxScan);
		if (ret != 1) { return ret; }



		rawData.initializeScanBrowser(0, maxScan);

		for (int scani=0; scani<maxScan; scani++) {

			nodeServer.updateJobCompletionRate((double)scani/(double)(maxScan-1));

			sc = rawData.getNextScan();
			processOneScan(sc, filterParameters.windowLength);

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


	private void processOneScan(Scan sc, double windowLength) {

		Vector<Double> massWindow = new Vector<Double>();
		Vector<Double> intensityWindow = new Vector<Double>();

		double currentMass;
		double lowLimit;
		double hiLimit;
		double mzVal;

		double elSum;

		double[] masses = sc.getMZValues();
		double[] intensities = sc.getIntensityValues();
		double[] newIntensities = new double[masses.length];

		int addi=0;
		for (int i=0; i<masses.length; i++) {

			currentMass = masses[i];
			lowLimit = currentMass - windowLength;
			hiLimit = currentMass + windowLength;

			// Remove all elements from window whose m/z value is less than the low limit
			if (massWindow.size()>0) {
				mzVal = massWindow.get(0).doubleValue();
				while ((massWindow.size()>0) && (mzVal<lowLimit))  {
					massWindow.remove(0);
					intensityWindow.remove(0);
					if (massWindow.size()>0) {
						mzVal = massWindow.get(0).doubleValue();
					}
				}
			}

			// Add new elements as long as their m/z values are less than the hi limit
			while ((addi<masses.length) && (masses[addi]<=hiLimit)) {
				massWindow.add(new Double(masses[addi]));
				intensityWindow.add(new Double(intensities[addi]));
				addi++;
			}

			elSum = 0;
			for (int j=0; j<intensityWindow.size(); j++) {
				elSum  += ((Double)(intensityWindow.get(j))).doubleValue();
			}

			newIntensities[i] = elSum / (double)intensityWindow.size();

		}
		sc.setIntensityValues(newIntensities);

	}

}

