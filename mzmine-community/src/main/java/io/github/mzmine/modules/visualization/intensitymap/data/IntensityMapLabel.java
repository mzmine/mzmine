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
import org.jetbrains.annotations.Nullable;

/**
 * A label of a position in the data, e.g. a feature apex from a feature list.
 *
 * @param seriesId    id of the overlay the label belongs to
 * @param x           data coordinate along x
 * @param y           data coordinate along y
 * @param xRange      extent of the labeled peak along x, e.g. its retention time range
 * @param yRange      extent of the labeled peak along y
 * @param name        annotation or compound name for searching, null if unknown
 * @param intensity   intensity at the position, stronger labels are placed first
 * @param text        short text next to the marker
 * @param description full text for the hover readout
 * @param annotated   true if the label names an annotation, not only a mass
 * @param secondary   placed after all primary labels, e.g. further ions of a compound
 * @param group       key of the group of the feature, e.g. its compound; null if not grouped
 * @param isotopes    further isotope signals of the feature, e.g. of its isotope pattern
 * @param grouped     signals grouped with the feature without rows of their own, e.g. the ions of a
 *                    GC-EI deconvolution
 */
public record IntensityMapLabel(@NotNull String seriesId, double x, double y,
                                @NotNull Range<Double> xRange, @NotNull Range<Double> yRange,
                                double intensity, @Nullable String name, @NotNull String text,
                                @NotNull String description, boolean annotated, boolean secondary,
                                @Nullable String group, @NotNull List<IntensityMapPeak> isotopes,
                                @NotNull List<IntensityMapPeak> grouped) {

  /**
   * @return the labeled apex
   */
  public @NotNull IntensityMapPeak apex() {
    return new IntensityMapPeak(x, y, intensity);
  }
}
