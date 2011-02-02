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

package net.sf.mzmine.modules.peaklistmethods.msms.msmsscore;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.ElementRule;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.FormulaPredictionEngine;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.HeuristicRule;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.ResultFormula;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.ResultFormulaWrapper;
import net.sf.mzmine.util.Range;

import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class MSMSScoreCalculator {

	public static MSMSScore evaluateMSMS(Scan msmsScan, double msmsTolerance,
			double msmsNoiseLevel, IMolecularFormula parentFormula) {

		return evaluateMSMS(msmsScan, msmsTolerance, msmsNoiseLevel,
				parentFormula, new HeuristicRule[] { HeuristicRule.LEWIS,
						HeuristicRule.SENIOR });

	}

	/**
	 * Returns a calculated similarity score of
	 */
	public static MSMSScore evaluateMSMS(Scan msmsScan, double msmsTolerance,
			double msmsNoiseLevel, IMolecularFormula parentFormula,
			HeuristicRule rules[]) {

		DataPoint msmsPeaks[] = msmsScan.getDataPoints();
		// Sorted by mass in descending order
		ArrayList<ElementRule> rulesSet = new ArrayList<ElementRule>();
		for (IIsotope isotope : parentFormula.isotopes()) {
			ElementRule rule = new ElementRule(isotope.getSymbol(), 0,
					parentFormula.getIsotopeCount(isotope));
			rulesSet.add(rule);
		}
		ElementRule msmsElementRules[] = rulesSet.toArray(new ElementRule[0]);

		int totalMSMSpeaks = 0, interpretedMSMSpeaks = 0;
		Map<DataPoint, String> msmsAnnotations = new Hashtable<DataPoint, String>();

		msmsCycle: for (DataPoint dp : msmsPeaks) {

			// Ignore all data points below the noise level
			if (dp.getIntensity() < msmsNoiseLevel)
				continue msmsCycle;

			// Check if this is an isotope
			Range isotopeCheckRange = new Range(dp.getMZ() - 1.4,
					dp.getMZ() - 0.6);
			for (DataPoint dpCheck : msmsPeaks) {
				// If we have any MS/MS peak with 1 neutron mass smaller m/z
				// and higher intensity, it means the current peak is an
				// isotope and we should ignore it
				if (isotopeCheckRange.contains(dpCheck.getMZ())
						&& (dpCheck.getIntensity() > dp.getIntensity())) {
					continue msmsCycle;
				}
			}

			// We don't know the charge of the fragment, so we will simply
			// assume 1
			double neutralLoss = msmsScan.getPrecursorMZ()
					* msmsScan.getPrecursorCharge() - dp.getMZ();

			// Ignore negative neutral losses and parent ion, <5 may be a
			// good threshold
			if (neutralLoss < 5)
				continue;

			Range msmsTargetRange = new Range(neutralLoss - msmsTolerance,
					neutralLoss + msmsTolerance);

			ResultFormulaWrapper wrap = new ResultFormulaWrapper();

			FormulaPredictionEngine msmsEngine = new FormulaPredictionEngine(
					msmsTargetRange, msmsElementRules, wrap);

			int foundMSMSformulas = msmsEngine.run();
			if (foundMSMSformulas > 0) {
				ResultFormula msmsFormula = wrap.getFormula();
				msmsAnnotations.put(dp, msmsFormula.getFormulaAsString());
				interpretedMSMSpeaks++;
			}

			totalMSMSpeaks++;

		}

		// If we did not evaluate any MS/MS peaks, we cannot calculate a score
		if (totalMSMSpeaks == 0)
			return null;

		double msmsScore = (double) interpretedMSMSpeaks / totalMSMSpeaks;

		MSMSScore result = new MSMSScore(msmsScore, msmsAnnotations);

		return result;

	}

}
