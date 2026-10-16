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

package io.github.mzmine.modules.dataprocessing.gapfill_gc_ei;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.PseudoSpectrumConsensusSignal;
import io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc.SpectralDeconvolutionUtils;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.MathUtils;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The signals of a feature list row that need to be found in a raw data file with a gap.
 *
 * @param row           the row to gap-fill
 * @param quantifierMz  the m/z of the row. The feature of this m/z is added to the row
 * @param rt            the retention time of the row
 * @param topMzs        the most common signals of the pseudo spectra of the row, see
 *                      {@link SpectralDeconvolutionUtils#getConsensusSignals}
 * @param detectedRange the retention time range of all detected features of the row
 * @param leftWidth     median distance from the start of the detected features to their apex
 * @param rightWidth    median distance from the apex of the detected features to their end
 */
public record GcEiGapFillTarget(@NotNull FeatureListRow row, double quantifierMz, float rt,
                                double @NotNull [] topMzs, @NotNull Range<Float> detectedRange,
                                float leftWidth, float rightWidth) {

  /**
   * @param numTopSignals number of signals to extract from the pseudo spectra
   * @param mzTolerance   tolerance to merge and match the pseudo spectra of all features
   * @return the target or null if the row has no pseudo spectrum
   */
  public static @Nullable GcEiGapFillTarget fromRow(@NotNull final FeatureListRow row,
      final int numTopSignals, @NotNull final MZTolerance mzTolerance) {
    final List<PseudoSpectrumConsensusSignal> signals = SpectralDeconvolutionUtils.getConsensusSignals(
        row, mzTolerance);
    if (signals.isEmpty()) {
      return null;
    }
    final double[] topMzs = signals.stream().limit(numTopSignals)
        .mapToDouble(PseudoSpectrumConsensusSignal::mz).toArray();

    // decision: peak shape from detected features, gap-filled features may be distorted
    List<? extends Feature> features = row.getFeatures().stream()
        .filter(f -> f.getFeatureStatus() == FeatureStatus.DETECTED).toList();
    if (features.isEmpty()) {
      features = row.getFeatures();
    }

    Range<Float> detectedRange = null;
    final double[] leftWidths = new double[features.size()];
    final double[] rightWidths = new double[features.size()];
    for (int i = 0; i < features.size(); i++) {
      final Feature feature = features.get(i);
      final Range<Float> range = feature.getRawDataPointsRTRange();
      detectedRange = detectedRange == null ? range : detectedRange.span(range);
      leftWidths[i] = feature.getRT() - range.lowerEndpoint();
      rightWidths[i] = range.upperEndpoint() - feature.getRT();
    }

    return new GcEiGapFillTarget(row, row.getAverageMZ(), row.getAverageRT(), topMzs,
        detectedRange, (float) MathUtils.calcMedian(leftWidths),
        (float) MathUtils.calcMedian(rightWidths));
  }

  /**
   * @return the quantifier followed by all top signals that are not within the m/z tolerance of
   * the quantifier
   */
  public double @NotNull [] signalsToConfirm(@NotNull final MZTolerance mzTolerance) {
    final double[] signals = new double[topMzs.length + 1];
    signals[0] = quantifierMz;
    int n = 1;
    for (final double mz : topMzs) {
      // a top signal at the quantifier m/z is confirmed by the quantifier
      if (!mzTolerance.checkWithinTolerance(mz, quantifierMz)) {
        signals[n++] = mz;
      }
    }
    return Arrays.copyOf(signals, n);
  }
}
