package io.github.mzmine.datamodel;

import com.google.common.collect.Streams;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import java.nio.DoubleBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Stores combinations of intensity and mz values
 *
 * @param <T>
 * @author https://github.com/SteffenHeu
 */
public interface MsSeries<T extends MassSpectrum> extends Iterable<DataPoint> {

  DoubleBuffer getIntensityValues();

  DoubleBuffer getMzValues();

  default double getIntensityValue(int index) {
    return getIntensityValues().get(index);
  }

  default double getMzValue(int index) {
    return getMzValues().get(index);
  }

  List<T> getScans();

  default T getScan(int index) {
    return getScans().get(index);
  }

  default int getNumberOfDataPoints() {
    return getMzValues().capacity();
  }

  @Override
  default Iterator<DataPoint> iterator() {
    return new DataPointIterator(this);
  }

  default Stream<DataPoint> stream() {
    return Streams.stream(this);
  }

  static class DataPointIterator implements Iterator<DataPoint>, DataPoint {

    private int cursor = -1;
    private final MsSeries<? extends MassSpectrum> data;

    DataPointIterator(MsSeries<? extends MassSpectrum> data) {
      this.data = data;
    }

    @Override
    public double getMZ() {
      return data.getMzValue(cursor);
    }

    @Override
    public double getIntensity() {
      return data.getIntensityValue(cursor);
    }

    @Override
    public boolean hasNext() {
      return (cursor + 1) < data.getNumberOfDataPoints();
    }

    @Override
    public DataPoint next() {
      cursor++;
      return new SimpleDataPoint(getMZ(), getIntensity());
    }
  }
}