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

package net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IsotopePattern;

import org.openscience.cdk.formula.IsotopeContainer;
import org.openscience.cdk.formula.IsotopePatternSimilarity;

public class IsotopePatternScoreCalculator {

	/**
	 * Returns a calculated similarity score of two isotope patterns in the
	 * range of 0 (not similar at all) to 1 (100% same). Score is calculated
	 * using CDK method.
	 * 
	 */
	public static double getSimilarityScore(IsotopePattern ip1,
			IsotopePattern ip2) {

		assert ip1 != null;
		assert ip2 != null;
		
		// First, normalize the isotopes to intensity 0..1
		IsotopePattern nip1 = ip1.normalizeTo(1);
		IsotopePattern nip2 = ip2.normalizeTo(1);

		// Create CDK isotope patterns
		org.openscience.cdk.formula.IsotopePattern cdkIP1 = new org.openscience.cdk.formula.IsotopePattern();
		org.openscience.cdk.formula.IsotopePattern cdkIP2 = new org.openscience.cdk.formula.IsotopePattern();

		// Fill the CDK isotope patterns with our isotopes
		for (DataPoint isotope : nip1.getDataPoints()) {
			IsotopeContainer cdkIs = new IsotopeContainer(isotope.getMZ(),
					isotope.getIntensity());
			cdkIP1.addIsotope(cdkIs);
		}
		for (DataPoint isotope : nip2.getDataPoints()) {
			IsotopeContainer cdkIs = new IsotopeContainer(isotope.getMZ(),
					isotope.getIntensity());
			cdkIP2.addIsotope(cdkIs);
		}

		// Create a CDK object for comparing isotope patterns
		IsotopePatternSimilarity cdkSimilarityCalculator = new IsotopePatternSimilarity();
		
		// Set the mass tolerance to 10 ppm
		cdkSimilarityCalculator.seTolerance(10);

		// Compare the CDK isotope patterns using CDK comparator
		double score = cdkSimilarityCalculator.compare(cdkIP1, cdkIP2);

		// Return the final score
		return score;
	}

}
