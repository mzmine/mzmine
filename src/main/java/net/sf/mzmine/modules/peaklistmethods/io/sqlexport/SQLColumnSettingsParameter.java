/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.io.sqlexport;

import java.util.Collection;

import javax.annotation.Nullable;

import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Simple Parameter implementation
 * 
 */
public class SQLColumnSettingsParameter implements
        UserParameter<SQLColumnSettings, SQLColumnSettingsComponent> {

    private static final String name = "Export columns";
    private static final String description = "Please set the mapping of peak list data to your database columns";

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
            SQLExportDataType dataType = (SQLExportDataType) value2
                    .getValueAt(row, 1);
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
                errorMessages.add(
                        "Please set the column value for column " + sqlColumn);
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
            SQLExportDataType dataType = SQLExportDataType
                    .valueOf(dataTypeName);
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
            SQLExportDataType dataType = (SQLExportDataType) value2
                    .getValueAt(row, 1);
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
            SQLColumnSettings newValue) {
        component.setValue(newValue);
    }

    @Override
    public SQLColumnSettingsParameter cloneParameter() {
        SQLColumnSettingsParameter copy = new SQLColumnSettingsParameter();
        copy.setValue(this.getValue());
        return copy;
    }

}
