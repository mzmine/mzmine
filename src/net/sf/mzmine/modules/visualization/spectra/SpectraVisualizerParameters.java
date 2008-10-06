/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.spectra;

import java.util.Iterator;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

import org.dom4j.Element;

public class SpectraVisualizerParameters extends SimpleParameterSet {

    public static final Parameter scanNumber = new SimpleParameter(
            ParameterType.INTEGER, "Scan number", "Scan number", null, null, 1,
            null);
    
    public static double barThickness = 0.0005d;

    public SpectraVisualizerParameters() {
        super(new Parameter[] { scanNumber });
    }
    
    public void exportValuesToXML(Element element) {
    	super.exportValuesToXML(element);
    	
        Element newElement = element.addElement(PARAMETER_ELEMENT_NAME);

        newElement.addAttribute(PARAMETER_NAME_ATTRIBUTE, "thickness");
        newElement.addAttribute(PARAMETER_TYPE_ATTRIBUTE,
                "DOUBLE");

        Object value = barThickness;
        if (value != null) {
            String valueAsString;
                valueAsString = value.toString();
            newElement.addText(valueAsString);
        }
    	
    }
    
    public void importValuesFromXML(Element element) {
    	super.importValuesFromXML(element);

        Iterator paramIter = element.elementIterator(PARAMETER_ELEMENT_NAME);

        while (paramIter.hasNext()) {
            Element paramElem = (Element) paramIter.next();
            String name = paramElem.attributeValue(PARAMETER_NAME_ATTRIBUTE);

            if (name.equals("thickness")) {
            	
                String valueText = paramElem.getText();
                if ((valueText == null) || (valueText.length() == 0))
                    break;

                barThickness = Double.parseDouble(valueText);
                break;
            }

        }

    	
    }

}
