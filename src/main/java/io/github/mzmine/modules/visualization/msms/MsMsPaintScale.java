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

package io.github.mzmine.modules.visualization.msms;

import com.google.common.collect.Range;
import com.google.common.primitives.Doubles;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import java.awt.Color;
import java.awt.Paint;

public class MsMsPaintScale extends PaintScale {

  private static final double LOWER_BOUND = 0d;
  private static final double UPPER_BOUND = 4d;

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
    float minS = 0.1f;
    float vFract = (float) (v % 1);
    float h;
    float s = Doubles.compare(vFract, 0d) == 0 ? 1f : minS + vFract * (1 - minS);
    float b = 1f;

    if (v > 3) {
      // Green
      h = 0.333f;
      b = 0.95f;
    } else if (v > 2) {
      // Purple
      h = 0.806f;
      b = 0.95f;
    } else if (v > 1) {
      // Red
      h = 1f;
    } else {
      // Blue
      h = 0.67f;
    }

    return Color.getHSBColor(h, s, b);
  }

}
