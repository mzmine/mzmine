package net.sf.mzmine.modules.visualization.new3d;

import com.google.common.collect.Range;

public class New3DDataset {
	
	private float[][] intensityValues;
	private int rtResolution;
	private int mzResolution;
	private double maxBinnedIntensity;
	private Range<Double> rtRange, mzRange;
	
	public New3DDataset(float[][] intensityValues, int rtResolution, int mzResolution,int maxBinnedIntensity,Range<Double> rtRange,Range<Double> mzRange){
		this.intensityValues = intensityValues;
		this.rtResolution = rtResolution;
		this.mzResolution = mzResolution;
		this.maxBinnedIntensity = maxBinnedIntensity;
		this.rtRange = rtRange;
		this.mzRange = mzRange;
	}
	
	float[][] getIntensityValues(){
		return intensityValues;
	}
	
	int getRtResolution(){
		return rtResolution;
	}
	
	int getMzResolution(){
		return mzResolution;
	}
	
	double getMaxBinnedIntensity(){
		return maxBinnedIntensity;
	}
	
	Range<Double> getRtRange(){
		return rtRange;
	}

	Range<Double> getMzRange(){
		return mzRange;
	}
}
