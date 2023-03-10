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

package io.github.mzmine.parameters.parametertypes.submodules;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import java.util.Collection;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Module combo parameter - to choose from a list of modules and setup their parameters
 * individually
 */
public class ModuleComboParameter<ModuleType extends MZmineModule> implements
    UserParameter<MZmineProcessingStep<ModuleType>, ModuleComboComponent>,
    EmbeddedParameterSet<ParameterSet, MZmineProcessingStep<ModuleType>> {

  private static final Logger logger = Logger.getLogger(ModuleComboParameter.class.getName());

  private final String name;
  private final String description;
  private final MZmineProcessingStep<ModuleType>[] modulesWithParams;
  private MZmineProcessingStep<ModuleType> value;

  @SuppressWarnings("unchecked")
  public ModuleComboParameter(String name, String description, @NotNull final ModuleType[] modules,
      @NotNull final ModuleType defaultValue) {
    assert modules.length > 0;

    this.name = name;
    this.description = description;
    this.modulesWithParams = new MZmineProcessingStep[modules.length];
    for (int i = 0; i < modules.length; i++) {
      ParameterSet moduleParams;
      try {
        Class<? extends ParameterSet> parameterSetClass = modules[i].getParameterSetClass();
        if (parameterSetClass != null) {
          moduleParams = parameterSetClass.newInstance().cloneParameterSet();
        } else {
          moduleParams = null;
        }
        MZmineProcessingStep<ModuleType> modWithParams = new MZmineProcessingStepImpl<ModuleType>(
            modules[i], moduleParams);
        this.modulesWithParams[i] = modWithParams;

        if (modules[i].equals(defaultValue)) {
          this.value = modWithParams;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (value == null) {
      logger.severe("Default value of parameter " + name
          + " not present in module selection. Defaulting to first entry.");
      this.value = modulesWithParams[0];
    }
  }

  public ModuleComboParameter(String name, String description,
      @NotNull final MZmineProcessingStep<ModuleType>[] modulesWithParams,
      @NotNull final MZmineProcessingStep<ModuleType> defaultValue) {
    this.name = name;
    this.description = description;
    this.modulesWithParams = modulesWithParams;
    this.value = defaultValue;
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
  public ModuleComboComponent createEditingComponent() {
    return new ModuleComboComponent(modulesWithParams);
  }

  public MZmineProcessingStep<ModuleType> getValue() {
    if (value == null) {
      return null;
    }
    // First check that the module has all parameters set
    ParameterSet embeddedParameters = value.getParameterSet();
    if (embeddedParameters == null) {
      return value;
    }
    for (Parameter<?> p : embeddedParameters.getParameters()) {
      if (p instanceof UserParameter<?, ?> up) {
        Object upValue = up.getValue();
        if (upValue == null) {
          return null;
        }
      }
    }
    return value;
  }

  @Override
  public void setValue(MZmineProcessingStep<ModuleType> value) {
    this.value = value;
    // also copy the parameter set to our internal parameter set
    for (int i = 0; i < modulesWithParams.length; i++) {
      if (modulesWithParams[i].getModule().equals(value.getModule())) {
        modulesWithParams[i] = new MZmineProcessingStepImpl<>(modulesWithParams[i].getModule(),
            value.getParameterSet().cloneParameterSet());
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public ModuleComboParameter<ModuleType> cloneParameter() {
    MZmineProcessingStep<ModuleType>[] newModules = new MZmineProcessingStep[modulesWithParams.length];
    MZmineProcessingStep<ModuleType> newValue = null;
    for (int i = 0; i < modulesWithParams.length; i++) {
      ModuleType module = modulesWithParams[i].getModule();
      ParameterSet params = modulesWithParams[i].getParameterSet();
      params = params.cloneParameterSet();
      newModules[i] = new MZmineProcessingStepImpl<ModuleType>(module, params);
      if (value.getModule().equals(modulesWithParams[i].getModule())) {
        newValue = newModules[i];
      }
    }
    if (newValue == null) {
      throw new IllegalStateException("Cloned value of parameter" + name + " is null.");
    }
    return new ModuleComboParameter<ModuleType>(name, description, newModules, newValue);
  }

  @Override
  public void setValueFromComponent(ModuleComboComponent component) {
    int index = component.getSelectedIndex();
    if (index < 0) {
      return;
    }
    this.value = modulesWithParams[index];
  }

  @Override
  public void setValueToComponent(ModuleComboComponent component,
      @Nullable MZmineProcessingStep<ModuleType> newValue) {
    component.setSelectedItem(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList items = xmlElement.getElementsByTagName("module");

    for (int i = 0; i < items.getLength(); i++) {
      Element moduleElement = (Element) items.item(i);
      String name = moduleElement.getAttribute("name");
      for (int j = 0; j < modulesWithParams.length; j++) {
        if (modulesWithParams[j].getModule().getName().equals(name)) {
          ParameterSet moduleParameters = modulesWithParams[j].getParameterSet();
          if (moduleParameters == null) {
            continue;
          }
          moduleParameters.loadValuesFromXML((Element) items.item(i));
        }
      }
    }
    String selectedAttr = xmlElement.getAttribute("selected_item");
    for (int j = 0; j < modulesWithParams.length; j++) {
      if (modulesWithParams[j].getModule().getName().equals(selectedAttr)) {
        value = modulesWithParams[j];
      }
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value != null) {
      xmlElement.setAttribute("selected_item", value.toString());
    }
    Document parentDocument = xmlElement.getOwnerDocument();
    for (MZmineProcessingStep<?> item : modulesWithParams) {
      Element newElement = parentDocument.createElement("module");
      newElement.setAttribute("name", item.getModule().getName());
      ParameterSet moduleParameters = item.getParameterSet();
      if (moduleParameters != null) {
        moduleParameters.saveValuesToXML(newElement);
      }
      xmlElement.appendChild(newElement);
    }
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    ParameterSet moduleParameters = value.getParameterSet();
    return moduleParameters == null || moduleParameters.checkParameterValues(errorMessages);
  }

  @Override
  public ParameterSet getEmbeddedParameters() {
    return value.getParameterSet();
  }
}
