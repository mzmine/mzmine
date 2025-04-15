package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils;

import java.util.ArrayList;
import java.util.List;

public class RowInfo {

    private List<Double> mzList;
    private List<Float> intensityList;
    private List<Float> rtList;

    public RowInfo(List<Double> mz, List<Float> intensity, List<Float> rt) {
        this.mzList = mz;
        this.intensityList = intensity;
        this.rtList = rt;
    }

    public List<Double> getMzList() {
        return mzList;
    }

    public List<Float> getIntensityList() {
        return intensityList;
    }

    public List<Float> getRtList() {
        return rtList;
    }
}
