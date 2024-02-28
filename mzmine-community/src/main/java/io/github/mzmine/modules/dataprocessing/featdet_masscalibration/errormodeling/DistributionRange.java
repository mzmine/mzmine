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

import com.google.common.collect.Range;

import java.util.List;

public class DistributionRange extends DistributionExtract {

  /**
   * Range of index values from original distribution that were extracted
   */
  protected Range<Integer> indexRange;

  /**
   * Range of values from original distribution that were extracted
   */
  protected Range<Double> valueRange;

  public DistributionRange(List<Double> originalItems, List<Double> extractedItems,
                           Range<Integer> indexRange, Range<Double> valueRange) {
    super(originalItems, extractedItems);
    this.indexRange = indexRange;
    this.valueRange = valueRange;
  }

  public Range<Integer> getIndexRange() {
    return indexRange;
  }

  public Range<Double> getValueRange() {
    return valueRange;
  }
}
