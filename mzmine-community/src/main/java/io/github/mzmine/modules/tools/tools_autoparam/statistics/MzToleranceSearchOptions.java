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

package io.github.mzmine.modules.tools.tools_autoparam.statistics;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;

/**
 * Ordered tolerance choices shared by statistics and parameter estimation.
 */
public final class MzToleranceSearchOptions {

  public static final List<MZTolerance> ALL_TOLERANCE_OPTIONS = List.of( //
      new MZTolerance(0.0005, 2), // 0
      new MZTolerance(0.001, 5), //  1
      new MZTolerance(0.003, 7), //  2
      MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA,             // 3
      new MZTolerance(0.008, 15), // 4
      new MZTolerance(0.01, 20), //  5
      new MZTolerance(0.015, 25), // 6
      new MZTolerance(0.02, 25), //  7
      new MZTolerance(0.05, 25), //  8
      new MZTolerance(0.1, 25), //   9
      new MZTolerance(0.3, 25), //  10
      new MZTolerance(0.5, 25)); // 11

  /**
   * Index of the widest tolerance estimated for a high-resolution mass spectrometer preset (QTOF
   * has the widest range). A wider estimate indicates low-resolution data.
   */
  public static final int MAX_HIGH_RESOLUTION_INDEX = 6;

  private MzToleranceSearchOptions() {
  }

}
