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

package net.sf.mzmine.modules.peakpicking.peakrecognition.minimumsearch;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class MinimumSearchPeakDetectorParameters extends SimpleParameterSet {

    public static final NumberFormat percentFormat = NumberFormat.getPercentInstance();

    public static final Parameter chromatographicThresholdLevel = new SimpleParameter(
            ParameterType.DOUBLE,
            "Chromatographic threshold",
            "Find such intensity that given percentage of the chromatogram data points is below, and ignore all data points below that intensity",
            "%", new Double(0.70), new Double(0.0), new Double(1.0),
            percentFormat);

    public static final Parameter searchRTRange = new SimpleParameter(
            ParameterType.DOUBLE,
            "Search minimum in RT range",
            "If a local minimum is minimal in this range of retention time, it will be considered a border between two peaks",
            null, new Double(10.0), new Double(0.0), null,
            MZmineCore.getRTFormat());

    public static final Parameter minRelativeHeight = new SimpleParameter(
            ParameterType.DOUBLE,
            "Minimum relative height",
            "Minimum height of a peak relative to the chromatogram top data point",
            null, 0.05, 0d, 1d, percentFormat);

    public static final Parameter minRatio = new SimpleParameter(
            ParameterType.DOUBLE,
            "Min ratio of peak top/edge",
            "Minimum ratio between peak's top intensity and side (lowest) data points",
            null, 2d, 0d, null);

    public MinimumSearchPeakDetectorParameters() {
        super(new Parameter[] { chromatographicThresholdLevel, searchRTRange,
                minRelativeHeight, minRatio });
    }

}
