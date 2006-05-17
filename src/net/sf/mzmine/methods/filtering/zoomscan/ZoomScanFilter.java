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
package net.sf.mzmine.methods.filtering.zoomscan;
import java.text.NumberFormat;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.MZmineProject;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.util.Logger;


public class ZoomScanFilter implements Method, TaskListener {


	public String getMethodDescription() {
		return new String("Zoom scan filter");
	}

	public boolean askParameters(MethodParameters parameters) {

		ZoomScanFilterParameters currentParameters = (ZoomScanFilterParameters)parameters;
		if (currentParameters==null) return false;

		// Initialize parameter setup dialog
		double[] paramValues = new double[1];
		paramValues[0] = currentParameters.minMZRange;

		String[] paramNames = new String[1];
		paramNames[0] = "Minimum M/Z range width";

		NumberFormat[] numberFormats = new NumberFormat[1];
		numberFormats[0] = NumberFormat.getNumberInstance(); numberFormats[0].setMinimumFractionDigits(3);

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

		// minMZRange
		d = psd.getFieldValue(0);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect minimum M/Z range width!");
			return false;
		}
		currentParameters.minMZRange = d;

		return true;
	}

    /**
     * Runs this method on a given project
     * @param project
     * @param parameters
     */
    public void runMethod(MethodParameters parameters, RawDataFile[] rawDataFiles, AlignmentResult[] alignmentResults) {

		Task filterTask;
		ZoomScanFilterParameters zsfParam = (ZoomScanFilterParameters)parameters;

		for (RawDataFile rawDataFile: rawDataFiles) {
			filterTask = new ZoomScanFilterTask(rawDataFile, zsfParam);
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
			ZoomScanFilterParameters zsfParam = (ZoomScanFilterParameters)((Object[])task.getResult())[2];

			// Add mean filtering to the history of the file
			newFile.addHistory(oldFile.getCurrentFile(), this, zsfParam.clone());

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