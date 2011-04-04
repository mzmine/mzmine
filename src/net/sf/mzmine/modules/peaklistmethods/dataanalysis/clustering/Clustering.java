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
package net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots.ColoringType;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.projectionplots.ProjectionPlotDataset;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

public class Clustering implements MZmineModule, ActionListener {

        private Logger logger = Logger.getLogger(this.getClass().getName());
        private Desktop desktop;
        private ClusteringParameters parameters;
        final String helpID = GUIUtils.generateHelpID(this);

        public Clustering() {

                this.desktop = MZmineCore.getDesktop();

                desktop.addMenuItem(MZmineMenu.DATAANALYSIS, "Clustering",
                        "Clustering algorithms", KeyEvent.VK_P, false, this, null);

                parameters = new ClusteringParameters();

        }

        @Override
        public String toString() {
                return "Clustering algorithms";
        }

        public ClusteringParameters getParameterSet() {
                return parameters;
        }

        public void actionPerformed(ActionEvent event) {

                PeakList selectedPeakLists[] = desktop.getSelectedPeakLists();
                if (selectedPeakLists.length != 1) {
                        desktop.displayErrorMessage("Please select a single aligned peaklist");
                        return;
                }

                if (selectedPeakLists[0].getNumberOfRows() == 0) {
                        desktop.displayErrorMessage("Selected peak list is empty");
                        return;
                }

                logger.finest("Showing Clustering algorithms setup dialog");
                ExitCode exitCode = parameters.showSetupDialog();

                if (exitCode == ExitCode.OK) {
                        logger.info("Opening new Clustering plot");

                        ProjectionPlotDataset dataset = new ClusteringTask(parameters, selectedPeakLists[0].getRawDataFiles(), selectedPeakLists[0].getRows());
                        MZmineCore.getTaskController().addTask(dataset);
                }

        }
}
