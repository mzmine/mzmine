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

import org.jetbrains.annotations.NotNull;

/**
 * How the plot shows intensity over the two coordinates: the 3D visualizer as a relief in
 * perspective, the 2D visualizer as a map seen from the top (user decision: the 2D view is the top
 * view of the same engine). Everything that differs between the two views is either a property here
 * or an exhaustive switch over the two projections, e.g. to build meshes or to pick.
 */
public sealed interface IntensityMapProjection permits IntensityMapPerspective,
    IntensityMapTopView {

  IntensityMapProjection PERSPECTIVE = new IntensityMapPerspective();
  IntensityMapProjection TOP_VIEW = new IntensityMapTopView();

  /**
   * @return "3D" or "2D", for titles and file names
   */
  @NotNull String label();

  /**
   * @return true if values have heights. Without heights all values lie in the plane and only
   * colors show intensity.
   */
  boolean heights();

  /**
   * @return true if the camera rotates, with presets for top, front, and side
   */
  boolean rotatable();

  /**
   * @return true if point lights shade the geometry, otherwise an ambient light keeps the colors of
   * the paint scale exactly
   */
  boolean lit();

  /**
   * @return true if the intensity transformation applies to colors, false if to heights
   */
  boolean transformsColors();

  /**
   * @return noise floor in percent of the maximum that is hidden by default
   */
  double defaultNoisePercent();

  /**
   * @return true for a depth test; without it, geometry draws in scene order
   */
  boolean depthTest();

  /**
   * @return true if the axes stay at a fixed plot area like a zoomed chart, and data outside it are
   * clipped
   */
  boolean fixedPlotArea();

  /**
   * @return true if sampling follows physical screen pixels, e.g. on HiDPI screens
   */
  boolean physicalPixels();

  double defaultTilt();

  double defaultTurn();

  /**
   * @param width width of the plot, in samples of the screen
   * @return data columns worth sampling along x
   */
  int viewColumns(double width);

  /**
   * @return data rows worth sampling along y
   */
  int viewRows(double width, double height);
}
