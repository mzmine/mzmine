/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.row_type_filter;

import io.github.mzmine.parameters.CompositeParametersParameter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.row_type_filter.filters.RowTypeFilter;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;

/**
 * Filter that defines filter targets {@link RowTypeFilterOption} like formula, structure or other.
 * Then a matching mode {@link MatchingMode} and a query text.
 */
public class RowTypeFilterParameter extends
    CompositeParametersParameter<RowTypeFilter, RowTypeFilterComponent> {

  private final ComboParameter<MatchingMode> matchingMode;
  private final ComboParameter<RowTypeFilterOption> selectedType;
  private final StringParameter searchValue;
  private RowTypeFilter value;

  public RowTypeFilterParameter() {
    this("Flexible filter (e.g., annotation)", """
        This filter facilitates flexible filtering by many feature list row columns like annotations and other fields.
        Select a filter target, a matching mode (e.g., contains ⊂, equals, greater equals ≥, etc), and enter a query value.
        Formula example: "Formula ≥ C6Cl2": Search for all formulas that contain at least 6C and 2Cl.
        SMILES example: "SMILES ⊂ COOH": Substructure search, annotation contains COOH substructure.""");
  }

  public RowTypeFilterParameter(String name, String description) {
    super(name, description);
    matchingMode = new ComboParameter<>("Matching mode",
        "Determines how the filter should match entries", MatchingMode.values(),
        MatchingMode.CONTAINS);
    selectedType = new ComboParameter<>("Type", "What type of information to match against",
        RowTypeFilterOption.values(), RowTypeFilterOption.COMPOUND_NAME);
    searchValue = new StringParameter("Search value", "The value to search for", "");
  }

  @Override
  public Parameter<?>[] getInternalParameters() {
    return new Parameter[]{matchingMode, selectedType, searchValue};
  }

  public static RowTypeFilterComponent createDefaultEditingComponent(boolean addPresetButton) {
    return new RowTypeFilterParameter().createEditingComponent(addPresetButton);
  }

  @Override
  public RowTypeFilterComponent createEditingComponent() {
    return createEditingComponent(true);
  }

  public RowTypeFilterComponent createEditingComponent(boolean addPresetButton) {
    return new RowTypeFilterComponent(value, selectedType.createEditingComponent(),
        matchingMode.createEditingComponent(), searchValue.createEditingComponent(),
        addPresetButton);
  }

  @Override
  public void setValueFromComponent(RowTypeFilterComponent comp) {
    setValue(comp.getValue());
  }

  @Override
  public void setValueToComponent(RowTypeFilterComponent comp, @Nullable RowTypeFilter newValue) {
    comp.setValue(newValue);
  }

  @Override
  public RowTypeFilter getValue() {
    final RowTypeFilterOption selectedType = this.selectedType.getValue();
    final MatchingMode matchingMode = this.matchingMode.getValue();
    final String query = searchValue.getValue();
    if (selectedType == null || matchingMode == null || query.isBlank()) {
      value = null;
    } else {
      value = RowTypeFilter.create(selectedType, matchingMode, query);
    }
    return value;
  }

  @Override
  public void setValue(@Nullable RowTypeFilter value) {
    this.value = value;
    if (value == null) {
      matchingMode.setValue(null);
      selectedType.setValue(null);
      searchValue.setValue("");
      return;
    }
    matchingMode.setValue(value.matchingMode());
    selectedType.setValue(value.selectedType());
    searchValue.setValue(value.query());
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return value != null;
  }

  @Override
  public RowTypeFilterParameter cloneParameter() {
    RowTypeFilterParameter clone = new RowTypeFilterParameter(getName(), getDescription());
    clone.setValue(getValue());
    return clone;
  }
}
