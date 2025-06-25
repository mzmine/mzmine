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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Stores all information regarding custom lipid classes, including the fragmentation rules.
 */
public class CustomLipidClassChoiceParameter
    implements UserParameter<CustomLipidClass[], CustomLipidClassChoiceComponent> {

  private final String name;
  private final String description;
  @Nullable
  private CustomLipidClass[] values;

  /**
   * Create the parameter.
   *
   * @param name name of the parameter.
   * @param description description of the parameter.
   */
  public CustomLipidClassChoiceParameter(String name, String description,
      @Nullable CustomLipidClass[] choices) {
    this.name = name;
    this.description = description;
    this.values = choices;
  }

  @Override
  public CustomLipidClassChoiceComponent createEditingComponent() {
    return new CustomLipidClassChoiceComponent(values);
  }

  @Override
  public void setValueFromComponent(final CustomLipidClassChoiceComponent component) {
    values = component.getValue().toArray(new CustomLipidClass[0]);
  }

  @Override
  public void setValueToComponent(CustomLipidClassChoiceComponent component,
      @Nullable CustomLipidClass[] newValue) {
    if (newValue == null) {
      component.setValue(List.of());
      return;
    }
    component.setValue(Arrays.asList(newValue));
  }

  @Override
  public CustomLipidClassChoiceParameter cloneParameter() {
    final CustomLipidClassChoiceParameter copy =
        new CustomLipidClassChoiceParameter(name, description, values);
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
  @Nullable
  public CustomLipidClass[] getValue() {
    return values;
  }

  @Override
  public void setValue(@Nullable CustomLipidClass[] newValue) {
    this.values = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList items = xmlElement.getElementsByTagName("item");
    ArrayList<CustomLipidClass> newValues = new ArrayList<>();
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    for (int i = 0; i < items.getLength(); i++) {
      String itemString = items.item(i).getTextContent();
      CustomLipidClass customLipidClass = gson.fromJson(itemString,
          new TypeToken<CustomLipidClass>() {
          }.getType());
      newValues.add(customLipidClass);
    }
    this.values = newValues.toArray(CustomLipidClass[]::new);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (values == null)
      return;
    Document parentDocument = xmlElement.getOwnerDocument();
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    for (CustomLipidClass item : values) {
      Element newElement = parentDocument.createElement("item");
      newElement.setTextContent(gson.toJson(item));
      xmlElement.appendChild(newElement);
    }
  }
}
