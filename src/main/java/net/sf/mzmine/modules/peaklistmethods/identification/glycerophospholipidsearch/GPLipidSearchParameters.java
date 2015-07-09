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

package net.sf.mzmine.modules.peaklistmethods.identification.glycerophospholipidsearch;

import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class GPLipidSearchParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    public static final MultiChoiceParameter<GPLipidType> lipidTypes = new MultiChoiceParameter<GPLipidType>(
	    "Type of lipids", "Selection of glycerophospholipis to consider",
	    GPLipidType.values());

    public static final IntegerParameter minChainLength = new IntegerParameter(
	    "Minimum fatty acid length",
	    "Minimum length of the fatty acid chain");

    public static final IntegerParameter maxChainLength = new IntegerParameter(
	    "Maximum fatty acid length",
	    "Maximum length of the fatty acid chain");

    public static final IntegerParameter maxDoubleBonds = new IntegerParameter(
	    "Maximum number of double bonds",
	    "Maximum number of double bonds in one fatty acid chain");

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

    public static final ComboParameter<IonizationType> ionizationMethod = new ComboParameter<IonizationType>(
	    "Ionization method",
	    "Type of ion used to calculate the ionized mass",
	    IonizationType.values());

    public GPLipidSearchParameters() {
	super(new Parameter[] { peakLists, lipidTypes, minChainLength,
		maxChainLength, maxDoubleBonds, mzTolerance, ionizationMethod });
    }

}
