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

package net.sf.mzmine.modules.visualization.spectra;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineRunnableModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

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

        int scanNumber = parameters.getParameter(
                SpectraVisualizerParameters.scanNumber).getValue();

        showNewSpectrumWindow(dataFiles[0], scanNumber);

        return ExitCode.OK;
    }

    public static SpectraVisualizerWindow showNewSpectrumWindow(
            RawDataFile dataFile, int scanNumber) {
        return showNewSpectrumWindow(dataFile, scanNumber, 
                null, null, null, null);
    }

    public static SpectraVisualizerWindow showNewSpectrumWindow(
            RawDataFile dataFile, int scanNumber, Feature peak) {
        return showNewSpectrumWindow(dataFile, scanNumber, peak, 
                null, null, null);
    }

    public static SpectraVisualizerWindow showNewSpectrumWindow(
            RawDataFile dataFile, int scanNumber, IsotopePattern detectedPattern) {
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
                dataFile);
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