/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.scatterplot;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineRunnableModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class ScatterPlotVisualizerModule implements MZmineRunnableModule {

    private static final String MODULE_NAME = "Scatter plot";
    private static final String MODULE_DESCRIPTION = "Scatter plot."; // TODO

    @Override
    public @Nonnull String getName() {
        return MODULE_NAME;
    }

    @Override
    public @Nonnull String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @Override
    @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {

        PeakList peakLists[] = parameters
                .getParameter(ScatterPlotParameters.peakLists).getValue()
                .getMatchingPeakLists();
        if ((peakLists == null) || (peakLists.length != 1)) {
            MZmineCore.getDesktop().displayErrorMessage(
                    MZmineCore.getDesktop().getMainWindow(),
                    "Please select a single aligned peak list");
            return ExitCode.ERROR;
        }

        PeakList peakList = peakLists[0];
        if (peakList.getNumberOfRawDataFiles() < 2) {
            MZmineCore
                    .getDesktop()
                    .displayErrorMessage(
                            MZmineCore.getDesktop().getMainWindow(),
                            "There is only one raw data file in the selected "
                                    + "peak list, it is necessary at least two for comparison");
            return ExitCode.ERROR;
        }

        ScatterPlotWindow newWindow = new ScatterPlotWindow(peakList);
        newWindow.setVisible(true);

        return ExitCode.OK;
    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.VISUALIZATIONPEAKLIST;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return ScatterPlotParameters.class;
    }

    public static void showNewScatterPlotWindow(PeakList peakList) {

        if (peakList.getNumberOfRawDataFiles() < 2) {
            MZmineCore
                    .getDesktop()
                    .displayErrorMessage(
                            MZmineCore.getDesktop().getMainWindow(),
                            "There is only one raw data file in the selected "
                                    + "peak list, it is necessary at least two for comparison");
            return;
        }

        ScatterPlotWindow newWindow = new ScatterPlotWindow(peakList);
        newWindow.setVisible(true);

    }

}