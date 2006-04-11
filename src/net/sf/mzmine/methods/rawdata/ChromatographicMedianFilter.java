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



public class ChromatographicMedianFilter implements Filter {

	private ChromatographicMedianFilterParameters parameters;

	// Labels for parameters
	private final String[] fieldNames = {	"Tolerance in M/Z tolerance",
											"One-sided scan window length" };


	public ChromatographicMedianFilterParameters askParameters(MainWindow mainWin, ChromatographicMedianFilterParameters currentValues) {

		// Initialize parameters
		ChromatographicMedianFilterParameters myParameters;
		if (currentValues==null) {
			myParameters = new ChromatographicMedianFilterParameters();
		} else {
			myParameters = currentValues;
		}

		// Show parameter setup dialog
		double[] paramValues = new double[2];
		paramValues[0] = myParameters.mzTolerance;
		paramValues[1] = myParameters.oneSidedWindowLength;

		NumberFormat[] numberFormats = new NumberFormat[2];
		numberFormats[0] = NumberFormat.getNumberInstance(); numberFormats[0].setMinimumFractionDigits(3);
		numberFormats[1] = NumberFormat.getIntegerInstance();

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
			mainWin.displayErrorMessage("Incorrect M/Z tolerance value!");
			return null;
		}
		myParameters.mzTolerance = d;

		int i;
		i = (int)java.lang.Math.round(psd.getFieldValue(1));
		if (i<=0) {
			mainWin.displayErrorMessage("Incorrect one-sided scan window length!");
			return null;
		}
		myParameters.oneSidedWindowLength = i;

		return myParameters;

	}


	public int doFiltering(NodeServer nodeServer, RawDataAtNode rawData, FilterParameters _filterParameters) {

		ChromatographicMedianFilterParameters filterParameters = (ChromatographicMedianFilterParameters)_filterParameters;


		Scan[] scanBuffer = new Scan[1+2*filterParameters.oneSidedWindowLength];

		Scan sc;
		int ret;

		int numberOfDatapoints = rawData.getNumberOfDatapoints();
		int maxScan = rawData.getNumberOfScans();


		ret = rawData.initializeForWriting(numberOfDatapoints, maxScan);
		if (ret != 1) { return ret; }


		rawData.initializeScanBrowser(0, maxScan);

		for (int scani=0; scani<(maxScan+filterParameters.oneSidedWindowLength); scani++) {

			nodeServer.updateJobCompletionRate((double)scani/(double)(maxScan-1));

			// Pickup next scan from original raw data file
			if (scani<maxScan) {
				sc = rawData.getNextScan();
			} else {
				sc = null;
			}


			// Advance scan buffer
			for (int bufferIndex=0; bufferIndex<(scanBuffer.length-1); bufferIndex++) { scanBuffer[bufferIndex] = scanBuffer[bufferIndex+1]; }
			scanBuffer[scanBuffer.length-1] = sc;

			// Pickup mid element in the buffer
			sc = scanBuffer[filterParameters.oneSidedWindowLength];

/////////////////

			if (sc != null) {

				Integer[] dataPointIndices = new Integer[scanBuffer.length];
				for (int bufferIndex=0; bufferIndex<scanBuffer.length; bufferIndex++) { dataPointIndices[bufferIndex] = new Integer(0); }

				double[] mzValues = sc.getMZValues();
				double[] intValues = sc.getIntensityValues();
				double[] newIntValues = new double[intValues.length];

				for (int datapointIndex=0; datapointIndex<mzValues.length; datapointIndex++) {

					double mzValue = mzValues[datapointIndex];
					double intValue = intValues[datapointIndex];

					Vector<Double> intValueBuffer = new Vector<Double>();
					intValueBuffer.add(new Double(intValue));

					// Loop through the buffer
					for (int bufferIndex=0; bufferIndex<scanBuffer.length; bufferIndex++) {
						// Exclude middle buffer element
						//if (bufferIndex==oneSidedWindowLength) { continue; }

						if ( (bufferIndex!=filterParameters.oneSidedWindowLength) && (scanBuffer[bufferIndex]!=null) ) {
							Object[] res = findClosestDatapointIntensity(mzValue, scanBuffer[bufferIndex], dataPointIndices[bufferIndex].intValue(), filterParameters);
							Double closestInt = (Double)(res[0]);
							dataPointIndices[bufferIndex] = (Integer)(res[1]);
							if (closestInt != null) { intValueBuffer.add(closestInt); }
						}
					}

					// Calculate median of all intensity values in the buffer
					double[] tmpIntensities = new double[intValueBuffer.size()];
					for (int bufferIndex=0; bufferIndex<tmpIntensities.length; bufferIndex++)
							{ tmpIntensities[bufferIndex] = intValueBuffer.get(bufferIndex).doubleValue(); }
					double medianIntensity = MyMath.calcQuantile(tmpIntensities, (double)0.5);

					newIntValues[datapointIndex] = medianIntensity;

				}

				// Write the modified scan to file
				Scan modifiedScan = new Scan(sc.getMZValues(), newIntValues, sc.getScanNumber(), sc.getMZRangeMin(), sc.getMZRangeMax());
				ret = rawData.setScan(modifiedScan);

				if (ret != 1) {
					rawData.finalizeScanBrowser();
					rawData.finalizeAfterWriting();
					return ret;
				}

			}

/////////////////

		}

		rawData.finalizeScanBrowser();

		ret = rawData.finalizeAfterWriting();

		return ret;

	}


	/**
	 * Searches for data point in a scan closest to given mz value.
	 * @param	mzValue		Search for datapoint that is closest to this mzvalue
	 * @param	s			Search among datapoints in this scan
	 * @param	startIndex	Start searching from this datapoint
	 * @return	Array of two objects,
	 *	[0] is intensity of closest datapoint as Double or null if not a single datapoint was close enough.
	 *	[1] is index of datapoint that was closest to given mz value (this will be used as starting point for next search) if nothing was close enough to given mz value, then this is the start index
	 * Return intensity of the found data point as Double.
	 * If not a single data point is close enough (mz tolerance) then null value is returned.
	 */
	private Object[] findClosestDatapointIntensity(double mzValue, Scan s, int startIndex, ChromatographicMedianFilterParameters param) {
		double[] massValues = s.getMZValues();
		double[] intensityValues = s.getIntensityValues();

		Integer closestIndex = null;

		double closestMZ = -1;
		double closestIntensity = -1;
		double closestDistance = Double.MAX_VALUE;

		double prevDistance = Double.MAX_VALUE;

		// Loop through datapoints
		for (int i=startIndex; i<massValues.length; i++) {

			// Check if this mass values is within range to mz value
			double tmpDistance = java.lang.Math.abs(massValues[i]-mzValue);
			if ( tmpDistance < param.mzTolerance ) {

				// If this is first datapoint within range, then save the index
				//if (firstIndex==null) { firstIndex = new Integer(i); }

				// If this is closest datapoint so far, then store its' mz and intensity
				if (tmpDistance <= closestDistance) {
					closestMZ = massValues[i];
					closestIntensity = intensityValues[i];
					closestDistance = tmpDistance;
					closestIndex = new Integer(i);
				}

			}

			if (tmpDistance>prevDistance) { break; }

			prevDistance = tmpDistance;

		}


		if (closestIndex==null) { closestIndex = new Integer(startIndex); }

		Object[] result = new Object[2];
		result[0] = new Double(closestIntensity);
		result[1] = closestIndex;

		return result;

	}



}

