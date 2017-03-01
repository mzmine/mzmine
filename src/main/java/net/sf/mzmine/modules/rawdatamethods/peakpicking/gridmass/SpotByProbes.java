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

import java.util.ArrayList;

class SpotByProbes implements Comparable<SpotByProbes> {
    ArrayList<Probe> probes = new ArrayList<Probe>();
    int minScan = Integer.MAX_VALUE;
    int maxScan = Integer.MIN_VALUE;
    double maxMZ = Double.NEGATIVE_INFINITY;
    double minMZ = Double.POSITIVE_INFINITY;
    double minIntensity = Double.POSITIVE_INFINITY;
    double maxIntensity = Double.NEGATIVE_INFINITY;
    static int sid = 0;
    int spotId = -1;
    Probe center = null;
    int consecutiveScans = 0;
    ArrayList<Datum> maxDatums = null;

    SpotByProbes() {
    }

    void assignSpotId() {
	if (spotId < 0) {
	    sid++;
	    spotId = sid;
	}
    }

    int size() {
	return probes.size();
    }

    public int compareTo(SpotByProbes other) {
	if (other == null || other.center == null)
	    return -1;
	return this.center.compareTo(other.center);
    }

    void addProbe(Probe p) {
	probes.add(p);
	if (center == null)
	    center = p;
	if (p.mz > maxMZ)
	    maxMZ = p.mz;
	if (p.mz < minMZ)
	    minMZ = p.mz;
	if (p.mzCenter > maxMZ)
	    maxMZ = p.mzCenter;
	if (p.mzCenter < minMZ)
	    minMZ = p.mzCenter;
	if (p.scan > maxScan)
	    maxScan = p.scan;
	if (p.scan < minScan)
	    minScan = p.scan;
	if (p.scanCenter > maxScan)
	    maxScan = p.scanCenter;
	if (p.scanCenter < minScan)
	    minScan = p.scanCenter;
	if (p.intensityCenter > maxIntensity)
	    maxIntensity = p.intensityCenter;
	if (p.intensityCenter < minIntensity)
	    minIntensity = p.intensityCenter;
    }

    void setSpotIdToDatum(Datum d) {
	d.spotId = spotId;
	if (d.mz > maxMZ)
	    maxMZ = d.mz;
	if (d.mz < minMZ)
	    minMZ = d.mz;
	if (d.scan > maxScan)
	    maxScan = d.scan;
	if (d.scan < minScan)
	    minScan = d.scan;
	if (d.intensity > maxIntensity)
	    maxIntensity = d.intensity;
	if (d.intensity < minIntensity)
	    minIntensity = d.intensity;
    }

    void addProbesFromSpot(SpotByProbes sbp, boolean clear) {
	for (Probe p : sbp.probes) {
	    addProbe(p);
	}
	if (center.intensityCenter < sbp.center.intensityCenter)
	    center = sbp.center;
	if (clear) {
	    sbp.clear();
	}
    }

    void clear() {
	probes.clear();
	center = null;
	spotId = -1;
	minScan = Integer.MAX_VALUE;
	maxScan = Integer.MIN_VALUE;
	maxMZ = Double.NEGATIVE_INFINITY;
	minMZ = Double.POSITIVE_INFINITY;
	minIntensity = Double.POSITIVE_INFINITY;
	maxIntensity = Double.NEGATIVE_INFINITY;
    }

    public String toString() {
	return spotId
		+ " : "
		+ (center != null ? "MZ=" + Math.round(center.mzCenter * 10000)
			/ 10000.0 + ", Scan=" + center.scanCenter
			+ ", Intensity="
			+ Math.round(center.intensityCenter * 10) / 10.0 + ", "
			: "") + "Scans=[" + minScan + "~" + maxScan
		+ "],  MZ=[" + Math.round(minMZ * 10000) / 10000.0 + "~"
		+ Math.round(maxMZ * 10000) / 10000.0 + "]";
    }

    public String toString(double[] rettimes) {
	return spotId
		+ " : "
		+ (center != null ? "MZ=" + Math.round(center.mzCenter * 10000)
			/ 10000.0 + ", Time="
			+ Math.round(rettimes[center.scanCenter] * 1000.0)
			/ 1000.0 + ", Intensity="
			+ Math.round(center.intensityCenter * 10) / 10.0 + ", "
			: "") + "Times=["
		+ Math.round(rettimes[minScan] * 1000.0) / 1000.0 + "~"
		+ Math.round(rettimes[maxScan] * 1000.0) / 1000.0 + "],  MZ=["
		+ Math.round(minMZ * 10000) / 10000.0 + "~"
		+ Math.round(maxMZ * 10000) / 10000.0 + "]";
    }

    public void printDebugInfo() {
	System.out.println("*** SpotId : " + spotId + " ***");
	for (Probe p : probes) {
	    System.out.println("SpotId=" + spotId + ", Probe Scan=" + p.scan
		    + ", Probe m/z=" + p.mz + " Feature Scan=" + p.scanCenter
		    + ", Feature m/z=" + p.mzCenter);
	}
    }

    void buildMaxDatumFromScans(Datum[][] roi, double minimumHeight) {

	int i, j;
	ArrayList<Datum> mxD = new ArrayList<Datum>();
	int cont = 0;
	consecutiveScans = 0;
	double theMinMZ = minMZ;// - mzTol;
	double theMaxMZ = maxMZ;// + mzTol;
	for (i = minScan; i <= maxScan; i++) {
	    Datum[] di = roi[i];
	    if (di != null && di.length > 0) {
		Datum max = null;
		int idx = GridMassTask.findFirstMass(theMinMZ, di);
		for (j = idx; j < di.length && di[j].mz <= theMaxMZ; j++) {
		    Datum d = di[j];
		    if (d.spotId == spotId) {
			if ((max == null || d.intensity > max.intensity)
				&& d.intensity > minimumHeight) { // d.mz >=
			    // theMinMZ &&
			    // (it is
			    // already
			    // assigned to
			    // spotid)
			    max = d;
			}
		    }
		}
		if (max != null && max.intensity > 0) {
		    mxD.add(max);
		    cont++;
		} else {
		    cont = 0;
		}
	    } else {
		cont = 0;
	    }
	    if (cont > consecutiveScans)
		consecutiveScans = cont;
	}
	maxDatums = mxD;

    }

    int getMaxDatumScans() {
	return (maxDatums == null || maxDatums.size() == 0 ? 0 : maxDatums
		.get(maxDatums.size() - 1).scan - maxDatums.get(0).scan + 1); // maxDatums.size()
    }

    int getContigousMaxDatumScans() {
	return (maxDatums == null || maxDatums.size() == 0 ? 0
		: consecutiveScans);
    }

    float getContigousToMaxDatumScansRatio() {
	if (maxDatums == null || maxDatums.size() == 0)
	    return 0;
	return ((float) getContigousMaxDatumScans() / (float) getMaxDatumScans());
    }

}
