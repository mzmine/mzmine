/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.parameters.AbstractParameter;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleComponent;
import java.util.Collection;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

public class ScanSelectionParameter extends
    AbstractParameter<ScanSelection, OptionalModuleComponent> implements
    EmbeddedParameterSet<ScanSelectionFiltersParameters, ScanSelection> {


  private final ScanSelectionFiltersParameters embeddedParameters;
  private @NotNull ScanSelection value;
  private boolean active;

  public ScanSelectionParameter() {
    this(ScanSelection.ALL_SCANS);
  }

  public ScanSelectionParameter(@NotNull ScanSelection defaultValue) {
    this("Scan filters", "Select scans that should be included.", defaultValue);
  }

  public ScanSelectionParameter(String name, String description,
      @NotNull ScanSelection defaultValue) {
    super(name, description, defaultValue);
    embeddedParameters = new ScanSelectionFiltersParameters(defaultValue);
    value = defaultValue;
    active = !value.equals(ScanSelection.ALL_SCANS);
  }


  /**
   * @return ScanSelection from the current dataset
   */
  public @NotNull ScanSelection createFilter() {
    return active ? getEmbeddedParameters().createFilter() : ScanSelection.ALL_SCANS;
  }

  @Override
  public @NotNull ScanSelection getValue() {
    return value;
  }

  @Override
  public void setValue(ScanSelection newValue) {
    this.value = Objects.requireNonNullElse(newValue, ScanSelection.ALL_SCANS);
  }

  @Override
  public ScanSelectionParameter cloneParameter() {
    return new ScanSelectionParameter(name, description, value);
  }

  public void setValue(final boolean active, final ScanSelection value) {
    this.active = active;
    setValue(value);
  }

  @Override
  public OptionalModuleComponent createEditingComponent() {
    return new OptionalModuleComponent(getEmbeddedParameters(), "", active);
  }

  @Override
  public void setValueFromComponent(OptionalModuleComponent component) {
    component.updateParameterSetFromComponents();
    value = embeddedParameters.createFilter();
    active = component.isSelected();
  }

  @Override
  public void setValueToComponent(OptionalModuleComponent component, ScanSelection newValue) {
    embeddedParameters.setFilter(newValue);
    component.setParameterValuesToComponents();
  }

  @Override
  public ScanSelectionFiltersParameters getEmbeddedParameters() {
    return embeddedParameters;
  }


  @Override
  public void loadValueFromXML(Element xmlElement) {
    embeddedParameters.loadValuesFromXML(xmlElement);
    String selectedAttr = xmlElement.getAttribute("selected");
    this.active = Objects.requireNonNullElse(Boolean.valueOf(selectedAttr), false);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    xmlElement.setAttribute("selected", String.valueOf(active));
    embeddedParameters.saveValuesToXML(xmlElement);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (!active) {
      return true;
    }
    return embeddedParameters.checkParameterValues(errorMessages);
  }
}
