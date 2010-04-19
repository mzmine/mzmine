/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.data;

public enum ParameterType {

    /**
     * Parameter values represented by String instance
     */
    STRING,

    /**
     * Parameter values represented by Integer instance
     */
    INTEGER,

    /**
     * Parameter values represented by Double instance
     */
    DOUBLE,

    /**
     * Parameter values represented by Range instance (range of double values)
     */
    RANGE,

    /**
     * Parameter values represented by Boolean instance
     */
    BOOLEAN,
    
    /**
     * Parameter values represented by a list of selection
     */
    MULTIPLE_SELECTION,
    
    /**
     * File name selection
     */
    FILE_NAME,
    
    /**
     * A list which can be ordered, e.g. by dragging
     */
    ORDERED_LIST

}
