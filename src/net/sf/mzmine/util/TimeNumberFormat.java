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

package net.sf.mzmine.util;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;


/**
 * NumberFormat implementation to format a given time in seconds
 */
public class TimeNumberFormat extends NumberFormat {

    private static DateFormat rtFormat = new SimpleDateFormat("m:ss");
    
    /**
     * @see java.text.NumberFormat#format(double, java.lang.StringBuffer, java.text.FieldPosition)
     */
    public StringBuffer format(double arg0, StringBuffer arg1,
            FieldPosition arg2) {
        return rtFormat.format(arg0 * 1000, arg1, arg2);
    }

    /**
     * @see java.text.NumberFormat#format(long, java.lang.StringBuffer, java.text.FieldPosition)
     */
    public StringBuffer format(long arg0, StringBuffer arg1,
            FieldPosition arg2) {
        return rtFormat.format(arg0 * 1000, arg1, arg2);
    }

    /**
     * @see java.text.NumberFormat#parse(java.lang.String, java.text.ParsePosition)
     */
    public Number parse(String arg0, ParsePosition arg1) {
        return rtFormat.parse(arg0, arg1).getTime() / 1000;
    }

}
