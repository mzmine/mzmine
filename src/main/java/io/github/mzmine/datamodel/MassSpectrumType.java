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

package io.github.mzmine.datamodel;

/**
 * Defines a type of the mass spectrum. For exact definition of different spectra types, see
 * Deutsch, E. W. (2012). File Formats Commonly Used in Mass Spectrometry Proteomics. Molecular &
 * Cellular Proteomics, 11(12), 1612–1621. doi:10.1074/mcp.R112.019695
 */
public enum MassSpectrumType {

  /**
   * Continuous (profile) mass spectrum. Continuous stream of connected data points forms a spectrum
   * consisting of individual peaks. Peaks represent detected ions. Each peak consists of multiple
   * data points.
   */
  PROFILE,

  /**
   * Thresholded mass spectrum = same as profile, but data points below certain intensity threshold
   * are removed.
   */
  THRESHOLDED,

  /**
   * Centroided mass spectrum = discrete data points, one for each detected ion.
   */
  CENTROIDED,

  /**
   * Mixed is only used to describe multiple spectra
   */
  MIXED;

  public static boolean isCentroided(MassSpectrumType spectraType) {
    return spectraType != null && spectraType.isCentroided();
  }

  public boolean isCentroided() {
    return switch (this) {
      case PROFILE -> false;
      // mixed means that at least one is centroided
      // thresholding is usually applied after centroiding
      case THRESHOLDED, MIXED, CENTROIDED -> true;
    };
  }
}
