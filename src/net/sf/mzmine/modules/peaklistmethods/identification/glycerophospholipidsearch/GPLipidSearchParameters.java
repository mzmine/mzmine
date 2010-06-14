/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class GPLipidSearchParameters extends SimpleParameterSet {

	public static final Parameter lipidTypes = new SimpleParameter(
			ParameterType.MULTIPLE_SELECTION, "Type of lipids",
			"Selection of glycerophospholipis to consider", null, GPLipidType.values());

	public static final Parameter minChainLength = new SimpleParameter(
			ParameterType.INTEGER, "Minimum fatty acid length",
			"Minimum length of the fatty acid chain", "carbons",
			new Integer(12), new Integer(0), null);

	public static final Parameter maxChainLength = new SimpleParameter(
			ParameterType.INTEGER, "Maximum fatty acid length",
			"Maximum length of the fatty acid chain", "carbons",
			new Integer(36), new Integer(0), null);

	public static final Parameter maxDoubleBonds = new SimpleParameter(
			ParameterType.INTEGER, "Maximum number of double bonds",
			"Maximum number of double bonds in one fatty acid chain", null,
			new Integer(3), new Integer(0), null);

	public static final Parameter mzTolerance = new SimpleParameter(
			ParameterType.DOUBLE, "m/z tolerance",
			"Maximum allowed m/z difference", "m/z", new Double(0.1),
			new Double(0.0), null, MZmineCore.getMZFormat());

	public static final Parameter ionizationMethod = new SimpleParameter(
			ParameterType.STRING, "Ionization method",
			"Type of ion used to calculate the ionized mass", null,
			IonizationType.values());

	public GPLipidSearchParameters() {
		super(new Parameter[] { lipidTypes, minChainLength, maxChainLength,
				maxDoubleBonds, mzTolerance, ionizationMethod });
	}

}
