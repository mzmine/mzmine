package io.github.mzmine.modules.tools.timstofmaldiacq.imaging;

import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import java.util.Arrays;
import java.util.Iterator;

public class IntensitySortedSeries<T extends IonTimeSeries<?>> implements Iterator<Integer> {

  private final Integer[] indices;
  T series;
  final double[] intensities;
  int index = -1;

  public IntensitySortedSeries(T series) {
    this.series = series;
    intensities = new double[series.getNumberOfValues()];
    series.getIntensityValues(intensities);

    indices = new Integer[series.getNumberOfValues()];

    for (int i = 0; i < indices.length; i++) {
      indices[i] = i;
    }

    // sort by descending intensity
    Arrays.sort(indices, (i1, i2) -> -1 * Double.compare(intensities[i1], intensities[i2]));
  }


  @Override
  public boolean hasNext() {
    return index < indices.length;
  }

  @Override
  public Integer next() {
    ++index;
    return indices[index];
  }
}
