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

package io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options;

import io.github.mzmine.parameters.parametertypes.ComboParameter;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public class SpectraMergeSelectPresetsParameter extends ComboParameter<SpectraMergeSelectPresets> {

  public static final String DEFAULT_NAME = "Presets";

  public SpectraMergeSelectPresetsParameter() {
    this(null);
  }

  public SpectraMergeSelectPresetsParameter(@Nullable String additionalDescriptionLine) {
    this(additionalDescriptionLine, SpectraMergeSelectPresets.values(),
        SpectraMergeSelectPresets.REPRESENTATIVE_SCANS);
  }

  public SpectraMergeSelectPresetsParameter(@Nullable String additionalDescriptionLine,
      final SpectraMergeSelectPresets[] choices, final SpectraMergeSelectPresets defaultValue) {
    if (additionalDescriptionLine == null) {
      additionalDescriptionLine = "";
    }
    var optionDescription = Arrays.stream(choices).map(SpectraMergeSelectPresets::getDescription)
        .collect(Collectors.joining("\n"));

    String description = """
        Various presets to merge and select scans with parameters below. \
        %s \
        Across/each energy refers to fragmentation energies if they are read from the raw data files - all scans of unknown energy are grouped together.
        %s""".formatted(additionalDescriptionLine, optionDescription);
    this(DEFAULT_NAME, description, choices, defaultValue);
  }

  public SpectraMergeSelectPresetsParameter(final String name, final String description,
      final SpectraMergeSelectPresets[] choices, final SpectraMergeSelectPresets defaultValue) {
    super(name, description, choices, defaultValue);
  }

  @Override
  public SpectraMergeSelectPresetsParameter cloneParameter() {
    return new SpectraMergeSelectPresetsParameter(getName(), getDescription(),
        getChoices().toArray(SpectraMergeSelectPresets[]::new), getValue());
  }
}
