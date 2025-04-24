package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that stores important information about each RowGroup
 */
public class RowInfo {

    private List<Double> mzList;
    private List<Float> intensityList;
    private List<Float> rtList;

    public RowInfo(List<Double> mz, List<Float> intensity, List<Float> rt) {
        this.mzList = mz;
        this.intensityList = intensity;
        this.rtList = rt;
    }

    /**
     * Gets a copy of all the mz values
     * @return List of mz
     */
    public List<Double> getMzList() {
        return mzList;
    }

    /**
     * Gets a copy of all the intensity values
     * @return List of intensities
     */
    public List<Float> getIntensityList() {
        return intensityList;
    }

    /**
     * Gets a copy of all the RT values
     * @return List of RT
     */
    public List<Float> getRtList() {
        return rtList;
    }
}
