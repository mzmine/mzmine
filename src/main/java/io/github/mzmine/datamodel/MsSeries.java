package io.github.mzmine.datamodel;

import java.nio.DoubleBuffer;
import java.util.List;

/**
 * Stores combinations of intensity and mz values
 *
 * @param <T>
 * @author https://github.com/SteffenHeu
 */
public interface MsSeries<T extends MassSpectrum> {

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
}