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

/*
 * Code created was by or on behalf of Syngenta and is released under the open source license in use
 * for the pre-existing code or project. Syngenta does not assert ownership or copyright any over
 * pre-existing work.
 */

package io.github.mzmine.modules.dataprocessing.id_adductsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import io.github.mzmine.parameters.UserParameter;

/**
 * Adducts parameter.
 *
 */
public class AdductsParameter implements UserParameter<List<AdductType>, AdductsComponent> {

  // Logger.
  private static final Logger logger = Logger.getLogger(AdductsParameter.class.getName());

  private final String name, description;
  private List<AdductType> choices = new ArrayList<>();
  private List<AdductType> value = new ArrayList<>();

  // XML tags.
  private static final String ADDUCTS_TAG = "adduct";
  private static final String NAME_ATTRIBUTE = "name";
  private static final String MASS_ATTRIBUTE = "mass_difference";
  private static final String SELECTED_ATTRIBUTE = "selected";

  /**
   * Create the parameter.
   *
   * @param name name of the parameter.
   * @param description description of the parameter.
   */
  public AdductsParameter(final String name, final String description) {
    this.name = name;
    this.description = description;

    this.choices.addAll(Arrays.asList(AdductType.getDefaultValues()));
  }

  @Override
  public AdductsComponent createEditingComponent() {
    return new AdductsComponent(choices);
  }

  @Override
  public void setValueFromComponent(final AdductsComponent component) {
    this.value = component.getValue();
    this.choices = component.getChoices();
  }

  @Override
  public void loadValueFromXML(final Element xmlElement) {

    // Get the XML tag.
    final NodeList items = xmlElement.getElementsByTagName(ADDUCTS_TAG);
    final int length = items.getLength();

    choices.clear();
    value.clear();

    // Process each adduct.
    for (int i = 0; i < length; i++) {

      final Node item = items.item(i);

      // Get attributes.
      final NamedNodeMap attributes = item.getAttributes();
      final Node nameNode = attributes.getNamedItem(NAME_ATTRIBUTE);
      final Node massNode = attributes.getNamedItem(MASS_ATTRIBUTE);
      final Node selectedNode = attributes.getNamedItem(SELECTED_ATTRIBUTE);

      // Valid attributes?
      if (nameNode != null && massNode != null) {

        try {
          // Create new adduct.
          final AdductType adduct =
              new AdductType(nameNode.getNodeValue(), Double.parseDouble(massNode.getNodeValue()));

          // A new choice?
          if (!choices.contains(adduct)) {
            choices.add(adduct);
          }

          // Selected?
          if (!value.contains(adduct) && selectedNode != null
              && Boolean.parseBoolean(selectedNode.getNodeValue())) {
            value.add(adduct);
          }
        } catch (NumberFormatException ex) {
          // Ignore.
          logger.warning("Illegal mass difference attribute in " + item.getNodeValue());
        }
      }
    }

  }

  @Override
  public void saveValueToXML(final Element xmlElement) {

    // Get choices and selections.
    final Document parent = xmlElement.getOwnerDocument();
    for (final AdductType item : choices) {

      final Element element = parent.createElement(ADDUCTS_TAG);
      element.setAttribute(NAME_ATTRIBUTE, item.getName());
      element.setAttribute(MASS_ATTRIBUTE, Double.toString(item.getMassDifference()));
      element.setAttribute(SELECTED_ATTRIBUTE, Boolean.toString(value.contains(item)));
      xmlElement.appendChild(element);
    }
  }

  @Override
  public AdductsParameter cloneParameter() {

    final AdductsParameter copy = new AdductsParameter(getName(), getDescription());
    copy.setChoices(new ArrayList<>(choices));
    copy.setValue(new ArrayList<>(value));
    return copy;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<AdductType> getValue() {
    return value;
  }

  @Override
  public void setValue(List<AdductType> newValue) {
    this.value = newValue;
  }

  public void setChoices(List<AdductType> newChoices) {
    this.choices = newChoices;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setValueToComponent(AdductsComponent component, List<AdductType> newValue) {
    component.setValue(newValue);
  }
}
