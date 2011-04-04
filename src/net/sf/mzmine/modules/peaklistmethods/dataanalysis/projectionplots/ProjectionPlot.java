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
package net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

public class ProjectionPlot implements MZmineModule, ActionListener {

        private Logger logger = Logger.getLogger(this.getClass().getName());
        private Desktop desktop;
        private ProjectionPlotParameters parameters;
        final String helpID = GUIUtils.generateHelpID(this);

        /**
         */
        public ProjectionPlot() {

                this.desktop = MZmineCore.getDesktop();

                desktop.addMenuItem(MZmineMenu.DATAANALYSIS,
                        "Principal component analysis (PCA)",
                        "Principal component analysis", KeyEvent.VK_P, false, this,
                        "PCA_PLOT");

                desktop.addMenuItem(MZmineMenu.DATAANALYSIS,
                        "Curvilinear distance analysis (CDA)",
                        "Curvilinear distance analysis", KeyEvent.VK_C, false, this,
                        "CDA_PLOT");

                desktop.addMenuItem(MZmineMenu.DATAANALYSIS, "Sammon's projection",
                        "Sammon's projection", KeyEvent.VK_S, false, this,
                        "SAMMON_PLOT");

                parameters = new ProjectionPlotParameters();
        }

        public String toString() {
                return "Projection plot analyzer";
        }

        public ProjectionPlotParameters getParameterSet() {
                return parameters;
        }

        public void actionPerformed(ActionEvent event) {

                PeakList selectedPeakLists[] = desktop.getSelectedPeakLists();
                if (selectedPeakLists.length != 1) {
                        desktop.displayErrorMessage("Please select a single aligned peaklist");
                        return;
                }

                if (selectedPeakLists[0].getNumberOfRows() == 0) {
                        desktop.displayErrorMessage("Selected alignment result is empty");
                        return;
                }

                logger.finest("Showing projection plot setup dialog");

                String command = event.getActionCommand();
                if (!command.equals("PCA_PLOT")) {
                        parameters.getParameter(ProjectionPlotParameters.xAxisComponent).setChoices(new Integer[]{1});
                        parameters.getParameter(ProjectionPlotParameters.yAxisComponent).setChoices(new Integer[]{2});
                }
                parameters.getParameter(ProjectionPlotParameters.dataFiles).setChoices(selectedPeakLists[0].getRawDataFiles());
                parameters.getParameter(ProjectionPlotParameters.rows).setChoices(selectedPeakLists[0].getRows());

                parameters.getParameter(ProjectionPlotParameters.dataFiles).setValue(selectedPeakLists[0].getRawDataFiles());
                parameters.getParameter(ProjectionPlotParameters.rows).setValue(selectedPeakLists[0].getRows());

                ExitCode exitCode = parameters.showSetupDialog();

                if (exitCode == ExitCode.OK) {
                        logger.info("Opening new projection plot");

                        ProjectionPlotDataset dataset = null;

                        if (command.equals("PCA_PLOT")) {
                                dataset = new PCADataset(selectedPeakLists[0], parameters);
                        }

                        if (command.equals("CDA_PLOT")) {
                                dataset = new CDADataset(selectedPeakLists[0], parameters);
                        }

                        if (command.equals("SAMMON_PLOT")) {
                                dataset = new SammonDataset(selectedPeakLists[0], parameters);
                        }

                        MZmineCore.getTaskController().addTask(dataset);

                }

        }
}
