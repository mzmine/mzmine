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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

/**
 * Sequential access to centroided scans, e.g., the mass lists of the selected scans of a raw data
 * file. Scans are visited in retention time order and can be visited again after {@link #reset()}.
 * Decouples {@link FastChromatogramBuilder} from raw data files so that the algorithm can be tested
 * and benchmarked on plain arrays.
 */
public interface MzIntensityScans {

  /**
   * @return the number of scans
   */
  int getNumberOfScans();

  /**
   * Moves before the first scan. The next call to {@link #nextScan()} loads the first scan again.
   */
  void reset();

  /**
   * Loads the next scan.
   *
   * @return false if there is no next scan
   */
  boolean nextScan();

  /**
   * @return number of data points in the current scan
   */
  int getNumberOfDataPoints();

  /**
   * @return m/z of data point at index in the current scan. Values should be sorted ascending.
   */
  double getMz(int index);

  /**
   * @return intensity of data point at index in the current scan
   */
  double getIntensity(int index);
}
