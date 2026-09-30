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

package io.github.mzmine.parameters.parametertypes.combowithinput;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Value of {@link MZToleranceOrAutoParameter}.
 *
 * @param option    {@link AutoOrCustomOption#AUTO} estimates the tolerance from the data
 * @param tolerance the custom tolerance. A module that estimates the tolerance stores its estimate
 *                  here in the parameters of the applied method, so for processed data this is the
 *                  tolerance that was used, for both options.
 */
public record MZToleranceOrAuto(@NotNull AutoOrCustomOption option,
                                @Nullable MZTolerance tolerance) implements
    ComboWithInputValue<AutoOrCustomOption, MZTolerance> {

  @NotNull
  public static MZToleranceOrAuto custom(@NotNull MZTolerance tolerance) {
    return new MZToleranceOrAuto(AutoOrCustomOption.CUSTOM, tolerance);
  }

  /**
   * @param tolerance the custom tolerance that is kept for a later switch to
   *                  {@link AutoOrCustomOption#CUSTOM}, or the estimated tolerance
   */
  @NotNull
  public static MZToleranceOrAuto auto(@Nullable MZTolerance tolerance) {
    return new MZToleranceOrAuto(AutoOrCustomOption.AUTO, tolerance);
  }

  public boolean isAuto() {
    return option == AutoOrCustomOption.AUTO;
  }

  @Override
  public @NotNull AutoOrCustomOption getSelectedOption() {
    return option;
  }

  @Override
  public @Nullable MZTolerance getEmbeddedValue() {
    return tolerance;
  }
}
