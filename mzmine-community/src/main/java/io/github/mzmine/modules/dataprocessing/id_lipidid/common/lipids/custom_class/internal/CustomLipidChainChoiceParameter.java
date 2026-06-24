/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.internal;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.parameters.UserParameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Not intended to be used as part of a module, does not support saving.
 */
class CustomLipidChainChoiceParameter implements
    UserParameter<LipidChainType[], CustomLipidChainChoiceComponent> {

  private final String name;
  private final String description;
  private LipidChainType[] values;

  private CustomLipidChainChoiceComponent component;

  /**
   * Create the parameter.
   *
   * @param name        name of the parameter.
   * @param description description of the parameter.
   */
  public CustomLipidChainChoiceParameter(String name, String description,
      LipidChainType[] choices) {
    this.name = name;
    this.description = description;
    this.values = choices;
  }

  @Override
  public CustomLipidChainChoiceComponent createEditingComponent() {
    component = new CustomLipidChainChoiceComponent(values);
    return component;
  }

  @Override
  public void setValueFromComponent(
      CustomLipidChainChoiceComponent customLipidChainChoiceComponent) {
    assert customLipidChainChoiceComponent == component;
    values = component.getValue().toArray(new LipidChainType[0]);
  }

  @Override
  public void setValueToComponent(CustomLipidChainChoiceComponent customLipidChainChoiceComponent,
      @Nullable LipidChainType[] newValue) {
    assert customLipidChainChoiceComponent == component;
    if (newValue == null) {
      component.setValue(List.of());
      return;
    }
    component.setValue(Arrays.asList(newValue));
  }

  @Override
  public CustomLipidChainChoiceParameter cloneParameter() {

    final CustomLipidChainChoiceParameter copy = new CustomLipidChainChoiceParameter(name,
        description, values);
    copy.setValue(values);
    return copy;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public LipidChainType[] getValue() {
    return values;
  }

  @Override
  public void setValue(LipidChainType[] newValue) {
    this.values = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    throw new UnsupportedOperationException(
        "This parameter is only intended for GUI usage and for loading and saving to an xml file.");
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    throw new UnsupportedOperationException(
        "This parameter is only intended for GUI usage and for loading and saving to an xml file.");
  }
}
