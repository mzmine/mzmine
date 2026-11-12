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

package io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import org.jetbrains.annotations.NotNull;

/**
 * The values that a chromatogram builder used, also when they were estimated from the data, see
 * {@link ADAPChromatogramBuilderParameters#getAppliedSettings(java.util.List)}.
 *
 * @param minConsecutiveScans min number of consecutive scans of at least the min group intensity
 * @param minGroupIntensity   min intensity of the consecutive scans
 * @param minHeight           min height within the consecutive scans
 * @param mzTolerance         scan to scan m/z tolerance
 */
public record ChromatogramBuilderSettings(int minConsecutiveScans, double minGroupIntensity,
                                          double minHeight, @NotNull MZTolerance mzTolerance) {

  @Override
  public String toString() {
    return "min consecutive scans %d, min intensity for consecutive scans %.3G, min height %.3G, m/z tolerance %s".formatted(
        minConsecutiveScans, minGroupIntensity, minHeight, mzTolerance);
  }
}
