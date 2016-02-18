/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.filtering.peakcomparisonrowfilter;

import java.text.DecimalFormat;

import com.google.common.collect.Range;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class PeakComparisonRowFilterParameters extends SimpleParameterSet {

    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final StringParameter SUFFIX = new StringParameter(
            "Name suffix", "Suffix to be added to peak list name", "filtered");

    public static final IntegerParameter COLUMN_INDEX_1 = new IntegerParameter(
            "1st peak column to compare (zero indexed)",
            "index of second column for comparison, e.g. \"0\"", 0);

    public static final IntegerParameter COLUMN_INDEX_2 = new IntegerParameter(
            "2nd peak column to compare (zero indexed)",
            "index of second column for comparison,e.g. \"1\"", 1);

    public static final OptionalParameter<DoubleRangeParameter> FOLD_CHANGE = new OptionalParameter<>(
            new DoubleRangeParameter("Fold change range : log2(peak1/peak2)",
                    "Range of fold change to return", new DecimalFormat("0.0"),
                    Range.closed(-5.0, 5.0)));

    public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
            "Remove source peak list after filtering",
            "If checked, the original peak list will be removed leaving only the filtered version");

    public PeakComparisonRowFilterParameters() {
        super(new Parameter[] { PEAK_LISTS, SUFFIX, COLUMN_INDEX_1,
                COLUMN_INDEX_2, FOLD_CHANGE, AUTO_REMOVE });
    }

}
