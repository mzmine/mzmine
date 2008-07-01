package net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.simplechromatogram;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.modules.peakpicking.threestep.massdetection.MzPeak;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.Chromatogram;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ChromatogramBuilder;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ConnectedMzPeak;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.MatchScore;

public class SimpleChromatogramBuilder implements ChromatogramBuilder {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private float mzTolerance, minimumChromatogramDuration;
	private Vector<Chromatogram> underConstructionChromatograms;

	public SimpleChromatogramBuilder(
			SimpleChromatogramBuilderParameters parameters) {

		minimumChromatogramDuration = (Float) parameters
				.getParameterValue(SimpleChromatogramBuilderParameters.minimumChromatogramDuration);
		mzTolerance = (Float) parameters
				.getParameterValue(SimpleChromatogramBuilderParameters.mzTolerance);

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
		for (Chromatogram currentChromatogram : underConstructionChromatograms) {
			for (ConnectedMzPeak currentMzPeak : cMzPeaks) {
				MatchScore score = new MatchScore(currentChromatogram,
						currentMzPeak, mzTolerance);

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
			Chromatogram currentChromatogram = score.getChromatogram();
			if (currentChromatogram.isGrowing()) {
				continue;
			}

			// Add MzPeak to the proper Chromatogram and set status connected
			currentChromatogram.addMzPeak(cMzPeak);
			cMzPeak.setConnected();

		}

		// Check if there are any under-construction peaks that were not
		// connected (finished region)
		Iterator<Chromatogram> iteratorConPeak = underConstructionChromatograms
				.iterator();
		while (iteratorConPeak.hasNext()) {

			Chromatogram currentChromatogram = iteratorConPeak.next();

			// If nothing was added,
			if (!currentChromatogram.isGrowing()) {
				
				if (currentChromatogram.isLastConnectedMzPeakZero()) 
					continue;

				// Check length of detected Chromatogram (filter according to
				// parameter)
				float chromatoLength = currentChromatogram
						.getLastConnectedMzPeaksRTRange().getSize();

				if (chromatoLength < minimumChromatogramDuration) {

					// Verify if the connected area is the only present in the
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

					SimpleDataPoint zeroDataPoint = new SimpleDataPoint(
							currentChromatogram.getMZ(), 0);
					ConnectedMzPeak zeroChromatoPoint = new ConnectedMzPeak(
							scan, new MzPeak(zeroDataPoint));
					currentChromatogram.addMzPeak(zeroChromatoPoint);
					currentChromatogram.resetGrowingState();

			} else
				currentChromatogram.resetGrowingState();
		}

		// If there are some unconnected MzPeaks, then start a new
		// under-construction peak for each of them

		for (ConnectedMzPeak cMzPeak : cMzPeaks) {
			if (!cMzPeak.isConnected()) {
				Chromatogram newChromatogram = new Chromatogram(dataFile,
						cMzPeak);
				underConstructionChromatograms.add(newChromatogram);
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
			float chromatoLength = currentChromatogram
					.getLastConnectedMzPeaksRTRange().getSize();

			if (chromatoLength < minimumChromatogramDuration) {

				// Verify if the connected area is the only present in the
				// current chromatogram
				if (!currentChromatogram.hasPreviousConnectedMzPeaks())
					iteratorConPeak.remove();

			}
		}

		Chromatogram[] chromatograms = underConstructionChromatograms
				.toArray(new Chromatogram[0]);
		freeMemory();
		return chromatograms;
	}
	
	private void freeMemory() {
		System.gc();
	}

}
