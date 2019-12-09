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

package io.github.mzmine.modules.dataprocessing.align_join;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.MassListParameter;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;

/**
 * Parameters to compare spectra in join aligner
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class JoinAlignerSpectraSimilarityScoreParameters
        extends SimpleParameterSet {

    public static final MassListParameter massList = new MassListParameter();

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
            "Spectral m/z tolerance",
            "Spectral m/z tolerance is used to match all signals between spectra of two compared raw files",
            0.001, 10);

    public static final IntegerParameter msLevel = new IntegerParameter(
            "MS level",
            "Choose the MS level of the scans that should be compared. Enter \"1\" for MS1 scans or \"2\" for MS/MS scans on MS level 2",
            2, 1, 1000);

    public static final ModuleComboParameter<SpectralSimilarityFunction> similarityFunction = new ModuleComboParameter<>(
            "Compare spectra similarity",
            "Algorithm to calculate similarity and filter matches",
            SpectralSimilarityFunction.FUNCTIONS);

    public JoinAlignerSpectraSimilarityScoreParameters() {
        super(new Parameter[] { massList, mzTolerance, msLevel,
                similarityFunction });
    }

}
