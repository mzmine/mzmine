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

package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.simplepeakbuilder;

import java.util.Arrays;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.modules.peakpicking.threestep.massdetection.MzPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.ConnectedPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.PeakBuilder;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.Chromatogram;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ConnectedMzPeak;

/**
 * This class implements a simple peak builder. This takes all detected MzPeaks
 * in one Scan and try to find a possible relationship between each one of these
 * with MzPeaks of the previous scan. This relationship is set by a match score
 * using MatchScore class, according with the parameters of Tolerance of MZ and
 * Intensity. Also it can apply a second search for possible peaks (threshold
 * level), over a already detected peak.
 * 
 */
public class SimplePeakBuilder implements PeakBuilder {

	/**
	 * 
	 * @param parameters
	 */
	public SimplePeakBuilder(SimplePeakBuilderParameters parameters) {

	}

	/**
	 * @see net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.PeakBuilder#addChromatogram(net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.Chromatogram, net.sf.mzmine.data.RawDataFile)
	 */
	public Peak[] addChromatogram(Chromatogram chromatogram,
			RawDataFile dataFile) {

		ConnectedMzPeak[] allConnectedMzPeaks = chromatogram.getConnectedMzPeaks();
		ConnectedPeak simplePeak = new ConnectedPeak(chromatogram.getDataFile(), allConnectedMzPeaks[0]);
		
		int[] scanNumbers = dataFile.getScanNumbers(1);
		int a = 0;
		Arrays.sort(scanNumbers);
		
		for (int i = 0; i < scanNumbers.length; i++) {
			//logger.finest(" Ciclo de i " + i);
			//ConnectedMzPeak mzValue = chromatogram
				//	.getConnectedMzPeak(scanNumbers[i]);
			//if (mzValue != null){
			if (allConnectedMzPeaks[a].getScan().getScanNumber() == scanNumbers[i]){
				simplePeak.addMzPeak(allConnectedMzPeaks[a]);
				a++;
				if (a> allConnectedMzPeaks.length -1 )
					a = allConnectedMzPeaks.length -1;
			}
			else{
				SimpleDataPoint zeroDataPoint = new SimpleDataPoint(
						chromatogram.getMZ(), 0);
				ConnectedMzPeak zeroChromatoPoint = new ConnectedMzPeak(
						dataFile.getScan(scanNumbers[i]), new MzPeak(zeroDataPoint));
				simplePeak.addMzPeak(zeroChromatoPoint);
			}
		}
		
		
		/*for(int i=1; i<allConnectedMzPeaks.length; i++){
			simplePeak.addMzPeak(allConnectedMzPeaks[i]);
		}*/
		
		Peak[] peaks = { simplePeak };
		
		return peaks;
		
	}

}
