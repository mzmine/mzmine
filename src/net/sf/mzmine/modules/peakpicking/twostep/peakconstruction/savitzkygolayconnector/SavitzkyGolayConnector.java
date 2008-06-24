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

package net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.savitzkygolayconnector;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak;
import net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.ConnectedMzPeak;
import net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.ConnectedPeak;
import net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.MatchScore;
import net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.PeakBuilder;

/**
 * This class implements a peak builder using a match score to link MzPeaks in
 * the axis of retention time. Also uses Savitzky-Golay coefficients to
 * calculate the first and second derivative (smoothed) of raw data points
 * (intensity) that conforms each peak. The first derivative is used to
 * determine the peak's range, and the second derivative to determine the
 * intensity of the peak.
 * 
 */
public class SavitzkyGolayConnector implements PeakBuilder {

	//private Logger logger = Logger.getLogger(this.getClass().getName());

	private float intTolerance, mzTolerance;
	private float minimumPeakHeight, minimumPeakDuration;
	private Vector<ConnectedPeak> underConstructionPeaks;

	public SavitzkyGolayConnector(SavitzkyGolayConnectorParameters parameters) {
		intTolerance = (Float) parameters
				.getParameterValue(SavitzkyGolayConnectorParameters.intTolerance);
		minimumPeakDuration = (Float) parameters
				.getParameterValue(SavitzkyGolayConnectorParameters.minimumPeakDuration);
		minimumPeakHeight = (Float) parameters
				.getParameterValue(SavitzkyGolayConnectorParameters.minimumPeakHeight);
		mzTolerance = (Float) parameters
				.getParameterValue(SavitzkyGolayConnectorParameters.mzTolerance);

		underConstructionPeaks = new Vector<ConnectedPeak>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.PeakBuilder#addScan(net.sf.mzmine.data.Scan,
	 *      net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak[],
	 *      net.sf.mzmine.data.RawDataFile)
	 */
	public Peak[] addScan(Scan scan, MzPeak[] mzValues, RawDataFile dataFile) {

		Vector<Peak> finishedPeaks = new Vector<Peak>();
		Vector<ConnectedMzPeak> cMzPeaks = new Vector<ConnectedMzPeak>();

		// Calculate scores between MzPeaks
		TreeSet<MatchScore> scores = new TreeSet<MatchScore>();

		// Convert MzPeak in ConnectedMzPeak to deal with status property
		// (boolean connected)
		for (MzPeak mzPeak : mzValues)
			cMzPeaks.add(new ConnectedMzPeak(scan, mzPeak));

		// Calculate score for each ConnectedMzPeak
		for (ConnectedPeak ucPeak : underConstructionPeaks) {
			for (ConnectedMzPeak currentMzPeak : cMzPeaks) {
				MatchScore score = new MatchScore(ucPeak, currentMzPeak,
						mzTolerance, intTolerance);

				if (score.getScore() < Float.MAX_VALUE) {
					scores.add(score);
				}
			}
		}

		// Connect the best scoring pairs of under-construction and 1d peaks

		Iterator<MatchScore> scoreIterator = scores.iterator();
		while (scoreIterator.hasNext()) {
			MatchScore score = scoreIterator.next();

			// If ConnectedMzPeak is already connected, then move to next score
			ConnectedMzPeak cMzPeak = score.getMzPeak();
			if (cMzPeak.isConnected()) {
				continue;
			}

			// If ConnectedMzPeak is growing, then move on to next score
			ConnectedPeak ucPeak = score.getPeak();
			if (ucPeak.isGrowing()) {
				continue;
			}

			// Add MzPeak to the proper Peak and set status connected
			ucPeak.addMzPeak(cMzPeak);
			cMzPeak.setConnected();
		}

		// Check if there are any under-construction peaks that were not
		// connected (finished)

		Iterator<ConnectedPeak> iteratorConPeak = underConstructionPeaks
				.iterator();
		while (iteratorConPeak.hasNext()) {

			ConnectedPeak ucPeak = iteratorConPeak.next();
			// If nothing was added,
			if (!ucPeak.isGrowing()) {

				// Finalize peak
				ucPeak.finalizedAddingDatapoints(PeakStatus.DETECTED);

				// Check length & height
				float ucLength = ucPeak.getRawDataPointsRTRange().getSize();
				float ucHeight = ucPeak.getHeight();

				if ((ucLength >= minimumPeakDuration)
						&& (ucHeight >= minimumPeakHeight)) {
					// Apply second criteria to identify possible peaks.

					Peak[] possiblePeaks = SGPeaksSearch(ucPeak);

					if (possiblePeaks.length != 0) {
						for (Peak p : possiblePeaks) {
							float pLength = p.getRawDataPointsRTRange()
									.getSize();
							float pHeight = p.getHeight();
							if ((pLength >= minimumPeakDuration)
									&& (pHeight >= minimumPeakHeight)) {
								finishedPeaks.add(p);
							}
						}
					}
					else
						finishedPeaks.add(ucPeak);

				}

				// Remove the peak from under construction peaks
				iteratorConPeak.remove();

			} else
				ucPeak.resetGrowingState();
		}

		// If there are some unconnected MzPeaks, then start a new
		// under-construction peak for each of them

		for (ConnectedMzPeak cMzPeak : cMzPeaks) {
			if (!cMzPeak.isConnected()) {
				SavitzkyGolayPeak ucPeak = new SavitzkyGolayPeak(dataFile,
						cMzPeak);
				underConstructionPeaks.add(ucPeak);
			}

		}

		return finishedPeaks.toArray(new Peak[0]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.PeakBuilder#finishPeaks()
	 */
	public Peak[] finishPeaks() {
		Vector<Peak> finishedPeaks = new Vector<Peak>();
		for (ConnectedPeak ucPeak : underConstructionPeaks) {
			// Finalize peak
			ucPeak.finalizedAddingDatapoints(PeakStatus.DETECTED);

			// Check length & height
			float ucLength = ucPeak.getRawDataPointsRTRange().getSize();
			float ucHeight = ucPeak.getHeight();

			if ((ucLength >= minimumPeakDuration)
					&& (ucHeight >= minimumPeakHeight)) {

				Peak[] possiblePeaks = SGPeaksSearch(ucPeak);

				if (possiblePeaks.length != 0) {
					for (Peak p : possiblePeaks) {
						float pLength = p.getRawDataPointsRTRange().getSize();
						float pHeight = p.getHeight();
						if ((pLength >= minimumPeakDuration)
								&& (pHeight >= minimumPeakHeight)) {
							finishedPeaks.add(p);
						}
					}
				}
				else
					finishedPeaks.add(ucPeak);

			}
		}
		return finishedPeaks.toArray(new Peak[0]);
	}

	/**
	 * 
	 * 
	 * @param ConnectedPeak
	 *            ucPeak
	 * @return Peak[]
	 */
	private Peak[] SGPeaksSearch(ConnectedPeak ucPeak) {

		boolean activeFirstPeak = false, activeSecondPeak = false, passThreshold= false;
		int crossZero = 0;

		ConnectedMzPeak[] mzValues = ucPeak.getConnectedMzPeaks();

		float[] derivativeOfIntensities = new float[mzValues.length];
		derivativeOfIntensities = ((SavitzkyGolayPeak) ucPeak)
				.getDerivative(false);
		float noiseThreshold = ((SavitzkyGolayPeak) ucPeak)
				.getDerivativeThreshold();

		Vector<ConnectedPeak> newPeaks = new Vector<ConnectedPeak>();
		Vector<ConnectedMzPeak> newMzPeaks = new Vector<ConnectedMzPeak>();
		Vector<ConnectedMzPeak> newOverlappedMzPeaks = new Vector<ConnectedMzPeak>();

		for (int i = 1; i < derivativeOfIntensities.length; i++) {

			if (((derivativeOfIntensities[i-1] < 0.0f) && (derivativeOfIntensities[i] > 0.0f))
					|| ((derivativeOfIntensities[i-1] > 0.0f) && (derivativeOfIntensities[i] < 0.0f))){

				if ((derivativeOfIntensities[i-1] < 0.0f) && (derivativeOfIntensities[i] > 0.0f)){
					if (crossZero == 0){
						activeFirstPeak = true;
					}						
					if (crossZero == 2){
						if (passThreshold){
							activeSecondPeak = true;
						}
						else{
							newMzPeaks.clear();
							crossZero = 0;
						}
					}					

				}
				
				if (crossZero == 3){
					activeFirstPeak = false;
				}

				//Always increment
				passThreshold = false;
				if ((activeFirstPeak) || (activeSecondPeak)){
					crossZero++;
				}

			}

			if (Math.abs(derivativeOfIntensities[i]) > Math.abs(noiseThreshold)){
				passThreshold = true;
				
				if ((crossZero == 0) && (derivativeOfIntensities[i] > 0)){
					activeFirstPeak = true;
					crossZero++;
				}
			}
				

			//if (true) {
			if ((activeFirstPeak)) {

				/*ConnectedMzPeak temp = new ConnectedMzPeak(mzValues[i]
						.getScan(), new MzPeak(new SimpleDataPoint(mzValues[i]
						.getMzPeak().getMZ(), derivativeOfIntensities[i]*100)));
				newMzPeaks.add(temp);*/

				newMzPeaks.add(mzValues[i]);
			}

			if (activeSecondPeak) {

				/*ConnectedMzPeak temp = new ConnectedMzPeak(mzValues[i]
						.getScan(), new MzPeak(new SimpleDataPoint(mzValues[i]
						.getMzPeak().getMZ(), derivativeOfIntensities[i] * 100)));
				newOverlappedMzPeaks.add(temp);*/
				
				newOverlappedMzPeaks.add(mzValues[i]);
			}

			if ((newMzPeaks.size() > 0) && (!activeFirstPeak)) {
				ConnectedPeak SGPeak = new ConnectedPeak(ucPeak.getDataFile(),
						newMzPeaks.elementAt(0));
				for (int j = 1; j < newMzPeaks.size(); j++) {
					SGPeak.addMzPeak(newMzPeaks.elementAt(j));
				}
				newMzPeaks.clear();
				SGPeak.finalizedAddingDatapoints(PeakStatus.DETECTED);
				newPeaks.add(SGPeak);
				
				if ((newOverlappedMzPeaks.size() > 0) && (activeSecondPeak)){
					for (ConnectedMzPeak p : newOverlappedMzPeaks)
							newMzPeaks.add(p);
					activeSecondPeak = false;
					activeFirstPeak = true;
					crossZero = 2;
					newOverlappedMzPeaks.clear();
				}
			}

		}

		return newPeaks.toArray(new ConnectedPeak[0]);
	}

}
