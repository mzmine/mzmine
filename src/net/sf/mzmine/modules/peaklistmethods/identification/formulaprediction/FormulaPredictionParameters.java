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

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction;

import java.text.NumberFormat;

import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class FormulaPredictionParameters extends SimpleParameterSet {

	public static final NumberFormat percentFormat = NumberFormat
			.getPercentInstance();

	public static final Parameter rawMass = new SimpleParameter(
			ParameterType.DOUBLE, "Peak m/z", "Detected m/z value of the peak",
			"m/z", null, new Double(0.0), null, MZmineCore.getMZFormat());

	public static final Parameter charge = new SimpleParameter(
			ParameterType.INTEGER, "Charge",
			"This value is used to calculate the neutral mass", null,
			new Integer(1), new Integer(1), null, null);

	public static final Parameter ionizationMethod = new SimpleParameter(
			ParameterType.STRING, "Ionization method",
			"Type of ion used to calculate the neutral mass", null,
			IonizationType.values());

	public static final Parameter neutralMass = new SimpleParameter(
			ParameterType.DOUBLE, "Neutral mass",
			"Value to use in the search query", null, null, null, null,
			MZmineCore.getMZFormat());

	public static final Parameter numOfResults = new SimpleParameter(
			ParameterType.INTEGER, "Number of results",
			"Maximum number of results to display", null, new Integer(1000),
			new Integer(1), null, null);

	public static final Parameter massTolerance = new SimpleParameter(
			ParameterType.DOUBLE, "Mass tolerance",
			"Tolerance of the mass value to search (+/- range)", "amu",
			new Double(0.0010), new Double(0), null, MZmineCore.getMZFormat());

	public static final Parameter elements = new SimpleParameter(
			ParameterType.STRING,
			"Elements",
			"Elements to consider for a formula and minimum and maximum number of atoms of each element");

	public static final Parameter isotopeFilter = new SimpleParameter(
			ParameterType.BOOLEAN, "Isotope pattern filter",
			"Search only for formulas with a isotope pattern similar", null,
			false, null, null, null);

	public static final Parameter isotopeScoreTolerance = new SimpleParameter(
			ParameterType.DOUBLE, "Isotope pattern score threshold",
			"Threshold level for isotope pattern score", "%", new Double(0.65),
			new Double(0.0), new Double(1.0), percentFormat);
	
	public static final Parameter heuristicRules = new SimpleParameter(
			ParameterType.MULTIPLE_SELECTION, "Heuristic rules",
			"Search only for formulas which correspond to the set rules", null, HeuristicRule.values());
	

	public FormulaPredictionParameters() {
		super(new Parameter[] { rawMass, charge, ionizationMethod, neutralMass,
				numOfResults, massTolerance, elements, isotopeFilter,
				isotopeScoreTolerance, heuristicRules});
	}

}
