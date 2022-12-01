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

package io.github.mzmine.util.scans;

import static java.util.Comparator.comparingInt;
import static java.util.Comparator.nullsLast;
import static java.util.Comparator.reverseOrder;

import io.github.mzmine.datamodel.Scan;
import java.util.Comparator;

/**
 * Sorts fragment scans from best to lesser representative MS/MS scans. Generally MS2>MS3>MS4
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public abstract class FragmentScanSorter {

  /**
   * MS1>MS2>MS3 then highest TIC then highest number of data points
   */
  public static final Comparator<Scan> DEFAULT_TIC = comparingInt(Scan::getMSLevel) //
      .thenComparing(Scan::getTIC, nullsLast(reverseOrder()))
      .thenComparing(Scan::getNumberOfDataPoints, reverseOrder());

  /**
   * MS1>MS2>MS3 then highest number of data points then highest TIC
   */
  public static final Comparator<Scan> DEFAULT_NUMBER_OF_DATA_POINTS = comparingInt(
      Scan::getMSLevel) //
      .thenComparing(Scan::getNumberOfDataPoints, reverseOrder())
      .thenComparing(Scan::getTIC, nullsLast(reverseOrder()));


}
