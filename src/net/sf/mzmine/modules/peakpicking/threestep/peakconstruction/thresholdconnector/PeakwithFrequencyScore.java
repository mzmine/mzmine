/* Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.thresholdconnector;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.ConnectedMzPeak;
import net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.ConnectedPeak;

public class PeakwithFrequencyScore extends ConnectedPeak{

	private TreeMap<Integer, Integer> binsFrequency;
	private float amplitudeOfNoise, maxIntensity;

	public PeakwithFrequencyScore(RawDataFile dataFile, ConnectedMzPeak mzValue, float amplitudeOfNoise ) {
		super(dataFile, mzValue);
		
		maxIntensity = mzValue.getMzPeak().getIntensity();
		
		binsFrequency = new TreeMap<Integer, Integer>();
		
		this.amplitudeOfNoise = amplitudeOfNoise;
		
		int numberOfBin = (int) Math.ceil(mzValue.getMzPeak().getIntensity() / amplitudeOfNoise);
		
		binsFrequency.put(numberOfBin, 1);		
	}
	public void addNewIntensity(float intensity) {
		int frequencyValue = 1;
		int numberOfBin;
		if (intensity < amplitudeOfNoise)
			numberOfBin = 1;
		else
			numberOfBin = (int) Math.floor(intensity / amplitudeOfNoise) ;
		
		
		if (binsFrequency.containsKey(numberOfBin)) {
			frequencyValue = binsFrequency.get(numberOfBin);
			frequencyValue++;
		}
		binsFrequency.put(numberOfBin, frequencyValue);	
		
		if (intensity > maxIntensity)
			maxIntensity = intensity;
	}
	
	public float getNoiseThreshold(){

		int numberOfBin = 0;
		int maxFrequency = 0;
		
		Set<Integer> c = binsFrequency.keySet();
		Iterator<Integer> iteratorBin = c.iterator();

		while (iteratorBin.hasNext()){
			int bin = iteratorBin.next();
			int freq = binsFrequency.get(bin);
			
			if (freq > maxFrequency){
				maxFrequency = freq;
				numberOfBin = bin;
			}
		}
		
		
		float noiseThreshold = (numberOfBin + 1 ) * amplitudeOfNoise;
		float percentage = noiseThreshold/maxIntensity;
		if (percentage > 0.3)
			noiseThreshold = amplitudeOfNoise;
		
		return noiseThreshold;
	}

}
