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

package net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.baselineconnector;

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
 * This class implements a simple peak builder. This takes all detected MzPeaks
 * in one Scan and try to find a possible relationship between each one of these
 * with MzPeaks of the previous scan. This relationship is set by a match score
 * using MatchScore class, according with the parameters of Tolerance of MZ and
 * Intensity. Also it can apply a second search for possible peaks (threshold
 * level), over a already detected peak.
 * 
 */
public class BaselineConnector implements PeakBuilder {

	private float baselineLevel, mzTolerance;
	private Vector<ConnectedPeak> underConstructionPeaks;

	public BaselineConnector(BaselineConnectorParameters parameters) {
		baselineLevel = (Float) parameters
				.getParameterValue(BaselineConnectorParameters.baselineLevel);
		mzTolerance = (Float) parameters
				.getParameterValue(BaselineConnectorParameters.mzTolerance);
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
		// (boolean connected) and verify if it has an intensity bigger than
		// baseline level.
		for (MzPeak mzPeak : mzValues) {
			if (mzPeak.getIntensity() >= baselineLevel)
				cMzPeaks.add(new ConnectedMzPeak(scan, mzPeak));
		}

		// Calculate score for each ConnectedMzPeak
		for (ConnectedPeak ucPeak : underConstructionPeaks) {
			for (ConnectedMzPeak currentMzPeak : cMzPeaks) {
				MatchScore score = new MatchScore(ucPeak, currentMzPeak,
						mzTolerance, Float.MAX_VALUE);

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
				finishedPeaks.add(ucPeak);

				// Remove the peak from under construction peaks
				iteratorConPeak.remove();

			} else
				ucPeak.resetGrowingState();
		}

		// If there are some unconnected MzPeaks, then start a new
		// under-construction peak for each of them

		for (ConnectedMzPeak cMzPeak : cMzPeaks) {
			if (!cMzPeak.isConnected()) {
				ConnectedPeak ucPeak = new ConnectedPeak(dataFile, cMzPeak);
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
			finishedPeaks.add(ucPeak);
		}
		return finishedPeaks.toArray(new Peak[0]);
	}

}
