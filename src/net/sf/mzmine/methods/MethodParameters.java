/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.methods;

import java.io.Serializable;

import net.sf.mzmine.data.Parameter;

import org.w3c.dom.Element;
import org.w3c.dom.Document;


/**
 *
 */
public interface MethodParameters extends Serializable {

    /**
     * @return parameters in human readable form
     */
    public String toString();

    /**
     * Adds parameters to XML document
     */
    public Element addToXML(Document doc);

    /**
     * Reads parameters from XML
     * @param doc XML document supposed to contain parameters for the method (may not contain them, though)
     */
    public void readFromXML(Element element);

    
    /**
     * 
     * @return all parameters 
     */
    public Parameter[] getParameters();
    
}
