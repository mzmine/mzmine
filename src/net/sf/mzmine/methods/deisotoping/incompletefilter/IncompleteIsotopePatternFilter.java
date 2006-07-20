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
package net.sf.mzmine.methods.deisotoping.incompletefilter;

import java.text.NumberFormat;
import java.util.logging.Logger;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;


/**
 * This class implements a peak picker based on searching for local maximums in each spectra
 */
public class IncompleteIsotopePatternFilter implements Method {

	public String getModuleDescription() {
		return new String("Incomplete isotope pattern filter");
	}

	/**
	 * Method asks parameter values from user
	 */
	public boolean askParameters(MethodParameters parameters) {

		if (parameters==null) return false;
		IncompleteIsotopePatternFilterParameters currentParameters = (IncompleteIsotopePatternFilterParameters)parameters;

		// Initialize parameter setup dialog
		double[] paramValues = new double[1];
		paramValues[0] = currentParameters.minimumNumberOfPeaks;

		String[] paramNames = new String[1];
		paramNames[0] = "Minimum number of peaks";

		// Define number formats for displaying each parameter
		NumberFormat[] numberFormats = new NumberFormat[1];
		numberFormats[0] = NumberFormat.getNumberInstance(); numberFormats[0].setMinimumFractionDigits(0);

		MainWindow mainWin = null;
		ParameterSetupDialog psd = new ParameterSetupDialog(mainWin, "Please check the parameter values", paramNames, paramValues, numberFormats);
		psd.setVisible(true);


		// Check if user clicked Cancel-button
		if (psd.getExitCode()==-1) {
			return false;
		}

		// Read parameter values
		int d;
		d = (int)psd.getFieldValue(0);
		if (d<1) {
			mainWin.displayErrorMessage("Incorrect minimum number of peaks!");
			return false;
		}
		currentParameters.minimumNumberOfPeaks = d;

		return true;

	}


	public void runMethod(MethodParameters parameters, RawDataFile[] rawDataFiles, AlignmentResult[] alignmentResults) {
		// TODO
	}

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.taskcontrol.TaskController, net.sf.mzmine.userinterface.Desktop, java.util.logging.Logger)
     */
    public void initModule(TaskController taskController, Desktop desktop, Logger logger) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.methods.Method#askParameters()
     */
    public MethodParameters askParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.methods.MethodParameters, net.sf.mzmine.io.OpenedRawDataFile[], net.sf.mzmine.methods.alignment.AlignmentResult[])
     */
    public void runMethod(MethodParameters parameters, OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {
        // TODO Auto-generated method stub
        
    }


}

