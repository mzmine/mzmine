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

package io.github.mzmine.parameters.parametertypes.datatype;

import io.github.mzmine.datamodel.features.types.fx.ColumnID;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DataTypeCheckListParameter implements
    UserParameter<Map<String, Boolean>, DataTypeCheckListComponent> {

  private static final Logger logger = Logger.getLogger(DataTypeCheckListParameter.class.getName());
  private static final String DATA_TYPE_ELEMENT = "datatype";
  private static final String DATA_TYPE_VISIBLE_ATTR = "visible";
  private static final String DATA_TYPE_KEY_ATTR = "key";
  private static final String DATA_TYPE_NAME_ATTR = "name";
  private static final String DATA_TYPE_SUB_COL_INDEX_ATTR = "sub_col_index";
  private static final String DATA_TYPE_COLUMN_TYPE_ATTR = "col_type";
  private final String name;
  private final String desc;
  private DataTypeCheckListComponent comp;
  private Map<String, Boolean> value;


  public DataTypeCheckListParameter(@NotNull String name, @NotNull String description) {
    this.name = name;
    this.desc = description;
    this.value = new HashMap<>();
  }

  /**
   * Adds a data type to the list. The datatype is activated by default.
   *
   * @param dt The data type
   */
  public void addDataType(ColumnID dt) {
    addDataType(dt, true);
  }

  /**
   * Adds a data type to the list.
   *
   * @param dt The data type.
   * @param b  Selected or not.
   */
  public void addDataType(ColumnID dt, Boolean b) {
    final String key = getKey(dt);
    if (value.keySet().contains(key)) {
      logger.info("Already contains data type " + dt + ". Overwriting...");
    }

    value.put(key, b);
  }

  /**
   * Checks if the data type column has been displayed before. If the data type is not present yet,
   * it is added to the list and shown by default.
   *
   * @param dataType The data type.
   * @return true/false
   */
  public boolean isDataTypeVisible(ColumnID dataType) {
    Boolean val = value.get(getKey(dataType));
    if (val == null) {
      val = true;
      addDataType(dataType, val);
    }
    return val;
  }

  /**
   * Uses the combined header string as key (raw data unspecific)
   *
   * @param dataType the column
   * @return combined header key
   */
  public String getKey(ColumnID dataType) {
    return dataType.getCombinedHeaderString();
  }


  /**
   * Sets data type visibility value
   *
   * @param type data type
   * @param val  true/false
   */
  public void setDataTypeVisible(ColumnID type, Boolean val) {
    setDataTypeVisible(type.getCombinedHeaderString(), val);
  }

  /**
   * Sets data type visibility value
   *
   * @param typeHeader Name of the data type
   * @param val        true/false
   */
  public void setDataTypeVisible(String typeHeader, Boolean val) {
    value.put(typeHeader, val);
  }

  /**
   * Sets data types and their visibility values
   *
   * @param map Map containing new data types and their values
   */
  public void setDataTypesAndVisibility(Map<String, Boolean> map) {
    value = new HashMap<>(map);
  }

  @Override
  public String getDescription() {
    return desc;
  }

  @Override
  public DataTypeCheckListComponent createEditingComponent() {
    comp = new DataTypeCheckListComponent();
    return comp;
  }

  @Override
  public void setValueFromComponent(DataTypeCheckListComponent dataTypeCheckListComponent) {
    assert dataTypeCheckListComponent == comp;

    value = dataTypeCheckListComponent.getValue();
  }

  @Override
  public void setValueToComponent(DataTypeCheckListComponent dataTypeCheckListComponent,
      Map<String, Boolean> newValue) {
    assert dataTypeCheckListComponent == comp;
    if (!(newValue instanceof HashMap)) {
      return;
    }
    comp.setValue(newValue);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Map<String, Boolean> getValue() {
    return value;
  }

  @Override
  public void setValue(Map<String, Boolean> newValue) {
    this.value = newValue;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList childs = xmlElement.getElementsByTagName(DATA_TYPE_ELEMENT);

    for (int i = 0; i < childs.getLength(); i++) {
      Element e = (Element) childs.item(i);
      String key = e.getAttribute(DATA_TYPE_KEY_ATTR);
      Boolean val = Boolean.valueOf(e.getAttribute(DATA_TYPE_VISIBLE_ATTR));
      value.put(key, val);
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    Document doc = xmlElement.getOwnerDocument();

    value.forEach((dt, b) -> {
      Element element = doc.createElement(DATA_TYPE_ELEMENT);
      element.setAttribute(DATA_TYPE_KEY_ATTR, dt);
      element.setAttribute(DATA_TYPE_VISIBLE_ATTR, b.toString());
      xmlElement.appendChild(element);
    });
  }

  @Override
  public boolean isSensitive() {
    return false;
  }

  @Override
  public boolean checkValue(Collection errorMessages) {
    return value != null;
  }

  @Override
  public UserParameter cloneParameter() {
    return null;
  }

  public void setAll(boolean visible) {
    value.keySet().forEach(key -> value.put(key, visible));
  }
}
