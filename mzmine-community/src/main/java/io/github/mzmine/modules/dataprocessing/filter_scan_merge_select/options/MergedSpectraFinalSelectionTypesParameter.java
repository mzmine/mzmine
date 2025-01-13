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

import io.github.mzmine.parameters.parametertypes.CheckComboParameter;
import java.util.Collection;
import java.util.List;

public class MergedSpectraFinalSelectionTypesParameter extends
    CheckComboParameter<MergedSpectraFinalSelectionTypes> {

  public static final String DEFAULT_NAME = "Merging options";

  public MergedSpectraFinalSelectionTypesParameter(final MergedSpectraFinalSelectionTypes[] choices,
      final List<MergedSpectraFinalSelectionTypes> defaultValue) {
    var advancedValidDescription = MergedSpectraFinalSelectionTypes.getAdvancedValidDescription();
    this(DEFAULT_NAME, """
        The final list of scans will contain scans from all selected groups (combinations possible).
        %s""".formatted(advancedValidDescription), choices, defaultValue);
  }

  public MergedSpectraFinalSelectionTypesParameter(final String name, final String description,
      final MergedSpectraFinalSelectionTypes[] choices,
      final List<MergedSpectraFinalSelectionTypes> defaultValue) {
    super(name, description, choices, defaultValue);
  }


  @Override
  public boolean checkValue(final Collection<String> errorMessages) {
    var types = getValue();
    var result = super.checkValue(errorMessages);
    if (!MergedSpectraFinalSelectionTypes.isValidSelection(types, true)) {
      errorMessages.add(MergedSpectraFinalSelectionTypes.getAdvancedValidDescription());
      return false;
    }
    return result;
  }

  @Override
  public MergedSpectraFinalSelectionTypesParameter cloneParameter() {
    return new MergedSpectraFinalSelectionTypesParameter(getName(), getDescription(),
        getChoices().toArray(MergedSpectraFinalSelectionTypes[]::new), getValue());
  }
}
