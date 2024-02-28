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
import java.awt.geom.Ellipse2D;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

/**
 * Renderer for the mass list in spectrum plot
 */
public class ArrowRenderer extends XYLineAndShapeRenderer {


  public static final Ellipse2D circle = new Ellipse2D.Double(-2.5, 5, 5, 5);

  public static final Shape rightArrow = new Polygon(new int[]{0, 0, 3}, new int[]{-3, 3, 0}, 3);
  public static final Shape leftArrow = new Polygon(new int[]{0, 0, -3}, new int[]{-3, 3, 0}, 3);
  public static final Shape downArrow = new Polygon(new int[]{-3, 3, 0}, new int[]{0, 0, 3}, 3);
  public static final Shape upArrow = new Polygon(new int[]{-3, 3, 0}, new int[]{3, 3, 0}, 3);
  public static final Shape diamond = new Polygon(new int[]{0, -3, 0, 3}, new int[]{0, 3, 6, 3}, 4);
  private final ShapeType shapeType;

  private final Color[] color;

  public ArrowRenderer(Color... color) {
    this(ShapeType.CIRCLE, color);
  }


  public ArrowRenderer(ShapeType shapeType, Color... color) {
    this(shapeType, circle, color);
  }

  public ArrowRenderer(ShapeType shapeType, Shape shape, Color... color) {
    super(false, true);
    this.color = color;
    this.shapeType = shapeType;
    setDefaultShape(shape);
    setDefaultSeriesVisibleInLegend(false);
  }

  public ShapeType getShapeType() {
    return shapeType;
  }

  @Override
  public Shape getItemShape(int series, int item) {
    return getDefaultShape();
  }

  public enum ShapeType {
    UP, DOWN, LEFT, RIGHT, DIAMOND, CIRCLE
  }

  @Override
  public Paint getItemPaint(int series, int item) {
    return series < color.length ? color[series] : color[0];
  }

}
