/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.github.mzmine.parameters.UserParameter;

public class CustomLipidClassChoiceParameter
		implements UserParameter<CustomLipidClass[], CustomLipidClassChoiceComponent> {

	private final String name;
	private final String description;
	private CustomLipidClass[] choices;
	private CustomLipidClass[] values;

  /**
   * Create the parameter.
   *
   * @param name name of the parameter.
   * @param description description of the parameter.
   */
  public CustomLipidClassChoiceParameter(String name, String description,
			CustomLipidClass[] choices) {
    this.name = name;
    this.description = description;
    this.choices = choices;
    this.values = choices;
  }

  @Override
  public CustomLipidClassChoiceComponent createEditingComponent() {
    return new CustomLipidClassChoiceComponent(choices);
  }

  @Override
  public void setValueFromComponent(final CustomLipidClassChoiceComponent component) {
		values = component.getValue().toArray(new CustomLipidClass[component.getValue().size()]);
		choices = component.getChoices().toArray(new CustomLipidClass[component.getChoices().size()]);
  }

  @Override
  public void setValueToComponent(CustomLipidClassChoiceComponent component,
			CustomLipidClass[] newValue) {
		component.setValue(Arrays.asList(newValue));
  }

  @Override
  public CustomLipidClassChoiceParameter cloneParameter() {

    final CustomLipidClassChoiceParameter copy =
        new CustomLipidClassChoiceParameter(name, description, choices);
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
	public CustomLipidClass[] getValue() {
    return values;
  }

	public CustomLipidClass[] getChoices() {
    return choices;
  }

  @Override
	public void setValue(CustomLipidClass[] newValue) {
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
    for (int i = 0; i < items.getLength(); i++) {
      String itemString = items.item(i).getTextContent();
		for (int j = 0; j < choices.length; j++) {
			if (choices[j].toString().equals(itemString)) {
				newValues.add(choices[j]);
        }
      }
    }
	this.values = newValues.toArray(new CustomLipidClass[0]);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (values == null)
      return;
    Document parentDocument = xmlElement.getOwnerDocument();
	for (CustomLipidClass item : values) {
      Element newElement = parentDocument.createElement("item");
      newElement.setTextContent(item.toString());
      xmlElement.appendChild(newElement);
    }
  }

}
