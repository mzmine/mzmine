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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import io.github.mzmine.util.collections.IndexRange;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Optional preprocessing of the intensity values before maximum and edge detection in a
 * {@link MassDetector}. Implementations transform the original intensities (e.g. by smoothing)
 * while leaving the original array untouched, so the mass detector can keep the original
 * intensities for the intensity calculation.
 */
public interface MassDetectorPreprocessor {

  /**
   * Preprocess the intensities used for maximum and edge detection. The original array must not be
   * modified. Implementations may return the input unchanged (no preprocessing).
   *
   * @param intensities the original intensities of the whole spectrum.
   * @param ranges      the consecutive ranges of equidistant points detected in the spectrum.
   *                    Points within one range are equidistant; ranges are separated by larger m/z
   *                    gaps and must be processed independently.
   * @return the intensities to use for maximum and edge detection. May be the same array as
   * {@code intensities}.
   */
  double @NotNull [] preprocessIntensities(double @NotNull [] intensities,
      @NotNull List<IndexRange> ranges);
}
