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
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Simplifies the creating multiple options of algorithms or filters using a
 * {@link ModuleOptionsEnum}
 */
public class ModuleOptionsEnumComboParameter<EnumType extends Enum<EnumType> & ModuleOptionsEnum> implements
    UserParameter<EnumType, ModuleOptionsEnumComponent<EnumType>>,
    EmbeddedParameterSet<ParameterSet, EnumType> {

  private static final String MODULE_ELEMENT = "module"; // same as old ModuleComboParameter
  private static final String NAME_ATTRIBUTE = "name";
  private static final String SELECTED_ATTRIBUTE = "selected_item";

  private final String name;
  private final String description;
  protected final EnumMap<EnumType, ParameterSet> parametersMap;
  private EnumType selectedValue;

  public ModuleOptionsEnumComboParameter(final String name, final String description,
      @NotNull final EnumType defaultValue) {
    this(name, description, defaultValue.getDeclaringClass().getEnumConstants(), defaultValue);
  }

  public ModuleOptionsEnumComboParameter(final String name, final String description,
      @NotNull final EnumType[] options, @NotNull final EnumType defaultValue) {

    this.name = name;
    this.description = description;
    this.selectedValue = defaultValue;

    parametersMap = new EnumMap<>(defaultValue.getDeclaringClass());
    for (final EnumType option : options) {
      parametersMap.put(option, option.getModuleParameters());
    }
  }

  /**
   * Used in clone parameter
   */
  protected ModuleOptionsEnumComboParameter(final String name, final String description,
      final EnumType selectedValue, final EnumMap<EnumType, ParameterSet> parameters) {
    this.name = name;
    this.description = description;
    this.selectedValue = selectedValue;
    this.parametersMap = parameters;
  }

  public void setValue(final EnumType value, ParameterSet parameters) {
    setValue(value);
    setEmbeddedParameters(parameters);
  }


  public ParameterSet getEmbeddedParameters(EnumType option) {
    return parametersMap.get(option);
  }

  @Override
  public ParameterSet getEmbeddedParameters() {
    return getEmbeddedParameters(selectedValue);
  }

  public void setEmbeddedParameters(ParameterSet params) {
    var embeddedParameters = getEmbeddedParameters();
    if (embeddedParameters == null) {
      parametersMap.put(selectedValue, params);
    } else {
      // copy parameters over. Just in case if there is already a component showing those parameters
      ParameterUtils.copyParameters(params, embeddedParameters);
    }
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
  public EnumType getValue() {
    return selectedValue;
  }

  @Override
  public void setValue(EnumType value) {
    this.selectedValue = value;
  }

  /**
   * @return parameters of selected option
   */
  public ParameterSet setOptionGetParameters(EnumType option) {
    setValue(option);
    return getEmbeddedParameters(option);
  }

  @Override
  public ModuleOptionsEnumComboParameter<EnumType> cloneParameter() {
    EnumMap<EnumType, ParameterSet> copy = new EnumMap<>(parametersMap);
    for (final EnumType key : copy.keySet()) {
      var cloneParam = copy.get(key).cloneParameterSet();
      copy.put(key, cloneParam);
    }
    return new ModuleOptionsEnumComboParameter<>(name, description, selectedValue, copy);
  }

  @Override
  public ModuleOptionsEnumComponent<EnumType> createEditingComponent() {
    return new ModuleOptionsEnumComponent<>(name, parametersMap, selectedValue, true);
  }

  @Override
  public void setValueFromComponent(ModuleOptionsEnumComponent<EnumType> component) {
    this.selectedValue = component.getValue();
    component.updateParameterSetFromComponents();
  }

  @Override
  public void setValueToComponent(ModuleOptionsEnumComponent<EnumType> component,
      @Nullable EnumType newValue) {
    component.setSelectedValue(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String selectedAttr = xmlElement.getAttribute(SELECTED_ATTRIBUTE);

    var childNodes = xmlElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      if (!(childNodes.item(i) instanceof Element nextElement) || !MODULE_ELEMENT.equals(
          nextElement.getTagName())) {
        continue;
      }

      String optionName = nextElement.getAttribute(NAME_ATTRIBUTE);
      getOptionByName(optionName).ifPresent(option -> {
        var parameters = parametersMap.get(option);
        if (parameters == null) {
          return;
        }
        parameters.loadValuesFromXML(nextElement);
      });
    }

    getOptionByName(selectedAttr).ifPresent(this::setValue);
  }

  private @NotNull Optional<EnumType> getOptionByName(final String id) {
    // find by stable ID
    return parametersMap.keySet().stream().filter(key -> key.getStableId().equals(id)).findFirst();
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    xmlElement.setAttribute(SELECTED_ATTRIBUTE, selectedValue.getStableId());

    var document = xmlElement.getOwnerDocument();

    for (final var entry : parametersMap.entrySet()) {
      Element paramElement = document.createElement(MODULE_ELEMENT);
      // save with stable ID which should not change even if module name changes
      paramElement.setAttribute(NAME_ATTRIBUTE, entry.getKey().getStableId());
      xmlElement.appendChild(paramElement);
      var parameterSet = entry.getValue();
      parameterSet.saveValuesToXML(paramElement);
    }
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (selectedValue == null) {
      return false;
    }
    return checkEmbeddedValues(errorMessages);
  }

  @Override
  public void setSkipSensitiveParameters(boolean skipSensitiveParameters) {
    // delegate skipSensitiveParameters to embedded ParameterSet
    for (var params : parametersMap.values()) {
      params.setSkipSensitiveParameters(skipSensitiveParameters);
    }
  }

  @Override
  public boolean valueEquals(Parameter<?> that) {
    if (!(that instanceof ModuleOptionsEnumComboParameter<?> thatOpt)) {
      return false;
    }

    if (!Objects.equals(selectedValue, thatOpt.getValue())) {
      return false;
    }

    return ParameterUtils.equalValues(getEmbeddedParameters(), thatOpt.getEmbeddedParameters(),
        false, false);
  }

  /**
   * @return method to retrieve value and parameters
   */
  public ValueWithParameters<EnumType> getValueWithParameters() {
    var value = getValue();
    var params = getEmbeddedParameters();
    return new ValueWithParameters<>(value, params);
  }

}
