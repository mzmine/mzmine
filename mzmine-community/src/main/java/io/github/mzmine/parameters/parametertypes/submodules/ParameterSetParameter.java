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
package io.github.mzmine.parameters.parametertypes.submodules;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterContainer;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import java.util.Collection;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * This parameter has an embedded parameterset that is displayed as a sub panel in the dialog with
 * button to show and hide. also see {@link OptionalModuleParameter} and {@link SubModuleParameter}
 */
public class ParameterSetParameter<SUB extends ParameterSet> implements
    UserParameter<SUB, OptionalModuleComponent>, ParameterContainer,
    EmbeddedParameterSet<SUB, SUB> {

  private static final Logger logger = Logger.getLogger(ParameterSetParameter.class.getName());
  private final String name;
  private final String description;
  private SUB value;

  public ParameterSetParameter(String name, String description, SUB parameters) {
    this.name = name;
    this.description = description;
    // requires cloning to avoid usage of static parameters
    this.value = (SUB) parameters.cloneParameterSet();
  }

  public SUB getValue() {
    return value;
  }

  public void setValue(final SUB parameters) {
    if (value == null) {
      this.value = parameters;
    } else {
      // copy parameters over. Just in case if there is already a component showing those parameters
      ParameterUtils.copyParameters(parameters, value);
    }
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public ParameterSetParameter<SUB> cloneParameter() {
    return new ParameterSetParameter<>(this.name, this.description,
        (SUB) value.cloneParameterSet());
  }

  @Override
  public void setValueToComponent(final OptionalModuleComponent component,
      final @Nullable SUB parameters) {
    component.setParameterValuesToComponents();
  }

  @Override
  public void setValueFromComponent(final OptionalModuleComponent component) {
    component.updateParameterSetFromComponents();
  }

  @Override
  public OptionalModuleComponent createEditingComponent() {
    return new OptionalModuleComponent(this.value, EmbeddedComponentOptions.VIEW_IN_PANEL, true);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (this.value == null) {
      return;
    }

    value.saveValuesToXML(xmlElement);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    value.loadValuesFromXML(xmlElement);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return checkEmbeddedValues(errorMessages);
  }

  @Override
  public void setSkipSensitiveParameters(boolean skipSensitiveParameters) {
    // delegate skipSensitiveParameters embedded ParameterContainers
    value.setSkipSensitiveParameters(skipSensitiveParameters);
  }

  @Override
  public SUB getEmbeddedParameters() {
    return getValue();
  }

  public void setEmbeddedParameters(final SUB embeddedParameters) {
    setValue(embeddedParameters);
  }

  @Override
  public boolean valueEquals(Parameter<?> that) {
    if (!(that instanceof ParameterSetParameter<?> thatOpt)) {
      return false;
    }

    return ParameterUtils.equalValues(getEmbeddedParameters(), thatOpt.getEmbeddedParameters(),
        false, false);
  }
}
