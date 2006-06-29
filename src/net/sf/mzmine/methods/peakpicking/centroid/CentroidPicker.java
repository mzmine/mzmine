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
package net.sf.mzmine.methods.peakpicking.centroid;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.interfaces.PeakList;
import net.sf.mzmine.io.MZmineProject;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.MyMath;
import net.sf.mzmine.util.Logger;
import net.sf.mzmine.visualizers.peaklist.table.TableView;


/**
 * This class implements a peak picker based on searching for local maximums in each spectra
 */
public class CentroidPicker implements Method, TaskListener {


	public String getMethodDescription() {
		return new String("Centroid peak picker");
	}

	/**
	 * Method asks parameter values from user
	 */
	public boolean askParameters(MethodParameters parameters) {

		CentroidPickerParameters currentParameters = (CentroidPickerParameters)parameters;
		if (currentParameters==null) return false;

		// Initialize parameter setup dialog
		double[] paramValues = new double[7];
		paramValues[0] = currentParameters.binSize;
		paramValues[1] = currentParameters.chromatographicThresholdLevel;
		paramValues[2] = currentParameters.noiseLevel;
		paramValues[3] = currentParameters.minimumPeakHeight;
		paramValues[4] = currentParameters.minimumPeakDuration;
		paramValues[5] = currentParameters.mzTolerance;
		paramValues[6] = currentParameters.intTolerance;

		String[] paramNames = new String[7];
		paramNames[0] = "M/Z bin size (Da)";
		paramNames[1] = "Chromatographic threshold level (%)";
		paramNames[2] = "Noise level (absolute value)";
		paramNames[3] = "Minimum peak height (absolute value)";
		paramNames[4] = "Minimum peak duration (seconds)";
		paramNames[5] = "Tolerance for m/z variation (Da)";
		paramNames[6] = "Tolerance for intensity variation (%)";

		NumberFormat[] numberFormats = new NumberFormat[7];
		numberFormats[0] = NumberFormat.getNumberInstance(); numberFormats[0].setMinimumFractionDigits(2);
		numberFormats[1] = NumberFormat.getPercentInstance();
		numberFormats[2] = NumberFormat.getNumberInstance(); numberFormats[2].setMinimumFractionDigits(0);
		numberFormats[3] = NumberFormat.getNumberInstance(); numberFormats[3].setMinimumFractionDigits(0);
		numberFormats[4] = NumberFormat.getNumberInstance(); numberFormats[4].setMinimumFractionDigits(1);
		numberFormats[5] = NumberFormat.getNumberInstance(); numberFormats[5].setMinimumFractionDigits(3);
		numberFormats[6] = NumberFormat.getPercentInstance();

		// Show parameter setup dialog
		MainWindow mainWin = MainWindow.getInstance();
		ParameterSetupDialog psd = new ParameterSetupDialog(mainWin, "Please check the parameter values", paramNames, paramValues, numberFormats);
		psd.show();

		// Check if user clicked Cancel-button
		if (psd.getExitCode()==-1) {
			return false;
		}


		// Read parameter values
		double d;


		d = psd.getFieldValue(0);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect bin size!");
			return false;
		}
		currentParameters.binSize = d;

		d = psd.getFieldValue(1);
		if ((d<0) || (d>1)) {
			mainWin.displayErrorMessage("Incorrect chromatographic threshold level!");
			return false;
		}
		currentParameters.chromatographicThresholdLevel = d;


		d = psd.getFieldValue(2);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect noise level!");
			return false;
		}
		currentParameters.noiseLevel = d;

		d = psd.getFieldValue(3);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect minimum peak height!");
			return false;
		}
		currentParameters.minimumPeakHeight = d;

		d = psd.getFieldValue(4);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect minimum peak duration!");
			return false;
		}
		currentParameters.minimumPeakDuration = d;

		d = psd.getFieldValue(5);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect m/z tolerance value!");
			return false;
		}
		currentParameters.mzTolerance = d;

		d = psd.getFieldValue(6);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect intensity tolerance value!");
			return false;
		}
		currentParameters.intTolerance = d;

		return true;
	}

	public void runMethod(MethodParameters parameters, RawDataFile[] rawDataFiles, AlignmentResult[] alignmentResults) {

		Task peakPickerTask;
		CentroidPickerParameters rpParam = (CentroidPickerParameters)parameters;

		for (RawDataFile rawDataFile: rawDataFiles) {
			peakPickerTask = new CentroidPickerTask(rawDataFile, rpParam);
			TaskController.getInstance().addTask(peakPickerTask, this);
		}

	}


    public void taskStarted(Task task) {
		// do nothing
	}

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

			RawDataFile rawData = (RawDataFile)((Object[])task.getResult())[0];
			PeakList peakList = (PeakList)((Object[])task.getResult())[1];
			CentroidPickerParameters params = (CentroidPickerParameters)((Object[])task.getResult())[2];

			// Add peak picking to the history of the file
			rawData.addHistory(rawData.getCurrentFile(), this, params.clone());

			// Add peak list to MZmineProject
			MZmineProject.getCurrentProject().setPeakList(rawData, peakList);

			MainWindow.getInstance().addInternalFrame(new TableView(rawData));

			MainWindow.getInstance().getMainMenu().updateMenuAvailability();

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            Logger.putFatal("Error while finding peaks in a file: " + task.getErrorMessage());
            MainWindow.getInstance().displayErrorMessage(
                    "Error while finding peaks in a file: " + task.getErrorMessage());

        }

	}


}
