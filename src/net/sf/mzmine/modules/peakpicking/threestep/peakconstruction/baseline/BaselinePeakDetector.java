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

package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.baseline;

import java.util.Vector;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.ConnectedPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.PeakBuilder;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.Chromatogram;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ConnectedMzPeak;

/**
 * This class implements a simple peak builder. This takes all collected MzPeaks
 * in one chromatogram and try to find all possible peaks. This detection
 * follows the concept of baseline in a chromatogram to set a peak (threshold
 * level).
 * 
 */
public class BaselinePeakDetector implements PeakBuilder {

	// private Logger logger = Logger.getLogger(this.getClass().getName());

	private double minimumPeakHeight, minimumPeakDuration, baselineLevel;

	public BaselinePeakDetector(BaselinePeakDetectorParameters parameters) {
		minimumPeakHeight = (Double) parameters
				.getParameterValue(BaselinePeakDetectorParameters.minimumPeakHeight);
		minimumPeakDuration = (Double) parameters
				.getParameterValue(BaselinePeakDetectorParameters.minimumPeakDuration);
		baselineLevel = (Double) parameters
				.getParameterValue(BaselinePeakDetectorParameters.baselineLevel);
	}

	/**
	 * @see net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.PeakBuilder#addChromatogram(net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.Chromatogram,
	 *      net.sf.mzmine.data.RawDataFile)
	 */
	public ChromatographicPeak[] addChromatogram(Chromatogram chromatogram,
			RawDataFile dataFile) {

		ConnectedMzPeak[] cMzPeaks = chromatogram.getConnectedMzPeaks();

		// logger.finest("Number of cMzPeaks " + cMzPeaks.length);

		Vector<ConnectedMzPeak> regionOfMzPeaks = new Vector<ConnectedMzPeak>();
		Vector<ConnectedPeak> underDetectionPeaks = new Vector<ConnectedPeak>();

		if (cMzPeaks.length > 0) {

			for (ConnectedMzPeak mzPeak : cMzPeaks) {

				if (mzPeak.getMzPeak().getIntensity() > baselineLevel) {
					regionOfMzPeaks.add(mzPeak);
				} else if (regionOfMzPeaks.size() != 0) {
					ConnectedPeak peak = new ConnectedPeak(dataFile,
							regionOfMzPeaks.get(0));
					for (int i = 0; i < regionOfMzPeaks.size(); i++) {
						peak.addMzPeak(regionOfMzPeaks.get(i));
					}
					regionOfMzPeaks.clear();

					double pLength = peak.getRawDataPointsRTRange().getSize();
					double pHeight = peak.getHeight();
					if ((pLength >= minimumPeakDuration)
							&& (pHeight >= minimumPeakHeight)) {
						underDetectionPeaks.add(peak);
					}

				}

			}

			if (regionOfMzPeaks.size() != 0) {
				ConnectedPeak peak = new ConnectedPeak(dataFile,
						regionOfMzPeaks.get(0));
				for (int i = 0; i < regionOfMzPeaks.size(); i++) {
					peak.addMzPeak(regionOfMzPeaks.get(i));
				}

				double pLength = peak.getRawDataPointsRTRange().getSize();
				double pHeight = peak.getHeight();
				if ((pLength >= minimumPeakDuration)
						&& (pHeight >= minimumPeakHeight)) {
					underDetectionPeaks.add(peak);
				}

			}

		}

		// logger.finest(" Numero de picos " + underDetectionPeaks.size());
		return underDetectionPeaks.toArray(new ChromatographicPeak[0]);
	}

}
