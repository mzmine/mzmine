package net.sf.mzmine.modules.identification.pubchem;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class PubChemSearchParameters extends SimpleParameterSet {
	
	public static final Parameter rawMass = new SimpleParameter(
			ParameterType.FLOAT, "Peak mass",
			"Peak mass value of raw data", "(m/z)", null,
			new Float(0.0), null, MZmineCore.getMZFormat());

	public static final Parameter charge = new SimpleParameter(
			ParameterType.INTEGER, "Charge",
			"This value is used to calculate the neutral mass", null, new Integer(1),
			new Integer(0), null, null);

	public static final Parameter ionizationMethod = new SimpleParameter(
			ParameterType.STRING, "Ionization method",
			"Ionization method selected in the instrument to measure", null, TypeOfIonization.values());

	public static final Parameter mzToleranceField = new SimpleParameter(
			ParameterType.FLOAT, "Mass tolerance",
			"Tolerance of the mass value to search (+/- range)", "(m/z)", new Float(0.0010),
			new Float(0.0001), new Float(1.0), MZmineCore.getMZFormat());

	public static final Parameter numOfResults = new SimpleParameter(
			ParameterType.INTEGER, "Number of results",
			"Maximum number of results to display", null, new Integer(15),
			new Integer(1), null, null);

	public static final Parameter neutralMass = new SimpleParameter(
			ParameterType.FLOAT, "Neutral mass",
			"Value to use in the search query", null, null,
			new Float(0.0), null, MZmineCore.getMZFormat());

	public PubChemSearchParameters() {
		super(new Parameter[] { rawMass, charge, numOfResults, mzToleranceField,
				ionizationMethod, neutralMass });
	}
	
}

