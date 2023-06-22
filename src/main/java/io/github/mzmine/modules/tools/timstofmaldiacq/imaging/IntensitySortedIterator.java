/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.tools.timstofmaldiacq.imaging;

import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.IntStream;

public class IntensitySortedIterator<T extends IonTimeSeries<?>> implements Iterator<Integer> {

  private final int[] indices;
  int index = -1;

  public IntensitySortedIterator(T series) {
    final double[] intensities;
    intensities = new double[series.getNumberOfValues()];
    series.getIntensityValues(intensities);

    // sort by descending intensity
    indices = IntStream.range(0, series.getNumberOfValues())
        .mapToObj(i -> new IndexedValue(i, series.getIntensity(i)))
        .sorted(Comparator.comparingDouble(IndexedValue::intensity).reversed())
        .mapToInt(IndexedValue::index).toArray();
  }


  @Override
  public boolean hasNext() {
    return index + 1 < indices.length;
  }

  @Override
  public Integer next() {
    ++index;
    return indices[index];
  }

  private record IndexedValue(int index, double intensity) {

  }
}
