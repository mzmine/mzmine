package net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.simplechromatogram;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.modules.peakpicking.threestep.massdetection.MzPeak;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.Chromatogram;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ChromatogramBuilder;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ConnectedMzPeak;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.MatchScore;

public class SimpleChromatogramBuilder implements ChromatogramBuilder {

	private float intTolerance, mzTolerance;
	private float minimumChromatogramHeight, minimumChromatogramDuration;
	private Vector<Chromatogram> underConstructionChromatograms;

	public SimpleChromatogramBuilder(
			SimpleChromatogramBuilderParameters parameters) {

		minimumChromatogramHeight = (Float) parameters
				.getParameterValue(SimpleChromatogramBuilderParameters.minimumChromatogramHeight);
		minimumChromatogramDuration = (Float) parameters
				.getParameterValue(SimpleChromatogramBuilderParameters.minimumChromatogramDuration);
		mzTolerance = (Float) parameters
				.getParameterValue(SimpleChromatogramBuilderParameters.mzTolerance);
		intTolerance = (Float) parameters
				.getParameterValue(SimpleChromatogramBuilderParameters.intTolerance);

		underConstructionChromatograms = new Vector<Chromatogram>();
	}

	public void addScan(Scan scan, MzPeak[] mzValues) {

		// Convert MzPeak in ConnectedMzPeak to deal with status property
		// (boolean connected)
		Vector<ConnectedMzPeak> cMzPeaks = new Vector<ConnectedMzPeak>();
		for (MzPeak mzPeak : mzValues)
			cMzPeaks.add(new ConnectedMzPeak(scan, mzPeak));

		// Calculate scores between Chromatogram and MzPeaks
		TreeSet<MatchScore> scores = new TreeSet<MatchScore>();
		for (Chromatogram currentChromatogram : underConstructionChromatograms) {
			for (ConnectedMzPeak currentMzPeak : cMzPeaks) {
				MatchScore score = new MatchScore(currentChromatogram,
						currentMzPeak, mzTolerance, intTolerance);

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

			// If Chromatogram is growing, then move on to next score
			Chromatogram currentChromatogram = score.getPeak();
			if (currentChromatogram.isGrowing()) {
				continue;
			}

			// Add MzPeak to the proper Chromatogram and set status connected
			// (filter according to parameter)
			if (cMzPeak.getMzPeak().getIntensity() > minimumChromatogramHeight) {
				currentChromatogram.addMzPeak(cMzPeak);
				cMzPeak.setConnected();
			}
		}

		// Check if there are any under-construction peaks that were not
		// connected (finished region)
		Iterator<Chromatogram> iteratorConPeak = underConstructionChromatograms
				.iterator();
		while (iteratorConPeak.hasNext()) {

			Chromatogram currentChromatogram = iteratorConPeak.next();

			// If nothing was added,
			if (!currentChromatogram.isGrowing()) {

				// Check length of detected Chromatogram (filter according to
				// parameter)
				float chromatoLength = currentChromatogram
						.getLastConnectedMzPeaksRTRange().getSize();

				if (chromatoLength < minimumChromatogramDuration) {

					// Verify if the connected area is the only present in the
					// current chromatogram , if not just remove from current
					// chromatogram this region
					if (currentChromatogram.hasPreviousConnectedMzPeaks())
						currentChromatogram.removeLastConnectedMzPeaks();
					else
						iteratorConPeak.remove();
				}

				if (!currentChromatogram.isLastConnectedMzPeakZero()) {

					// Set separator between regions in a chromatogram.
					SimpleDataPoint zeroDataPoint = new SimpleDataPoint(
							currentChromatogram.getMZ(), 0);
					ConnectedMzPeak zeroChromatoPoint = new ConnectedMzPeak(
							scan, new MzPeak(zeroDataPoint));
					currentChromatogram.addMzPeak(zeroChromatoPoint);
				}

			} else
				currentChromatogram.resetGrowingState();
		}

		// If there are some unconnected MzPeaks, then start a new
		// under-construction peak for each of them

		for (ConnectedMzPeak cMzPeak : cMzPeaks) {
			if (!cMzPeak.isConnected()) {
				
				// (filter according to parameter)
				if (cMzPeak.getMzPeak().getIntensity() > minimumChromatogramHeight) {
					Chromatogram newChromatogram = new Chromatogram(cMzPeak,
							minimumChromatogramHeight);
					underConstructionChromatograms.add(newChromatogram);
				}
			}

		}

	}

	/**
	 * Return all chromatograms with possible peaks.
	 * 
	 */
	public Chromatogram[] finishChromatograms() {
		return underConstructionChromatograms.toArray(new Chromatogram[0]);
	}

}
