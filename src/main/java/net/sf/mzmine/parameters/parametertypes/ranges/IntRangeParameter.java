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

import java.util.Collection;

import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Range;

public class IntRangeParameter
        implements UserParameter<Range<Integer>, IntRangeComponent> {

    private final String name, description;
    private final boolean valueRequired;
    private Range<Integer> value;

    public IntRangeParameter(String name, String description) {
        this(name, description, true, null);
    }

    public IntRangeParameter(String name, String description,
            boolean valueRequired, Range<Integer> defaultValue) {
        this.name = name;
        this.description = description;
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

    @Override
    public IntRangeComponent createEditingComponent() {
        return new IntRangeComponent();
    }

    public Range<Integer> getValue() {
        return value;
    }

    @Override
    public void setValue(Range<Integer> value) {
        this.value = value;
    }

    @Override
    public IntRangeParameter cloneParameter() {
        IntRangeParameter copy = new IntRangeParameter(name, description);
        copy.setValue(this.getValue());
        return copy;
    }

    @Override
    public void setValueFromComponent(IntRangeComponent component) {
        value = component.getValue();
    }

    @Override
    public void setValueToComponent(IntRangeComponent component,
            Range<Integer> newValue) {
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
        Integer min = Integer.valueOf(minText);
        Integer max = Integer.valueOf(maxText);
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
