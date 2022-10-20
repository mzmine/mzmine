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

package io.github.mzmine.modules.io.export_msmsquality;

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;

public record SpectrumMsMsQuality(float chimerity, MSMSScore explainedIntensityScore,
                                  MSMSScore explainedPeaksScore, int numPeaks,
                                  float spectralEntropy, float normalizedEntropy,
                                  float weightedEntropy, float normalizedWeightedEntropy,
                                  CompoundDBAnnotation annotation) {

  public String toCsvString(char separator) {
    return (annotation != null ? annotation.getCompoundName() : "") + separator //
        + chimerity + separator //
        + explainedIntensityScore.getScore() + separator //
        + explainedPeaksScore.getScore() + separator //
        + numPeaks + separator //
        + spectralEntropy + separator //
        + normalizedEntropy + separator //
        + weightedEntropy + separator //
        + normalizedWeightedEntropy + separator; //
  }
}
