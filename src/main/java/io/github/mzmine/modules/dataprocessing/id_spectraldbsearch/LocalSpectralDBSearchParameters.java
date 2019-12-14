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

package io.github.mzmine.modules.dataprocessing.id_spectraldbsearch;

import java.awt.Window;
import java.util.Collection;
import javax.swing.JComponent;

import io.github.mzmine.gui.framework.listener.DelayedDocumentListener;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerComponent;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.MassListParameter;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;

public class LocalSpectralDBSearchParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    public static final FileNameParameter dataBaseFile = new FileNameParameter(
            "Spectral database file",
            "(GNPS json, MONA json, NIST msp, JCAMP-DX jdx) Name of file that contains information for peak identification");

    public static final OptionalModuleParameter<MassListDeisotoperParameters> deisotoping = new OptionalModuleParameter<>(
            "13C deisotoping", "Removes 13C isotope signals from mass lists",
            new MassListDeisotoperParameters(), true);

    public static final BooleanParameter cropSpectraToOverlap = new BooleanParameter(
            "Crop spectra to m/z overlap",
            "Crop query and library spectra to overlapping m/z range (+- spectra m/z tolerance). This is helptful if spectra were acquired with different fragmentation energies / methods.",
            true);

    public static final IntegerParameter msLevel = new IntegerParameter(
            "MS level",
            "Choose the MS level of the scans that should be compared with the database. Enter \"1\" for MS1 scans or \"2\" for MS/MS scans on MS level 2",
            2, 1, 1000);

    public static final BooleanParameter allMS2Spectra = new BooleanParameter(
            "Check all scans (only for MS2)",
            "Check all (or only most intense) MS2 scan. This option does not apply to MS1 scans.",
            false);

    public static final OptionalParameter<IntegerParameter> needsIsotopePattern = new OptionalParameter<>(
            new IntegerParameter("Min matched isotope signals",
                    "Useful for scans and libraries with isotope pattern. Minimum matched signals of 13C isotopes, distance of H and 2H or Cl isotopes. Can not be applied with deisotoping",
                    3, 0, 1000),
            false);

    public static final MZToleranceParameter mzTolerancePrecursor = new MZToleranceParameter(
            "Precursor m/z tolerance",
            "Precursor m/z tolerance is used to filter library entries", 0.001,
            5);

    public static final MassListParameter massList = new MassListParameter(
            "MassList", "MassList for either MS1 or MS/MS scans to match");

    public static final OptionalParameter<RTToleranceParameter> rtTolerance = new OptionalParameter<>(
            new RTToleranceParameter());

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
            "Spectral m/z tolerance",
            "Spectral m/z tolerance is used to match all signals in the query and library spectra (usually higher than precursor m/z tolerance)",
            0.0015, 10);

    public static final DoubleParameter noiseLevel = new DoubleParameter(
            "Minimum ion intensity",
            "Signals below this level will be filtered away from mass lists",
            MZmineCore.getConfiguration().getIntensityFormat(), 0d);

    public static final IntegerParameter minMatch = new IntegerParameter(
            "Minimum  matched signals",
            "Minimum number of matched signals in masslist and spectral library entry (within mz tolerance)",
            4);

    public static final ModuleComboParameter<SpectralSimilarityFunction> similarityFunction = new ModuleComboParameter<>(
            "Similarity",
            "Algorithm to calculate similarity and filter matches",
            SpectralSimilarityFunction.FUNCTIONS);

    /**
     * for SelectedRowsParameters
     * 
     * @param parameters
     */
    protected LocalSpectralDBSearchParameters(Parameter[] parameters) {
        super(parameters);
    }

    public LocalSpectralDBSearchParameters() {
        super(new Parameter[] { peakLists, massList, dataBaseFile, msLevel,
                allMS2Spectra, mzTolerancePrecursor, noiseLevel, deisotoping,
                needsIsotopePattern, cropSpectraToOverlap, mzTolerance,
                rtTolerance, minMatch, similarityFunction });
    }

    @Override
    public boolean checkParameterValues(Collection<String> errorMessages) {
        boolean check = super.checkParameterValues(errorMessages);

        // not both isotope and deisotope
        boolean isotope = !getParameter(deisotoping).getValue()
                || !getParameter(needsIsotopePattern).getValue();
        if (!isotope) {
            errorMessages.add(
                    "Choose only one of \"deisotoping\" and \"need isotope pattern\" at the same time");
            return false;
        }
        return check;
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
        if ((getParameters() == null) || (getParameters().length == 0))
            return ExitCode.OK;
        ParameterSetupDialog dialog = new ParameterSetupDialog(parent,
                valueCheckRequired, this);

        int level = getParameter(msLevel).getValue() == null ? 2
                : getParameter(msLevel).getValue();

        IntegerComponent msLevelComp = (IntegerComponent) dialog
                .getComponentForParameter(msLevel);
        JComponent mzTolPrecursor = dialog
                .getComponentForParameter(mzTolerancePrecursor);
        mzTolPrecursor.setEnabled(level > 1);
        msLevelComp.addDocumentListener(new DelayedDocumentListener(e -> {
            try {
                int level2 = Integer.parseInt(msLevelComp.getText());
                mzTolPrecursor.setEnabled(level2 > 1);
            } catch (Exception ex) {
                // do nothing user might be still typing
                mzTolPrecursor.setEnabled(false);
            }
        }));

        dialog.setVisible(true);
        return dialog.getExitCode();
    }

}
