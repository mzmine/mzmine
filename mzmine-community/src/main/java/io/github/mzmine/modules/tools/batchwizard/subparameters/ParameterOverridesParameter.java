/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.tools.batchwizard.ParameterCustomizationPane;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Parameter that stores a list of parameter overrides for batch wizard modules. The UI component is
 * a ParameterCustomizationPane that allows users to select modules and customize their parameters.
 */
public class ParameterOverridesParameter implements
    UserParameter<List<ParameterOverride>, ParameterCustomizationPane> {

  private static final String PARAMETER_NAME = "parameterCustomization";
  private static final String OVERRIDE_ELEMENT = "override";
  private static final String MODULE_CLASS_ATTR = "moduleClass";
  private static final String MODULE_NAME_ATTR = "moduleName";
  private static final String PARAM_NAME_ATTR = "parameterName";

  private List<ParameterOverride> value;

  public ParameterOverridesParameter() {
    this.value = new ArrayList<>();
  }

  @Override
  public String getName() {
    return PARAMETER_NAME;
  }

  @Override
  public List<ParameterOverride> getValue() {
    return value;
  }

  @Override
  public void setValue(List<ParameterOverride> newValue) {
    this.value = newValue == null ? new ArrayList<>() : new ArrayList<>(newValue);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    // Always valid - can have 0 or more overrides
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    List<ParameterOverride> overrides = new ArrayList<>();
    NodeList nodes = xmlElement.getElementsByTagName(OVERRIDE_ELEMENT);

    for (int i = 0; i < nodes.getLength(); i++) {
      Element overrideElement = (Element) nodes.item(i);
      String moduleClass = overrideElement.getAttribute(MODULE_CLASS_ATTR);
      String moduleName = overrideElement.getAttribute(MODULE_NAME_ATTR);
      String paramName = overrideElement.getAttribute(PARAM_NAME_ATTR);
      final MZmineModule module = MZmineCore.getInitializedModules().get(moduleClass);
      final ParameterSet moduleParameters = ConfigService.getConfiguration()
          .getModuleParameters(module.getClass());
      // clone the parameter
      Parameter<?> parameter = Arrays.stream(moduleParameters.getParameters())
          .filter(p -> p.getName().equals(paramName)).findFirst().map(Parameter::cloneParameter)
          .orElse(null);
      parameter.loadValueFromXML(overrideElement);

      overrides.add(new ParameterOverride(moduleClass, moduleName, parameter));
    }

    this.value = overrides;
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null || value.isEmpty()) {
      return;
    }

    Document doc = xmlElement.getOwnerDocument();

    for (ParameterOverride override : value) {
      Element overrideElement = doc.createElement(OVERRIDE_ELEMENT);
      overrideElement.setAttribute(MODULE_CLASS_ATTR, override.moduleClassName());
      overrideElement.setAttribute(MODULE_NAME_ATTR, override.moduleName());
      overrideElement.setAttribute(PARAM_NAME_ATTR, override.parameterWithValue().getName());
      override.parameterWithValue().saveValueToXML(overrideElement);
      xmlElement.appendChild(overrideElement);
    }
  }

  @Override
  public UserParameter<List<ParameterOverride>, ParameterCustomizationPane> cloneParameter() {
    ParameterOverridesParameter clone = new ParameterOverridesParameter();
    clone.setValue(this.getValue());
    return clone;
  }

  @Override
  public String getDescription() {
    return "Customize specific parameters of individual batch modules. "
        + "Select a module, choose a parameter, and set a custom value.";
  }

  @Override
  public ParameterCustomizationPane createEditingComponent() {
    return new ParameterCustomizationPane();
  }

  @Override
  public void setValueFromComponent(ParameterCustomizationPane component) {
    this.value = component.getParameterOverrides();
  }

  @Override
  public void setValueToComponent(ParameterCustomizationPane component,
      @Nullable List<ParameterOverride> newValue) {
    component.setParameterOverrides(newValue == null ? new ArrayList<>() : newValue);
  }

  @Override
  public Priority getComponentVgrowPriority() {
    return Priority.ALWAYS;
  }
}