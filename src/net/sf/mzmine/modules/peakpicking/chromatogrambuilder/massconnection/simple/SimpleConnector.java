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

package net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massconnection.simple;

import java.util.Arrays;
import java.util.TreeMap;

import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massconnection.Chromatogram;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massconnection.MassConnector;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.DataPointSorter;

public class SimpleConnector implements MassConnector {

	private double mzTolerance;

	// Mapping of last data point m/z --> chromatogram
	private TreeMap<Double, Chromatogram> buildingChromatograms;

	public SimpleConnector(SimpleConnectorParameters parameters) {

		mzTolerance = (Double) parameters
				.getParameterValue(SimpleConnectorParameters.mzTolerance);

		buildingChromatograms = new TreeMap<Double, Chromatogram>();
	}

	public void addScan(RawDataFile dataFile, int scanNumber, MzPeak[] mzValues) {

		// Sort m/z peaks by descending intensity
		Arrays.sort(mzValues, new DataPointSorter(false, false));

		// Create an empty set of updated chromatograms
		TreeMap<Double, Chromatogram> connectedChromatograms = new TreeMap<Double, Chromatogram>();

		for (MzPeak mzPeak : mzValues) {

			// Use binary search to find chromatograms within m/z tolerance
			double mzKeys[] = CollectionUtils
					.toDoubleArray(buildingChromatograms.keySet());
			int index = Arrays.binarySearch(mzKeys, mzPeak.getMZ()
					- mzTolerance);
			if (index < 0)
				index = (index + 1) * -1;
			Chromatogram bestChromatogram = null;
			double bestKey = -1;

			while (index < mzKeys.length) {

				if (mzKeys[index] > mzPeak.getMZ() + mzTolerance)
					break;

				Chromatogram chrom = buildingChromatograms.get(mzKeys[index]);
				if ((bestChromatogram == null)
						|| (chrom.getLastMzPeak().getIntensity() > bestChromatogram
								.getLastMzPeak().getIntensity())) {
					bestChromatogram = chrom;
					bestKey = mzKeys[index];
				}

				index++;

			}

			if (bestChromatogram != null) {
				buildingChromatograms.remove(bestKey);
			} else {
				bestChromatogram = new Chromatogram(dataFile);
			}

			bestChromatogram.addMzPeak(scanNumber, mzPeak);
			connectedChromatograms.put(mzPeak.getMZ(), bestChromatogram);

		}

		for (Chromatogram testChrom : buildingChromatograms.values()) {
			connectedChromatograms.put(testChrom.getMZ(), testChrom);
		}

		buildingChromatograms = connectedChromatograms;

	}

	public Chromatogram[] finishChromatograms() {
		Chromatogram[] chromatograms = buildingChromatograms.values().toArray(
				new Chromatogram[0]);
		return chromatograms;
	}

}
