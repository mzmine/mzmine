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

package io.github.mzmine.modules.visualization.intensitymap.plot;

import javafx.scene.Node;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import org.jetbrains.annotations.NotNull;

/**
 * Small toolbar icons for profile views that the icon font does not provide. Colors follow the
 * theme text color.
 */
final class IntensityMapIcons {

  private IntensityMapIcons() {
  }

  /**
   * @return a Gaussian peak shape, for chromatograms and mobilograms
   */
  static @NotNull Node peak() {
    final SVGPath path = new SVGPath();
    path.setContent("M1 15 H3 C5.5 15 6 2.5 8 2.5 C10 2.5 10.5 15 13 15 H15");
    path.setStyle("-fx-fill: transparent; -fx-stroke: -fx-text-base-color; -fx-stroke-width: 1.6;");
    path.setStrokeLineCap(StrokeLineCap.ROUND);
    return path;
  }

  /**
   * @return centroid sticks of different heights, for mass spectra
   */
  static @NotNull Node spectrum() {
    final SVGPath path = new SVGPath();
    path.setContent("M1 15.5 H15 V16.3 H1 Z M2.3 15.5 V9 H3.7 V15.5 Z M5.3 15.5 V3 H6.7 V15.5 Z"
        + " M8.3 15.5 V11 H9.7 V15.5 Z M11.3 15.5 V6 H12.7 V15.5 Z");
    path.setStyle("-fx-fill: -fx-text-base-color;");
    return path;
  }
}
