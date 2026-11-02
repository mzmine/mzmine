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

package io.github.mzmine.modules.visualization.intensitymap.data;

import com.google.common.collect.Range;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The stored data of a feature, drawn over the raw data of its overlay in the 3D view: the outline
 * of its ranges on the floor and the data points of its ion trace.
 *
 * @param seriesId overlay of the feature
 * @param xRange   extent along x, e.g. the retention time range
 * @param yRange   extent along y, e.g. the m/z or mobility range
 * @param points   data points of the stored ion trace in display coordinates, empty to only outline
 *                 the feature
 */
public record IntensityMapFeatureTrace(@NotNull String seriesId, @NotNull Range<Double> xRange,
                                       @NotNull Range<Double> yRange,
                                       @NotNull List<IntensityMapPeak> points) {

  public IntensityMapFeatureTrace {
    points = List.copyOf(points);
  }
}
