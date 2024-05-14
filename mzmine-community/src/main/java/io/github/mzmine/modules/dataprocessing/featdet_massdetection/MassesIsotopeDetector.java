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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.IsotopesUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.List;
import org.openscience.cdk.Element;

/**
 * For detecting isotopes in mass detection
 */
public record MassesIsotopeDetector(boolean active, List<Element> elements, int maxCharge,
                                    MZTolerance mzTol, List<Double> mzDiffs, double maxMzDiff) {

  public static MassesIsotopeDetector createInactiveDefault() {
    return new MassesIsotopeDetector(false, List.of(), 1, null, List.of(), 0);
  }

  public boolean isPossibleIsotopeMz(final double exactMz, final DoubleArrayList mzs) {
    if (!active || mzs.isEmpty()) {
      return false;
    }

    // If the difference between current m/z and last detected m/z is greater than maximum
    // possible isotope m/z difference do not call isPossibleIsotopeMz
    return (mzs.getDouble(mzs.size() - 1) - exactMz) > maxMzDiff
           && IsotopesUtils.isPossibleIsotopeMz(exactMz, mzs, mzDiffs, mzTol);
  }
}
