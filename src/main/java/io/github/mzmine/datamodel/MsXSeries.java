package io.github.mzmine.datamodel;

import java.nio.DoubleBuffer;
import java.util.List;

public interface MsXSeries<T extends MassSpectrum> {

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

  Number getX(int index);
}