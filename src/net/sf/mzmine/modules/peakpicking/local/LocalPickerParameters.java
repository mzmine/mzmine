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

package net.sf.mzmine.modules.peakpicking.local;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

class LocalPickerParameters extends SimpleParameterSet {

    public static final NumberFormat percentFormat = NumberFormat.getPercentInstance();

    public static final Parameter suffix = new SimpleParameter(
            ParameterType.STRING, "Filename suffix",
            "Suffix to be added to filename", null, "peaklist", null);

    public static final Parameter binSize = new SimpleParameter(
            ParameterType.FLOAT, "M/Z bin width",
            "Width of m/z range for each precalculated XIC", "m/z", new Float(
                    0.25), new Float(0.01), null,
            MZmineCore.getDesktop().getMZFormat());

    public static final Parameter chromatographicThresholdLevel = new SimpleParameter(
            ParameterType.FLOAT, "Chromatographic threshold level",
            "Used in defining threshold level value from an XIC", "%",
            new Float(0.0), new Float(0.0), new Float(1.0), percentFormat);

    public static final Parameter noiseLevel = new SimpleParameter(
            ParameterType.FLOAT, "Noise level",
            "Intensities less than this value are interpreted as noise",
            "absolute", new Float(10.0), new Float(0.0), null,
            MZmineCore.getDesktop().getIntensityFormat());

    public static final Parameter minimumPeakHeight = new SimpleParameter(
            ParameterType.FLOAT, "Min peak height",
            "Minimum acceptable peak height", "absolute", new Float(100.0),
            new Float(0.0), null, MZmineCore.getDesktop().getIntensityFormat());

    public static final Parameter minimumPeakDuration = new SimpleParameter(
            ParameterType.FLOAT, "Min peak duration",
            "Minimum acceptable peak duration", "seconds", new Float(10.0),
            new Float(0.0), null, MZmineCore.getDesktop().getRTFormat());

    public static final Parameter mzTolerance = new SimpleParameter(
            ParameterType.FLOAT,
            "M/Z tolerance",
            "Maximum allowed distance in M/Z between centroid peaks in successive scans",
            "Da", new Float(0.1), new Float(0.0), null,
            MZmineCore.getDesktop().getMZFormat());

    public static final Parameter intTolerance = new SimpleParameter(
            ParameterType.FLOAT,
            "Intensity tolerance",
            "Maximum allowed deviation from expected /\\ shape of a peak in chromatographic direction",
            "%", new Float(0.15), new Float(0.0), null, percentFormat);

    LocalPickerParameters() {
        super(new Parameter[] { suffix, binSize, chromatographicThresholdLevel,
                noiseLevel, minimumPeakHeight, minimumPeakDuration,
                mzTolerance, intTolerance });
    }

}
