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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.jetbrains.annotations.NotNull;

public record IntensityNormalizer(IntensityNormalizerOptions option, boolean scientificFormat) {

  /**
   * no normalization and regular number format
   */
  @NotNull
  public static IntensityNormalizer createDefault() {
    return new IntensityNormalizer(IntensityNormalizerOptions.NO_NORMALIZATION, false);
  }

  /**
   * no normalization and scientific format
   */
  @NotNull
  public static IntensityNormalizer createScientific() {
    return new IntensityNormalizer(IntensityNormalizerOptions.NO_NORMALIZATION, true);
  }

  @NotNull
  public NumberFormat createExportFormat() {
    DecimalFormat format = scientificFormat ? new DecimalFormat("0.#E0") : new DecimalFormat("0.#");
    format.setMaximumFractionDigits(getMaximumFractionDigitsExport());
    return format;
  }

  @NotNull
  public NumberFormat createGuiFormat() {
    DecimalFormat format = scientificFormat ? new DecimalFormat("0.#E0") : new DecimalFormat("0.#");
    format.setMaximumFractionDigits(getMaximumFractionDigitsGui());
    return format;
  }

  private int getMaximumFractionDigitsExport() {
    if (scientificFormat) {
      return 7;
    }
    return switch (option) {
      case NO_NORMALIZATION -> 6; // hard to estimate here what the amplitude of values is...
      case HIGHEST_SIGNAL_AS_100, SUM_AS_100 -> 6;
      case HIGHEST_SIGNAL_AS_1, SUM_AS_1 -> 8;
    };
  }

  private int getMaximumFractionDigitsGui() {
    if (scientificFormat) {
      return 2;
    }
    return switch (option) {
      case NO_NORMALIZATION -> 1; // hard to estimate here what the amplitude of values is...
      case HIGHEST_SIGNAL_AS_100, SUM_AS_100 -> 1;
      case HIGHEST_SIGNAL_AS_1, SUM_AS_1 -> 3;
    };
  }


  public double[] normalize(double[] intensities) {
    return normalize(intensities, false);
  }

  public double[] normalize(double[] intensities, boolean inPlace) {
    return option.normalize(intensities, inPlace);
  }

  public DataPoint[] normalize(DataPoint[] intensities) {
    return normalize(intensities, false);
  }

  public DataPoint[] normalize(DataPoint[] intensities, boolean inPlace) {
    return option.normalize(intensities, inPlace);
  }

}
