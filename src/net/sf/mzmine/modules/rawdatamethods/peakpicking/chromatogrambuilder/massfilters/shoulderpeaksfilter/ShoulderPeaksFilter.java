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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.massfilters.shoulderpeaksfilter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.MzPeak;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.massdetection.exactmass.PeakModel;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.chromatogrambuilder.massfilters.MassFilter;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class ShoulderPeaksFilter implements MassFilter {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private ShoulderPeaksFilterParameters parameters;

	private PeakModel peakModel;

	public ShoulderPeaksFilter() {
		parameters = new ShoulderPeaksFilterParameters();
	}

	public MzPeak[] filterMassValues(MzPeak[] mzPeaks) {

		// Try to create an instance of the peak model
		try {
			PeakModelType type = (PeakModelType) parameters
					.getParameterValue(ShoulderPeaksFilterParameters.peakModel);
			if (type == null) type = PeakModelType.GAUSS;
			Class modelClass = type.getModelClass();
			peakModel = (PeakModel) modelClass.newInstance();
		} catch (Exception e) {
			logger.log(Level.WARNING, "Could not create instance of peak model class", e);
		}
		
		// If peakModel is null, just don't do any filtering
		if (peakModel == null)
			return mzPeaks;

		// Create a tree set of detected mzPeaks sorted by MZ in ascending order
		TreeSet<MzPeak> finalMZPeaks = new TreeSet<MzPeak>(new DataPointSorter(
				SortingProperty.MZ, SortingDirection.Ascending));

		// Create a tree set of candidate mzPeaks sorted by intensity in
		// descending order.
		TreeSet<MzPeak> candidatePeaks = new TreeSet<MzPeak>(
				new DataPointSorter(SortingProperty.Intensity,
						SortingDirection.Descending));
		candidatePeaks.addAll(Arrays.asList(mzPeaks));

		while (candidatePeaks.size() > 0) {

			// Always take the biggest (intensity) peak
			MzPeak currentCandidate = candidatePeaks.first();

			// Add this candidate to the final tree set sorted by MZ and remove
			// from tree set sorted by intensity
			finalMZPeaks.add(currentCandidate);
			candidatePeaks.remove(currentCandidate);

			// Remove from tree set sorted by intensity all FTMS shoulder peaks,
			// taking as a main peak the current candidate
			removeLateralPeaks(currentCandidate, candidatePeaks, peakModel);

		}

		return finalMZPeaks.toArray(new MzPeak[0]);
	}

	public String getName() {
		return "FTML shoulder peaks filter";
	}

	public SimpleParameterSet getParameters() {
		return parameters;
	}

	public String toString() {
		return getName();
	}

	/**
	 * This function remove peaks encountered in the lateral of a main peak
	 * (currentCandidate) that are considered as garbage, for example FTMS
	 * shoulder peaks.
	 * 
	 * First calculates a peak model (Gauss, Lorenzian, etc) defined by
	 * peakModelName parameter, with the same position (m/z) and height
	 * (intensity) of the currentCandidate, and the defined resolution
	 * (resolution parameter). Second search and remove all the lateral peaks
	 * that are under the curve of the modeled peak.
	 * 
	 */
	private void removeLateralPeaks(MzPeak currentCandidate,
			TreeSet<MzPeak> candidates, PeakModel peakModel) {

		int resolution = (Integer) parameters
				.getParameterValue(ShoulderPeaksFilterParameters.resolution);

		// We set our peak model with same position(m/z), height(intensity) and
		// resolution of the current peak
		peakModel.setParameters(currentCandidate.getMZ(), currentCandidate
				.getIntensity(), resolution);

		// We search over all peak candidates and remove all of them that are
		// under the curve defined by our peak model
		Iterator<MzPeak> candidatesIterator = candidates.iterator();
		while (candidatesIterator.hasNext()) {

			MzPeak lateralCandidate = candidatesIterator.next();

			// Condition in x domain (m/z)
			if ((lateralCandidate.getIntensity() < peakModel
					.getIntensity(lateralCandidate.getMZ()))) {
				candidatesIterator.remove();
			}
		}

	}
}
