/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.identification.pubchem;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class PubChemSearchParameters extends SimpleParameterSet {

	public static final NumberFormat percentFormat = NumberFormat
			.getPercentInstance();

	public static final Parameter rawMass = new SimpleParameter(
			ParameterType.DOUBLE, "Peak mass", "Peak mass value of raw data",
			"(m/z)", null, new Double(0.0), null, MZmineCore.getMZFormat());

	public static final Parameter charge = new SimpleParameter(
			ParameterType.INTEGER, "Charge",
			"This value is used to calculate the neutral mass", null,
			new Integer(1), new Integer(0), null, null);

	public static final Parameter ionizationMethod = new SimpleParameter(
			ParameterType.STRING, "Ionization method",
			"Type of ion used to calculate the neutral mass", null,
			TypeOfIonization.values());

	public static final Parameter neutralMass = new SimpleParameter(
			ParameterType.DOUBLE, "Neutral mass",
			"Value to use in the search query", null, null, new Double(0.0),
			null, MZmineCore.getMZFormat());

	public static final Parameter chargedMol = new SimpleParameter(
			ParameterType.BOOLEAN, "Molecule with charge",
			"Search only for molecules with non-zero charge", null, false,
			null, null, null);

	public static final Parameter numOfResults = new SimpleParameter(
			ParameterType.INTEGER, "Number of results",
			"Maximum number of results to display", null, new Integer(15),
			new Integer(1), null, null);

	public static final Parameter mzToleranceField = new SimpleParameter(
			ParameterType.DOUBLE, "Mass tolerance",
			"Tolerance of the mass value to search (+/- range)", "(m/z)",
			new Double(0.0010), new Double(0.0001), new Double(1.0), MZmineCore
					.getMZFormat());

	public static final Parameter isotopeFilter = new SimpleParameter(
			ParameterType.BOOLEAN, "Isotope pattern filter",
			"Search only for compounds with a isotope pattern similar", null,
			false, null, null, null);

	public static final Parameter isotopeScoreTolerance = new SimpleParameter(
			ParameterType.DOUBLE, "Isotope pattern score threshold",
			"Threshold level for isotope pattern score", "%", new Double(0.65),
			new Double(0.0), new Double(1.0), percentFormat);

	public PubChemSearchParameters() {
		super(new Parameter[] { rawMass, charge, ionizationMethod, neutralMass,
				chargedMol, numOfResults, mzToleranceField, isotopeFilter,
				isotopeScoreTolerance });
	}

}
