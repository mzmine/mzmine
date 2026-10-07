/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * This class contains methods for MS/MS lipid identifications
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class MSMSLipidTools {

  /**
   * Calculate the explained intensity of MS/MS signals by lipid fragmentation rules in %
   */
  public @NotNull Double calculateMsMsScore(final DataPoint @NotNull [] massList,
      final @NotNull Set<LipidFragment> annotatedFragments, final @NotNull Double precursor,
      final @NotNull MZTolerance mzTolRangeMSMS) {
    final boolean includePrecursor = annotatedFragments.stream().anyMatch(
        lipidFragment -> lipidFragment.getRuleType() == LipidFragmentationRuleType.PRECURSOR);
    final double intensityAllSignals =
        includePrecursor ? Arrays.stream(massList).mapToDouble(DataPoint::getIntensity).sum()
            : Arrays.stream(massList)
                .filter(dp -> !mzTolRangeMSMS.checkWithinTolerance(dp.getMZ(), precursor))
                .mapToDouble(DataPoint::getIntensity).sum();

    // Multiple fragmentation rules may annotate the same observed data point. Explained intensity
    // describes spectrum signals, so each observed m/z must only contribute once to the score.
    final Map<Double, Double> matchedIntensityByMz = annotatedFragments.stream()
        .map(LipidFragment::getDataPoint)
        .collect(Collectors.toMap(DataPoint::getMZ, DataPoint::getIntensity, Double::max));
    final double intensityMatchedSignals = matchedIntensityByMz.values().stream()
        .mapToDouble(Double::doubleValue).sum();
    return intensityMatchedSignals / intensityAllSignals;
  }

}
