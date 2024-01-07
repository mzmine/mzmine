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

package io.github.mzmine.gui.chartbasics.chartutils.paintscales;

import com.google.common.collect.Range;
import java.awt.Color;
import java.awt.Paint;
import org.jfree.chart.renderer.LookupPaintScale;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class PaintScale extends LookupPaintScale {

  private PaintScaleColorStyle paintScaleColorStyle;
  private PaintScaleBoundStyle paintScaleBoundStyle;

  public PaintScale(Range<Double> scaleRange) {
    // we add a minimum value on top of the upper endpoint to avoid errors for empty datasets
    // with lower==upper value
    super(scaleRange.lowerEndpoint(),
        getValidUpperBound(scaleRange.lowerEndpoint(), scaleRange.upperEndpoint()),
        new Color(0, 0, 0, 0f));
  }

  public PaintScale(PaintScaleColorStyle paintScaleColorStyle,
      PaintScaleBoundStyle paintScaleBoundStyle, Range<Double> scaleRange) {
    this(paintScaleColorStyle, paintScaleBoundStyle, scaleRange, Color.BLACK);
  }

  public PaintScale(PaintScaleColorStyle paintScaleColorStyle,
      PaintScaleBoundStyle paintScaleBoundStyle, Range<Double> scaleRange, Color color) {
    // we add a minimum value on top of the upper endpoint to avoid errors for empty datasets
    // with lower==upper value
    super(scaleRange.lowerEndpoint(),
        getValidUpperBound(scaleRange.lowerEndpoint(), scaleRange.upperEndpoint()), color);
    this.paintScaleColorStyle = paintScaleColorStyle;
    this.paintScaleBoundStyle = paintScaleBoundStyle;
  }

  public PaintScaleColorStyle getPaintScaleColorStyle() {
    return paintScaleColorStyle;
  }

  public void setPaintScaleColorStyle(PaintScaleColorStyle paintScaleColorStyle) {
    this.paintScaleColorStyle = paintScaleColorStyle;
  }

  public PaintScaleBoundStyle getPaintScaleBoundStyle() {
    return paintScaleBoundStyle;
  }

  public void setPaintScaleBoundStyle(PaintScaleBoundStyle paintScaleBoundStyle) {
    this.paintScaleBoundStyle = paintScaleBoundStyle;
  }

  @Override
  public String toString() {
    return paintScaleColorStyle.toString();
  }

  @Override
  public Paint getPaint(double value) {
    // overridden, so we can use percentiles to create the paint scale and return the lowest
    // or highest value's color when the value is outside the percentile.
    if (value < getLowerBound()) {
      return getPaint(getLowerBound());
    }
    if (value > getUpperBound()) {
      return getPaint(getUpperBound());
    }

    return super.getPaint(value);
  }

  private static double getValidUpperBound(double lower, double upper) {
    final double diff = upper - lower;
    if (Double.compare(diff, 0d) > 0) {
      return upper;
    }
    return lower + 0.1d;
  }
}
