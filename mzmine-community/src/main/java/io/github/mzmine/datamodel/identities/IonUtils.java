/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.datamodel.identities;

import org.jetbrains.annotations.NotNull;

public class IonUtils {

  public static final double ELECTRON_MASS = 5.4857990927E-4;

  /**
   * Add or remove electron masses depending on charge
   */
  public static double correctByElectronMass(double mass, int charge) {
    // electron
    return mass - ELECTRON_MASS * charge;
  }

  /**
   * @return charge followed by sign like 1+ or 2-. Empty string for charge 0
   */
  @NotNull
  public static String getChargeString(int charge) {
    if (charge == 0) {
      return "";
    }
    return Math.abs(charge) + getSign(charge);
  }


  public static String getSign(int number) {
    return number < 0 ? "-" : "+";
  }

  public static String getSign(double number) {
    return number < 0 ? "-" : "+";
  }

  public static String getSign(float number) {
    return number < 0 ? "-" : "+";
  }

  public static String getSign(long number) {
    return number < 0 ? "-" : "+";
  }

}
