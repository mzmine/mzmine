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
import java.util.Hashtable;

import javax.swing.JOptionPane;

import net.sf.mzmine.datastructures.RawDataAtNode;
import net.sf.mzmine.datastructures.Scan;
import net.sf.mzmine.obsoletedistributionframework.NodeServer;
import net.sf.mzmine.userinterface.MainWindow;
import net.sf.mzmine.userinterface.Statusbar;


public class SavitzkyGolayFilter implements Filter {

	private Hashtable<Integer, Integer> Hvalues;
	private Hashtable<Integer, int[]> Avalues;

	private MainWindow mainWin;
	private Statusbar statBar;

	public SavitzkyGolayFilterParameters askParameters(MainWindow _mainWin, SavitzkyGolayFilterParameters currentValues) {
		mainWin = _mainWin;
		statBar = mainWin.getStatusBar();

		// Initialize parameter values
		SavitzkyGolayFilterParameters tmpParameters;
		if (currentValues==null) {
			tmpParameters = new SavitzkyGolayFilterParameters();
		} else {
			tmpParameters = currentValues;
		}

		// Define different options and currently selected item
		String[] possibilities = {"5","7","9","11","13","15","17","19","21","23","25"};
		String selectedValue = "5";
		for (String s : possibilities) {
			if (Integer.parseInt(s)==tmpParameters.numberOfDataPoints) {
				selectedValue = s;
			}
		}

		// Show dialog


		String s = (String)JOptionPane.showInputDialog(
						mainWin,
						"Select number of data points used for smoothing:",
						"Savitzky-Golay filter",
						JOptionPane.PLAIN_MESSAGE,
						null,
						possibilities,
						new Integer(tmpParameters.numberOfDataPoints).toString());


		if (s==null) { return null; }

		try {
			tmpParameters.numberOfDataPoints = Integer.parseInt(s);

		} catch (NumberFormatException exe) {
			return null;
		}

		// Return parameter values to caller
		return tmpParameters;

	}


	public int doFiltering(NodeServer nodeServer, RawDataAtNode rawData, FilterParameters _filterParameters) {

		// Get parameters
		SavitzkyGolayFilterParameters filterParameters = (SavitzkyGolayFilterParameters)_filterParameters;

		// Initialize AH values
		initializeAHValues();

		int numberOfDatapoints = rawData.getNumberOfDatapoints();
		int maxScan = rawData.getNumberOfScans();

		int ret = rawData.initializeForWriting(numberOfDatapoints, maxScan);
		if (ret != 1) { return ret; }


		int[] aVals = Avalues.get(new Integer(filterParameters.numberOfDataPoints));
		int h = Hvalues.get(new Integer(filterParameters.numberOfDataPoints)).intValue();

		rawData.initializeScanBrowser(0, maxScan);

		for (int scani=0; scani<maxScan; scani++) {
			nodeServer.updateJobCompletionRate((double)scani/(double)(maxScan-1));

			Scan sc = rawData.getNextScan();
			processOneScan(sc, filterParameters.numberOfDataPoints, h, aVals);

			ret = rawData.setScan(sc);

			if (ret != 1) {
				rawData.finalizeScanBrowser();
				rawData.finalizeAfterWriting();
				return ret;
			}

		}

		rawData.finalizeScanBrowser();
		return rawData.finalizeAfterWriting();

	}


	private void processOneScan(Scan sc, int numOfDataPoints, int h, int[] aVals) {

		int marginSize = (numOfDataPoints+1)/2-1;
		double sumOfInts;

		double[] masses = sc.getMZValues();
		double[] intensities = sc.getIntensityValues();
		double[] newIntensities = new double[masses.length];

		int addi=0;
		for (int spectrumInd=marginSize; spectrumInd<(masses.length-marginSize); spectrumInd++) {

			sumOfInts = aVals[0] * intensities[spectrumInd];

			for (int windowInd=1; windowInd<=marginSize; windowInd++) {
				sumOfInts += aVals[windowInd] * (intensities[spectrumInd+windowInd] +intensities[spectrumInd-windowInd] );
			}

			sumOfInts = sumOfInts / h;

			if (sumOfInts<0) { sumOfInts = 0; }
			newIntensities[spectrumInd] = sumOfInts;

		}
		sc.setIntensityValues(newIntensities);

	}

	/**
	 * Initialize Avalues and Hvalues
	 * These are actually constants, but it is difficult to define them as static final
	 */
	private void initializeAHValues() {
		Avalues = new Hashtable<Integer, int[]>();
		Hvalues = new Hashtable<Integer, Integer>();

		int[] a5Ints =  {  17,  12,  -3,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0}; Avalues.put(new Integer(5), a5Ints);
		int[] a7Ints =  {   7,   6,   3,  -2,   0,   0,   0,   0,   0,   0,   0,   0,   0}; Avalues.put(new Integer(7), a7Ints);
		int[] a9Ints =  {  59,  54,  39,  14, -21,   0,   0,   0,   0,   0,   0,   0,   0}; Avalues.put(new Integer(9), a9Ints);
		int[] a11Ints = {  89,  84,  69,  44,   9, -36,   0,   0,   0,   0,   0,   0,   0}; Avalues.put(new Integer(11), a11Ints);
		int[] a13Ints = {  25,  24,  21,  16,   9,   0, -11,   0,   0,   0,   0,   0,   0}; Avalues.put(new Integer(13), a13Ints);
		int[] a15Ints = { 167, 162, 147, 122,  87,  42, -13, -78,   0,   0,   0,   0,   0}; Avalues.put(new Integer(15), a15Ints);
		int[] a17Ints = {  43,  42,  39,  34,  27,  18,   7,  -6, -21,   0,   0,   0,   0}; Avalues.put(new Integer(17), a17Ints);
		int[] a19Ints = { 269, 264, 249, 224, 189, 144,  89,  24, -51,-136,   0,   0,   0}; Avalues.put(new Integer(19), a19Ints);
		int[] a21Ints = { 329, 324, 309, 284, 249, 204, 149,  84,   9, -76,-171,   0,   0}; Avalues.put(new Integer(21), a21Ints);
		int[] a23Ints = {  79,  78,  75,  70,  63,  54,  43,  30,  15,  -2, -21, -42,   0}; Avalues.put(new Integer(23), a23Ints);
		int[] a25Ints = { 467, 462, 447, 422, 387, 343, 287, 222, 147,  62, -33,-138,-253}; Avalues.put(new Integer(25), a25Ints);

		Integer h5Int = new Integer(35); Hvalues.put(new Integer(5), h5Int);
		Integer h7Int = new Integer(21); Hvalues.put(new Integer(7), h7Int);
		Integer h9Int = new Integer(231); Hvalues.put(new Integer(9), h9Int);
		Integer h11Int = new Integer(429); Hvalues.put(new Integer(11), h11Int);
		Integer h13Int = new Integer(143); Hvalues.put(new Integer(13), h13Int);
		Integer h15Int = new Integer(1105); Hvalues.put(new Integer(15), h15Int);
		Integer h17Int = new Integer(323); Hvalues.put(new Integer(17), h17Int);
		Integer h19Int = new Integer(2261); Hvalues.put(new Integer(19), h19Int);
		Integer h21Int = new Integer(3059); Hvalues.put(new Integer(21), h21Int);
		Integer h23Int = new Integer(805); Hvalues.put(new Integer(23), h23Int);
		Integer h25Int = new Integer(5175); Hvalues.put(new Integer(25), h25Int);
	}


}

