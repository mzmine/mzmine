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

package net.sf.mzmine.modules.peakpicking.accuratemass;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class AccurateMassPickerParameters extends SimpleParameterSet {

    public static final Parameter suffix = new SimpleParameter(
            ParameterType.STRING, "Filename suffix",
            "Suffix to be added to filename", null, "peaklist", null);

    public static final Parameter mzTolerance = new SimpleParameter(
            ParameterType.FLOAT,
            "M/Z tolerance",
            "Maximum allowed distance in M/Z between data points in successive scans",
            "m/z", new Float(0.1), new Float(0.0), null,
            MZmineCore.getMZFormat());

    public static final Parameter minDataPoints = new SimpleParameter(
            ParameterType.INTEGER, "Min data points",
            "Minimum number of data points to form a peak", null, new Integer(
                    10), new Integer(10), null);

    public AccurateMassPickerParameters() {
        super(new Parameter[] { suffix, mzTolerance, minDataPoints });
    }

}
