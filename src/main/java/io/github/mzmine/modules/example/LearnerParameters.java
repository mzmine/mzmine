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

package io.github.mzmine.modules.example;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class LearnerParameters extends SimpleParameterSet {
    /*
     * Define any parameters here (see io.github.mzmine.parameters for parameter
     * types)
     */
    public static final PeakListsParameter peakLists = new PeakListsParameter();

    public static final StringParameter suffix = new StringParameter(
            "Name suffix", "Suffix to be added to feature list name",
            "deisotoped");

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

    public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

    public static final IntegerParameter maximumCharge = new IntegerParameter(
            "Maximum charge",
            "Maximum charge to consider for detecting the isotope patterns");

    public static final BooleanParameter autoRemove = new BooleanParameter(
            "Remove original peaklist",
            "If checked, original peaklist will be removed and only deisotoped version remains");

    /**
     * Create a new parameterset
     */
    public LearnerParameters() {
        /*
         * The order of the parameters is used to construct the parameter dialog
         * automatically
         */
        super(new Parameter[] { peakLists, suffix, mzTolerance, rtTolerance,
                maximumCharge, autoRemove });
    }

}
