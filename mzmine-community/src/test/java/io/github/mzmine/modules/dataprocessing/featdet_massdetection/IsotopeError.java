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

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.util.MathUtils;
import org.jetbrains.annotations.NotNull;

public record IsotopeError(Scan scan, @NotNull DataPoint mainPeak, @NotNull DataPoint isotope) {

  public static final double DISTANCE = IsotopePatternCalculator.THIRTHEEN_C_DISTANCE;

  public double mainPeakMz() {
    return mainPeak.getMZ();
  }

  public double isotopePeakMz() {
    return isotope.getMZ();
  }

  public double measuredDistance() {
    return isotopePeakMz() - mainPeakMz();
  }

  public double absoluteError() {
    return measuredDistance() - DISTANCE;
  }

  public double ppmError() {
    return MathUtils.getPpmDiff(mainPeakMz() + DISTANCE, isotopePeakMz());
  }
}
