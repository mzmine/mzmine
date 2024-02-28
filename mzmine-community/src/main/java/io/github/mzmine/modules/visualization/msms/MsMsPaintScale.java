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
