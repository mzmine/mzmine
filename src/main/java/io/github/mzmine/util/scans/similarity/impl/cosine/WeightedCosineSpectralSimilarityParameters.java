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

package io.github.mzmine.util.scans.similarity.impl.cosine;

import java.text.DecimalFormat;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.util.scans.similarity.Weights;

/**
 * 
 */
public class WeightedCosineSpectralSimilarityParameters
        extends SimpleParameterSet {

    public static final ComboParameter<Weights> weight = new ComboParameter<>(
            "Weights", "Weights for m/z and intensity", Weights.VALUES,
            Weights.MASSBANK);
    public static final DoubleParameter minCosine = new DoubleParameter(
            "Minimum  cos similarity",
            "Minimum cosine similarity. (All signals in the masslist against the spectral library entry. "
                    + "Considers only signals which were found in both the masslist and the library entry)",
            new DecimalFormat("0.000"), 0.7);

    public static final BooleanParameter removeUnmatched = new BooleanParameter(
            "Remove unmatched signals",
            "CAUTION: Remove unmatched signals before cosine calculation. (Does only use signals which are present in both the query and library spectrum) Leeds to higher cosine similarity but also to a higher false positive rate. Especially good for noisy library or query spectra.",
            false);

    public WeightedCosineSpectralSimilarityParameters() {
        super(new Parameter[] { weight, minCosine, removeUnmatched });
    }

}
