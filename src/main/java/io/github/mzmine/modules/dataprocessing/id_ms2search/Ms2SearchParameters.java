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

package io.github.mzmine.modules.dataprocessing.id_ms2search;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.MassListParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class Ms2SearchParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakList1 = new PeakListsParameter(
            "Feature List 1", 1, 1);

    public static final PeakListsParameter peakList2 = new PeakListsParameter(
            "Feature List 2", 1, 1);

    public static final MassListParameter massList = new MassListParameter();

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

    public static final DoubleParameter intensityThreshold = new DoubleParameter(
            "Minimum MS2 ion intensity",
            "Minimum ion intensity to consider in MS2 comparison");

    public static final IntegerParameter minimumIonsMatched = new IntegerParameter(
            "Minimum ion(s) matched per MS2 comparison",
            "Minimum number of peaks between two MS2s that must match");

    public static final DoubleParameter scoreThreshold = new DoubleParameter(
            "Minimum spectral match score to report",
            "Minimum MS2 comparison score to report");

    public Ms2SearchParameters() {
        super(new Parameter[] { peakList1, peakList2, massList, mzTolerance,
                intensityThreshold, minimumIonsMatched, scoreThreshold });
    }

}
