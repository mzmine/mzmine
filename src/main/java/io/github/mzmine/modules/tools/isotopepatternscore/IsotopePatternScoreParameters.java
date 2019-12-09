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

package io.github.mzmine.modules.tools.isotopepatternscore;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class IsotopePatternScoreParameters extends SimpleParameterSet {

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
            "Isotope m/z tolerance",
            "m/z tolerance which defines what isotopes would be considered same when "
                    + "comparing two isotopic patterns. This tolerance needs to be "
                    + "higher than general m/z precision of the data, because some "
                    + "small isotopes may overlap with the sides of bigger isotopic "
                    + "peaks.");

    public static final DoubleParameter isotopeNoiseLevel = new DoubleParameter(
            "Minimum absolute intensity",
            "Minimum absolute intensity of the isotopes to be compared. Isotopes below this intensity will be ignored.",
            MZmineCore.getConfiguration().getIntensityFormat());

    public static final PercentParameter isotopePatternScoreThreshold = new PercentParameter(
            "Minimum score",
            "If the score between isotope pattern is lower, discard this match");

    public IsotopePatternScoreParameters() {
        super(new Parameter[] { mzTolerance, isotopeNoiseLevel,
                isotopePatternScoreThreshold });
    }

}
