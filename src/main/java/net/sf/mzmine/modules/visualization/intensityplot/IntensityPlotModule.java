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

package net.sf.mzmine.modules.visualization.intensityplot;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineRunnableModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsSelectionType;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 * Peak intensity plot module
 */
public class IntensityPlotModule implements MZmineRunnableModule {

    private static final String MODULE_NAME = "Peak intensity plot";
    private static final String MODULE_DESCRIPTION = "Peak intensity plot."; // TODO

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
        IntensityPlotWindow newFrame = new IntensityPlotWindow(parameters);
        newFrame.setVisible(true);
        return ExitCode.OK;
    }

    public static void showIntensityPlot(@Nonnull MZmineProject project,
            PeakList peakList, PeakListRow rows[]) {

        ParameterSet parameters = MZmineCore.getConfiguration()
                .getModuleParameters(IntensityPlotModule.class);

        parameters.getParameter(IntensityPlotParameters.peakList).setValue(
                PeakListsSelectionType.SPECIFIC_PEAKLISTS,
                new PeakList[] { peakList });

        parameters.getParameter(IntensityPlotParameters.dataFiles)
                .setChoices(peakList.getRawDataFiles());

        parameters.getParameter(IntensityPlotParameters.dataFiles)
                .setValue(peakList.getRawDataFiles());

        parameters.getParameter(IntensityPlotParameters.selectedRows)
                .setValue(rows);

        UserParameter<?, ?> projectParams[] = project.getParameters();
        Object xAxisSources[] = new Object[projectParams.length + 1];
        xAxisSources[0] = IntensityPlotParameters.rawDataFilesOption;

        for (int i = 0; i < projectParams.length; i++) {
            xAxisSources[i + 1] = new ParameterWrapper(projectParams[i]);
        }

        parameters.getParameter(IntensityPlotParameters.xAxisValueSource)
                .setChoices(xAxisSources);

        ExitCode exitCode = parameters.showSetupDialog(null, true);

        if (exitCode == ExitCode.OK) {
            PeakListRow selectedRows[] = parameters
                    .getParameter(IntensityPlotParameters.selectedRows)
                    .getMatchingRows(peakList);
            if (selectedRows.length == 0) {
                MZmineCore.getDesktop().displayErrorMessage(null,
                        "No rows selected");
                return;
            }

            IntensityPlotWindow newFrame = new IntensityPlotWindow(
                    parameters.cloneParameterSet());
            newFrame.setVisible(true);
        }

    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.VISUALIZATIONPEAKLIST;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return IntensityPlotParameters.class;
    }

}