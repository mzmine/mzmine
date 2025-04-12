package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts;

import java.util.Objects;

/**
 * Class that represents the adducts and ISF found in my features
 */
public class FoundAdduct {
    private String adductName;
    private double mzFeature;
    private double intensity;
    private double rt;
    private int charge;

    public FoundAdduct(String adductName, double mzFeature, double intensity, double rt, int charge) {
        this.adductName = adductName;
        this.mzFeature = mzFeature;
        this.intensity = intensity;
        this.rt = rt;
        this.charge = charge;
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
    public int getCharge() {return charge;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FoundAdduct adduct = (FoundAdduct) o;
        return Double.compare(adduct.mzFeature, mzFeature) == 0 && Double.compare(adduct.intensity, intensity) == 0 && Double.compare(adduct.rt, rt) == 0 && charge == adduct.charge && Objects.equals(adductName, adduct.adductName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adductName, mzFeature, intensity, rt, charge);
    }
}
