/*
 * Copyright 2006 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.methods.peakpicking.recursivethreshold;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.MZmineProject;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.interfaces.PeakList;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.util.Logger;
import net.sf.mzmine.util.MyMath;


public class RecursiveThresholdPicker implements Method, TaskListener {


	public String getMethodDescription() {
		return new String("Recursive peak picker");
	}

	/**
	 * Method asks parameter values from user
	 */
	public boolean askParameters(MethodParameters parameters) {

		if (parameters==null) return false;
		RecursiveThresholdPickerParameters currentParameters = (RecursiveThresholdPickerParameters)parameters;

		// Initialize parameter setup dialog
		double[] paramValues = new double[9];
		paramValues[0] = currentParameters.binSize;
		paramValues[1] = currentParameters.chromatographicThresholdLevel;
		paramValues[2] = currentParameters.noiseLevel;
		paramValues[3] = currentParameters.minimumPeakHeight;
		paramValues[4] = currentParameters.minimumPeakDuration;
		paramValues[5] = currentParameters.minimumMZPeakWidth;
		paramValues[6] = currentParameters.maximumMZPeakWidth;
		paramValues[7] = currentParameters.mzTolerance;
		paramValues[8] = currentParameters.intTolerance;

		String[] paramNames = new String[9];
		paramNames[0] = "M/Z bin size (Da)";
		paramNames[1] = "Chromatographic threshold level (%)";
		paramNames[2] = "Noise level (absolute value)";
		paramNames[3] = "Minimum peak height (absolute value)";
		paramNames[4] = "Minimum peak duration (seconds)";
		paramNames[5] = "Minimum M/Z peak width (Da)";
		paramNames[6] = "Maximum M/Z peak width (Da)";
		paramNames[7] = "Tolerance for M/Z variation (Da)";
		paramNames[8] = "Tolerance for intensity variation (%)";

		NumberFormat[] numberFormats = new NumberFormat[9];
		numberFormats[0] = NumberFormat.getNumberInstance(); numberFormats[0].setMinimumFractionDigits(3);
		numberFormats[1] = NumberFormat.getPercentInstance();
		numberFormats[2] = NumberFormat.getNumberInstance(); numberFormats[2].setMinimumFractionDigits(0);
		numberFormats[3] = NumberFormat.getNumberInstance(); numberFormats[3].setMinimumFractionDigits(0);
		numberFormats[4] = NumberFormat.getNumberInstance(); numberFormats[4].setMinimumFractionDigits(1);
		numberFormats[5] = NumberFormat.getNumberInstance(); numberFormats[5].setMinimumFractionDigits(3);
		numberFormats[6] = NumberFormat.getNumberInstance(); numberFormats[6].setMinimumFractionDigits(3);
		numberFormats[7] = NumberFormat.getNumberInstance(); numberFormats[7].setMinimumFractionDigits(3);
		numberFormats[8] = NumberFormat.getPercentInstance();

		// Show parameter setup dialog
		MainWindow mainWin = MainWindow.getInstance();
		ParameterSetupDialog psd = new ParameterSetupDialog(mainWin, "Please check the parameter values", paramNames, paramValues, numberFormats);
		psd.show();

		// Check if user clicked Cancel-button
		if (psd.getExitCode()==-1) {
			return false;
		}

		// Read parameter values from dialog
		double d;

		d = psd.getFieldValue(0);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect " + paramNames[0]);
			return false;
		}
		currentParameters.binSize= d;

		d = psd.getFieldValue(1);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect " + paramNames[1]);
			return false;
		}
		currentParameters.chromatographicThresholdLevel = d;


		d = psd.getFieldValue(2);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect " + paramNames[2]);
			return false;
		}
		currentParameters.noiseLevel = d;

		d = psd.getFieldValue(3);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect " + paramNames[3]);
			return false;
		}
		currentParameters.minimumPeakHeight = d;

		d = psd.getFieldValue(4);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect " + paramNames[4]);
			return false;
		}
		currentParameters.minimumPeakDuration = d;

		d = psd.getFieldValue(5);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect " + paramNames[5]);
			return false;
		}
		currentParameters.minimumMZPeakWidth = d;

		d = psd.getFieldValue(6);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect " + paramNames[6]);
			return false;
		}
		currentParameters.maximumMZPeakWidth = d;

		d = psd.getFieldValue(7);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect " + paramNames[7]);
			return false;
		}
		currentParameters.mzTolerance = d;

		d = psd.getFieldValue(8);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect " + paramNames[8]);
			return false;
		}
		currentParameters.intTolerance = d;

		return true;

	}


	public void runMethod(MethodParameters parameters, RawDataFile[] rawDataFiles, AlignmentResult[] alignmentResults) {

		Task peakPickerTask;
		RecursiveThresholdPickerParameters rpParam = (RecursiveThresholdPickerParameters)parameters;

		for (RawDataFile rawDataFile: rawDataFiles) {
			peakPickerTask = new RecursiveThresholdPickerTask(rawDataFile, rpParam);
			TaskController.getInstance().addTask(peakPickerTask, this);
		}

	}


    public void taskStarted(Task task) {
		// do nothing
	}

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

			// TODO
			/*
			RawDataFile oldFile = (RawDataFile)((Object[])task.getResult())[0];
			RawDataFile newFile = (RawDataFile)((Object[])task.getResult())[1];
			ChromatographicMedianFilterParameters cmfParam = (ChromatographicMedianFilterParameters)((Object[])task.getResult())[2];

			// Add filtering to the history of the file
			newFile.addHistory(oldFile.getCurrentFile(), this, cmfParam.clone());

			// Update MZmineProject about replacement of oldFile by newFile
			MZmineProject.getCurrentProject().updateFile(oldFile, newFile);
			*/

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            Logger.putFatal("Error while finding peaks in a file: " + task.getErrorMessage());
            MainWindow.getInstance().displayErrorMessage(
                    "Error while finding peaks in a file: " + task.getErrorMessage());

        }

	}



}

