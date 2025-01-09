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

package io.github.mzmine.modules.dataprocessing.filter_scan_merge_select;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

/**
 * Option to simply select all or most intense input scan instead of spectral merging in
 * {@link SpectraMergeSelectModule} and {@link SpectraMergeSelectParameter}
 */
public class InputSpectraSelectParameters extends SimpleParameterSet {

  /**
   * Options to select scans without merging
   */
  public enum SelectInputScans implements UniqueIdSupplier {
    MOST_INTENSE_ACROSS_SAMPLES, MOST_INTENSE_PER_SAMPLE, ALL_SCANS, NONE;

    public static SelectInputScans[] valuesExcludingNone() {
      return Arrays.stream(values()).filter(v -> v != NONE).toArray(SelectInputScans[]::new);
    }

    @Override
    public @NotNull String toString() {
      return switch (this) {
        case MOST_INTENSE_ACROSS_SAMPLES -> "Most intense scan across samples";
        case MOST_INTENSE_PER_SAMPLE -> "Most intense scan per sample";
        case ALL_SCANS -> "All scans";
        case NONE -> "None";
      };
    }

    @Override
    public @NotNull String getUniqueID() {
      return switch (this) {
        case MOST_INTENSE_ACROSS_SAMPLES -> "most_intense_across_samples";
        case MOST_INTENSE_PER_SAMPLE -> "most_intense_per_sample";
        case ALL_SCANS -> "all_scans";
        case NONE -> "none";
      };
    }

    public boolean isSingleScan() {
      return this == MOST_INTENSE_ACROSS_SAMPLES;
    }
  }

  public static ComboParameter<InputSpectraSelectParameters.SelectInputScans> inputSelectionType = new ComboParameter<>(
      "Select input scans", "Select input scans without merging",
      SelectInputScans.valuesExcludingNone(), SelectInputScans.MOST_INTENSE_ACROSS_SAMPLES);

  public InputSpectraSelectParameters() {
    super(inputSelectionType);
  }

}
