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
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.MergedSpectraFinalSelectionTypes;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import org.jetbrains.annotations.NotNull;

/**
 * Option to simply select all or most intense source scan instead of spectral merging in
 * {@link SpectraMergeSelectModule} and {@link SpectraMergeSelectParameter}
 */
public class SourceSpectraSelectParameters extends SimpleParameterSet {

  /**
   * Create enum to make it easier to setup. Also had some instance where the full set of
   * {@link MergedSpectraFinalSelectionTypes} would end up as options in the parameters
   */
  public enum SourceOptions implements UniqueIdSupplier {
    SINGLE_MOST_INTENSE_SOURCE_SCAN, ALL_SOURCE_SCANS;

    @Override
    public @NotNull String getUniqueID() {
      return toFinalSelectionTypes().getUniqueID();
    }

    @Override
    public @NotNull String toString() {
      return toFinalSelectionTypes().toString();
    }

    public @NotNull MergedSpectraFinalSelectionTypes toFinalSelectionTypes() {
      return switch (this) {
        case ALL_SOURCE_SCANS -> MergedSpectraFinalSelectionTypes.ALL_SOURCE_SCANS;
        case SINGLE_MOST_INTENSE_SOURCE_SCAN ->
            MergedSpectraFinalSelectionTypes.SINGLE_MOST_INTENSE_SOURCE_SCAN;
      };
    }
  }

  public static ComboParameter<SourceSpectraSelectParameters.SourceOptions> sourceSelectionTypes = new ComboParameter<>(
      "Scan selection", "Select the source scans without merging", SourceOptions.values(),
      SourceOptions.SINGLE_MOST_INTENSE_SOURCE_SCAN);

  public SourceSpectraSelectParameters() {
    super(sourceSelectionTypes);
  }

}
