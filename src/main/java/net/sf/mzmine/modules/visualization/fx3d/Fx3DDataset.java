package net.sf.mzmine.modules.visualization.fx3d;

import com.google.common.collect.Range;
import com.sun.javafx.scene.control.Properties;

public class Fx3DDataset extends Properties {

    private float[][] intensityValues;
    private int rtResolution;
    private int mzResolution;
    private double maxBinnedIntensity;
    private Range<Double> rtRange, mzRange;

    public Fx3DDataset(float[][] intensityValues, int rtResolution,
            int mzResolution, double maxBinnedIntensity, Range<Double> rtRange,
            Range<Double> mzRange) {
        this.intensityValues = intensityValues;
        this.rtResolution = rtResolution;
        this.mzResolution = mzResolution;
        this.maxBinnedIntensity = maxBinnedIntensity;
        this.rtRange = rtRange;
        this.mzRange = mzRange;
    }

    float[][] getIntensityValues() {
        return intensityValues;
    }

    int getRtResolution() {
        return rtResolution;
    }

    int getMzResolution() {
        return mzResolution;
    }

    double getMaxBinnedIntensity() {
        return maxBinnedIntensity;
    }

    Range<Double> getRtRange() {
        return rtRange;
    }

    Range<Double> getMzRange() {
        return mzRange;
    }
}
