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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.tic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineRunnableModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

import com.google.common.collect.Range;

/**
 * TIC/XIC visualizer using JFreeChart library
 */
public class TICVisualizerModule implements MZmineRunnableModule {

    private static final String MODULE_NAME = "TIC/XIC visualizer";
    private static final String MODULE_DESCRIPTION = "TIC/XIC visualizer."; // TODO

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
        final RawDataFile[] dataFiles = parameters
                .getParameter(TICVisualizerParameters.DATA_FILES).getValue()
                .getMatchingRawDataFiles();
        final Range<Double> mzRange = parameters
                .getParameter(TICVisualizerParameters.MZ_RANGE).getValue();
        final ScanSelection scanSelection = parameters
                .getParameter(TICVisualizerParameters.scanSelection).getValue();
        final TICPlotType plotType = parameters
                .getParameter(TICVisualizerParameters.PLOT_TYPE).getValue();
        final Feature[] selectionPeaks = parameters
                .getParameter(TICVisualizerParameters.PEAKS).getValue();

        // Add the window to the desktop only if we actually have any raw
        // data to show.
        boolean weHaveData = false;
        for (RawDataFile dataFile : dataFiles) {
            Scan selectedScans[] = scanSelection.getMatchingScans(dataFile);
            if (selectedScans.length > 0)
                weHaveData = true;
        }

        if (weHaveData) {
            TICVisualizerWindow window = new TICVisualizerWindow(dataFiles,
                    plotType, scanSelection, mzRange, selectionPeaks,
                    ((TICVisualizerParameters) parameters).getPeakLabelMap());
            window.setVisible(true);

        } else {

            MZmineCore.getDesktop().displayErrorMessage(
                    MZmineCore.getDesktop().getMainWindow(), "No scans found");
        }

        return ExitCode.OK;
    }

    public static void setupNewTICVisualizer(final RawDataFile dataFile) {

        setupNewTICVisualizer(new RawDataFile[] { dataFile });
    }

    public static void setupNewTICVisualizer(final RawDataFile[] dataFiles) {
        setupNewTICVisualizer(
                MZmineCore.getProjectManager().getCurrentProject()
                        .getDataFiles(),
                dataFiles, new Feature[0], new Feature[0], null, null, null);
    }

    public static void setupNewTICVisualizer(final RawDataFile[] allFiles,
            final RawDataFile[] selectedFiles, final Feature[] allPeaks,
            final Feature[] selectedPeaks,
            final Map<Feature, String> peakLabels, ScanSelection scanSelection,
            final Range<Double> mzRange) {

        assert allFiles != null;

        final TICVisualizerModule myInstance = MZmineCore
                .getModuleInstance(TICVisualizerModule.class);
        final TICVisualizerParameters myParameters = (TICVisualizerParameters) MZmineCore
                .getConfiguration()
                .getModuleParameters(TICVisualizerModule.class);
        myParameters.getParameter(TICVisualizerParameters.PLOT_TYPE)
                .setValue(TICPlotType.BASEPEAK);

        if (scanSelection != null) {
            myParameters.getParameter(TICVisualizerParameters.scanSelection)
                    .setValue(scanSelection);
        }

        if (mzRange != null) {
            myParameters.getParameter(TICVisualizerParameters.MZ_RANGE)
                    .setValue(mzRange);
        }

        if (myParameters.showSetupDialog(null, true, allFiles, selectedFiles,
                allPeaks, selectedPeaks) == ExitCode.OK) {

            final TICVisualizerParameters p = (TICVisualizerParameters) myParameters
                    .cloneParameterSet();

            if (peakLabels != null) {
                p.setPeakLabelMap(peakLabels);
            }

            myInstance.runModule(
                    MZmineCore.getProjectManager().getCurrentProject(), p,
                    new ArrayList<Task>());
        }
    }

    public static void showNewTICVisualizerWindow(final RawDataFile[] dataFiles,
            final Feature[] selectionPeaks,
            final Map<Feature, String> peakLabels,
            final ScanSelection scanSelection, final TICPlotType plotType,
            final Range<Double> mzRange) {

        TICVisualizerWindow window = new TICVisualizerWindow(dataFiles,
                plotType, scanSelection, mzRange, selectionPeaks, peakLabels);
        window.setVisible(true);
    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.VISUALIZATIONRAWDATA;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return TICVisualizerParameters.class;
    }
}