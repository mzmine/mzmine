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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapFeatureTrace;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapLayer;
import java.util.List;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class IntensityMapFeatureTracesTest {

  @Test
  void lcFeaturesGetOutlineAndStoredPoints() {
    final RawDataFile file = mock(RawDataFile.class);
    final Feature feature = feature(file);
    final List<IntensityMapLayer> layers = List.of(
        new IntensityMapLayer("in", file, Range.closed(200d, 201d), Color.BLACK, false),
        new IntensityMapLayer("other m/z", file, Range.closed(300d, 301d), Color.BLACK, false),
        new IntensityMapLayer("other file", mock(RawDataFile.class), Range.closed(200d, 201d),
            Color.BLACK, false));

    final List<IntensityMapFeatureTrace> traces = IntensityMapFeatureTraces.of(List.of(feature),
        layers, IntensityMapDimensions.LC_MS);

    assertEquals(1, traces.size());
    final IntensityMapFeatureTrace trace = traces.getFirst();
    assertEquals("in", trace.seriesId());
    assertEquals(Range.closed(1.0, 2.0), trace.xRange());
    assertEquals(Range.closed(200.4, 200.6), trace.yRange());
    // the gap-filled zero is left out
    assertEquals(2, trace.points().size());
    assertEquals(1.5, trace.points().get(1).x(), 1e-6);
    assertEquals(200.51, trace.points().get(1).y(), 1e-9);
    assertEquals(80, trace.points().get(1).intensity());
  }

  @Test
  void mobilityFeaturesAreOutlinedAndImagesNot() {
    final RawDataFile file = mock(RawDataFile.class);
    final Feature feature = feature(file);
    when(feature.getMobilityRange()).thenReturn(Range.closed(0.9f, 1.1f));
    final List<IntensityMapLayer> layers = List.of(
        new IntensityMapLayer("in", file, Range.closed(200d, 201d), Color.BLACK, false));

    final List<IntensityMapFeatureTrace> mobility = IntensityMapFeatureTraces.of(List.of(feature),
        layers, IntensityMapDimensions.RT_MOBILITY);

    assertEquals(1, mobility.size());
    assertEquals(0.9, mobility.getFirst().yRange().lowerEndpoint(), 1e-6);
    assertTrue(mobility.getFirst().points().isEmpty());
    assertTrue(
        IntensityMapFeatureTraces.of(List.of(feature), layers, IntensityMapDimensions.IMAGING)
            .isEmpty());
  }

  @SuppressWarnings("unchecked")
  private static @NotNull Feature feature(@NotNull final RawDataFile file) {
    final Feature feature = mock(Feature.class);
    when(feature.getRawDataFile()).thenReturn(file);
    when(feature.getMZ()).thenReturn(200.5);
    when(feature.getRawDataPointsRTRange()).thenReturn(Range.closed(1f, 2f));
    when(feature.getRawDataPointsMZRange()).thenReturn(Range.closed(200.4, 200.6));
    final IonTimeSeries<Scan> data = mock(IonTimeSeries.class);
    when(data.getNumberOfValues()).thenReturn(3);
    when(data.getRetentionTime(0)).thenReturn(1f);
    when(data.getRetentionTime(1)).thenReturn(1.5f);
    when(data.getRetentionTime(2)).thenReturn(2f);
    when(data.getMZ(0)).thenReturn(200.49);
    when(data.getMZ(1)).thenReturn(200.51);
    when(data.getMZ(2)).thenReturn(0d);
    when(data.getIntensity(0)).thenReturn(20d);
    when(data.getIntensity(1)).thenReturn(80d);
    when(data.getIntensity(2)).thenReturn(0d);
    when(feature.getFeatureData()).thenReturn((IonTimeSeries) data);
    return feature;
  }
}
