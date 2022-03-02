/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
    super(scaleRange.lowerEndpoint(), scaleRange.upperEndpoint() + 0.1d, color);
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
    if (Double.compare(diff, 0d) != 0) {
      return upper;
    }
    final double valid = upper + 0.1;
    assert Double.compare(lower, valid) < 0;
    return valid;
  }
}
