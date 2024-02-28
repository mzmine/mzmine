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


package io.github.mzmine.modules.dataprocessing.featdet_masscalibration.errormodeling;

import com.google.common.math.Stats;

import java.util.List;

/**
 * Class containing methods estimating distribution bias
 * <p>
 * used with distributions of measurement errors to estimate systematic error of the measurements
 * <p>
 * for instance: after extracting a certain substantial subset of all mass measurement PPM errors,
 * the measured mass spectrum bias can be estimated by taking the arithmetic mean of considered errors
 */
public class BiasEstimator {
  /**
   * Returns arithmetic mean of a distribution of items
   * when the list of items is empty, zero is returned
   * (conveniently, as this method is used to estimate bias from a distribution of errors)
   *
   * @param items
   * @return
   */
  public static double arithmeticMean(List<Double> items) {
    if (items.size() == 0) {
      return 0;
    }
    return Stats.meanOf(items);
  }
}
