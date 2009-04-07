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

package net.sf.mzmine.modules.isotopes.isotopeprediction;

import java.util.logging.Logger;

import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.modules.visualization.spectra.PeakListDataSet;
import net.sf.mzmine.modules.visualization.spectra.SpectraDataSet;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizer;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class IsotopePatternCalculatorTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage, description, formula;
	private double minAbundance, isotopeHeight;
	private int charge;
	// private int processedAtoms, totalNumberOfAtoms;
	private IsotopePattern isotopePattern;
	private boolean autoHeight = false, sumOfMasses = false;
	private boolean positiveCharge;
	private SpectraPlot plot;

	public IsotopePatternCalculatorTask(
			IsotopePatternCalculatorParameters parameters, SpectraPlot plot) {

		this.plot = plot;

		formula = (String) parameters
				.getParameterValue(IsotopePatternCalculatorParameters.formula);
		minAbundance = (Double) parameters
				.getParameterValue(IsotopePatternCalculatorParameters.minimalAbundance);
		charge = (Integer) parameters
				.getParameterValue(IsotopePatternCalculatorParameters.charge);
		isotopeHeight = (Double) parameters
				.getParameterValue(IsotopePatternCalculatorParameters.isotopeHeight);
		autoHeight = (Boolean) parameters
				.getParameterValue(IsotopePatternCalculatorParameters.autoHeight);
		String signOfCharge = (String) parameters
				.getParameterValue(IsotopePatternCalculatorParameters.signOfCharge);
		positiveCharge = signOfCharge.equals("Positive");

		sumOfMasses = (Boolean) parameters
				.getParameterValue(IsotopePatternCalculatorParameters.sumOfMasses);

		description = "Isotope pattern calculation of " + formula;

	}

	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public double getFinishedPercentage() {
		return 1;// processedAtoms/totalNumberOfAtoms;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getTaskDescription() {
		return description;
	}

	public void run() {

		status = TaskStatus.PROCESSING;
		logger.info("Running isotope pattern calculation of "
				+ formula);

		FormulaAnalyzer analyzer = new FormulaAnalyzer();

		if (status == TaskStatus.CANCELED)
			return;
		try {
			isotopePattern = analyzer.getIsotopePattern(formula, minAbundance,
					charge, positiveCharge, isotopeHeight, autoHeight,
					sumOfMasses, IonizationType.NO_IONIZATION);

		} catch (Exception e) {
			// e.printStackTrace();
			status = TaskStatus.ERROR;
			errorMessage = analyzer.getMessageError();// "Chemical formula or common organic compound not valid"
			// ;
		}

		if (isotopePattern == null) {
			status = TaskStatus.ERROR;
			errorMessage = analyzer.getMessageError();
			return;
		}

		if (plot == null) {
			SpectraVisualizer.showIsotopePattern(isotopePattern);
		} else {
			PeakListDataSet predictedPeakDataSet = new PeakListDataSet(
					isotopePattern);
			SpectraDataSet rawSpectraDataSet = (SpectraDataSet) plot
					.getXYPlot().getDataset(0);
			double increase = predictedPeakDataSet.getIncrease();
			if (predictedPeakDataSet.isAutoIncrease()) {
				if (rawSpectraDataSet != null) {
					increase = rawSpectraDataSet
							.getBiggestIntensity(isotopePattern
							.getDataPoints());
				} else {
					increase = Math.pow(10, 6);
				}
			}
			predictedPeakDataSet.setIncreaseIntensity(increase);
			plot.addPeaksDataSet(predictedPeakDataSet);
		}

		status = TaskStatus.FINISHED;

	}

	public String getFormula() {
		return formula;
	}

	public IsotopePattern getIsotopePattern() {
		return isotopePattern;
	}

}
