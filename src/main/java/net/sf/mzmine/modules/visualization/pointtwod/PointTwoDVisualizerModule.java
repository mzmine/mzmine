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

package net.sf.mzmine.modules.visualization.pointtwod;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineRunnableModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.ScanUtils;

import com.google.common.collect.Range;

/**
 * 2D visualizer using JFreeChart library
 */
public class PointTwoDVisualizerModule implements MZmineRunnableModule {

    private static final String MODULE_NAME = "Point 2D visualizer";
    private static final String MODULE_DESCRIPTION = "Point 2D visualizer."; // TODO

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
        RawDataFile dataFiles[] = parameters
                .getParameter(PointTwoDVisualizerParameters.dataFiles).getValue()
                .getMatchingRawDataFiles();
        ScanSelection scanSel = parameters.getParameter(
                PointTwoDVisualizerParameters.scanSelection).getValue();
        Scan scans[] = scanSel.getMatchingScans(dataFiles[0]);
        Range<Double> rtRange = ScanUtils.findRtRange(scans);

        Range<Double> mzRange = parameters.getParameter(
                PointTwoDVisualizerParameters.mzRange).getValue();
        PointTwoDVisualizerWindow newWindow = new PointTwoDVisualizerWindow(dataFiles[0],
                scans, rtRange, mzRange, parameters);

        newWindow.setVisible(true);

        return ExitCode.OK;
    }

    public static void showPoint2DVisualizerSetupDialog(RawDataFile dataFile) {
        showPoint2DVisualizerSetupDialog(dataFile, null, null);
    }

    public static void showPoint2DVisualizerSetupDialog(RawDataFile dataFile,
            Range<Double> mzRange, Range<Double> rtRange) {

        ParameterSet parameters = MZmineCore.getConfiguration()
                .getModuleParameters(PointTwoDVisualizerModule.class);

        parameters.getParameter(PointTwoDVisualizerParameters.dataFiles).setValue(
                RawDataFilesSelectionType.SPECIFIC_FILES,
                new RawDataFile[] { dataFile });

        if (rtRange != null)
            parameters.getParameter(PointTwoDVisualizerParameters.scanSelection)
                    .setValue(new ScanSelection(rtRange, 1));
        if (mzRange != null)
            parameters.getParameter(PointTwoDVisualizerParameters.mzRange).setValue(
                    mzRange);

        ExitCode exitCode = parameters.showSetupDialog(MZmineCore.getDesktop()
                .getMainWindow(), true);

        if (exitCode != ExitCode.OK)
            return;

        ScanSelection scanSel = parameters.getParameter(
                PointTwoDVisualizerParameters.scanSelection).getValue();
        Scan scans[] = scanSel.getMatchingScans(dataFile);
        rtRange = ScanUtils.findRtRange(scans);

        mzRange = parameters.getParameter(PointTwoDVisualizerParameters.mzRange)
                .getValue();

        PointTwoDVisualizerWindow newWindow = new PointTwoDVisualizerWindow(dataFile,
                scans, rtRange, mzRange, parameters);

        newWindow.setVisible(true);

    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.VISUALIZATIONRAWDATA;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return PointTwoDVisualizerParameters.class;
    }

}
