/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.statistics;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;

/**
 * Can iterate over rows and provides the original data if possible
 */
public interface DataTable extends Iterable<double[]> {


  /**
   * Some tables may represent samples as columns or as rows
   *
   * @return the number of samples
   */
  int getNumberOfSamples();

  /**
   * Some tables may represent features as columns or as rows
   *
   * @return the number of features
   */
  int getNumberOfFeatures();

  /**
   * The data array of a feature.
   *
   * @param index index of feature in data
   * @param copy  either copy the data array or provide the original for inplace modification
   * @return the original array or a copy
   */
  double[] getFeatureData(int index, boolean copy);

  /**
   * @param index sample index
   * @return a new array of the sample data
   */
  default double[] getSampleData(int index) {
    final int numFeatures = getNumberOfFeatures();
    final double[] data = new double[numFeatures];
    for (int featureIndex = 0; featureIndex < numFeatures; featureIndex++) {
      data[featureIndex] = getValue(featureIndex, index);
    }
    return data;
  }


  /**
   * @return a copy of all data structures
   */
  <T extends DataTable> T copy();


  /**
   * @return the value at index
   */
  default double getValue(int featureIndex, int sampleIndex) {
    return getFeatureData(featureIndex, false)[sampleIndex];
  }


  void setFeatureData(int index, double[] data);

  void setSampleData(int index, double[] data);

  void setValue(int featureIndex, int sampleIndex, double value);

  /**
   * Iterator for the features
   *
   * @param copy default iterator does not copy and uses inplace array
   * @return a feature data array iterator
   */
  default Iterator<double[]> featuresIterator(final boolean copy) {
    return new Iterator<>() {
      private int currentIndex = 0;

      @Override
      public boolean hasNext() {
        return currentIndex < getNumberOfFeatures();
      }

      @Override
      public double[] next() {
        return getFeatureData(currentIndex++, copy);
      }
    };
  }

  @Override
  default @NotNull Iterator<double[]> iterator() {
    return featuresIterator(false);
  }

  @Override
  default Spliterator<double[]> spliterator() {
    return Spliterators.spliterator(iterator(), getNumberOfFeatures(),
        Spliterator.SIZED | Spliterator.ORDERED | Spliterator.IMMUTABLE);
  }


  /**
   * Stream all values in no guaranteed order
   */
  default @NotNull DoubleStream streamValues() {
    return IntStream.range(0, getNumberOfFeatures()).boxed().mapMultiToDouble((row, consumer) -> {
      double[] data = getFeatureData(row, false);
      for (double value : data) {
        consumer.accept(value);
      }
    });
  }

}
