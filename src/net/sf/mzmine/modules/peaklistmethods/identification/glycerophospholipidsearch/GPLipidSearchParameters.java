/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

import java.text.NumberFormat;

import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.NumberParameter;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;

public class GPLipidSearchParameters extends SimpleParameterSet {

	public static final PeakListsParameter peakLists = new PeakListsParameter();

	public static final MultiChoiceParameter<GPLipidType> lipidTypes = new MultiChoiceParameter<GPLipidType>(
			"Type of lipids", "Selection of glycerophospholipis to consider",
			GPLipidType.values());

	public static final NumberParameter minChainLength = new NumberParameter(
			"Minimum fatty acid length",
			"Minimum length of the fatty acid chain",
			NumberFormat.getIntegerInstance());

	public static final NumberParameter maxChainLength = new NumberParameter(
			"Maximum fatty acid length",
			"Maximum length of the fatty acid chain",
			NumberFormat.getIntegerInstance());

	public static final NumberParameter maxDoubleBonds = new NumberParameter(
			"Maximum number of double bonds",
			"Maximum number of double bonds in one fatty acid chain",
			NumberFormat.getIntegerInstance());

	public static final NumberParameter mzTolerance = new NumberParameter(
			"m/z tolerance", "Maximum allowed m/z difference",
			MZmineCore.getMZFormat());

	public static final ComboParameter<IonizationType> ionizationMethod = new ComboParameter<IonizationType>(
			"Ionization method",
			"Type of ion used to calculate the ionized mass",
			IonizationType.values());

	public GPLipidSearchParameters() {
		super(new Parameter[] { peakLists, lipidTypes, minChainLength,
				maxChainLength, maxDoubleBonds, mzTolerance, ionizationMethod });
	}

}
