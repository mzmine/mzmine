/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Used to allow a list of types to be imported from a csv. Provides a mapping of selection state,
 * mzmine data type and column name in the csv.
 */
public class ImportTypeParameter implements UserParameter<List<ImportType>, ImportTypeComponent> {

  private static final String SELECTED = "selected";
  private static final String COL_NAME = "column";
  private static final String TYPE_NAME = "datatype";
  private static final String SUB_ELEMENT = "importtype";
  private final String name;
  private final String description;
  private final List<ImportType> values = new ArrayList<>();

  public ImportTypeParameter(String name, String description, List<ImportType> values) {
    this.name = name;
    this.description = description;
    this.values.addAll(values);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<ImportType> getValue() {
    return values;
  }

  @Override
  public void setValue(List<ImportType> newValue) {
    values.clear();
    values.addAll(newValue);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    final NodeList childNodes = xmlElement.getElementsByTagName(SUB_ELEMENT);

    for (int i = 0; i < childNodes.getLength(); i++) {
        final Element typeElement = (Element) childNodes.item(i);
        boolean selected = Boolean.valueOf(typeElement.getAttribute(SELECTED));
        String columnName = typeElement.getAttribute(COL_NAME);
        String typeName = typeElement.getAttribute(TYPE_NAME);

        for (ImportType val : values) {
          if (val.getDataType().getClass().getName().equals(typeName)) {
            val.setSelected(selected);
            val.setCsvColumnName(columnName);
            break;
          }
        }
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    final Document doc = xmlElement.getOwnerDocument();

    for (final ImportType value : values) {
      final Element element = doc.createElement(SUB_ELEMENT);
      element.setAttribute(SELECTED, String.valueOf(value.isSelected()));
      element.setAttribute(COL_NAME, value.getCsvColumnName());
      element.setAttribute(TYPE_NAME, value.getDataType().getClass().getName());
      xmlElement.appendChild(element);
    }
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public ImportTypeComponent createEditingComponent() {
    return new ImportTypeComponent();
  }

  @Override
  public void setValueFromComponent(ImportTypeComponent importTypeComponent) {
    values.clear();
    values.addAll(importTypeComponent.getValue());
  }

  @Override
  public void setValueToComponent(ImportTypeComponent importTypeComponent,
      List<ImportType> newValue) {
    importTypeComponent.setValue(newValue);
  }

  @Override
  public UserParameter<List<ImportType>, ImportTypeComponent> cloneParameter() {

    List<ImportType> newValue = new ArrayList<>(values.size());
    for (ImportType importType : values) {
      newValue.add(new ImportType(importType.isSelected(), importType.getCsvColumnName(),
          importType.getDataType()));
    }

    return new ImportTypeParameter(name, description, newValue);
  }
}
