package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils;

import java.util.List;

/**
 * Class that stores important information about each RowGroup.
 * These are the m/z, intensity and RT values for a group of features.
 */
public class RowInfo {

    /**
     * List that stores m/z values in Double format.
     */
    private List<Double> mzList;
    /**
     * List that stores intensity values in Float format.
     */
    private List<Float> intensityList;
    /**
     * List that stores RT values in Float format.
     */
    private List<Float> rtList;

    /**
     * Creates a new RowInfo object with the specified info.
     * @param mz The m/z values for a group of features.
     * @param intensity The intensity values for a group of features.
     * @param rt The RT values for a group of features.
     */
    public RowInfo(List<Double> mz, List<Float> intensity, List<Float> rt) {
        this.mzList = mz;
        this.intensityList = intensity;
        this.rtList = rt;
    }

    /**
     * Gets a copy of all the mz values.
     * @return List of mz.
     */
    public List<Double> getMzList() {
        return mzList;
    }

    /**
     * Gets a copy of all the intensity values.
     * @return List of intensities.
     */
    public List<Float> getIntensityList() {
        return intensityList;
    }

    /**
     * Gets a copy of all the RT values.
     * @return List of RT.
     */
    public List<Float> getRtList() {
        return rtList;
    }
}
