/*
 * Copyright 2006-2008 The MZmine Development Team
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


package net.sf.mzmine.modules.isotopes.isotopeprediction;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.modules.visualization.spectra.PeakListDataSet;
import net.sf.mzmine.modules.visualization.spectra.SpectraDataSet;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizer;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class IsotopePatternCalculator implements MZmineModule, TaskListener,
		ActionListener {

	private static IsotopePatternCalculator myInstance;

	private IsotopePatternCalculatorParameters parameters;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private Desktop desktop;

	private SpectraPlot plot;

	/**
	 * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
	 */
	public void initModule() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new IsotopePatternCalculatorParameters();

		myInstance = this;

		desktop
				.addMenuItem(
						MZmineMenu.VISUALIZATION,
						"Isotope pattern calculator",
						"Calculation of isotope pattern using a given chemical formula",
						KeyEvent.VK_P, this, null);

	}

	public static IsotopePatternCalculator getInstance() {
		return myInstance;
	}

	public void setParameters(ParameterSet parameters) {
		this.parameters = (IsotopePatternCalculatorParameters) parameters;
	}

	/**
	 * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	public void actionPerformed(ActionEvent e) {
		logger.finest("Opening a new spectra visualizer setup dialog");

		ParameterSetupDialog dialog = new ParameterSetupDialog(
				"Please set parameter values for " + toString(), parameters);

		dialog.setVisible(true);

		if (dialog.getExitCode() != ExitCode.OK)
			return;

		runTask();

	}

	public void showIsotopePatternCalculatorWindow(SpectraPlot plot) {

		this.plot = plot;

		ParameterSetupDialog dialog = new ParameterSetupDialog(
				"Please set parameter values for " + toString(), parameters);

		dialog.setVisible(true);

		if (dialog.getExitCode() != ExitCode.OK)
			return;

		runTask();

	}

	public void runTask() {

		// prepare a new group of tasks
		Task tasks[] = new IsotopePatternCalculatorTask[1];
		tasks[0] = new IsotopePatternCalculatorTask(
				(IsotopePatternCalculatorParameters) parameters.clone());

		TaskGroup newGroup = new TaskGroup(tasks, this, null);

		// start the group
		newGroup.start();

	}

	public String toString() {
		return "Isotope pattern calculator";
	}

	public void taskFinished(Task task) {
		IsotopePatternCalculatorTask ipcTask = (IsotopePatternCalculatorTask) task;

		if (task.getStatus() == Task.TaskStatus.FINISHED) {
			logger.info("Finished isotope pattern of " + ipcTask.getFormula());

			IsotopePattern ip = ipcTask.getIsotopePattern();

			if (plot == null) {
				SpectraVisualizer visualizer = SpectraVisualizer.getInstance();
				visualizer.showNewSpectrumWindow(null, ip);
			} else {
				PeakListDataSet predictedPeakDataSet = new PeakListDataSet(ip);
				PeakListDataSet rawPeakDataSet = (PeakListDataSet) plot
						.getXYPlot().getDataset(1);
				float increase = predictedPeakDataSet.getIncrease();
				if (increase < 0) {
					if (rawPeakDataSet != null) {
						increase = ((int) (rawPeakDataSet
								.getBiggestIntensity(ip.getIsotopeMass()) * 100) / 100);
					} else {
						increase = (float) Math.pow(10, 4);
					}
				}
				logger.finest("Value of increase " + increase);
				predictedPeakDataSet.setIncreaseIntensity(increase);
				plot.addPeaksDataSet(predictedPeakDataSet);
			}

			this.plot = null;
		}

		if (task.getStatus() == Task.TaskStatus.ERROR) {
			String msg = "Error while running isotope pattern calculation of "
					+ ipcTask.getFormula() + ": " + task.getErrorMessage();
			logger.severe(msg);
			desktop.displayErrorMessage(msg);
		}
	}

	public void taskStarted(Task task) {
		IsotopePatternCalculatorTask ipcTask = (IsotopePatternCalculatorTask) task;
		logger.info("Running isotope pattern calculation of "
				+ ipcTask.getFormula());

	}

}
