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

package io.github.mzmine.modules.io.export_features_sql;

import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Simple Parameter implementation
 * 
 */
public class SQLColumnSettingsParameter
    implements UserParameter<SQLColumnSettings, SQLColumnSettingsComponent> {

  private static final String name = "Export columns";
  private static final String description =
      "Please set the mapping of feature list data to your database columns";

  @Nullable
  private SQLColumnSettings value;

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SQLColumnSettings getValue() {
    return value;
  }

  @Override
  public void setValue(SQLColumnSettings newValue) {
    this.value = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    final SQLColumnSettings value2 = value;
    if (value2 == null) {
      errorMessages.add(name + " parameter is not set properly");
      return false;
    }
    if (value2.getRowCount() == 0) {
      errorMessages.add("Please set at least one column to export");
      return false;
    }

    for (int row = 0; row < value2.getRowCount(); row++) {
      String sqlColumn = (String) value2.getValueAt(row, 0);
      SQLExportDataType dataType = (SQLExportDataType) value2.getValueAt(row, 1);
      String columnValue = (String) value2.getValueAt(row, 2);

      if ((sqlColumn == null) || (sqlColumn.length() == 0)) {
        errorMessages.add("Please set the column names");
        return false;
      }

      if (dataType == null) {
        errorMessages.add("Please set the column data types properly");
        return false;
      }

      if ((dataType.hasAdditionalValue())
          && ((columnValue == null) || (columnValue.length() == 0))) {
        errorMessages.add("Please set the column value for column " + sqlColumn);
        return false;
      }
    }

    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList items = xmlElement.getElementsByTagName("column");
    SQLColumnSettings newValue = new SQLColumnSettings();
    for (int i = 0; i < items.getLength(); i++) {
      newValue.addNewRow();
      Element moduleElement = (Element) items.item(i);
      String sqlColumn = moduleElement.getAttribute("name");
      String dataTypeName = moduleElement.getAttribute("datatype");
      SQLExportDataType dataType = SQLExportDataType.valueOf(dataTypeName);
      String columnValue = moduleElement.getAttribute("columnValue");
      newValue.setValueAt(sqlColumn, i, 0);
      newValue.setValueAt(dataType, i, 1);
      newValue.setValueAt(columnValue, i, 2);
    }
    this.value = newValue;

  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    final SQLColumnSettings value2 = value;
    if (value2 == null)
      return;
    Document parentDocument = xmlElement.getOwnerDocument();
    for (int row = 0; row < value2.getRowCount(); row++) {
      String sqlColumn = (String) value2.getValueAt(row, 0);
      SQLExportDataType dataType = (SQLExportDataType) value2.getValueAt(row, 1);
      String columnValue = (String) value2.getValueAt(row, 2);
      Element newElement = parentDocument.createElement("column");
      newElement.setAttribute("name", sqlColumn);
      newElement.setAttribute("datatype", dataType.name());
      newElement.setAttribute("columnValue", columnValue);
      xmlElement.appendChild(newElement);
    }
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public SQLColumnSettingsComponent createEditingComponent() {
    return new SQLColumnSettingsComponent();
  }

  @Override
  public void setValueFromComponent(SQLColumnSettingsComponent component) {
    this.value = component.getValue();
  }

  @Override
  public void setValueToComponent(SQLColumnSettingsComponent component,
      @Nullable SQLColumnSettings newValue) {
    if (newValue == null) {
      return;
    }
    component.setValue(newValue);
  }

  @Override
  public SQLColumnSettingsParameter cloneParameter() {
    SQLColumnSettingsParameter copy = new SQLColumnSettingsParameter();
    copy.setValue(this.getValue());
    return copy;
  }

}
