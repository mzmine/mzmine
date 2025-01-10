/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.featuredata;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.otherdetectors.MrmTransition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MrmUtils {

  /**
   * Extracts the {@link IonTimeSeries} in the specific rt range of the feature.
   */
  public static @Nullable IonTimeSeries<? extends Scan> getChromInRtRange(
      @NotNull FeatureListRow row, @Nullable RawDataFile file, @Nullable MrmTransition t) {
    if (file == null) {
      return null;
    }

    return getChromInRtRange((ModularFeature) row.getFeature(file), t);
  }

  /**
   * Extracts the {@link IonTimeSeries} in the specific rt range of the feature.
   */
  private static @Nullable IonTimeSeries<? extends Scan> getChromInRtRange(
      @Nullable ModularFeature feature, @Nullable MrmTransition t) {
    if (feature == null || feature.getFeatureStatus() == FeatureStatus.UNKNOWN || t == null
        || Range.singleton(0f).equals(feature.getRawDataPointsRTRange())) {
      return null;
    }

    final IonTimeSeries<? extends Scan> chrom = t.chromatogram();
    // sub series is pretty optimised, so we don't bother with a specific method
    final IonTimeSeries<? extends Scan> chromRange = chrom.subSeries(null,
        feature.getRawDataPointsRTRange().lowerEndpoint(),
        feature.getRawDataPointsRTRange().upperEndpoint());

    if (chromRange.getNumberOfValues() == 0) {
      return null;
    }
    return chromRange;
  }
}
