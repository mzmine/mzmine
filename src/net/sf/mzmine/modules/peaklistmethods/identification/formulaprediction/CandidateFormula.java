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

import java.util.Hashtable;
import java.util.TreeSet;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.Range;

import org.openscience.cdk.formula.MolecularFormula;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class CandidateFormula {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private final IMolecularFormula cdkFormula;
	private String stringFormula, htmlFormula;
	private Hashtable<HeuristicRule, Boolean> conformingRules;
	private Double msmsScore;
	private IsotopePattern predictedIsotopePattern;
	private PeakListRow peakListRow;
	private FormulaPredictionParameters parameters;
	private Double isotopeScore;

	CandidateFormula(ElementRule elements[], int elementCounts[],
			PeakListRow peakListRow, FormulaPredictionParameters parameters) {

		conformingRules = new Hashtable<HeuristicRule, Boolean>();

		cdkFormula = new MolecularFormula();

		for (int i = 0; i < elements.length; i++) {
			if (elementCounts[i] == 0)
				continue;
			cdkFormula.addIsotope(elements[i].getElementObject(),
					elementCounts[i]);
		}

		this.peakListRow = peakListRow;
		this.parameters = parameters;
	}

	public boolean conformsRule(HeuristicRule rule) {

		Boolean check = conformingRules.get(rule);

		if (check == null) {
			switch (rule) {
			case LEWIS:
				check = HeuristicRuleChecker.checkLewisOctetRule(cdkFormula);
				break;
			case SENIOR:
				check = HeuristicRuleChecker.checkSeniorRule(cdkFormula);
				break;
			case HC:
				check = HeuristicRuleChecker.checkHC(cdkFormula);
				break;
			case NOPS:
				check = HeuristicRuleChecker.checkNOPS(cdkFormula);
				break;
			case HNOPS:
				check = HeuristicRuleChecker.checkHNOPS(cdkFormula);
				break;

			default:
				throw new IllegalArgumentException("Unknown rule: " + rule);
			}
			conformingRules.put(rule, check);
		}

		return check;
	}

	public Double getMSMSScore() {

		if (msmsScore == null) {
			ChromatographicPeak bestPeak = peakListRow.getBestPeak();
			RawDataFile dataFile = bestPeak.getDataFile();
			int msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();
			Scan msmsScan = dataFile.getScan(msmsScanNumber);
			DataPoint dataPoints[] = msmsScan.getDataPoints();

			double msmsTolerance = (Double) parameters
					.getParameterValue(FormulaPredictionParameters.msmsTolerance);
			double msmsNoiseLevel = (Double) parameters
					.getParameterValue(FormulaPredictionParameters.msmsNoiseLevel);
			int totalMSMSpeaks = 0, interpretedMSMSpeaks = 0;

			for (DataPoint dp : dataPoints) {
				if (dp.getIntensity() < msmsNoiseLevel)
					continue;
				
				
				int charge = bestPeak.getCharge();

				// If we don't know the charge of the precursor, assume 1
				if (charge < 1)
					charge = 1;

				// We don't know the charge of the fragment, so we will simply
				// assume 1
				double neutralLoss = msmsScan.getPrecursorMZ()
						* bestPeak.getCharge() - dp.getMZ();
				
				// Ignore negative neutral losses and parent ion, 5 is a good threshold 
				if (neutralLoss < 5) continue;

				

				// Sorted by mass in descending order
				TreeSet<ElementRule> rulesSet = new TreeSet<ElementRule>();
				
				for (IIsotope isotope : cdkFormula.isotopes()) {
					ElementRule rule = new ElementRule(isotope.getSymbol(), 0, cdkFormula.getIsotopeCount(isotope));
					rulesSet.add(rule);
				}
				
				ElementRule elementRules[] = rulesSet.toArray(new ElementRule[0]);
				
				Range targetRange = new Range(neutralLoss - msmsTolerance, neutralLoss + msmsTolerance);
				
				int currentCounts[] = new int[elementRules.length];
				
				searchCycle: while (true) {

					double mass = 0;
					for (int i = 0; i < elementRules.length; i++) {
						mass += elementRules[i].getMass() * currentCounts[i];
					}

					if (targetRange.contains(mass)) {
						interpretedMSMSpeaks++;
						logger.finest("Interpreted neutral loss " + neutralLoss
								+ " (" + dp.getMZ() + " m/z) as " + convertToStr(currentCounts, elementRules));
						break searchCycle;
					}


					// Increase the count of the most heavy element
					currentCounts[0]++;

					// check the validity of the counts
					for (int i = 0; i < currentCounts.length; i++) {
						if (currentCounts[i] > elementRules[i].getMaxCount()) {
							currentCounts[i] = elementRules[i].getMinCount();
							if (i == currentCounts.length - 1) break searchCycle;
							currentCounts[i + 1]++;
						}
					}
					

				}

				
				totalMSMSpeaks++;
				

			}

			if (totalMSMSpeaks == 0)
				msmsScore = 0d;
			else
				msmsScore = (double) interpretedMSMSpeaks / totalMSMSpeaks;
		}

		return msmsScore;
	}

	public String getFormulaAsString() {
		if (stringFormula == null)
			stringFormula = MolecularFormulaManipulator.getString(cdkFormula);
		return stringFormula;
	}

	public String getFormulaAsHTML() {
		if (htmlFormula == null)
			htmlFormula = MolecularFormulaManipulator.getHTML(cdkFormula);
		return htmlFormula;
	}

	public IMolecularFormula getFormulaAsObject() {
		return cdkFormula;
	}

	public IsotopePattern getPredictedIsotopes() {
		if (predictedIsotopePattern == null) {
			IonizationType ionType = (IonizationType) parameters
					.getParameterValue(FormulaPredictionParameters.ionizationMethod);
			int charge = (Integer) parameters
					.getParameterValue(FormulaPredictionParameters.charge);
			String originalFormula = getFormulaAsString();
			String adjustedFormula = FormulaUtils.ionizeFormula(
					originalFormula, ionType.getPolarity(), charge);
			predictedIsotopePattern = IsotopePatternCalculator
					.calculateIsotopePattern(adjustedFormula, charge,
							ionType.getPolarity());
		}
		return predictedIsotopePattern;
	}

	public Double getIsotopeScore() {
		IsotopePattern detectedPattern = peakListRow.getBestIsotopePattern();
		if (detectedPattern == null)
			return null;
		if (isotopeScore == null) {
			isotopeScore = IsotopePatternScoreCalculator.getSimilarityScore(
					detectedPattern, getPredictedIsotopes());
		}
		return isotopeScore;
	}

	public double getExactMass() {
		return MolecularFormulaManipulator.getTotalExactMass(cdkFormula);
	}

	public String toString() {
		return getFormulaAsString();
	}

	private String  convertToStr(int elementCounts[], ElementRule elements[]) {
		MolecularFormula xx = new MolecularFormula();
		for (int i = 0; i < elements.length; i++) {
			if (elementCounts[i] == 0)
				continue;
			xx.addIsotope(elements[i].getElementObject(),
					elementCounts[i]);
		}
		return MolecularFormulaManipulator.getString(xx);
	}
}
