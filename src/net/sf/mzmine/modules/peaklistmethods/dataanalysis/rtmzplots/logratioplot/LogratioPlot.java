/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots.logratioplot;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots.RTMZAnalyzerWindow;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;

import org.jfree.data.xy.AbstractXYZDataset;

public class LogratioPlot implements MZmineModule, ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private Desktop desktop;

	private ParameterSet parameters;

	final String helpID = GUIUtils.generateHelpID(this);

	public LogratioPlot() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new LogratioParameters();

		desktop.addMenuItem(
				MZmineMenu.DATAANALYSIS,
				"Logratio analysis",
				"Plots a difference of each peak between two groups of samples",
				KeyEvent.VK_L, false, this, "LOGRATIO_PLOT");

	}

	public String toString() {
		return "RT vs m/z analyzer";
	}

	public ParameterSet getParameterSet() {
		return parameters;
	}

	public void actionPerformed(ActionEvent event) {

		PeakList[] alignedPeakLists = desktop.getSelectedPeakLists();

		if (alignedPeakLists.length == 0) {
			desktop.displayErrorMessage("Please select at least one aligned peak list.");
		}

		for (PeakList pl : alignedPeakLists) {

			if (pl.getRawDataFiles().length < 2) {
				desktop.displayErrorMessage("Alignment " + pl.toString()
						+ " contains less than two data files");
				continue;
			}

			// Show opened raw data file selection and parameter setup dialog
			ExitCode exitCode = parameters.showSetupDialog();

			if (exitCode != ExitCode.OK) {
				logger.info("Analysis cancelled.");
				return;
			}

			// Create dataset & paint scale
			AbstractXYZDataset dataset = new LogratioDataset(pl, parameters);
			InterpolatingLookupPaintScale paintScale = new InterpolatingLookupPaintScale();
			paintScale.add(-1.00, new Color(0, 255, 0));
			paintScale.add(0.00, new Color(0, 0, 0));
			paintScale.add(1.00, new Color(255, 0, 0));

			// Create & show window
			RTMZAnalyzerWindow window = new RTMZAnalyzerWindow(desktop,
					dataset, pl, paintScale);
			window.setVisible(true);

		}

	}

}
