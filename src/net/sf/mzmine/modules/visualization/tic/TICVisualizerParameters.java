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

package net.sf.mzmine.modules.visualization.tic;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.Range;

public class TICVisualizerParameters extends SimpleParameterSet {

    public static final String plotTypeTIC = "TIC";
    public static final String plotTypeBP = "Base peak intensity";

    public static final String[] plotTypes = { plotTypeTIC, plotTypeBP };
    public static final Integer[] msLevels = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

    public static final Parameter msLevel = new SimpleParameter(
            ParameterType.INTEGER, "MS level", "MS level of plotted scans", 1,
            msLevels);

    public static final Parameter plotType = new SimpleParameter(
            ParameterType.STRING, "Plot type",
            "Type of Y value calculation (TIC = sum, base peak = max)",
            plotTypeTIC, plotTypes);

    public static final Parameter retentionTimeRange = new SimpleParameter(
            ParameterType.RANGE, "Retention time",
            "Retention time (X axis) range", null, new Range(0, 600),
            new Double(0), null, MZmineCore.getRTFormat());

    public static final Parameter mzRange = new SimpleParameter(
            ParameterType.RANGE,
            "m/z range",
            "Range of m/z values. If this range does not include the whole scan m/z range, the resulting visualizer is XIC type.",
            "m/z", new Range(0, 1000), new Double(0), null,
            MZmineCore.getMZFormat());

    public TICVisualizerParameters() {
        super(
                new Parameter[] { msLevel, plotType, retentionTimeRange,
                        mzRange });
    }

}
