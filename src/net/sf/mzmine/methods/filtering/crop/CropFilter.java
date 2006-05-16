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
package net.sf.mzmine.methods.filtering.crop;

import java.text.NumberFormat;

import java.awt.Frame;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.MZmineProject;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.Logger;

public class CropFilter implements Method, TaskListener {


	public String getMethodDescription() {
		return new String("Crop filter");
	}

	public boolean askParameters(MethodParameters parameters) {

		CropFilterParameters currentParameters = (CropFilterParameters)parameters;
		if (currentParameters==null) return false;

		// Show parameter setup dialog
		double[] paramValues = new double[4];
		paramValues[0] = currentParameters.minMZ;
		paramValues[1] = currentParameters.maxMZ;
		paramValues[2] = currentParameters.minRT;
		paramValues[3] = currentParameters.maxRT;

		String[] paramNames = new String[4];
		paramNames[0] = "Minimum M/Z (Da)";
		paramNames[1] = "Maximum M/Z (Da)";
		paramNames[2] = "Minimum RT (seconds)";
		paramNames[3] = "Maximum RT (seconds)";

		NumberFormat[] numberFormats = new NumberFormat[4];
		numberFormats[0] = NumberFormat.getNumberInstance(); numberFormats[0].setMinimumFractionDigits(3);
		numberFormats[1] = NumberFormat.getNumberInstance(); numberFormats[1].setMinimumFractionDigits(3);
		numberFormats[2] = NumberFormat.getNumberInstance(); numberFormats[2].setMinimumFractionDigits(1);
		numberFormats[3] = NumberFormat.getNumberInstance(); numberFormats[3].setMinimumFractionDigits(1);

		MainWindow mainWin = MainWindow.getInstance();
		ParameterSetupDialog psd = new ParameterSetupDialog((Frame)mainWin, "Please check the parameter values", paramNames, paramValues, numberFormats);
		psd.setVisible(true);

		// Check if user clicked Cancel-button
		if (psd.getExitCode()==-1) {
			return false;
		}

		// Read parameter values
		double d;

		// minMZ
		d = psd.getFieldValue(0);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect minimum M/Z value!");
			return false;
		}
		currentParameters.minMZ = d;

		// maxMZ
		d = psd.getFieldValue(1);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect maximum M/Z value!");
			return false;
		}
		currentParameters.maxMZ = d;

		// minRT
		d = psd.getFieldValue(2);
		if (d<0) {
			mainWin.displayErrorMessage("Incorrect minimum RT value!");
			return false;
		}
		currentParameters.minRT = d;

		// maxRT
		d = psd.getFieldValue(3);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect maximum RT value!");
			return false;
		}
		currentParameters.maxRT = d;

		return true;

	}

	public void runMethod(MethodParameters parameters, RawDataFile[] rawDataFiles, AlignmentResult[] alignmentResults) {

		Task filterTask;
		CropFilterParameters cmfParam = (CropFilterParameters)parameters;

		for (RawDataFile rawDataFile: rawDataFiles) {
			filterTask = new CropFilterTask(rawDataFile, cmfParam);
			TaskController.getInstance().addTask(filterTask, this);
		}


	}

    public void taskStarted(Task task) {
		// do nothing
	}

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

			RawDataFile oldFile = (RawDataFile)((Object[])task.getResult())[0];
			RawDataFile newFile = (RawDataFile)((Object[])task.getResult())[1];
			CropFilterParameters cfParam = (CropFilterParameters)((Object[])task.getResult())[2];

			// Add mean filtering to the history of the file
			newFile.addHistory(oldFile.getCurrentFile(), this, cfParam.clone());

			// Update MZmineProject about replacement of oldFile by newFile
			MZmineProject.getCurrentProject().updateFile(oldFile, newFile);

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            Logger.putFatal("Error while filtering a file: " + task.getErrorMessage());
            MainWindow.getInstance().displayErrorMessage(
                    "Error while filtering a file: " + task.getErrorMessage());

        }

	}

}