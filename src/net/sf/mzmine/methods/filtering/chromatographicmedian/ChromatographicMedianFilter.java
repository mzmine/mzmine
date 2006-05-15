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
package net.sf.mzmine.methods.filtering.chromatographicmedian;
import java.text.NumberFormat;
import java.util.Vector;
import java.awt.Frame;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;




public class ChromatographicMedianFilter implements Method {

	private ChromatographicMedianFilterParameters parameters;

	// Labels for parameters
	private final String[] fieldNames = {	"Tolerance in M/Z tolerance",
											"One-sided scan window length" };


	public String getMethodDescription() {
		return new String("Chromatographic median filter");
	}

	public ChromatographicMedianFilterParameters askParameters(MethodParameters currentValues) {

		// Initialize parameters
		ChromatographicMedianFilterParameters myParameters;
		if (currentValues==null) {
			myParameters = new ChromatographicMedianFilterParameters();
		} else {
			myParameters = (ChromatographicMedianFilterParameters)currentValues;
		}

		// Show parameter setup dialog
		double[] paramValues = new double[2];
		paramValues[0] = myParameters.mzTolerance;
		paramValues[1] = myParameters.oneSidedWindowLength;

		NumberFormat[] numberFormats = new NumberFormat[2];
		numberFormats[0] = NumberFormat.getNumberInstance(); numberFormats[0].setMinimumFractionDigits(3);
		numberFormats[1] = NumberFormat.getIntegerInstance();

		MainWindow mainWin = MainWindow.getInstance();
		ParameterSetupDialog psd = new ParameterSetupDialog((Frame)mainWin, "Please check the parameter values", fieldNames, paramValues, numberFormats);
		psd.setVisible(true);


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

	public void runMethod(MethodParameters parameters, Object[] targets) {
	}





}

