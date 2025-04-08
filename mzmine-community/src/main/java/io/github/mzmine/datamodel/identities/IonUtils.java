/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

  // needs to escape \\# in case comments are used in regex
  public static final String SMILES_CHARACTERS_SET_FULL = "[A-Za-z0-9=\\#$:%()*@\\[\\].+-]";
  public static final String SMILES_CHARACTERS_SET_BONDS = "[A-Za-z0-9=\\#-$:%*@.]";

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
    if (charge == 1) {
      return "+";
    }
    if (charge == -1) {
      return "-";
    }
    return Math.abs(charge) + getSign(charge);
  }


  /**
   * @param n a number
   * @return signed number as string but -1 as - and 1 as +. 2 is +2
   */
  public static String getSignedNumberOmit1(int n) {
    if (n == 0) {
      return "";
    } else if (n == -1) {
      return "-";
    } else if (n == 1) {
      return "+";
    }
    var v = String.valueOf(n);
    return n < 0 ? v : "+" + v;
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
