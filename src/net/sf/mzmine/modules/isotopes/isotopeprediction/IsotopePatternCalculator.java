/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.isotopes.isotopeprediction;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.main.mzmineclient.MZmineModule;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class IsotopePatternCalculator implements MZmineModule, ActionListener {

	private static IsotopePatternCalculator myInstance;

	private IsotopePatternCalculatorParameters parameters;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private Desktop desktop;

	/**
	 * @see net.sf.mzmine.main.mzmineclient.MZmineModule#initModule(net.sf.mzmine.main.mzmineclient.MZmineCore)
	 */
	public void initModule() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new IsotopePatternCalculatorParameters();

		myInstance = this;

		desktop
				.addMenuItem(
						MZmineMenu.ISOTOPES,
						"Isotope pattern calculator",
						"Calculation of isotope pattern using a given chemical formula",
						KeyEvent.VK_C, false, this, null);

	}

	public static IsotopePatternCalculator getInstance() {
		return myInstance;
	}

	public void setParameters(ParameterSet parameters) {
		this.parameters = (IsotopePatternCalculatorParameters) parameters;
	}

	/**
	 * @see net.sf.mzmine.main.mzmineclient.MZmineModule#getParameterSet()
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

		runTask(null);

	}

	public void showIsotopePatternCalculatorWindow(SpectraPlot plot) {

		ParameterSetupDialog dialog = new ParameterSetupDialog(
				"Please set parameter values for " + toString(), parameters);

		dialog.setVisible(true);

		if (dialog.getExitCode() != ExitCode.OK)
			return;

		runTask(plot);

	}

	public void runTask(SpectraPlot plot) {

		Task task = new IsotopePatternCalculatorTask(
				(IsotopePatternCalculatorParameters) parameters.clone(), plot);

		MZmineCore.getTaskController().addTask(task);

	}

	public String toString() {
		return "Isotope pattern calculator";
	}

}
