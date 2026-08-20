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

package io.github.mzmine.parameters.parametertypes.absoluterelative;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class AbsoluteAndRelativeIntTest {

  private static final Logger logger = Logger.getLogger(AbsoluteAndRelativeIntTest.class.getName());

  @Test
  void getMaximumValue() {
    final AbsoluteAndRelativeInt rel = new AbsoluteAndRelativeInt(1, 0.5f);
    final AbsoluteAndRelativeInt rel51 = new AbsoluteAndRelativeInt(1, 0.51f);
    final AbsoluteAndRelativeInt rel49 = new AbsoluteAndRelativeInt(1, 0.49f);

//    StringBuilder msg = new StringBuilder("""
//
//        Rounding test:
//        Percentage,Total N,Result x
//        """);
//    for (int i = 2; i <= 7; i++) {
//      msg.append("%.2f,%d,%d\n".formatted(rel51.getRelative(), i, rel51.getMaximumValue(i)));
//    }
//    for (int i = 2; i <= 7; i++) {
//      msg.append("%.2f,%d,%d\n".formatted(rel.getRelative(), i, rel.getMaximumValue(i)));
//    }
//    for (int i = 2; i <= 7; i++) {
//      msg.append("%.2f,%d,%d\n".formatted(rel49.getRelative(), i, rel49.getMaximumValue(i)));
//    }
//
//    logger.info(msg.toString());
    assertEquals(1, rel49.getMaximumValue(3));
    assertEquals(2, rel49.getMaximumValue(4));
    assertEquals(2, rel49.getMaximumValue(5));
    assertEquals(3, rel49.getMaximumValue(6));

    assertEquals(1, rel.getMaximumValue(2));
    assertEquals(2, rel.getMaximumValue(3));
    assertEquals(2, rel.getMaximumValue(4));
    assertEquals(3, rel.getMaximumValue(5));
    assertEquals(3, rel.getMaximumValue(6));
    assertEquals(4, rel.getMaximumValue(7));
    assertEquals(4, rel.getMaximumValue(8));
    assertEquals(5, rel.getMaximumValue(9));
    assertEquals(5, rel.getMaximumValue(10));
    assertEquals(6, rel.getMaximumValue(11));
    assertEquals(6, rel.getMaximumValue(12));
  }
}