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

package net.sf.mzmine.modules.gapfilling.peakfinder;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.mzmineclient.MZmineCore;

public class PeakFinderParameters extends SimpleParameterSet {

    public static final NumberFormat percentFormat = NumberFormat.getPercentInstance();

    public static final String RTToleranceTypeAbsolute = "Absolute";
    public static final String RTToleranceTypeRelative = "Relative";

    public static final Object[] RTToleranceTypePossibleValues = {
            RTToleranceTypeAbsolute, RTToleranceTypeRelative };

    public static final Parameter suffix = new SimpleParameter(
            ParameterType.STRING, "Name suffix",
            "Suffix to be added to peak list name", null, "gap-filled", null);

    public static final Parameter intTolerance = new SimpleParameter(
            ParameterType.DOUBLE,
            "Intensity tolerance",
            "Maximum allowed deviation from expected /\\ shape of a peak in chromatographic direction",
            "%", new Double(0.20), new Double(0.0), null, percentFormat);

    public static final Parameter MZTolerance = new SimpleParameter(
            ParameterType.DOUBLE, "M/Z tolerance",
            "Maximum allowed M/Z difference", "m/z", new Double(0.2), new Double(
                    0.0), null, MZmineCore.getMZFormat());

    public static final Parameter RTToleranceType = new SimpleParameter(
            ParameterType.STRING,
            "Retention time tolerance type",
            "Maximum RT difference can be defined either using absolute or relative value",
            RTToleranceTypeAbsolute, RTToleranceTypePossibleValues);

    public static final Parameter RTToleranceValueAbs = new SimpleParameter(
            ParameterType.DOUBLE, "Absolute RT tolerance",
            "Maximum allowed absolute RT difference", null, new Double(15.0),
            new Double(0.0), null, MZmineCore.getRTFormat());

    public static final Parameter RTToleranceValuePercent = new SimpleParameter(
            ParameterType.DOUBLE, "Relative RT tolerance",
            "Maximum allowed relative RT difference", "%", new Double(0.15),
            new Double(0.0), null, percentFormat);

    public PeakFinderParameters() {
        super(new Parameter[] { suffix, intTolerance, MZTolerance,
                RTToleranceType, RTToleranceValueAbs, RTToleranceValuePercent });
    }

}
