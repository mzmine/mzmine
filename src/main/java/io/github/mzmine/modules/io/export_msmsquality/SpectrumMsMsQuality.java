/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.modules.tools.msmsscore.MSMSScore;
import io.github.mzmine.util.io.CSVUtils;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public record SpectrumMsMsQuality(int rowId, float purity, MSMSScore score, int numPeaks,
                                  float spectralEntropy, float normalizedEntropy,
                                  float weightedEntropy, float normalizedWeightedEntropy,
                                  FeatureAnnotation annotation, @Nullable Double tic,
                                  @Nullable Double bpi, double precursorIntensity,
                                  List<String> spotNames, String mobRange, String precursorMz,
                                  List<Float> ce) {

  public static String getHeader(CharSequence separator) {
    return String.join(separator, "row_id", "compound", "adduct", "purity_score",
        "explained_intensity", "explained_peaks", "num_peaks", "spectral_entropy",
        "normalized_entropy", "weighted_entropy", "normalized_weighted_entropy", "tic_ms2",
        "bpi_ms2", "precursor_intensity", "spots", "mobility_range", "precursor_mz",
        "collision_energy");
  }

  public String toCsvString(CharSequence separator) {
    return Stream.of(Integer.toString(rowId),
            annotation != null ? annotation.getCompoundName() : "",
            annotation != null && annotation.getAdductType() != null ? annotation.getAdductType()
                .toString(false) : "", Float.toString(purity),
            (score.explainedIntensity() >= 0 ? String.valueOf(score.explainedIntensity()) : "0"),
            (score.explainedSignals() >= 0 ? String.valueOf(score.explainedSignals()) : "0"),
            Integer.toString(numPeaks), Float.toString(spectralEntropy),
            Float.toString(normalizedEntropy), Float.toString(weightedEntropy),
            Float.toString(normalizedWeightedEntropy), tic != null ? "%.0f".formatted(tic) : "0",
            bpi != null ? "%.0f".formatted(bpi) : "0", Double.toString(precursorIntensity),
            spotNames.stream().collect(Collectors.joining(", ")), mobRange, precursorMz,
            ce.stream().map("%.1f"::formatted).collect(Collectors.joining(", ")))
        .map(input -> CSVUtils.escape(input, separator.toString()))
        .collect(Collectors.joining(separator));
  }
}
