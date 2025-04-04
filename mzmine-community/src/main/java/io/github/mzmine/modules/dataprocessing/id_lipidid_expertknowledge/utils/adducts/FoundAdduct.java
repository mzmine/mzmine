package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts;

public class FoundAdduct {
    private String adductName;
    private double mzFeature;
    private double intensity;
    private double rt;

    public FoundAdduct(String adductName, double mzFeature, double intensity, double rt) {
        this.adductName = adductName;
        this.mzFeature = mzFeature;
        this.intensity = intensity;
        this.rt = rt;
    }

    public String getAdductName() {
        return adductName;
    }

    public double getMzFeature() {
        return mzFeature;
    }

    public double getIntensity() {
        return intensity;
    }

    public double getRt() {
        return rt;
    }
}
