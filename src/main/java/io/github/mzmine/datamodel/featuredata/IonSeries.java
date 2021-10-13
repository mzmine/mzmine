/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.featuredata;

import com.google.common.collect.Streams;
import io.github.mzmine.datamodel.DataPoint;
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
    return getMZValueBuffer().capacity();
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
