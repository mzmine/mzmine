/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

/**
 * A standard reference point for one file-specific normalization function.
 *
 * @param abundance          the abundance of the standard in this specific file
 * @param referenceAbundance the level this standard is normalized to, usually the median abundance
 *                           of the standard over all reference samples. Dividing by it makes the
 *                           {@link #factor()} a relative correction around 1, so that different
 *                           standards become interchangeable and the intensity scale is preserved.
 */
public record StandardCompoundReferencePoint(double mz, float rt, double abundance,
                                             double referenceAbundance) {

  /**
   * A point without a reference level. Only used for
   * {@link StandardCompoundFactorMode#ABSOLUTE_LEGACY}, where the factor is 1 / abundance.
   */
  public StandardCompoundReferencePoint(double mz, float rt, double abundance) {
    this(mz, rt, abundance, 1d);
  }

  /**
   * @return the normalization factor of this point, see {@link StandardCompoundFactorMode}
   */
  public double factor() {
    return referenceAbundance / abundance;
  }
}

