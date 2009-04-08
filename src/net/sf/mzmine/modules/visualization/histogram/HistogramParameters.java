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

package net.sf.mzmine.modules.visualization.histogram;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.histogram.histogramdatalabel.HistogramDataType;
import net.sf.mzmine.util.Range;

public class HistogramParameters extends SimpleParameterSet {

    public static final Parameter dataFiles = new SimpleParameter(
            ParameterType.MULTIPLE_SELECTION, "Raw data files",
            "Column of peaks to be plotted", null, null, 1, null);

    public static final Parameter dataType = new SimpleParameter(
            ParameterType.STRING, "Plotted data type",
            "Peak's data to be plotted", null, HistogramDataType.values());

    public static final Parameter rangeData = new SimpleParameter(
            ParameterType.RANGE, "Plotted data range",
            "Range of data to be plotted", null, new Range(0, 1000),
            new Double(0), null, MZmineCore.getMZFormat());

    public static final Parameter numOfBins = new SimpleParameter(
            ParameterType.INTEGER, "Number of bins",
            "The plot is divides into this number of bins", new Integer(5));

    public HistogramParameters() {
        super(new Parameter[] { dataFiles, dataType, rangeData, numOfBins });
    }

}