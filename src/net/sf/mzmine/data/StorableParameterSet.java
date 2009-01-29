/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.data;

import org.dom4j.Element;

/**
 * This interface extends the ParametersSet with the functions to load/save
 * parameters into XML elements.
 */
public interface StorableParameterSet extends ParameterSet {

    /**
     * Export parameter values to XML representation
     * 
     */
    public void exportValuesToXML(Element element);

    /**
     * Import parameter values from XML representation
     */
    public void importValuesFromXML(Element element);

}
