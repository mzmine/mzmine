package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils;

public class DetectedAdduct {
    String adductName;
    double mzFeature;
    double intensity;
    double rt;

    public DetectedAdduct(String adductName, double mzFeature, double intensity, double rt) {
        this.adductName = adductName;
        this.mzFeature = mzFeature;
        this.intensity = intensity;
        this.rt = rt;
    }
}
