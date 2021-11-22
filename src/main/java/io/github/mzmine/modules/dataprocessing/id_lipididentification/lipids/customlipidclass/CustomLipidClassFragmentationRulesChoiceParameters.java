/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.customlipidclass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.parameters.UserParameter;

public class CustomLipidClassFragmentationRulesChoiceParameters implements
    UserParameter<LipidFragmentationRule[], CustomLipidClassFragmentationRulesChoiceComponent> {

  private final String name;
  private final String description;
  private LipidFragmentationRule[] choices;
  private LipidFragmentationRule[] values;

  /**
   * Create the parameter.
   *
   * @param name name of the parameter.
   * @param description description of the parameter.
   */
  public CustomLipidClassFragmentationRulesChoiceParameters(String name, String description,
      LipidFragmentationRule[] choices) {
    this.name = name;
    this.description = description;
    this.choices = choices;
    this.values = choices;
  }

  @Override
  public CustomLipidClassFragmentationRulesChoiceComponent createEditingComponent() {
    return new CustomLipidClassFragmentationRulesChoiceComponent(choices);
  }

  @Override
  public void setValueFromComponent(
      final CustomLipidClassFragmentationRulesChoiceComponent component) {
    values = component.getValue().toArray(new LipidFragmentationRule[0]);
    choices =
        component.getChoices().toArray(new LipidFragmentationRule[0]);
  }

  @Override
  public void setValueToComponent(CustomLipidClassFragmentationRulesChoiceComponent component,
      LipidFragmentationRule[] newValue) {
    component.setValue(Arrays.asList(newValue));
  }

  @Override
  public CustomLipidClassFragmentationRulesChoiceParameters cloneParameter() {

    final CustomLipidClassFragmentationRulesChoiceParameters copy =
        new CustomLipidClassFragmentationRulesChoiceParameters(name, description, choices);
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
  public LipidFragmentationRule[] getValue() {
    return values;
  }

  public LipidFragmentationRule[] getChoices() {
    return choices;
  }

  @Override
  public void setValue(LipidFragmentationRule[] newValue) {
    this.values = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList items = xmlElement.getElementsByTagName("item");
    ArrayList<LipidFragmentationRule> newValues = new ArrayList<>();
    for (int i = 0; i < items.getLength(); i++) {
      String itemString = items.item(i).getTextContent();
      for (int j = 0; j < choices.length; j++) {
        if (choices[j].toString().equals(itemString)) {
          newValues.add(choices[j]);
        }
      }
    }
    this.values = newValues.toArray(new LipidFragmentationRule[0]);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (values == null)
      return;
    Document parentDocument = xmlElement.getOwnerDocument();
    for (LipidFragmentationRule item : values) {
      Element newElement = parentDocument.createElement("item");
      newElement.setTextContent(item.toString());
      xmlElement.appendChild(newElement);
    }
  }
}
