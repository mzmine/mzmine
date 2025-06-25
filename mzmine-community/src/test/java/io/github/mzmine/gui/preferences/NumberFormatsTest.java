/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.gui.preferences;

import static io.github.mzmine.gui.preferences.NumberFormats.IsZero;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.junit.jupiter.api.Test;

class NumberFormatsTest {

  @Test
  void testIsZero() {
    NumberFormat form = new DecimalFormat("0.00");
    isZero(form, 0.0, true);
    isZero(form, 0.004, true);
    isZero(form, 0.04, false);

    form = new DecimalFormat("0.##");
    isZero(form, 0.0, true);
    isZero(form, 0.004, true);
    isZero(form, 0.04, false);

    form = new DecimalFormat("0.##%");
    isZero(form, 0.0, true);
    isZero(form, 0.00004, true);
    isZero(form, 0.0004, false);

    form = new DecimalFormat("0.##E0");
    isZero(form, 0.0, true);
    isZero(form, 1.004, false);
    isZero(form, 0.004, false);
    isZero(form, 0.04, false);

  }

  private static void isZero(final NumberFormat form, final double number, final boolean result) {
    assertEquals(result, IsZero(form.format(number), form));
  }
}