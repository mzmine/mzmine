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
package net.sf.mzmine.methods.filtering.savitzkygolay;
import java.util.Hashtable;

import javax.swing.JOptionPane;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.MZmineProject;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.userinterface.mainwindow.Statusbar;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.util.Logger;


public class SavitzkyGolayFilter implements Method, TaskListener {


	public String getMethodDescription() {
		return new String("Savitzky Golay filter");
	}

	public boolean askParameters(MethodParameters parameters) {

		SavitzkyGolayFilterParameters currentParameters = (SavitzkyGolayFilterParameters)parameters;
		if (currentParameters==null) return false;

		// Define different options and currently selected item
		String[] possibilities = {"5","7","9","11","13","15","17","19","21","23","25"};
		String selectedValue = "5";
		for (String s : possibilities) {
			if (Integer.parseInt(s)==currentParameters.numberOfDataPoints) {
				selectedValue = s;
			}
		}

		// Show dialog
		MainWindow mainWin = MainWindow.getInstance();

		String s = (String)JOptionPane.showInputDialog(
						mainWin,
						"Select number of data points used for smoothing:",
						"Savitzky-Golay filter",
						JOptionPane.PLAIN_MESSAGE,
						null,
						possibilities,
						new Integer(currentParameters.numberOfDataPoints).toString());
		if (s==null) { return false; }

		try {
			currentParameters.numberOfDataPoints = Integer.parseInt(s);
		} catch (NumberFormatException exe) {
			return false;
		}

		return true;

	}

    /**
     * Runs this method on a given project
     * @param project
     * @param parameters
     */
    public void runMethod(MethodParameters parameters, RawDataFile[] rawDataFiles, AlignmentResult[] alignmentResults) {

		Task filterTask;
		SavitzkyGolayFilterParameters sgfParam = (SavitzkyGolayFilterParameters)parameters;

		for (RawDataFile rawDataFile: rawDataFiles) {
			filterTask = new SavitzkyGolayFilterTask(rawDataFile, sgfParam);
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
			SavitzkyGolayFilterParameters sgfParam = (SavitzkyGolayFilterParameters)((Object[])task.getResult())[2];

			// Add mean filtering to the history of the file
			newFile.addHistory(oldFile.getCurrentFile(), this, sgfParam.clone());

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

