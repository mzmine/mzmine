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

package net.sf.mzmine.modules.peakpicking.peakrecognition.chromatographicthreshold;

import java.util.Vector;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.modules.peakpicking.peakrecognition.PeakResolver;
import net.sf.mzmine.modules.peakpicking.peakrecognition.ResolvedPeak;
import net.sf.mzmine.util.MathUtils;

/**
 * This class implements a chromatographic threshold peak builder.
 * 
 */
public class ChromatographicThresholdPeakDetector implements PeakResolver {

	// private Logger logger = Logger.getLogger(this.getClass().getName());

	private double chromatographicThresholdLevel, minimumPeakHeight,
			minimumPeakDuration;

	public ChromatographicThresholdPeakDetector(
			ChromatographicThresholdPeakDetectorParameters parameters) {

		minimumPeakHeight = (Double) parameters
				.getParameterValue(ChromatographicThresholdPeakDetectorParameters.minimumPeakHeight);
		minimumPeakDuration = (Double) parameters
				.getParameterValue(ChromatographicThresholdPeakDetectorParameters.minimumPeakDuration);
		chromatographicThresholdLevel = (Double) parameters
				.getParameterValue(ChromatographicThresholdPeakDetectorParameters.chromatographicThresholdLevel);

	}

	/**
	 */
    public ChromatographicPeak[] resolvePeaks(ChromatographicPeak chromatogram,
            int scanNumbers[], double retentionTimes[], double intensities[]) {

        Vector<ResolvedPeak> resolvedPeaks = new Vector<ResolvedPeak>();

        /*        ConnectedMzPeak[] cMzPeaks = chromatogram.getConnectedMzPeaks();

		double recursiveThresholdlevelPeak;

		double[] chromatoIntensities = new double[scanNumbers.length];
		double sumIntensities = 0;

		for (int i = 0; i < scanNumbers.length; i++) {

			ConnectedMzPeak mzValue = chromatogram
					.getConnectedMzPeak(scanNumbers[i]);
			if (mzValue != null) {
				chromatoIntensities[i] = mzValue.getMzPeak().getIntensity();
			} else
				chromatoIntensities[i] = 0;
			sumIntensities += chromatoIntensities[i];
		}

		recursiveThresholdlevelPeak = MathUtils.calcQuantile(
				chromatoIntensities, chromatographicThresholdLevel);


		for (ConnectedMzPeak mzPeak : cMzPeaks) {

			if (mzPeak.getMzPeak().getIntensity() > recursiveThresholdlevelPeak) {
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
					resolvedPeaks.add(peak);
				}

			}

		}

		if (regionOfMzPeaks.size() != 0) {
			ConnectedPeak peak = new ConnectedPeak(dataFile, regionOfMzPeaks
					.get(0));
			for (int i = 0; i < regionOfMzPeaks.size(); i++) {
				peak.addMzPeak(regionOfMzPeaks.get(i));
			}
			double pLength = peak.getRawDataPointsRTRange().getSize();
			double pHeight = peak.getHeight();
			if ((pLength >= minimumPeakDuration)
					&& (pHeight >= minimumPeakHeight)) {
				resolvedPeaks.add(peak);
			}
		}*/

		return resolvedPeaks.toArray(new ChromatographicPeak[0]);
	}

}
