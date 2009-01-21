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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklist.rowsfilter;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.mzmineclient.MZmineCore;

public class RowsFilterParameters extends SimpleParameterSet {

    public static final Parameter suffix = new SimpleParameter(
            ParameterType.STRING, "Name suffix",
            "Suffix to be added to peak list name", null, "filtered", null);

    public static final Parameter minPeaks = new SimpleParameter(
            ParameterType.INTEGER, "Minimum peaks in a row",
            "Minimum number of peak detections required to select a row",
            "peaks", new Integer(1), new Integer(1), null);

    public static final Parameter minIsotopePatternSize = new SimpleParameter(
            ParameterType.INTEGER, "Minimum peaks in an isotope pattern",
            "Minimum number of peaks required in an isotope pattern",
            "peaks", new Integer(1), new Integer(1), null);    
    
    public static final Parameter minMZ = new SimpleParameter(
            ParameterType.DOUBLE, "Minimum m/z",
            "Minimum average m/z value of a row", "m/z", 0d,
            MZmineCore.getMZFormat());

    public static final Parameter maxMZ = new SimpleParameter(
            ParameterType.DOUBLE, "Maximum m/z",
            "Maximum average m/z value of a row", "m/z", 0d,
            MZmineCore.getMZFormat());

    public static final Parameter minRT = new SimpleParameter(
            ParameterType.DOUBLE, "Minimum retention time",
            "Maximum average retention time of a row", null, 0d,
            MZmineCore.getRTFormat());

    public static final Parameter maxRT = new SimpleParameter(
            ParameterType.DOUBLE, "Maximum retention time",
            "Maximum average retention time of a row", null, 0d,
            MZmineCore.getRTFormat());

    public static final Parameter identified = new SimpleParameter(
            ParameterType.BOOLEAN, "Only identified?",
            "Select to filter only identified compounds");

    public static final Parameter autoRemove = new SimpleParameter(
            ParameterType.BOOLEAN,
            "Remove source peak list after filtering",
            "If checked, original peak list will be removed and only filtered version remains",
            new Boolean(true));

    public RowsFilterParameters() {
        super(new Parameter[] { suffix, minPeaks, minIsotopePatternSize, minMZ, maxMZ, minRT, maxRT,
                identified, autoRemove });
    }

}
