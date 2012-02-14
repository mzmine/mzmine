/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.histogram;

import java.util.Collection;

import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.util.Range;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Simple Parameter implementation
 * 
 * 
 */
public class HistogramRangeParameter implements
		UserParameter<Range, HistogramRangeEditor> {

	private String name, description;
	private HistogramDataType selectedType = HistogramDataType.MASS;
	private Range value;

	public HistogramRangeParameter() {
		this.name = "Plotted data";
		this.description = "Plotted data type and range";
	}

	/**
	 * @see net.sf.mzmine.data.Parameter#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @see net.sf.mzmine.data.Parameter#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public HistogramRangeEditor createEditingComponent() {
		return new HistogramRangeEditor();
	}

	@Override
	public HistogramRangeParameter cloneParameter() {
		HistogramRangeParameter copy = new HistogramRangeParameter();
		copy.selectedType = this.selectedType;
		copy.value = this.value;
		return copy;
	}

	@Override
	public void setValueFromComponent(HistogramRangeEditor component) {
		this.selectedType = component.getSelectedType();
		this.value = component.getValue();
	}

	@Override
	public void setValueToComponent(HistogramRangeEditor component,
			Range newValue) {
		component.setValue(component.getSelectedType(), newValue);
	}

	@Override
	public Range getValue() {
		return value;
	}

	public HistogramDataType getType() {
		return selectedType;
	}

	@Override
	public void setValue(Range newValue) {
		value = newValue;
	}

	@Override
	public void loadValueFromXML(Element xmlElement) {

		String typeAttr = xmlElement.getAttribute("selected");
		if (typeAttr.length() == 0)
			return;

		this.selectedType = HistogramDataType.valueOf(typeAttr);

		NodeList minNodes = xmlElement.getElementsByTagName("min");
		if (minNodes.getLength() != 1)
			return;
		NodeList maxNodes = xmlElement.getElementsByTagName("max");
		if (maxNodes.getLength() != 1)
			return;

		String minText = minNodes.item(0).getTextContent();
		String maxText = maxNodes.item(0).getTextContent();
		double min = Double.valueOf(minText);
		double max = Double.valueOf(maxText);
		value = new Range(min, max);

	}

	@Override
	public void saveValueToXML(Element xmlElement) {
		if (value == null)
			return;

		Document parentDocument = xmlElement.getOwnerDocument();
		Element newElement = parentDocument.createElement("min");
		newElement.setTextContent(String.valueOf(value.getMin()));
		xmlElement.appendChild(newElement);
		newElement = parentDocument.createElement("max");
		newElement.setTextContent(String.valueOf(value.getMax()));
		xmlElement.appendChild(newElement);

		xmlElement.setAttribute("selected", selectedType.name());
	}

	@Override
	public boolean checkValue(Collection<String> errorMessages) {
		if (value == null) {
			errorMessages.add(name + " is not set");
			return false;
		}		
		return true;
	}
}
