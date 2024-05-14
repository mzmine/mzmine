/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.simplespectra.renderers;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Shape;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

/**
 * Renderer for the mass list in spectrum plot
 */
public class UnifyScansPrecursorRenderer extends XYLineAndShapeRenderer {

  private static final Shape rightArrow;
  private static final Shape leftArrow;
  private static final Shape downArrow;

  static {
    rightArrow = new Polygon(new int[]{0, 0, 1}, new int[]{-1, 1, 0}, 3);
    leftArrow = new Polygon(new int[]{0, 0, -1}, new int[]{-1, 1, 0}, 3);
    downArrow = new Polygon(new int[]{-1, 1, 0}, new int[]{1, 1, 0}, 3);
  }

  private final Color color;

  public UnifyScansPrecursorRenderer(Color color) {
    super(false, true);

    this.color = color;
  }

  @Override
  public Shape getItemShape(int series, int item) {
    // sequence of lower and upper limit of precursor selection windows
    return item % 2 == 0 ? rightArrow : leftArrow;
  }

  @Override
  public Paint getItemPaint(int row, int column) {
    return color;
  }

}
