/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max;

import io.github.mzmine.parameters.parametertypes.combowithinput.ComboWithInputValue;
import org.jetbrains.annotations.NotNull;

/**
 * @param option source of the saturation threshold
 * @param manual threshold in raw intensity, only used for {@link SaturationThresholdOption#MANUAL}
 */
public record SaturationThresholdValue(@NotNull SaturationThresholdOption option,
                                       double manual) implements
    ComboWithInputValue<SaturationThresholdOption, Double> {

  public static final SaturationThresholdValue OFF = new SaturationThresholdValue(
      SaturationThresholdOption.OFF, 0d);

  @Override
  public @NotNull SaturationThresholdOption getSelectedOption() {
    return option;
  }

  @Override
  public Double getEmbeddedValue() {
    return manual;
  }
}
