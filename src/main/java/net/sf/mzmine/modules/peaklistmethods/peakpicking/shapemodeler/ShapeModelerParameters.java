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

package net.sf.mzmine.modules.peaklistmethods.peakpicking.shapemodeler;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

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
	    "Remove original peak list",
	    "If checked, original peak list will be removed and only resolved version remains");

    public ShapeModelerParameters() {
	super(new Parameter[] { peakLists, suffix, massResolution,
		shapeModelerType, autoRemove });
    }

}
