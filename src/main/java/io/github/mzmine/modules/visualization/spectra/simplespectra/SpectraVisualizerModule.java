/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra;

import java.util.Collection;

import javax.annotation.Nonnull;

import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

/**
 * Spectrum visualizer
 */
public class SpectraVisualizerModule implements MZmineRunnableModule {

    private static final String MODULE_NAME = "Spectra visualizer";
    private static final String MODULE_DESCRIPTION = "Spectra visualizer."; // TODO

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
                .getParameter(SpectraVisualizerParameters.dataFiles).getValue()
                .getMatchingRawDataFiles();

        int scanNumber = parameters
                .getParameter(SpectraVisualizerParameters.scanNumber)
                .getValue();

        showNewSpectrumWindow(dataFiles[0], scanNumber);

        return ExitCode.OK;
    }

    public static SpectraVisualizerWindow showNewSpectrumWindow(
            RawDataFile dataFile, int scanNumber) {
        return showNewSpectrumWindow(dataFile, scanNumber, null, null, null,
                null);
    }

    public static SpectraVisualizerWindow showNewSpectrumWindow(
            RawDataFile dataFile, int scanNumber, Feature peak) {
        return showNewSpectrumWindow(dataFile, scanNumber, peak, null, null,
                null);
    }

    public static SpectraVisualizerWindow showNewSpectrumWindow(
            RawDataFile dataFile, int scanNumber,
            IsotopePattern detectedPattern) {
        return showNewSpectrumWindow(dataFile, scanNumber, null,
                detectedPattern, null, null);
    }

    public static SpectraVisualizerWindow showNewSpectrumWindow(
            RawDataFile dataFile, int scanNumber, Feature peak,
            IsotopePattern detectedPattern, IsotopePattern predictedPattern) {
        return showNewSpectrumWindow(dataFile, scanNumber, peak,
                detectedPattern, predictedPattern, null);
    }

    public static SpectraVisualizerWindow showNewSpectrumWindow(
            RawDataFile dataFile, int scanNumber, Feature peak,
            IsotopePattern detectedPattern, IsotopePattern predictedPattern,
            IsotopePattern spectrum) {

        Scan scan = dataFile.getScan(scanNumber);

        if (scan == null) {
            MZmineCore.getDesktop().displayErrorMessage(
                    MZmineCore.getDesktop().getMainWindow(),
                    "Raw data file " + dataFile + " does not contain scan #"
                            + scanNumber);
            return null;
        }

        SpectraVisualizerWindow newWindow = new SpectraVisualizerWindow(
                dataFile, true);
        newWindow.loadRawData(scan);

        if (peak != null)
            newWindow.loadSinglePeak(peak);

        if (detectedPattern != null)
            newWindow.loadIsotopes(detectedPattern);

        if (predictedPattern != null)
            newWindow.loadIsotopes(predictedPattern);

        if (spectrum != null)
            newWindow.loadSpectrum(spectrum);

        newWindow.setVisible(true);

        return newWindow;

    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.VISUALIZATIONRAWDATA;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return SpectraVisualizerParameters.class;
    }

}
