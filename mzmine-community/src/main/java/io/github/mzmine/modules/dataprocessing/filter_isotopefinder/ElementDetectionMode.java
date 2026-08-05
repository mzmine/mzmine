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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

/**
 * Opt-in element auto-detection strategy for the signal-based isotope finder. Controls whether the
 * heavy-isotope upper bound is derived from the user-configured elements only, from elements
 * inferred from the observed pattern, or both.
 */
public enum ElementDetectionMode {

  /**
   * Keep the current behavior: heavy-isotope bounds come from the chosen elements with a crude
   * atom-count estimate. Default.
   */
  USER_DEFINED,
  /**
   * Infer popular heavy elements (Cl, Br, S, Si) from the pattern and use the detected atom
   * counts.
   */
  AUTO_DETECT,
  /**
   * Combine the user-configured heavy elements with the auto-detected ones.
   */
  USER_PLUS_AUTO;

  @Override
  public String toString() {
    return switch (this) {
      case USER_DEFINED -> "User-defined elements only";
      case AUTO_DETECT -> "Auto-detect heavy elements";
      case USER_PLUS_AUTO -> "User-defined + auto-detect";
    };
  }
}
