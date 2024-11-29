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

import java.util.Arrays;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class NormalizeIntensityComboParameter extends ComboParameter<NormalizeIntensityOptions> {

  public NormalizeIntensityComboParameter() {
    this(NormalizeIntensityOptions.ORIGINAL);
  }

  public NormalizeIntensityComboParameter(final NormalizeIntensityOptions defaultValue) {
    this(NormalizeIntensityOptions.values(), defaultValue);
  }

  public NormalizeIntensityComboParameter(final NormalizeIntensityOptions[] choices,
      final NormalizeIntensityOptions defaultValue) {
    this("Intensity normalization", """
            This parameter may use the original intensity (usually absolute values) or various normalizations.""",
        choices, defaultValue);
  }

  public NormalizeIntensityComboParameter(final String name, final String description) {
    this(name, description, NormalizeIntensityOptions.values(), NormalizeIntensityOptions.ORIGINAL);
  }

  public NormalizeIntensityComboParameter(final String name, String description,
      final NormalizeIntensityOptions[] choices, final NormalizeIntensityOptions defaultValue) {
    String choicesDescription = Arrays.stream(choices)
        .map(NormalizeIntensityOptions::getDescription).collect(Collectors.joining("\n"));
    description += "\n" + choicesDescription;
    super(name, description, choices, defaultValue);
  }

  /**
   * Useful if scientific format is untested or not supported
   */
  @NotNull
  public static NormalizeIntensityComboParameter createWithoutScientific() {
    return new NormalizeIntensityComboParameter(NormalizeIntensityOptions.valuesNoScientific(),
        NormalizeIntensityOptions.ORIGINAL);
  }
}
