/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.util.ArrayUtils;
import java.util.Arrays;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum IntensityNormalizerOptions {
  NO_NORMALIZATION, HIGHEST_SIGNAL_AS_100, HIGHEST_SIGNAL_AS_1, SUM_AS_100, SUM_AS_1;


  @Override
  public String toString() {
    return switch (this) {
      case NO_NORMALIZATION -> "No normalization";
      case HIGHEST_SIGNAL_AS_100 -> "Highest signal as 100%";
      case HIGHEST_SIGNAL_AS_1 -> "Highest signal as 1";
      case SUM_AS_100 -> "Sum as 100%";
      case SUM_AS_1 -> "Sum as 1";
    };
  }

  /**
   * ID for export, do not change values
   */
  @NotNull
  public String getUniqueID() {
    return switch (this) {
      case NO_NORMALIZATION -> "no_normalization";
      case HIGHEST_SIGNAL_AS_100 -> "max100";
      case HIGHEST_SIGNAL_AS_1 -> "max1";
      case SUM_AS_100 -> "sum100";
      case SUM_AS_1 -> "sum1";
    };
  }

  @Nullable
  public static IntensityNormalizerOptions forUniqueID(final String id) {
    for (IntensityNormalizerOptions o : IntensityNormalizerOptions.values()) {
      if (o.getUniqueID().equals(id)) {
        return o;
      }
    }
    return null;
  }

  /**
   * Description of options
   */
  @NotNull
  public String getDescription() {
    return this.toString() + ": " + switch (this) {
      case NO_NORMALIZATION -> "No normalization (original values)";
      case HIGHEST_SIGNAL_AS_100, HIGHEST_SIGNAL_AS_1 -> "Normalize to highest signal";
      case SUM_AS_100 -> "Normalize values sum to 100";
      case SUM_AS_1 -> "Normalize values sum to 1";
    };
  }

  /**
   * @return the normalization factor based on input data. Ready to be multiplied with the input
   * data for normalization.
   */
  public double getNormalizationFactorForData(double[] intensities) {
    return switch (this) {
      case NO_NORMALIZATION -> 1;
      case HIGHEST_SIGNAL_AS_100, HIGHEST_SIGNAL_AS_1 ->
          1d / ArrayUtils.max(intensities).orElse(1d) * getBaseNormalizationFactor();
      case SUM_AS_100, SUM_AS_1 -> 1d / ArrayUtils.sum(intensities) * getBaseNormalizationFactor();
    };
  }

  /**
   * @param values    any object
   * @param extractor extract intensity from values
   * @return the normalization factor based on input data. Ready to be multiplied with the input
   * data for normalization.
   */
  public <T> double getNormalizationFactorForData(T[] values, ToDoubleFunction<T> extractor) {
    return switch (this) {
      case NO_NORMALIZATION -> 1;
      case HIGHEST_SIGNAL_AS_100, HIGHEST_SIGNAL_AS_1 ->
          1d / ArrayUtils.max(values, extractor).orElse(1d) * getBaseNormalizationFactor();
      case SUM_AS_100, SUM_AS_1 ->
          1d / ArrayUtils.sum(values, extractor) * getBaseNormalizationFactor();
    };
  }

  /**
   * Base might be 1 or 100% based
   */
  public double getBaseNormalizationFactor() {
    return switch (this) {
      case HIGHEST_SIGNAL_AS_100, SUM_AS_100 -> 100d;
      case HIGHEST_SIGNAL_AS_1, SUM_AS_1, NO_NORMALIZATION -> 1d;
    };
  }

  public double[] normalize(double[] intensities) {
    return normalize(intensities, false);
  }

  public double[] normalize(double[] intensities, boolean inPlace) {
    if (this == IntensityNormalizerOptions.NO_NORMALIZATION || intensities == null
        || intensities.length == 0) {
      return intensities;
    }
    if (!inPlace) {
      // defensive copy
      intensities = Arrays.copyOf(intensities, intensities.length);
    }

    double normFactor = getNormalizationFactorForData(intensities);
    for (int i = 0; i < intensities.length; i++) {
      intensities[i] = intensities[i] * normFactor;
    }

    return intensities;
  }

  public DataPoint[] normalize(DataPoint[] intensities) {
    return normalize(intensities, false);
  }

  public DataPoint[] normalize(DataPoint[] intensities, boolean inPlace) {
    if (this == IntensityNormalizerOptions.NO_NORMALIZATION || intensities == null
        || intensities.length == 0) {
      return intensities;
    }
    final DataPoint[] target;
    if (inPlace) {
      target = intensities;
    } else {
      target = new DataPoint[intensities.length];
    }

    double normFactor = getNormalizationFactorForData(intensities, DataPoint::getIntensity);
    for (int i = 0; i < intensities.length; i++) {
      DataPoint value = intensities[i];
      target[i] = new SimpleDataPoint(value.getMZ(), value.getIntensity() * normFactor);
    }

    return target;
  }
}
