/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.filter_clearannotations;

import java.text.DecimalFormat;

import com.google.common.collect.Range;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class PeaklistClearAnnotationsParameters extends SimpleParameterSet {

    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final BooleanParameter CLEAR_IDENTITY = new BooleanParameter(
            "Clear identities?",
            "If checked, the identities will be removed from the selected peaklists");

    public static final BooleanParameter CLEAR_COMMENT = new BooleanParameter(
            "Clear comments?",
            "If checked, the comments will be removed from the selected peaklists");

    /*
     * public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
     * "Remove source feature list after filtering",
     * "If checked, the original feature list will be removed leaving only the filtered version"
     * );
     */

    public PeaklistClearAnnotationsParameters() {
        super(new Parameter[] { PEAK_LISTS, CLEAR_IDENTITY, CLEAR_COMMENT });
    }

}
