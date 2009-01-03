/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massconnection.scoreconnector;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleMzPeak;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.Chromatogram;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ChromatogramBuilder;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ConnectedMzPeak;

public class ScoreConnector implements ChromatogramBuilder {

	private double mzTolerance, minimumTimeSpan;
	private Vector<Chromatogram> underConstructionChromatograms;

	public ScoreConnector(ScoreConnectorParameters parameters) {

		minimumTimeSpan = (Double) parameters
				.getParameterValue(ScoreConnectorParameters.minimumTimeSpan);
		mzTolerance = (Double) parameters
				.getParameterValue(ScoreConnectorParameters.mzTolerance);

		underConstructionChromatograms = new Vector<Chromatogram>();
	}

	public void addScan(RawDataFile dataFile, Scan scan, MzPeak[] mzValues) {

		// Convert MzPeak in ConnectedMzPeak to deal with status property
		// (boolean connected)
		Vector<ConnectedMzPeak> cMzPeaks = new Vector<ConnectedMzPeak>();
		for (MzPeak mzPeak : mzValues)
			cMzPeaks.add(new ConnectedMzPeak(scan, mzPeak));

		// Calculate scores between Chromatogram and MzPeaks
		TreeSet<MatchScore> scores = new TreeSet<MatchScore>();
		double mz;
		MatchScore score;

		for (Chromatogram currentChromatogram : underConstructionChromatograms) {
			double chromatogramMz = currentChromatogram.getLastMz();

			for (ConnectedMzPeak currentMzPeak : cMzPeaks) {

				mz = currentMzPeak.getMzPeak().getMZ();
				
				// Tune-up
				if (mz > (chromatogramMz + mzTolerance))
					break;
				if (mz < (chromatogramMz - mzTolerance))
					continue;

				double mzDifference = Math.abs(chromatogramMz - mz);

				if (mzDifference <= mzTolerance) {
					score = new MatchScore(currentChromatogram,
							currentMzPeak);
					if (score.getScore() < Double.MAX_VALUE) {
						scores.add(score);
					}
				}
			}
		}

		// Connect the best scoring pairs of under-construction and 1d peaks
		ConnectedMzPeak cMzPeak;
		Iterator<MatchScore> scoreIterator = scores.iterator();
		while (scoreIterator.hasNext()) {

			score = scoreIterator.next();

			// If ConnectedMzPeak is already connected, then move to next score
			cMzPeak = score.getMzPeak();
			if (cMzPeak.isConnected()) {
				continue;
			}

			// If Chromatogram is growing, then move on to next score
			Chromatogram currentChromatogram = score.getChromatogram();
			if (currentChromatogram.isGrowing()) {
				continue;
			}

			// Add MzPeak to the proper Chromatogram and set status connected
			currentChromatogram.addMzPeak(cMzPeak);
			cMzPeak.setConnected();
			cMzPeaks.remove(cMzPeak);

		}

		// Check if there are any under-construction peaks that were not
		// connected then finish the region of chromatogram
		Iterator<Chromatogram> iteratorConPeak = underConstructionChromatograms
				.iterator();
		while (iteratorConPeak.hasNext()) {

			Chromatogram currentChromatogram = iteratorConPeak.next();

			// If nothing was added,
			if (!currentChromatogram.isGrowing()) {

				// Closed region?
				if (currentChromatogram.isLastConnectedMzPeakZero())
					continue;

				// Check length of detected Chromatogram (filter according to
				// parameter)
				double chromatoLength = currentChromatogram
						.getLastConnectedMzPeaksRTRange().getSize();

				if (chromatoLength < minimumTimeSpan) {

					// Verify if the connected area (region) is the only present
					// in the
					// current chromatogram , if not just remove from current
					// chromatogram this region
					if (currentChromatogram.hasPreviousConnectedMzPeaks()) {

						currentChromatogram.removeLastConnectedMzPeaks();
						continue;

					} else {
						iteratorConPeak.remove();
						continue;
					}
				}

				// Close the region in this chromatogram
				SimpleDataPoint zeroDataPoint = new SimpleDataPoint(
						currentChromatogram.getLastMz(), 0);
				ConnectedMzPeak zeroChromatoPoint = new ConnectedMzPeak(scan,
						new SimpleMzPeak(zeroDataPoint));
				currentChromatogram.addMzPeak(zeroChromatoPoint);
				currentChromatogram.resetGrowingState();

			} else
				currentChromatogram.resetGrowingState();
		}

		// If there are some unconnected MzPeaks, then start a new
		// under-construction peak for each of them

		for (ConnectedMzPeak cmp : cMzPeaks) {
			if (!cmp.isConnected()) {
				underConstructionChromatograms.add(new Chromatogram(dataFile,
						cmp));
			}

		}

	}

	/**
	 * Return all chromatograms with possible peaks.
	 * 
	 */
	public Chromatogram[] finishChromatograms() {

		Iterator<Chromatogram> iteratorConPeak = underConstructionChromatograms
				.iterator();
		while (iteratorConPeak.hasNext()) {

			Chromatogram currentChromatogram = iteratorConPeak.next();

			// Check length of detected Chromatogram (filter according to
			// parameter)
			double chromatoLength = currentChromatogram
					.getLastConnectedMzPeaksRTRange().getSize();

			if (chromatoLength < minimumTimeSpan) {

				// Verify if the connected area is the only present in the
				// current chromatogram
				if (!currentChromatogram.hasPreviousConnectedMzPeaks())
					iteratorConPeak.remove();

			}
		}

		Chromatogram[] chromatograms = underConstructionChromatograms
				.toArray(new Chromatogram[0]);
		return chromatograms;
	}

}
