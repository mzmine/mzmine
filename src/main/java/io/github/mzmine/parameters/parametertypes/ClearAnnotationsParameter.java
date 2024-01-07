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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ClearAnnotationsParameter implements
    UserParameter<Map<DataType<?>, Boolean>, ClearAnnotationsComponent> {

  private static final String DATA_TYPE_ELEMENT = "datatype";
  private static final String DATA_TYPE_ATTR = "type";

  private final String name;
  private final String description;
  private Map<DataType<?>, Boolean> value;

  public ClearAnnotationsParameter(String name, String description) {
    this.name = name;
    this.description = description;
    value = DataTypes.getInstances().stream().filter(DataTypes::isMainAnnotation)
        .collect(Collectors.toMap(t -> t, t -> false));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return getValue() != null;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

    value = DataTypes.getInstances().stream().filter(DataTypes::isMainAnnotation)
        .collect(Collectors.toMap(t -> t, t -> false));

    final NodeList childNodes = xmlElement.getChildNodes();
    final int length = childNodes.getLength();
    for (int i = 0; i < length; i++) {
      final Node item = childNodes.item(i);
      if (!(item instanceof Element element)) {
        continue;
      }
      final Optional<DataType<?>> opt = value.keySet().stream()
          .filter(type -> type.getClass().getName().equals(element.getAttribute(DATA_TYPE_ATTR)))
          .findFirst();

      if (opt.isEmpty()) {
        continue;
      }

      final DataType<?> dataType = opt.get();
      value.put(dataType, Boolean.valueOf(element.getTextContent()));
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    final Document ownerDocument = xmlElement.getOwnerDocument();

    for (Entry<DataType<?>, Boolean> entry : value.entrySet()) {
      final Element element = ownerDocument.createElement(DATA_TYPE_ELEMENT);
      element.setAttribute(DATA_TYPE_ATTR, entry.getKey().getClass().getName());
      element.setTextContent(String.valueOf(entry.getValue()));
      xmlElement.appendChild(element);
    }
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public ClearAnnotationsComponent createEditingComponent() {
    return new ClearAnnotationsComponent(new HashMap<>(value));
  }

  @Override
  public void setValueFromComponent(ClearAnnotationsComponent clearAnnotationsComponent) {
    value = clearAnnotationsComponent.getValue();
  }

  @Override
  public void setValueToComponent(ClearAnnotationsComponent clearAnnotationsComponent,
      @Nullable Map<DataType<?>, Boolean> newValue) {
    clearAnnotationsComponent.setValue(newValue);
  }

  @Override
  public Map<DataType<?>, Boolean> getValue() {
    return value;
  }

  @Override
  public void setValue(Map<DataType<?>, Boolean> newValue) {
    this.value = newValue;
  }

  @Override
  public UserParameter<Map<DataType<?>, Boolean>, ClearAnnotationsComponent> cloneParameter() {
    var val = new LinkedHashMap<>(value);
    var param = new ClearAnnotationsParameter(name, description);
    param.setValue(val);
    return param;
  }
}
