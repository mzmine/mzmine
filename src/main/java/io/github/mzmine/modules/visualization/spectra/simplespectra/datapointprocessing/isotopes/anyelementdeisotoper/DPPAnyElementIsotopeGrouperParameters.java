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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.anyelementdeisotoper;

import java.awt.Color;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ColorParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

/**
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPAnyElementIsotopeGrouperParameters extends SimpleParameterSet {

    public static final DoubleRangeParameter mzRange = new DoubleRangeParameter(
            "m/z range", "The range of m/z to scan for isotope peaks",
            MZmineCore.getConfiguration().getMZFormat());

    public static final StringParameter element = new StringParameter("Element",
            "Element symbol of the element to deisotope for.");

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

    public static final IntegerParameter maximumCharge = new IntegerParameter(
            "Maximum charge",
            "Maximum charge to consider for detecting the isotope patterns");

    public static final BooleanParameter autoRemove = new BooleanParameter(
            "Remove non-isotopes",
            "If checked, all peaks without an isotope pattern will not be displayed and not passed to the next module.");

    public static final BooleanParameter displayResults = new BooleanParameter(
            "Display results",
            "Check if you want to display the deisotoping results in the plot. Displaying too much datasets might decrease clarity.",
            false);

    public static final ColorParameter datasetColor = new ColorParameter(
            "Dataset color",
            "Set the color you want the detected isotope patterns to be displayed with.",
            Color.GREEN);

    public DPPAnyElementIsotopeGrouperParameters() {
        super(new Parameter[] { element, mzTolerance, mzRange, maximumCharge,
                autoRemove, displayResults, datasetColor });
    }

}
