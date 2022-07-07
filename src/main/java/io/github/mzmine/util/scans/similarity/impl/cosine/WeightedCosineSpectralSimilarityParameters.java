/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.util.scans.similarity.impl.cosine;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import io.github.mzmine.util.scans.similarity.Weights;
import java.text.DecimalFormat;
import javafx.collections.FXCollections;

/**
 *
 */
public class WeightedCosineSpectralSimilarityParameters extends SimpleParameterSet {

  public static final ComboParameter<Weights> weight =
      new ComboParameter<>("Weights", "Weights for m/z and intensity",
          FXCollections.observableArrayList(Weights.VALUES), Weights.MASSBANK);
  public static final DoubleParameter minCosine = new DoubleParameter("Minimum  cos similarity",
      "Minimum cosine similarity. (All signals in the masslist against the spectral library entry. "
          + "Considers only signals which were found in both the masslist and the library entry)",
      new DecimalFormat("0.000"), 0.7);

  public static final ComboParameter<HandleUnmatchedSignalOptions> handleUnmatched =
      new ComboParameter<>("Handle unmatched signals",
          "Options to handle signals that only occur in one scan. (Usually - replace intensities of missing pairs to zero for a negative weight)",
          HandleUnmatchedSignalOptions.values(),
          HandleUnmatchedSignalOptions.KEEP_ALL_AND_MATCH_TO_ZERO);

  public WeightedCosineSpectralSimilarityParameters() {
    super(new Parameter[] {weight, minCosine, handleUnmatched});
  }

}
