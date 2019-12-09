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

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase;

import java.awt.Window;
import java.util.Collection;
import javax.swing.JComponent;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.MassListParameter;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameterComponent;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;

/**
 * Module to compare single spectra with spectral databases
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class SpectraIdentificationSpectralDatabaseParameters
        extends SimpleParameterSet {

    public static final FileNameParameter dataBaseFile = new FileNameParameter(
            "Database file",
            "(GNPS json, MONA json, NIST msp, JCAMP-DX jdx) Name of file that contains information for peak identification");

    public static final MassListParameter massList = new MassListParameter();

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
            "Spectral m/z tolerance",
            "Tolerance to match spectral signals in the query and library spectra (usually higher than precursor m/z tolerance (if used))",
            0.0015, 10);

    public static final OptionalParameter<DoubleParameter> usePrecursorMZ = new OptionalParameter<>(
            new DoubleParameter("Use precursor m/z",
                    "Use precursor m/z as a filter. Precursor m/z of library entry and this scan need to be within m/z tolerance. Entries without precursor m/z are skipped.",
                    MZmineCore.getConfiguration().getMZFormat(), 0d));
    public static final MZToleranceParameter mzTolerancePrecursor = new MZToleranceParameter(
            "Precursor m/z tolerance",
            "Precursor m/z tolerance is used to filter library entries", 0.001,
            5);

    public static final DoubleParameter noiseLevel = new DoubleParameter(
            "Minimum ion intensity",
            "Signals below this level will be filtered away from mass lists",
            MZmineCore.getConfiguration().getIntensityFormat(), 0d);

    public static final IntegerParameter minMatch = new IntegerParameter(
            "Minimum  matched signals",
            "Minimum number of matched signals in spectra and spectral library entry (within mz tolerance)",
            20);

    public static final OptionalParameter<IntegerParameter> needsIsotopePattern = new OptionalParameter<>(
            new IntegerParameter("Min matched isotope signals",
                    "Useful for scans and libraries with isotope pattern. Minimum matched signals of 13C isotopes, distance of H and 2H or Cl isotopes. Can not be applied with deisotoping",
                    3, 0, 1000),
            false);

    public static final OptionalModuleParameter<MassListDeisotoperParameters> deisotoping = new OptionalModuleParameter<>(
            "13C deisotoping", "Removes 13C isotope signals from mass lists",
            new MassListDeisotoperParameters(), true);

    public static final BooleanParameter cropSpectraToOverlap = new BooleanParameter(
            "Crop spectra to m/z overlap",
            "Crop query and library spectra to overlapping m/z range (+- spectra m/z tolerance). This is helptful if spectra were acquired with different fragmentation energies / methods.",
            true);

    public static final ModuleComboParameter<SpectralSimilarityFunction> similarityFunction = new ModuleComboParameter<>(
            "Similarity",
            "Algorithm to calculate similarity and filter matches",
            SpectralSimilarityFunction.FUNCTIONS);

    public SpectraIdentificationSpectralDatabaseParameters() {
        super(new Parameter[] { massList, dataBaseFile, usePrecursorMZ,
                mzTolerancePrecursor, noiseLevel, deisotoping,
                needsIsotopePattern, cropSpectraToOverlap, mzTolerance,
                minMatch, similarityFunction });
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

        // only enable precursor mz tolerance if precursor mz is used
        OptionalParameterComponent usePreComp = (OptionalParameterComponent) dialog
                .getComponentForParameter(usePrecursorMZ);
        JComponent mzTolPrecursor = dialog
                .getComponentForParameter(mzTolerancePrecursor);
        mzTolPrecursor.setEnabled(getParameter(usePrecursorMZ).getValue());
        usePreComp.addItemListener(e -> {
            mzTolPrecursor.setEnabled(usePreComp.isSelected());
        });

        dialog.setVisible(true);
        return dialog.getExitCode();
    }

    public ExitCode showSetupDialog(Scan scan, Window parent,
            boolean valueCheckRequired) {
        // set precursor mz to parameter if MS2 scan
        // otherwise leave the value to the one specified before
        if (scan.getPrecursorMZ() != 0)
            this.getParameter(usePrecursorMZ).getEmbeddedParameter()
                    .setValue(scan.getPrecursorMZ());
        else
            this.getParameter(usePrecursorMZ).setValue(false);

        return this.showSetupDialog(parent, valueCheckRequired);
    }
}
