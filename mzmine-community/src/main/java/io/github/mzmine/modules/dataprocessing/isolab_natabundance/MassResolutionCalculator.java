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

package io.github.mzmine.modules.dataprocessing.isolab_natabundance;

public class MassResolutionCalculator {

  /**
   * Calculates mass resolution R = m / Δm using Dalton tolerance.
   *
   * @param mzValue The m/z value (mass-to-charge ratio).
   * @param deltaM  The tolerance in Dalton (absolute tolerance).
   * @return The calculated resolution.
   */
  public static double calculateResolutionFromDalton(double mzValue, double deltaM) {
    return mzValue / deltaM;
  }

  /**
   * Calculates mass resolution R = 1 / (Δm / m) using ppm tolerance.
   *
   * @param mzValue The m/z value (mass-to-charge ratio).
   * @param ppm     The tolerance in parts per million (ppm).
   * @return The calculated resolution.
   */
  public static double calculateResolutionFromPPM(double mzValue, double ppm) {
    // Convert ppm to relative tolerance Δm / m
    double deltaMOverM = ppm / 1_000_000.0; // ppm to fraction
    // Resolution R = 1 / (Δm / m)
    return 1.0 / deltaMOverM;
  }
}
