/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.datatype;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.fx.ColumnID;
import io.github.mzmine.datamodel.features.types.fx.ColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    defaultDisableColumns();
  }

  private static @NotNull String getKey(boolean isFeatureType, Class<? extends DataType<?>> parent,
      Class<? extends DataType<?>> sub) {
    return ColumnID.buildUniqueIdString(
        isFeatureType ? ColumnType.FEATURE_TYPE : ColumnType.ROW_TYPE, parent, sub);
  }

  /**
   * Adds a data type to the list. The datatype is activated by default.
   *
   * @param dt The data type
   */
  public void addDataType(ColumnID dt) {
    addDataType(dt, dt.getDataType().getDefaultVisibility());
  }

  /**
   * Adds a data type to the list.
   *
   * @param dt The data type.
   * @param b  Selected or not.
   */
  public void addDataType(ColumnID dt, Boolean b) {
    final String key = getKey(dt);
    if (value.containsKey(key)) {
      logger.info("Already contains data type " + dt + ". Overwriting...");
    }

    value.put(key, b);
  }

  /**
   * Checks if the data type column has been displayed before. If the data type is not present yet,
   * it is added to the list and shown by default.
   *
   * @param dataTypeColumnId The data type.
   * @return true/false
   */
  public boolean isDataTypeVisible(ColumnID dataTypeColumnId) {
    Boolean val = value.get(getKey(dataTypeColumnId));
    if (val == null) {
      val = dataTypeColumnId.getDataType().getDefaultVisibility();

      // if this is a sub colum (subColIndex >= 0), use the visibility of the sub column type,
      // but only if the parent column is visible
      if (dataTypeColumnId.getDataType() instanceof SubColumnsFactory scf
          && dataTypeColumnId.getSubColIndex() >= 0) {
        if (isDataTypeVisible( // is the parent column visible?
            new ColumnID(dataTypeColumnId.getDataType(), dataTypeColumnId.getType(),
                dataTypeColumnId.getRaw(), -1))) {
          final int subColIndex = dataTypeColumnId.getSubColIndex();
          final DataType<?> subColDataType = (DataType<?>) scf.getType(subColIndex);
          val = subColDataType.getDefaultVisibility();
        } else {
          val = false;
        }
      }
      addDataType(dataTypeColumnId, val);
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
    return dataType.getUniqueIdString();
  }

  /**
   * Sets data type visibility value
   *
   * @param type data type
   * @param val  true/false
   */
  public void setDataTypeVisible(ColumnID type, Boolean val) {
    setDataTypeVisible(type.getUniqueIdString(), val);
  }

  /**
   * Sets data type visibility value
   *
   * @param typeUniqueId Name of the data type
   * @param val          true/false
   */
  public void setDataTypeVisible(String typeUniqueId, Boolean val) {
    value.put(typeUniqueId, val);
  }

  /**
   * Sets data types and their visibility values
   *
   * @param map Map containing new data types and their values
   */
  public void setDataTypesAndVisibility(Map<String, Boolean> map) {
    value.putAll(map);
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
      @Nullable Map<String, Boolean> newValue) {
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
    this.value.putAll(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList childs = xmlElement.getElementsByTagName(DATA_TYPE_ELEMENT);

    for (int i = 0; i < childs.getLength(); i++) {
      Element e = (Element) childs.item(i);
      String key = e.getAttribute(DATA_TYPE_KEY_ATTR);
      Boolean val = Boolean.valueOf(e.getAttribute(DATA_TYPE_VISIBLE_ATTR));

      final String replaced = key.replace("Feature:", "");
      if (key.contains(" ") || !replaced.equals(replaced.toLowerCase())) {
        // may be an old key from the time we were using the column headers
        continue;
      }

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
  public DataTypeCheckListParameter cloneParameter() {
    final DataTypeCheckListParameter clone = new DataTypeCheckListParameter(name, desc);
    clone.setValue(new HashMap<>(value));
    return clone;
  }

  public void setAll(boolean visible) {
    value.keySet().forEach(key -> value.put(key, visible));
  }

  /**
   * disable some types by default that don't make sense to show but would be shown usually. For
   * example, the mz type in ion identity would be shown by default because
   * {@link MZType#getDefaultVisibility()} mz itself is always on for a feature or a row.
   */
  private void defaultDisableColumns() {
    if (getName().toLowerCase().contains("row")) {
      value.put(getKey(false, IonIdentityListType.class, MZType.class), false);

      value.put(getKey(false, SpectralLibraryMatchesType.class, FormulaType.class), false);
      value.put(getKey(false, SpectralLibraryMatchesType.class, CCSType.class), false);

      value.put(getKey(false, CompoundDatabaseMatchesType.class, RTType.class), false);
      value.put(getKey(false, CompoundDatabaseMatchesType.class, CCSType.class), false);

      value.put(getKey(false, LipidMatchListType.class, FormulaType.class), false);
    }

    if (getName().toLowerCase().contains("feature")) {
      // add types here in the future
    }
  }
}
