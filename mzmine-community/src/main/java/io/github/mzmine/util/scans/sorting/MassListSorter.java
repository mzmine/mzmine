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

package io.github.mzmine.util.scans.sorting;

import io.github.mzmine.datamodel.MassSpectrum;
import java.util.Comparator;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.util.scans.ScanUtils;

public class MassListSorter implements Comparator<MassSpectrum> {
  private double noiseLevel;
  private ScanSortMode sort;

  public MassListSorter(double noiseLevel, ScanSortMode sort) {
    this.sort = sort;
    this.noiseLevel = noiseLevel;
  }

  @Override
  public int compare(MassSpectrum a, MassSpectrum b) {
    switch (sort) {
      case NUMBER_OF_SIGNALS:
        int result = Integer.compare(getNumberOfSignals(a), getNumberOfSignals(b));
        // same number of signals? use max TIC
        if (result == 0)
          return Double.compare(getTIC(a), getTIC(b));
        else
          return result;
      case MAX_TIC:
        return Double.compare(getTIC(a), getTIC(b));
    }
    throw new IllegalArgumentException("Should not reach. Not all cases of sort are considered");
  }

  /**
   * sum of intensity
   * 
   * @param a
   * @return
   */
  private double getTIC(MassSpectrum a) {
    return ScanUtils.getTIC(a, noiseLevel);
  }

  /**
   * Number of DP greater noise level
   * 
   * @param a
   * @return
   */
  private int getNumberOfSignals(MassSpectrum a) {
    return ScanUtils.getNumberOfSignals(a, noiseLevel);
  }

}
