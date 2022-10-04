/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
