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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.dataprocessing.norm_intensity.StandardCompoundNormalizationMode;
import org.jetbrains.annotations.NotNull;

public record StandardCompoundNormalizationRequirement(StandardCompoundNormalizationMode mode,
                                                       int manual) implements
    ComboWithInputValue<StandardCompoundNormalizationMode, Integer> {

  public static final StandardCompoundNormalizationRequirement DEFAULT = new StandardCompoundNormalizationRequirement(
      StandardCompoundNormalizationMode.getDefault(), 1);

  @Override
  public @NotNull StandardCompoundNormalizationMode getSelectedOption() {
    return mode;
  }

  @Override
  public Integer getEmbeddedValue() {
    return manual;
  }

  public void assertMinReferencePoints(int totalStandards, int detected,
      @NotNull RawDataFile rawFile) {
    int required = switch (mode) {
      case REQUIRE_N_SAMPLES -> manual;
      case REQUIRE_ALL_IN_ALL_SAMPLES -> totalStandards;
      case SKIP_FILES_WITHOUT_STANDARD -> 0;
    };
    if (detected >= required) {
      return;
    }

    throw new IllegalStateException(
        "Intensity normalization required %s internal standards but detected only %d/%d for file: %s".formatted(
            requiredString(), detected, totalStandards, rawFile.getName()));
  }

  @NotNull
  public String requiredString() {
    return switch (mode) {
      case REQUIRE_N_SAMPLES -> manual + "";
      case REQUIRE_ALL_IN_ALL_SAMPLES -> "all";
      case SKIP_FILES_WITHOUT_STANDARD -> "0";
    };
  }
}
