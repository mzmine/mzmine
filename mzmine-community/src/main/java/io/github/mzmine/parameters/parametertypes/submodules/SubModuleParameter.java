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
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Parameter that opens a separate dialog to set complex sub parameters. Also see
 * {@link ParameterSetParameter} and {@link OptionalModuleParameter} for parameters that open the
 * embedded parameterset in a sub panel
 */
public class SubModuleParameter<SUB extends ParameterSet> implements
    UserParameter<Boolean, SubModuleComponent>, EmbeddedParameterSet<SUB, Boolean> {

  private final String name;
  private final String description;
  private SUB embeddedParameters;

  public SubModuleParameter(String name, String description, SUB embeddedParameters) {
    this.name = name;
    this.description = description;
    // requires cloning to avoid usage of static parameters
    this.embeddedParameters = (SUB) embeddedParameters.cloneParameterSet();
  }

  public SUB getEmbeddedParameters() {
    return embeddedParameters;
  }

  public void setEmbeddedParameters(SUB param) {
    embeddedParameters = param;
  }

  /**
   *
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   *
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public SubModuleComponent createEditingComponent() {
    return new SubModuleComponent(embeddedParameters);
  }

  @Override
  public Boolean getValue() {
    // If the option is selected, first check that the module has all
    // parameters set
    for (Parameter<?> p : embeddedParameters.getParameters()) {
      if (p instanceof UserParameter<?, ?> up) {
        Object upValue = up.getValue();
        if (upValue == null) {
          return null;
        }
      }
    }
    return true;
  }

  @Override
  public void setValue(Boolean value) {
  }

  @Override
  public SubModuleParameter<SUB> cloneParameter() {
    final SUB embeddedParametersClone = (SUB) embeddedParameters.cloneParameterSet();
    final SubModuleParameter<SUB> copy = new SubModuleParameter<SUB>(name, description,
        embeddedParametersClone);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(SubModuleComponent component) {
    // uses a dialog on demand
  }

  @Override
  public void setValueToComponent(SubModuleComponent component, @Nullable Boolean newValue) {
    // uses a dialog on demand
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    embeddedParameters.loadValuesFromXML(xmlElement);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    embeddedParameters.saveValuesToXML(xmlElement);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return embeddedParameters.checkParameterValues(errorMessages);
  }
}
