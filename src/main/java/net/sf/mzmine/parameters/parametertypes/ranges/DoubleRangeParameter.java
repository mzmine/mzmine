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

package net.sf.mzmine.parameters.parametertypes.ranges;

import java.text.NumberFormat;
import java.util.Collection;

import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Range;

public class DoubleRangeParameter
        implements UserParameter<Range<Double>, DoubleRangeComponent> {

    private final String name, description;
    protected final boolean valueRequired;
    private NumberFormat format;
    private Range<Double> value;

    public DoubleRangeParameter(String name, String description,
            NumberFormat format) {
        this(name, description, format, true, null);
    }

    public DoubleRangeParameter(String name, String description,
            NumberFormat format, Range<Double> defaultValue) {
        this(name, description, format, true, defaultValue);
    }

    public DoubleRangeParameter(String name, String description,
            NumberFormat format, boolean valueRequired,
            Range<Double> defaultValue) {
        this.name = name;
        this.description = description;
        this.format = format;
        this.valueRequired = valueRequired;
        this.value = defaultValue;
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

    public boolean isValueRequired() {
        return valueRequired;
    }

    @Override
    public DoubleRangeComponent createEditingComponent() {
        return new DoubleRangeComponent(format);
    }

    public Range<Double> getValue() {
        return value;
    }

    @Override
    public void setValue(Range<Double> value) {
        this.value = value;
    }

    @Override
    public DoubleRangeParameter cloneParameter() {
        DoubleRangeParameter copy = new DoubleRangeParameter(name, description,
                format);
        copy.setValue(this.getValue());
        return copy;
    }

    @Override
    public void setValueFromComponent(DoubleRangeComponent component) {
        value = component.getValue();
    }

    @Override
    public void setValueToComponent(DoubleRangeComponent component,
            Range<Double> newValue) {
        component.setValue(newValue);
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
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
        value = Range.closed(min, max);
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
        if (value == null)
            return;
        Document parentDocument = xmlElement.getOwnerDocument();
        Element newElement = parentDocument.createElement("min");
        newElement.setTextContent(String.valueOf(value.lowerEndpoint()));
        xmlElement.appendChild(newElement);
        newElement = parentDocument.createElement("max");
        newElement.setTextContent(String.valueOf(value.upperEndpoint()));
        xmlElement.appendChild(newElement);
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
        if (valueRequired && (value == null)) {
            errorMessages.add(name + " is not set properly");
            return false;
        }
        if ((value != null)
                && (value.lowerEndpoint() > value.upperEndpoint())) {
            errorMessages.add(name
                    + " range maximum must be higher than minimum, or equal");
            return false;
        }

        return true;
    }

}
