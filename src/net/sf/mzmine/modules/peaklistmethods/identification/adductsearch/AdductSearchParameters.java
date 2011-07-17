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

package net.sf.mzmine.modules.peaklistmethods.identification.adductsearch;

import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.NumberParameter;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;

public class AdductSearchParameters extends SimpleParameterSet {

	public static final PeakListsParameter peakLists = new PeakListsParameter();

	public static final NumberParameter rtTolerance = new NumberParameter(
			"RT tolerance",
			"Maximum allowed difference of retention time to set a relationship between peaks",
			MZmineCore.getRTFormat());

	public static final MultiChoiceParameter<AdductType> adducts = new MultiChoiceParameter<AdductType>(
			"Adducts",
			"List of adducts, each one refers a specific distance in m/z axis between related peaks",
			AdductType.values());

	public static final NumberParameter customAdductValue = new NumberParameter(
			"Custom adduct value",
			"Mass value (m/z difference) for custom adduct",
			MZmineCore.getMZFormat());

	public static final NumberParameter mzTolerance = new NumberParameter(
			"m/z tolerance",
			"Tolerance value of the m/z difference between peaks",
			MZmineCore.getMZFormat());

	public static final NumberParameter maxAdductHeight = new NumberParameter(
			"Max adduct peak height",
			"Maximum height of the recognized adduct peak, relative to the main peak",
			NumberFormat.getPercentInstance());

	public AdductSearchParameters() {
		super(new Parameter[] { peakLists, rtTolerance, adducts,
				customAdductValue, mzTolerance, maxAdductHeight });
	}

}
