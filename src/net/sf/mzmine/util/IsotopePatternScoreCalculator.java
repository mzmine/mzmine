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

package net.sf.mzmine.util;

import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.impl.SimpleDataPoint;

public class IsotopePatternScoreCalculator {

	// A quarter of Hydrogen's mass
	private static double TOLERANCE = 0.25195625d;

	public static double getScore(IsotopePattern ip1, IsotopePattern ip2) {

		double diffMass, diffAbun, factor, totalFactor = 0d;
		double score = 0d, tempScore;
		DataPoint closestDp;
		int numIsotopes1 = ip1.getNumberOfIsotopes();
		int numIsotopes2 = ip1.getNumberOfIsotopes();
		int length = numIsotopes1;

		if (numIsotopes1 < numIsotopes2)
			length = numIsotopes2;

		DataPoint[] dp1 = ip1.getIsotopes().clone();
		DataPoint[] dp2 = ip2.getIsotopes().clone();

		dp1 = sortAndNormalizedByIntensity(dp1);
		dp2 = sortAndNormalizedByIntensity(dp2);

		for (int i = 0; i < length; i++) {

			factor = 1.0d;// Math.pow(2.0d, i);
			totalFactor += factor;

			closestDp = getClosestDataPoint(dp1[i], dp2);
			dp2 = removeDataPoint(closestDp, dp2);

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

	private static DataPoint[] sortAndNormalizedByIntensity(
			DataPoint[] dataPoints) {

		double intensity, biggestIntensity = Double.MIN_NORMAL;
		TreeSet<DataPoint> sortedDataPoints = new TreeSet<DataPoint>(
				new DataPointSorter(false, false));

		for (DataPoint dp : dataPoints) {

			intensity = dp.getIntensity();
			if (intensity > biggestIntensity)
				biggestIntensity = intensity;

		}

		for (DataPoint dp : dataPoints) {

			intensity = dp.getIntensity();
			intensity /= biggestIntensity;
			if (intensity < 0)
				intensity = 0;

			((SimpleDataPoint) dp).setIntensity(intensity);
			sortedDataPoints.add(dp);

		}

		return sortedDataPoints.toArray(new DataPoint[0]);

	}

	private static DataPoint getClosestDataPoint(DataPoint dp,
			DataPoint[] dataPoints) {

		double diff;
		TreeSet<DataPoint> sortedDataPoints = new TreeSet<DataPoint>(
				new DataPointSorter(false, false));

		for (DataPoint localDp : dataPoints) {
			diff = Math.abs(dp.getMZ() - localDp.getMZ());
			if (diff <= TOLERANCE) {
				sortedDataPoints.add(localDp);
			}
		}

		return sortedDataPoints.first();

	}

	private static DataPoint[] removeDataPoint(DataPoint dp,
			DataPoint[] dataPoints) {

		Vector<DataPoint> sortedDataPoints = new Vector<DataPoint>();

		for (DataPoint localDp : dataPoints) {
			if ((localDp.getMZ() == dp.getMZ())
					&& (localDp.getIntensity() == dp.getIntensity()))
				continue;
			sortedDataPoints.add(localDp);
		}

		return sortedDataPoints.toArray(new DataPoint[0]);

	}

}
