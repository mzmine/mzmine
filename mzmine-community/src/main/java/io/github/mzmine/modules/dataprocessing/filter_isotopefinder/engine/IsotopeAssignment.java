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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine;

/**
 * How a detected isotope-pattern signal was attributed during scoring. Diagnostic only (produced
 * when the engine runs with {@code keepDiagnostics}); used by the compound dashboard to label the
 * detected isotope pattern.
 */
public enum IsotopeAssignment {

  /**
   * The signal sits on the exact 13C grid at the monoisotopic offset (offset 0 from the mono).
   */
  MONOISOTOPIC,
  /**
   * The signal sits on the exact 13C grid at a non-zero offset (a pure 13C ladder peak).
   */
  CARBON_13,
  /**
   * The signal is off the exact 13C grid and was attributed to a heavy isotope (e.g. 37Cl, 81Br,
   * 34S, 30Si).
   */
  HEAVY_ISOTOPE,
  /**
   * The signal was kept in the pattern but could not be attributed to the 13C grid or a candidate
   * heavy element.
   */
  UNEXPLAINED
}
