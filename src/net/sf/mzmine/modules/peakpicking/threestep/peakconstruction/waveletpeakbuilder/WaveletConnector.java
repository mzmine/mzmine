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

package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.waveletpeakbuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;

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
public class WaveletConnector implements PeakBuilder {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private float minimumPeakHeight, minimumPeakDuration, mzTolerance, intTolerance, amplitudeOfNoise;
	private Vector<ConnectedPeak> underConstructionPeaks;
	double[] W;

	public WaveletConnector(WaveletConnectorParameters parameters) {

		minimumPeakHeight = (Float) parameters
		.getParameterValue(WaveletConnectorParameters.minimumPeakHeight);
		minimumPeakDuration = (Float) parameters
		.getParameterValue(WaveletConnectorParameters.minimumPeakDuration);
		mzTolerance = (Float) parameters
				.getParameterValue(WaveletConnectorParameters.mzTolerance);
		intTolerance = (Float) parameters
		.getParameterValue(WaveletConnectorParameters.intTolerance);
		amplitudeOfNoise = (Float) parameters
				.getParameterValue(WaveletConnectorParameters.amplitudeOfNoise);

		underConstructionPeaks = new Vector<ConnectedPeak>();
		
		preCalculateCWT(1000);
		
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
			((WaveletPeak) ucPeak).addNewIntensity(cMzPeak.getMzPeak().getIntensity());
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
				((WaveletPeak) ucPeak).performCWT(W);

				// Apply second criteria to identify possible peaks.

				Peak[] wavePeaks = ((WaveletPeak) ucPeak).getWaveletPeaks();

				if (wavePeaks.length != 0) {
					for (Peak p : wavePeaks) {
						float ucLength = p.getRawDataPointsRTRange().getSize();
						float ucHeight = p.getHeight();

						if ((ucLength >= minimumPeakDuration)
								&& (ucHeight >= minimumPeakHeight)) 
							finishedPeaks.add(p);
					}
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
				WaveletPeak ucPeak = new WaveletPeak(dataFile, cMzPeak, amplitudeOfNoise);
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
			((WaveletPeak) ucPeak).performCWT(W);

			Peak[] wavePeaks = ((WaveletPeak) ucPeak).getWaveletPeaks();

			if (wavePeaks.length != 0) {
				for (Peak p : wavePeaks) {
					float ucLength = p.getRawDataPointsRTRange().getSize();
					float ucHeight = p.getHeight();

					if ((ucLength >= minimumPeakDuration)
							&& (ucHeight >= minimumPeakHeight)) 
						finishedPeaks.add(p);
				}
			}
		}
		return finishedPeaks.toArray(new Peak[0]);
	}

	
	/**
	 * This method calculates the coefficients of our wavelet (Mex Hat).
	 */
	public void preCalculateCWT(double points) {
		W = new double[(int) points];
		int WAVELET_ESL = -5;
		int WAVELET_ESR = 5;
		
		double wstep = ((WAVELET_ESR - WAVELET_ESL) / points);

		/* c = 2 / ( sqrt(3) * pi^(1/4) ) */
		double c = 0.8673250705840776;

		double waveletIndex = WAVELET_ESL, wavIndex2;

		for (int j = 0; j < points; j++) {
			wavIndex2 = waveletIndex * waveletIndex;
			W[j] = c * (1.0 - wavIndex2)
					* Math.exp(- wavIndex2 / 2);
			waveletIndex += wstep;
		}

	}
	
}
