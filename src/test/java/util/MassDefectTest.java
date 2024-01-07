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

package util;

import io.github.mzmine.parameters.parametertypes.massdefect.MassDefectFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MassDefectTest {

  @Test
  public void testMassDefectFilter() {
    final MassDefectFilter filter = new MassDefectFilter(0.1000, 0.500);
    Assertions.assertTrue(filter.contains(542.234));
    Assertions.assertTrue(filter.contains(542.500));
    Assertions.assertFalse(filter.contains(542.5001));
    Assertions.assertFalse(filter.contains(542.6730));

    final MassDefectFilter filter2 = new MassDefectFilter(0.9, 0.1);
    Assertions.assertTrue(filter2.contains(412.9612));
    Assertions.assertTrue(filter2.contains(412.0832));
    Assertions.assertFalse(filter2.contains(412.8999));

    final MassDefectFilter filter3 = new MassDefectFilter(0.3, 0.85);
    Assertions.assertFalse(filter3.contains(785.8999));
    Assertions.assertTrue(filter3.contains(785.5398));
    Assertions.assertFalse(filter3.contains(785.2846));

    Assertions.assertThrows(IllegalArgumentException.class, () -> new MassDefectFilter(1.3, 0.3));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new MassDefectFilter(-1.1, -0.2));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new MassDefectFilter(0.9, -0.2));
  }
}
