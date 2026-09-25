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
 * The 3D view: a lit relief in perspective that can rotate.
 */
public record IntensityMapPerspective() implements IntensityMapProjection {

  @Override
  public @NotNull String label() {
    return "3D";
  }

  @Override
  public boolean heights() {
    return true;
  }

  @Override
  public boolean rotatable() {
    return true;
  }

  @Override
  public boolean lit() {
    return true;
  }

  @Override
  public boolean depthTest() {
    return true;
  }

  @Override
  public boolean fixedPlotArea() {
    return false;
  }

  @Override
  public boolean physicalPixels() {
    return true;
  }

  @Override
  public double defaultTilt() {
    return 38;
  }

  @Override
  public double defaultTurn() {
    return -32;
  }

  @Override
  public int viewColumns(final double width) {
    return (int) Math.ceil(Math.max(400, width));
  }

  @Override
  public int viewRows(final double width, final double height) {
    // the spectral axis gets more samples than screen pixels, bins retain maxima of narrow peaks
    return (int) Math.ceil(Math.max(600, Math.max(width, height) * 1.6));
  }
}
