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

package util.maths;

import io.github.mzmine.modules.dataprocessing.id_ecmscalcpotential.EcmsUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class EcmsUtilsTest {

  @Test
  public void testtubingVolume() {
    Assertions.assertEquals(9.500765233078083, EcmsUtils.getTubingVolume(750d, 0.127d));
  }

  @Test
  public void testDelayTime() {
    Assertions.assertEquals(57.0045913984685d, EcmsUtils.getDelayTime(10d / 60, 9.500765233078083));
  }

  @Test
  public void testPotential() {
    Assertions.assertEquals(1889.977043d, EcmsUtils.getPotentialAtRt(7.25f, 57.0045914d, 5));
  }
}
