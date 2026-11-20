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

package io.github.mzmine.modules.visualization.intensitymap;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapFeatureTrace;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapPeak;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapLayer;
import io.github.mzmine.util.RangeUtils;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Stored data of the features opened from the feature table, for the overlays that show them: the
 * outline of the resolved ranges, and for LC-MS the data points of the stored ion trace. Several
 * features share one view, whose window spans all of them; the outline shows the part of each
 * feature.
 */
final class IntensityMapFeatureTraces {

  private IntensityMapFeatureTraces() {
  }

  /**
   * @return one trace per feature and overlay of its raw data file and m/z; none for images and
   * single mobility frames
   */
  static @NotNull List<IntensityMapFeatureTrace> of(@NotNull final List<? extends Feature> features,
      @NotNull final List<IntensityMapLayer> layers, @NotNull final IntensityMapDimensions mode) {
    final List<IntensityMapFeatureTrace> traces = new ArrayList<>();
    for (final Feature feature : features) {
      final Double mz = feature.getMZ();
      final Range<Float> rtRange = feature.getRawDataPointsRTRange();
      final Range<Double> mzRange = feature.getRawDataPointsMZRange();
      if (mz == null || rtRange == null || mzRange == null) {
        continue;
      }
      for (final IntensityMapLayer layer : layers) {
        if (layer.file() != feature.getRawDataFile() || !layer.mzRange().contains(mz)) {
          continue;
        }
        switch (mode) {
          case LC_MS, AUTOMATIC -> traces.add(
              new IntensityMapFeatureTrace(layer.id(), RangeUtils.toDoubleRange(rtRange), mzRange,
                  points(feature)));
          case RT_MOBILITY -> {
            final Range<Float> mobility = feature.getMobilityRange();
            // decision: only the outline, the mobilograms of the trace would cover the map
            if (mobility != null) {
              traces.add(new IntensityMapFeatureTrace(layer.id(), RangeUtils.toDoubleRange(rtRange),
                  RangeUtils.toDoubleRange(mobility), List.of()));
            }
          }
          case IMAGING, MOBILITY_FRAME -> {
            // the image is the feature, a frame shows one point in time
          }
        }
      }
    }
    return traces;
  }

  /**
   * @return retention time, m/z, and intensity of the stored data points with signal
   */
  private static @NotNull List<IntensityMapPeak> points(@NotNull final Feature feature) {
    final IonTimeSeries<? extends Scan> data = feature.getFeatureData();
    if (data == null) {
      return List.of();
    }
    final List<IntensityMapPeak> points = new ArrayList<>(data.getNumberOfValues());
    for (int i = 0; i < data.getNumberOfValues(); i++) {
      final double intensity = data.getIntensity(i);
      if (intensity > 0) {
        points.add(new IntensityMapPeak(data.getRetentionTime(i), data.getMZ(i), intensity));
      }
    }
    return points;
  }
}
