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



public class ChromatographicMedianFilter implements Method, TaskListener {


	public String getMethodDescription() {
		return new String("Chromatographic median filter");
	}

	public boolean askParameters(MethodParameters parameters) {

		ChromatographicMedianFilterParameters currentParameters = (ChromatographicMedianFilterParameters)parameters;
		if (currentParameters==null) return false;

		// Initialize parameter setup dialog
		double[] paramValues = new double[2];
		paramValues[0] = currentParameters.mzTolerance;
		paramValues[1] = currentParameters.oneSidedWindowLength;

		String[] paramNames = new String[2];
		paramNames[0] = "Tolerance in M/Z tolerance";
		paramNames[1] = "One-sided scan window length";

		NumberFormat[] numberFormats = new NumberFormat[2];
		numberFormats[0] = NumberFormat.getNumberInstance(); numberFormats[0].setMinimumFractionDigits(3);
		numberFormats[1] = NumberFormat.getIntegerInstance();

		// Show parameter setup dialog
		MainWindow mainWin = MainWindow.getInstance();
		ParameterSetupDialog psd = new ParameterSetupDialog((Frame)mainWin, "Please check the parameter values", paramNames, paramValues, numberFormats);
		psd.setVisible(true);

		// Check if user clicked Cancel-button
		if (psd.getExitCode()==-1) {
			return false;
		}

		// Write values from dialog back to parameters object
		double d;

		d = psd.getFieldValue(0);
		if (d<=0) {
			mainWin.displayErrorMessage("Incorrect M/Z tolerance value!");
			return false;
		}
		currentParameters.mzTolerance = d;

		int i;
		i = (int)java.lang.Math.round(psd.getFieldValue(1));
		if (i<=0) {
			mainWin.displayErrorMessage("Incorrect one-sided scan window length!");
			return false;
		}
		currentParameters.oneSidedWindowLength = i;

		return true;

	}

	public void runMethod(MethodParameters parameters, RawDataFile[] rawDataFiles, AlignmentResult[] alignmentResults) {

		Task filterTask;
		ChromatographicMedianFilterParameters cmfParam = (ChromatographicMedianFilterParameters)parameters;

		for (RawDataFile rawDataFile: rawDataFiles) {
			filterTask = new ChromatographicMedianFilterTask(rawDataFile, cmfParam);
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
			ChromatographicMedianFilterParameters cmfParam = (ChromatographicMedianFilterParameters)((Object[])task.getResult())[2];

			// Add filtering to the history of the file
			newFile.addHistory(oldFile.getCurrentFile(), this, cmfParam.clone());

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

