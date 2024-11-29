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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.function.ToDoubleFunction;
import org.jetbrains.annotations.NotNull;

public enum NormalizeIntensityOptions {
  ORIGINAL, ORIGINAL_SCIENTIFIC_FORMAT, HIGHEST_SIGNAL_AS_100, HIGHEST_SIGNAL_AS_1, SUM_AS_100, SUM_AS_1;

  /**
   * Without scientific number format, which may not be compatible with all processing tools.
   * However, it better captures all numbers.
   */
  public static NormalizeIntensityOptions[] valuesNoScientific() {
    return Arrays.stream(values()).filter(
            v -> v != ORIGINAL_SCIENTIFIC_FORMAT && !v.name().toLowerCase().contains("scientific"))
        .toArray(NormalizeIntensityOptions[]::new);
  }

  @Override
  public String toString() {
    return switch (this) {
      case ORIGINAL -> "Original";
      case ORIGINAL_SCIENTIFIC_FORMAT -> "Original (scientific format)";
      case HIGHEST_SIGNAL_AS_100 -> "Highest signal as 100%";
      case HIGHEST_SIGNAL_AS_1 -> "Highest signal as 1";
      case SUM_AS_100 -> "Sum as 100%";
      case SUM_AS_1 -> "Sum as 1";
    };
  }

  /**
   * Description of options
   */
  @NotNull
  public String getDescription() {
    return this.toString() + ": " + switch (this) {
      case ORIGINAL -> "No normalization, regular number format";
      case ORIGINAL_SCIENTIFIC_FORMAT ->
          "No normalization, scientific exponential format, i.e., 1.005E4; Better value representation but limited tool compatibility";
      case HIGHEST_SIGNAL_AS_100, HIGHEST_SIGNAL_AS_1 -> "Normalize to highest signal";
      case SUM_AS_100 -> "Normalize values sum to 100";
      case SUM_AS_1 -> "Normalize values sum to 1";
    };
  }

  public double[] normalize(double[] intensities) {
    return normalize(intensities, false);
  }

  public double[] normalize(double[] intensities, boolean inPlace) {
    if (this == ORIGINAL || intensities == null || intensities.length == 0) {
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
    if (this == ORIGINAL || intensities == null || intensities.length == 0) {
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

  /**
   * @return the normalization factor based on input data. Ready to be multiplied with the input
   * data for normalization.
   */
  public double getNormalizationFactorForData(double[] intensities) {
    return switch (this) {
      case ORIGINAL, ORIGINAL_SCIENTIFIC_FORMAT -> 1;
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
      case ORIGINAL, ORIGINAL_SCIENTIFIC_FORMAT -> 1;
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
      case HIGHEST_SIGNAL_AS_1, SUM_AS_1, ORIGINAL, ORIGINAL_SCIENTIFIC_FORMAT -> 1d;
    };
  }

  /**
   * Formats for export
   */
  @NotNull
  public NumberFormat createExportFormat() {
    return switch (this) {
      case ORIGINAL -> new DecimalFormat("0.######");
      case ORIGINAL_SCIENTIFIC_FORMAT -> new DecimalFormat("0.#####E0");
      // percent format not precise enough for intensity export
      case HIGHEST_SIGNAL_AS_100, SUM_AS_100 -> new DecimalFormat("0.######");
      case HIGHEST_SIGNAL_AS_1, SUM_AS_1 -> new DecimalFormat("0.########");
    };
  }

  /**
   * Formats for the graphical user interface
   */
  @NotNull
  public NumberFormat createGuiFormat() {
    return switch (this) {
      case ORIGINAL -> new DecimalFormat("0.####");
      case ORIGINAL_SCIENTIFIC_FORMAT -> new DecimalFormat("0.##E0");
      // percent format not precise enough for intensity export
      case HIGHEST_SIGNAL_AS_100, SUM_AS_100 -> new DecimalFormat("0.##");
      case HIGHEST_SIGNAL_AS_1, SUM_AS_1 -> new DecimalFormat("0.####");
    };
  }
}
