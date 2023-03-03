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

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.IonMobilityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IonMobilityUtilsTest {

  @Test
  void testIsPotentialIsotope() {
    final MZTolerance tol = new MZTolerance(0.003, 15);
    Assertions.assertTrue(IonMobilityUtils.isPotentialIsotope(760.5594, 761.5627, tol));
    Assertions.assertTrue(IonMobilityUtils.isPotentialIsotope(760.5594, 759.5590, tol));
    Assertions.assertTrue(IonMobilityUtils.isPotentialIsotope(760.5594, 760.5627, tol));
    Assertions.assertTrue(IonMobilityUtils.isPotentialIsotope(760.5594, 762.5660, tol));
    Assertions.assertTrue(IonMobilityUtils.isPotentialIsotope(760.5594, 763.5693, tol));
    Assertions.assertFalse(IonMobilityUtils.isPotentialIsotope(760.5594, 763.6000, tol));
  }

}
