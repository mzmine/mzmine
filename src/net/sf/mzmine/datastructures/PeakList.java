/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/


package net.sf.mzmine.datastructures;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class PeakList implements Serializable {

	private int currentPeakID = 0;
	private Vector<Peak> peaks;
	private int selectedPeakID = -1;

	private Hashtable<Integer, double[]> isotopePatternBoundingBoxes;

	/**
	 * Constructor: Initializes a vector for storing individual peaks
	 */
	public PeakList() {
		peaks = new Vector<Peak>();
	}

	/**
	 * Returns all peaks
	 */
	public Vector<Peak> getPeaks() {
		return peaks;
	}

	/**
	 * Returns number of peaks
	 */
	public int getNumberOfPeaks() {
		return peaks.size();
	}

	/**
	 * Adds a new peak to peak list
	 */
	public void addPeak(Peak p) {
		currentPeakID++;
		p.setPeakID(currentPeakID);
		peaks.add(p);
	}

	/**
	 * This method adds a new peak to peak list and keeps its existing ID
	 * Useful when creating a new peak list using previous one as a template.
	 */
	public void addPeakKeepOldID(Peak p) {
		peaks.add(p);
	}

	/**
	 * Returns peak with given peakID
	 */
	public Peak getPeak(int peakID) {
		if (peaks == null) { return null; }

		for (Peak p : peaks) {
			if (p.getPeakID() == peakID) { return p; }
		}
		return null;
	}

	/**
	 * Returns peak ID of selected peak
	 */
	public int getSelectedPeakID() {
		return selectedPeakID;
	}

	public void setSelectedPeakID(int _selectedPeakID) {
		selectedPeakID = _selectedPeakID;
	}

	/**
	 * Returns all peaks that have some M/Z peaks inside defined scan range
	 */
	public Vector<Peak> getPeaksForScans(int startScan, int stopScan) {

		if (peaks==null) return null;

/*
		if ((startScan == 0) && (stopScan == (getNumberOfScans()-1)) ) {
			return peaks;
		}
*/

		Vector<Peak> peaksInside = new Vector<Peak>();

		for (Peak p : peaks) {
			if ( (p.getStartScanNumber()<=stopScan) && (p.getStopScanNumber()>=startScan) ) {
				peaksInside.add(p);
			}
		}
		return peaksInside;
	}

	public Vector<double[]> getIsotopePatternBoundingBoxesForScans(int startScan, int stopScan) {

		if (peaks == null) return null;

		if (isotopePatternBoundingBoxes == null) { calculateIsotopePatternBoundingBoxes();	}

		Enumeration<double[]> allBoxes = isotopePatternBoundingBoxes.elements();
		Vector<double[]> overlappingBoxes = new Vector<double[]>();

		while (allBoxes.hasMoreElements()) {

			double[] oneBox = allBoxes.nextElement();

			if ( (oneBox[1]<=stopScan) && (oneBox[3]>=startScan) ) {
				overlappingBoxes.add(oneBox);
			}

		}

		return overlappingBoxes;

	}

	/**
	 *
	 */
	public Peak findNearestPeak(double mz, double rt, double ratioMZvsRT) {
		Peak tmpP;
		double tmpDistance;
		//double rt = getScanTime(scan);

		Peak nearestP = null;
		double nearestDistance = Double.MAX_VALUE;

		if ((peaks==null) || (peaks.size()==0)) { return null; }

		for (int i=0; i<peaks.size(); i++) {
			tmpP = peaks.get(i);
			tmpDistance = java.lang.Math.abs(mz-tmpP.getMZ())+ratioMZvsRT*java.lang.Math.abs(rt-tmpP.getRT());
			if (nearestDistance>=tmpDistance) {
				nearestP = tmpP;
				nearestDistance = tmpDistance;
			}
		}

		return nearestP;
	}


	public void clearIsotopePatternInformation() {
		isotopePatternBoundingBoxes.clear();
		isotopePatternBoundingBoxes = null;
	}

	private void calculateIsotopePatternBoundingBoxes() {

		if (peaks == null) return;

		isotopePatternBoundingBoxes = new Hashtable<Integer, double[]>();

		for (Peak aPeak : peaks) {

			Integer isotopePatternID = new Integer(aPeak.getIsotopePatternID());

			// Not a member of any isotope pattern?
			if (aPeak.getIsotopePatternID()<0) { continue; }


			double[] boundingBox = isotopePatternBoundingBoxes.get(isotopePatternID);

			// Is this first member of the isotope pattern?
			if (boundingBox == null) {

				boundingBox = new double[4];
				boundingBox[0] = aPeak.getMZMinimum();
				boundingBox[1] = aPeak.getStartScanNumber();
				boundingBox[2] = aPeak.getMZMaximum();
				boundingBox[3] = aPeak.getStopScanNumber();

			} else {

				if (boundingBox[0]>aPeak.getMZMinimum()) { boundingBox[0] = aPeak.getMZMinimum(); }
				if (boundingBox[1]>aPeak.getStartScanNumber()) { boundingBox[1] = aPeak.getStartScanNumber(); }
				if (boundingBox[2]<aPeak.getMZMaximum()) { boundingBox[2] = aPeak.getMZMaximum(); }
				if (boundingBox[3]<aPeak.getStopScanNumber()) { boundingBox[3] = aPeak.getStopScanNumber(); }

			}


			isotopePatternBoundingBoxes.put(isotopePatternID, boundingBox);

		}

	}

}