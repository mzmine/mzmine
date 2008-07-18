package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.peakfillingmodels;

import net.sf.mzmine.data.ChromatographicPeak;

public interface PeakFillingModel {
	
	/**
	 * This method try to fill the shape of a detected chromatographic peak
	 * This returns an approximated shape of the peak using a mathematical function
	 * (Gaussian, BiGaussian, EMG, etc.)  
	 * 
	 * @param originalDetectedShape
	 * @return fillingPeak
	 */
	public ChromatographicPeak fillingPeak(ChromatographicPeak originalDetectedShape, float noiseAmplitude);

}
