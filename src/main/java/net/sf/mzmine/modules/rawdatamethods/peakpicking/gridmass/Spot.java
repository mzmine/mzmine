/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.peakpicking.gridmass;

class Spot {
    double minMZ = Double.MAX_VALUE;
    double maxMZ = Double.MIN_VALUE;
    int minScan = Integer.MAX_VALUE;
    int maxScan = Integer.MIN_VALUE;

    int pointsGTth = 0;
    double sumGTth = 0;
    int points = 0;
    double sum = 0;
    double mcMass = 0;
    double mcScan = 0;
    double maxIntensity = 0;
    int maxIntScan = 0;
    double maxIntensityMZ = 0;

    String scansGTth = "";
    int pointsScans = 0;
    int pointsNoSpot = 0; // points within spot frame that are part of other

    // spot (this is operated outside this class)

    void addPoint(int scan, double mz, double intensity) {
	// intensity is + if > threshold, and - if not
	points++;
	sum += Math.abs(intensity);
	if (intensity > 0) {
	    sumGTth += intensity;
	    pointsGTth++;

	    // Mass Center
	    mcMass += intensity * mz;
	    mcScan += intensity * scan;

	    // Max Intensity
	    if (intensity > maxIntensity) {
		maxIntensity = intensity;
		maxIntScan = scan;
		maxIntensityMZ = mz;
	    }

	}
	if (mz > maxMZ)
	    maxMZ = mz;
	if (mz < minMZ)
	    minMZ = mz;
	if (scan > maxScan)
	    maxScan = scan;
	if (scan < minScan)
	    minScan = scan;
    }

    double massCenterMZ() {
	return (sumGTth > 0 ? mcMass / sumGTth : 0);
    }

    double massCenterScan() {
	return (sumGTth > 0 ? mcScan / sumGTth : 0);
    }

    double fractionPoints() {
	return (points > 0 ? (double) pointsGTth / (double) points : 0);
    }

    double averageIntensityAllPoints() {
	return (points > 0 ? sumGTth / points : 0);
    }

    double averageIntensity() {
	return (pointsGTth > 0 ? sumGTth / pointsGTth : 0);
    }

    double fractionIntensity() {
	return (sum > 0 ? sumGTth / sum : 0);
    }

    double fractionPointsForMZResolution(double mzResolution) {
	return (double) points / (double) pixelArea(mzResolution);
    }

    double fractionGTthPointsForMZResolution(double mzResolution) {
	return (double) pointsGTth / (double) pixelArea(mzResolution);
    }

    int width() {
	return maxScan - minScan + 1;
    }

    double height() {
	return maxMZ - minMZ;
    }

    int pixelArea(double mzResolution) {
	return (int) Math.round((width() * (height() / mzResolution + 1)));
    }

}
