/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.parameters.parametertypes;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Collection;

import net.sf.mzmine.parameters.Parameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class WindowSettingsParameter implements Parameter<WindowSettings> {

    // Since this is a special parameter (cannot be directly set by the user),
    // we can use a singleton for the value and declare it as final
    private final WindowSettings value = new WindowSettings();

    @Override
    public String getName() {
	return "Window state";
    }

    @Override
    public WindowSettingsParameter cloneParameter() {
	return this;
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
	return true;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {

	// Window position
	NodeList posElement = xmlElement.getElementsByTagName("position");
	if (posElement.getLength() == 1) {
	    String posString = posElement.item(0).getTextContent();
	    String posArray[] = posString.split(":");
	    int posX = Integer.valueOf(posArray[0]);
	    int posY = Integer.valueOf(posArray[1]);
	    Point pos = new Point(posX, posY);
	    value.setPosition(pos);
	}

	// Window size
	NodeList sizeElement = xmlElement.getElementsByTagName("size");
	if (sizeElement.getLength() == 1) {
	    String sizeString = sizeElement.item(0).getTextContent();
	    String sizeArray[] = sizeString.split(":");
	    int width = Integer.parseInt(sizeArray[0]);
	    int height = Integer.parseInt(sizeArray[1]);
	    Dimension dim = new Dimension(width, height);
	    value.setDimension(dim);
	}
    }

    @Override
    public void saveValueToXML(Element xmlElement) {

	if (value == null)
	    return;

	Point pos = value.getPosition();
	Dimension dim = value.getDimension();

	if (pos == null || dim == null)
	    return;

	// Add elements
	Document doc = xmlElement.getOwnerDocument();
	Element positionElement = doc.createElement("position");
	xmlElement.appendChild(positionElement);
	positionElement.setTextContent(pos.x + ":" + pos.y);
	Element sizeElement = doc.createElement("size");
	xmlElement.appendChild(sizeElement);
	sizeElement.setTextContent(dim.width + ":" + dim.height);
    }

    @Override
    public WindowSettings getValue() {
	return value;
    }

    @Override
    public void setValue(WindowSettings newValue) {
	throw new IllegalArgumentException(
		"Please use getValue() to update the value");
    }

}
