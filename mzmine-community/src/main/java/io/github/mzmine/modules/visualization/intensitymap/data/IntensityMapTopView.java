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
 * The 2D view: a fixed top view onto the plane, a plane facing a perspective camera projects
 * without distortion. Values have no height and only colors show intensity.
 */
public record IntensityMapTopView() implements IntensityMapProjection {

  @Override
  public @NotNull String label() {
    return "2D";
  }

  @Override
  public boolean heights() {
    return false;
  }

  @Override
  public boolean rotatable() {
    return false;
  }

  /**
   * decision: the 2D view is unlit, so colors match the paint scale exactly
   */
  @Override
  public boolean lit() {
    return false;
  }

  /**
   * Colors are the only cue of intensity in 2D.
   */
  @Override
  public boolean transformsColors() {
    return true;
  }

  /**
   * decision (user decision after testing): 0.05 %, lower than in 3D, so the 2D view stays close to
   * the raw data
   */
  @Override
  public double defaultNoisePercent() {
    return 0.05;
  }

  /**
   * The flat map shows the feature with its closest neighbors, e.g. isotopes.
   */
  @Override
  public double featureZoomShare() {
    return 0.01;
  }

  /**
   * decision: coplanar floor, grid, data, and markers draw in scene order, a depth test needs
   * lifted layers, which drift apart from the axes at deep zoom
   */
  @Override
  public boolean depthTest() {
    return false;
  }

  @Override
  public boolean fixedPlotArea() {
    return true;
  }

  /**
   * decision: the 2D view samples logical pixels, cells of one physical pixel on HiDPI screens
   * still drop out between rows
   */
  @Override
  public boolean physicalPixels() {
    return false;
  }

  @Override
  public double defaultTilt() {
    return 90;
  }

  @Override
  public double defaultTurn() {
    return 0;
  }

  /**
   * decision (user request): one bin per pixel of the plot area, which keeps the maximum of the
   * pixel like the former 2D plot. Cells thinner than a pixel are not drawn, so centroids jittering
   * between finer m/z bins would appear as dashed traces.
   */
  @Override
  public int viewColumns(final double width) {
    return (int) Math.ceil(Math.max(100, width));
  }

  @Override
  public int viewRows(final double width, final double height) {
    return (int) Math.ceil(Math.max(100, height));
  }
}
