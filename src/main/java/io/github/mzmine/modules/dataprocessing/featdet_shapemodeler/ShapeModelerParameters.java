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

package io.github.mzmine.modules.dataprocessing.featdet_shapemodeler;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class ShapeModelerParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    public static final ComboParameter<ShapeModel> shapeModelerType = new ComboParameter<ShapeModel>(
            "Shape model", "This value defines the type of shape model",
            ShapeModel.values());

    public static final StringParameter suffix = new StringParameter("Suffix",
            "This string is added to filename as suffix", "shaped peaks");

    public static final DoubleParameter massResolution = new DoubleParameter(
            "Mass resolution",
            "Mass resolution is the dimensionless ratio of the mass of the peak divided by its width."
                    + "\nPeak width is taken as the full width at half maximum intensity (FWHM).");

    public static final BooleanParameter autoRemove = new BooleanParameter(
            "Remove original feature list",
            "If checked, original feature list will be removed and only resolved version remains");

    public ShapeModelerParameters() {
        super(new Parameter[] { peakLists, suffix, massResolution,
                shapeModelerType, autoRemove });
    }

}
