/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.align_join;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunctions;

/**
 * Parameters to compare spectra in join aligner
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class JoinAlignerSpectraSimilarityScoreParameters extends SimpleParameterSet {

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      "Spectral m/z tolerance",
      "Spectral m/z tolerance is used to match all signals between spectra of two compared raw files",
      0.001, 10);

  public static final IntegerParameter msLevel = new IntegerParameter("MS level",
      "Choose the MS level of the scans that should be compared. Enter \"1\" for MS1 scans or \"2\" for MS/MS scans on MS level 2",
      2, 1, 1000);

  public static final ModuleOptionsEnumComboParameter<SpectralSimilarityFunctions> similarityFunction = new ModuleOptionsEnumComboParameter<>(
      "Compare spectra similarity", "Algorithm to calculate similarity and filter matches",
      SpectralSimilarityFunctions.WEIGHTED_COSINE);

  public JoinAlignerSpectraSimilarityScoreParameters() {
    super(mzTolerance, msLevel, similarityFunction);
  }

}
