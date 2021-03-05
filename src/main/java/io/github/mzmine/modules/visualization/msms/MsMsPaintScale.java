/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.msms;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import java.awt.Color;
import java.awt.Paint;

public class MsMsPaintScale extends PaintScale {

  private static final double LOWER_BOUND = 0d;
  private static final double UPPER_BOUND = 2d;

  public MsMsPaintScale() {
    super(Range.closed(LOWER_BOUND, UPPER_BOUND));
  }

  @Override
  public double getLowerBound() {
    return LOWER_BOUND;
  }

  @Override
  public double getUpperBound() {
    return UPPER_BOUND;
  }

  @Override
  public Paint getPaint(double v) {
    // Shades of blue
    if (v > 1) {
      int rgbVal = (int) Math.round(220 - (v - 1) * 220);
      return new Color(255, rgbVal, rgbVal);
    // Shades of red
    } else {
      int rgbVal = (int) Math.round(220 - v * 220);
      return new Color(rgbVal, rgbVal, 255);
    }
  }

}
