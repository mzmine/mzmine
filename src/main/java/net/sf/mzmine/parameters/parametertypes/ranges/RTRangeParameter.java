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

import com.google.common.collect.Range;

import java.text.NumberFormat;
import java.util.Collection;

public class RTRangeParameter extends DoubleRangeParameter {

    public RTRangeParameter() {
	super("Retention time", "Retention time range in minutes", null, true, null);
    }

    public RTRangeParameter(boolean valueRequired) {
        super("Retention time", "Retention time range in minutes", null, valueRequired, null);
    }

    public RTRangeParameter(String name, String description, boolean valueRequired, Range<Double> defaultValue) {
	    super(name, description, null, valueRequired, defaultValue);
    }

    public RTRangeParameter(String name, String description,
                            NumberFormat format, boolean valueRequired,
                            Range<Double> defaultValue)
    {
        super(name, description, format, valueRequired, defaultValue);
    }
    
    @Override
    public boolean checkValue(Collection<String> errorMessages) {
	if (valueRequired && (this.getValue() == null)) {
	    errorMessages.add(this.getName() + " is not set properly");
	    return false;
	}
        if ((this.getValue() != null) && (this.getValue().lowerEndpoint() < 0.0)) {
	    errorMessages.add("RT lower end point must not be negative");
	    return false;
	}
        if ((this.getValue() != null) && (this.getValue().upperEndpoint() <= this.getValue().lowerEndpoint())) {
	    errorMessages.add("RT lower end point must be less than upper end point");
	    return false;
	}
        
	return true;
    }

    @Override
    public RTRangeComponent createEditingComponent() {
	return new RTRangeComponent();
    }

    @Override
    public RTRangeParameter cloneParameter() {
	RTRangeParameter copy = new RTRangeParameter(getName(),
		getDescription(), isValueRequired(), getValue());
	return copy;
    }

}
