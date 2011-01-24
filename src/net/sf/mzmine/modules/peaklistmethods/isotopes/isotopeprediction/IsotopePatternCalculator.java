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

package net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.IsotopePatternStatus;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.Polarity;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleIsotopePattern;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.formula.IsotopeContainer;
import org.openscience.cdk.formula.IsotopePatternGenerator;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * The reason why we introduce this as a module, rather than simple utility
 * class, is to remember the parameter values.
 */
public class IsotopePatternCalculator implements MZmineModule {

	public static final double ELECTRON_MASS = 5.4857990943E-4;

	private static IsotopePatternCalculatorParameters parameters;

	/**
	 * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
	 */
	public void initModule() {
		parameters = new IsotopePatternCalculatorParameters();
	}

	public void setParameters(ParameterSet parameters) {
		parameters = (IsotopePatternCalculatorParameters) parameters;
	}

	/**
	 * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	/**
	 * Returns
	 */
	public static IsotopePattern calculateIsotopePattern(
			String molecularFormula, int charge, Polarity polarity) {

		IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();

		IMolecularFormula formulaObject = MolecularFormulaManipulator
				.getMolecularFormula(molecularFormula, builder);

		// TODO: check if the formula is not too big (>100 of a single atom?).
		// if so, just cancel the prediction

		// Set the minimum abundance of isotope to 0.1%
		IsotopePatternGenerator generator = new IsotopePatternGenerator(0.001);

		org.openscience.cdk.formula.IsotopePattern pattern = generator
				.getIsotopes(formulaObject);

		int numOfIsotopes = pattern.getNumberOfIsotopes();

		DataPoint dataPoints[] = new DataPoint[numOfIsotopes];

		for (int i = 0; i < numOfIsotopes; i++) {
			IsotopeContainer isotope = pattern.getIsotope(i);

			// For each unit of charge, we have to add or remove a mass of a
			// single electron. If the charge is positive, we remove electron
			// mass. If the charge is negative, we add it.
			double mass = isotope.getMass()
					+ (polarity.getSign() * -1 * charge * ELECTRON_MASS);

			double mz = mass / charge;
			double intensity = isotope.getIntensity();

			dataPoints[i] = new SimpleDataPoint(mz, intensity);
		}

		SimpleIsotopePattern newPattern = new SimpleIsotopePattern(dataPoints,
				IsotopePatternStatus.PREDICTED, molecularFormula);

		return newPattern;

	}

	public static IsotopePattern showIsotopePredictionDialog() {

		if (parameters == null) {
			throw new IllegalStateException("Module not initialized");
		}

		ParameterSetupDialog dialog = new ParameterSetupDialog(
				"Please set the formula", parameters);

		dialog.setVisible(true);

		if (dialog.getExitCode() != ExitCode.OK)
			return null;

		String formula = (String) parameters
				.getParameterValue(IsotopePatternCalculatorParameters.formula);
		int charge = (Integer) parameters
				.getParameterValue(IsotopePatternCalculatorParameters.charge);
		Polarity polarity = (Polarity) parameters
				.getParameterValue(IsotopePatternCalculatorParameters.polarity);

		IsotopePattern predictedPattern = calculateIsotopePattern(formula,
				charge, polarity);

		return predictedPattern;

	}
}
