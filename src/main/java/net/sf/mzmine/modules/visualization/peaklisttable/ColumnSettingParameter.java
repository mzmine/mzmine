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

package net.sf.mzmine.modules.visualization.peaklisttable;

import java.util.Arrays;

import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ColumnSettingParameter<ValueType> extends
	MultiChoiceParameter<ValueType> {

    private int columnWidths[];

    public ColumnSettingParameter(String name, String description,
	    ValueType choices[]) {
	super(name, description, choices, choices, 0);
	columnWidths = new int[choices.length];
	Arrays.fill(columnWidths, 100);
    }

    public int getColumnWidth(int index) {
	return columnWidths[index];
    }

    public void setColumnWidth(int index, int width) {
	columnWidths[index] = width;
    }

    @Override
    public ColumnSettingParameter<ValueType> cloneParameter() {
	ColumnSettingParameter<ValueType> copy = new ColumnSettingParameter<ValueType>(
		getName(), getDescription(), getChoices());
	copy.setValue(getValue());
	copy.columnWidths = columnWidths.clone();
	return copy;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
	super.loadValueFromXML(xmlElement);

	// If loading of the parameters caused all columns to be hidden, ignore
	// the loaded value and set all to visible
	ValueType newValues[] = getValue();
	if (newValues.length == 0)
	    setValue(getChoices());

	NodeList items = xmlElement.getElementsByTagName("widths");
	if (items.getLength() != 1)
	    return;
	String widthsString = items.item(0).getTextContent();
	String widthsArray[] = widthsString.split(":");
	if (widthsArray.length != getChoices().length)
	    return;
	int newColumnWidths[] = new int[widthsArray.length];
	for (int i = 0; i < newColumnWidths.length; i++) {
	    newColumnWidths[i] = Integer.parseInt(widthsArray[i]);
	}
	columnWidths = newColumnWidths;
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
	super.saveValueToXML(xmlElement);
	Document parentDocument = xmlElement.getOwnerDocument();
	Element widthsElement = parentDocument.createElement("widths");
	StringBuilder widthsString = new StringBuilder();
	for (int i = 0; i < columnWidths.length; i++) {
	    widthsString.append(String.valueOf(columnWidths[i]));
	    if (i < columnWidths.length - 1)
		widthsString.append(":");
	}
	widthsElement.setTextContent(widthsString.toString());
	xmlElement.appendChild(widthsElement);

    }

}
