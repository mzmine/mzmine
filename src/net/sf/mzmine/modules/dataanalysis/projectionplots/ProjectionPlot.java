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

package net.sf.mzmine.modules.dataanalysis.projectionplots;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.util.dialogs.ExitCode;

public class ProjectionPlot implements MZmineModule, ActionListener,
        TaskListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Desktop desktop;

    private ProjectionPlotParameters parameters;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

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

    }

    public String toString() {
        return "Projection plot analyzer";
    }

    public void setParameters(ParameterSet parameters) {
        this.parameters = (ProjectionPlotParameters) parameters;
    }

    public ProjectionPlotParameters getParameterSet() {
        return parameters;
    }

    public void actionPerformed(ActionEvent event) {

        PeakList selectedAlignedPeakLists[] = desktop.getSelectedPeakLists();
        if (selectedAlignedPeakLists.length != 1) {
            desktop.displayErrorMessage("Please select a single aligned peaklist");
            return;
        }

        if (selectedAlignedPeakLists[0].getNumberOfRows() == 0) {
            desktop.displayErrorMessage("Selected alignment result is empty");
            return;
        }

        logger.finest("Showing projection plot setup dialog");

        if ((parameters == null)
                || (selectedAlignedPeakLists[0] != parameters.getSourcePeakList())) {
            parameters = new ProjectionPlotParameters(
                    selectedAlignedPeakLists[0]);
        }

        boolean forceXYComponents = true;
        String command = event.getActionCommand();
        if (command.equals("PCA_PLOT"))
            forceXYComponents = false;

        ProjectionPlotSetupDialog setupDialog = new ProjectionPlotSetupDialog(
                selectedAlignedPeakLists[0], parameters, forceXYComponents);
        setupDialog.setVisible(true);

        if (setupDialog.getExitCode() == ExitCode.OK) {
            logger.info("Opening new projection plot");

            ProjectionPlotDataset dataset = null;

            if (command.equals("PCA_PLOT"))
                dataset = new PCADataset(parameters);

            if (command.equals("CDA_PLOT"))
                dataset = new CDADataset(parameters);

            if (command.equals("SAMMON_PLOT"))
                dataset = new SammonDataset(parameters);

            new TaskGroup(dataset, this).start();

        }

    }

    public void taskStarted(Task task) {
        logger.info("Computing projection plot");
    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {
            logger.info("Finished computing projection plot.");
        }

        if (task.getStatus() == Task.TaskStatus.ERROR) {
            String msg = "Error while computing projection plot: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);

        }

    }

}
