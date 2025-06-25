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

package io.github.mzmine.datamodel.featuredata;

import com.google.common.collect.Streams;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Stores intensity and mz values.
 *
 * @author https://github.com/SteffenHeu
 */
public interface IonSeries extends Iterable<DataPoint>, IntensitySeries, MzSeries {

  @Override
  default int getNumberOfValues() {
    return (int) StorageUtils.numDoubles(getMZValueBuffer());
  }

  @Override
  default Iterator<DataPoint> iterator() {
    return new DataPointIterator(this);
  }

  default Stream<DataPoint> stream() {
    return Streams.stream(this);
  }

  IonSeries copy(MemoryMapStorage storage);

  static class DataPointIterator implements Iterator<DataPoint>, DataPoint {

    private final IonSeries data;
    private int cursor = -1;

    DataPointIterator(IonSeries data) {
      this.data = data;
    }

    @Override
    public double getMZ() {
      return data.getMZ(cursor);
    }

    @Override
    public double getIntensity() {
      return data.getIntensity(cursor);
    }

    @Override
    public boolean hasNext() {
      return (cursor + 1) < data.getNumberOfValues();
    }

    @Override
    public DataPoint next() {
      cursor++;
      return new SimpleDataPoint(getMZ(), getIntensity());
    }
  }

}
