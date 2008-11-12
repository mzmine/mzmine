/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.isotopes.isotopepatternscore;

import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.util.DataPointSorter;

public class IsotopePatternScoreCalculator {

	// A quarter of Hydrogen's mass
	private static double TOLERANCE = 0.25195625d;

	/**
	 * Receive to IsotopePattern and compare the second against the first to get
	 * a score value. This value is based in the difference of ratio between
	 * isotopes and mass.
	 * 
	 * @param ip1
	 *            Isotope pattern reference (predicted)
	 * @param ip2
	 *            Isotope pattern to be compared (detected)
	 * @return score a calculated double value
	 */
	public static double getScore(IsotopePattern ip1, IsotopePattern ip2) {

		double diffMass, diffAbun, factor, totalFactor = 0d;
		double score = 0d, tempScore;
		MzDataPoint closestDp;
		int numIsotopes1 = ip1.getNumberOfIsotopes();
		int numIsotopes2 = ip1.getNumberOfIsotopes();
		int length = numIsotopes1;

		// Maximum number of isotopes to be compared
		if (numIsotopes1 < numIsotopes2)
			length = numIsotopes2;

		MzDataPoint[] dp1 = ip1.getIsotopes().clone();
		MzDataPoint[] dp2 = ip2.getIsotopes().clone();

		// Normalize the intensity of isotopes regarding the biggest one
		dp1 = sortAndNormalizedByIntensity(dp1);
		dp2 = sortAndNormalizedByIntensity(dp2);

		for (int i = 0; i < length; i++) {

			factor = dp1[i].getIntensity();//1.0d;// Math.pow(2.0d, i);
			totalFactor += factor;
			
			// Search for the closest isotope in the second pattern (detected) to the
			// current isotope (predicted pattern)
			closestDp = getClosestDataPoint(dp1[i], dp2);
			
			if (closestDp == null)
				continue;

			// Remove from the second pattern the used isotope to set the score.
			dp2 = removeDataPoint(closestDp, dp2);

			// Calculate the score using the next formula.
			//
			// Score = { [ 1 - (IsotopeMass1[i] - IsotopeMass2[j]) + (1 -
			// (IsotopeIntensity1[i] / IsotopeIntensity2[j])) ] * factor[i] } /
			// totalFactor
			//
			// Where i is equal to the number of isotopes of the first pattern
			// and j is the closest isotope of the second pattern. Factor is the
			// given weight to each isotope, and totalFactor is the sum of all
			// values of factor.

			diffMass = dp1[i].getMZ() - closestDp.getMZ();
			diffMass = Math.abs(diffMass);

			diffAbun = 1.0d - (dp1[i].getIntensity() / closestDp.getIntensity());
			diffAbun = Math.abs(diffAbun);

			tempScore = 1 - (diffMass + diffAbun);

			if (tempScore < 0)
				tempScore = 0;

			score += (tempScore * factor);

		}

		return score / totalFactor;
	}

	/**
	 * Sort and normalize an array of DataPoint objects according with the
	 * biggest intensity DataPoint
	 * 
	 * @param dataPoints
	 * @return
	 */
	private static MzDataPoint[] sortAndNormalizedByIntensity(
			MzDataPoint[] dataPoints) {

		double intensity, biggestIntensity = Double.MIN_VALUE;
		TreeSet<MzDataPoint> sortedDataPoints = new TreeSet<MzDataPoint>(
				new DataPointSorter(false, false));

		for (MzDataPoint dp : dataPoints) {

			intensity = dp.getIntensity();
			if (intensity > biggestIntensity)
				biggestIntensity = intensity;

		}

		for (MzDataPoint dp : dataPoints) {

			intensity = dp.getIntensity();
			intensity /= biggestIntensity;
			if (intensity < 0)
				intensity = 0;

			((SimpleDataPoint) dp).setIntensity(intensity);
			sortedDataPoints.add(dp);

		}

		return sortedDataPoints.toArray(new MzDataPoint[0]);

	}

	/**
	 * Search and find the closest DataPoint in an array in terms of mass and
	 * intensity. Always return a DataPoint
	 * 
	 * @param dp
	 * @param dataPoints
	 * @return DataPoint
	 */
	private static MzDataPoint getClosestDataPoint(MzDataPoint dp,
			MzDataPoint[] dataPoints) {

		double diff;
		TreeSet<MzDataPoint> sortedDataPoints = new TreeSet<MzDataPoint>(
				new DataPointSorter(false, false));

		for (MzDataPoint localDp : dataPoints) {
			diff = Math.abs(dp.getMZ() - localDp.getMZ());
			if (diff <= TOLERANCE) {
				sortedDataPoints.add(localDp);
			}
		}
		
		if (sortedDataPoints.size() > 0)
			return sortedDataPoints.first();
		
		return null;

	}

	/**
	 * Remove an element from a DataPoint objects array.
	 * 
	 * @param dp
	 *            element to remove
	 * @param dataPoints
	 *            array of DataPoint objects
	 * @return
	 */
	private static MzDataPoint[] removeDataPoint(MzDataPoint dp,
			MzDataPoint[] dataPoints) {

		Vector<MzDataPoint> sortedDataPoints = new Vector<MzDataPoint>();

		for (MzDataPoint localDp : dataPoints) {
			if ((localDp.getMZ() == dp.getMZ())
					&& (localDp.getIntensity() == dp.getIntensity()))
				continue;
			sortedDataPoints.add(localDp);
		}

		return sortedDataPoints.toArray(new MzDataPoint[0]);

	}

}
