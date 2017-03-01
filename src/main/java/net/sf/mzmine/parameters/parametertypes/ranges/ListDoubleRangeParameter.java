/* 
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.mzmine.parameters.parametertypes.ranges;

import com.google.common.collect.Range;
import java.util.Collection;
import java.util.List;
import net.sf.mzmine.parameters.UserParameter;
import org.w3c.dom.Element;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */


public class ListDoubleRangeParameter 
        implements UserParameter <List <Range <Double>>, ListDoubleRangeComponent>
{
    private final String name, description;
    private final boolean valueRequired;
    
    private List <Range <Double>> value;
    
    public ListDoubleRangeParameter (String name, String description,
            boolean valueRequired, List <Range <Double>> defaultValue)
    {
        this.name = name;
        this.description = description;
        this.valueRequired = valueRequired;
        this.value = defaultValue;
    }
    
    @Override
    public String getName() {return name;}
    
    @Override
    public String getDescription() {return description;}
    
    public boolean isValueRequired() {return valueRequired;}
    
    @Override
    public ListDoubleRangeComponent createEditingComponent() {
        return new ListDoubleRangeComponent();
    }
    
    @Override
    public List <Range <Double>> getValue() {return value;}
    
    @Override 
    public void setValue (List <Range <Double>> value) {
        this.value = value;
    }
    
    @Override
    public ListDoubleRangeParameter cloneParameter() {
        ListDoubleRangeParameter copy = new ListDoubleRangeParameter(
                name, description, valueRequired, value);
        copy.setValue(value);
        return copy;
    }
    
    @Override
    public void setValueFromComponent(ListDoubleRangeComponent component) {
        value = component.getValue();
    }

    @Override
    public void setValueToComponent(ListDoubleRangeComponent component,
            List <Range <Double>> newValue) {
        component.setValue(newValue);
    }
    
    @Override
    public void loadValueFromXML(Element xmlElement)
    {
        value = dulab.adap.common.algorithms.String.toRanges(
                xmlElement.getTextContent());
    }
    
    @Override
    public void saveValueToXML (Element xmlElement)
    {
        if (value == null) return;
        
        xmlElement.setTextContent(
                dulab.adap.common.algorithms.String.fromRanges(value));
    }
    
    @Override
    public boolean checkValue(Collection <String> errorMessages)
    {
        if (valueRequired && value == null) {
            errorMessages.add(name + " is not set properly");
            return false;
        }
        
        return true;
    }
}
