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

package net.sf.mzmine.modules.peakpicking.anothercentroid;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class AnotherCentroidPickerParameters extends SimpleParameterSet {

    public static final NumberFormat percentFormat = NumberFormat.getPercentInstance();

    public static final Parameter suffix = new SimpleParameter(
            ParameterType.STRING, "Filename suffix",
            "Suffix to be added to filename", null, "peaklist", null);

    public static final Parameter noiseLevel = new SimpleParameter(
            ParameterType.FLOAT, "Noise level",
            "Intensities less than this value are interpreted as noise",
            "absolute", new Float(10.0), new Float(0.0), null,
            MZmineCore.getIntensityFormat());

    public static final Parameter minimumNumberOfIsotopicPeaks = new SimpleParameter(
            ParameterType.INTEGER, "Min number of isotopic peaks",
            "Minimum acceptable number of isotopic peaks per pattern", null,
            new Integer(3), new Integer(1), null,
            NumberFormat.getIntegerInstance());

    public static final Parameter maximumChargeState = new SimpleParameter(
            ParameterType.INTEGER, "Max charge state",
            "Maximum searched charge state", null, new Integer(1), new Integer(
                    1), null, NumberFormat.getIntegerInstance());

    public static final Parameter minimumPeakDuration = new SimpleParameter(
            ParameterType.FLOAT, "Min peak duration",
            "Minimum acceptable peak duration", null, new Float(10.0),
            new Float(0.0), null, MZmineCore.getRTFormat());

    public static final Parameter maximumPeakDuration = new SimpleParameter(
            ParameterType.FLOAT, "Max peak duration",
            "Maximum acceptable peak duration", null, new Float(10.0),
            new Float(0.0), null, MZmineCore.getRTFormat());

    public static final Parameter mzTolerance = new SimpleParameter(
            ParameterType.FLOAT, "m/z tolerance",
            "Maximum allowed distance in m/z between centroids of a peak",
            "m/z", new Float(0.1), new Float(0.0), null,
            MZmineCore.getMZFormat());

    public AnotherCentroidPickerParameters() {
        super(new Parameter[] { suffix, noiseLevel,
                minimumNumberOfIsotopicPeaks, maximumChargeState,
                minimumPeakDuration, maximumPeakDuration, mzTolerance });
    }

}
