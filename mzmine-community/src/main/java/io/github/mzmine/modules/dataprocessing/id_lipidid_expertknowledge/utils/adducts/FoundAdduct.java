package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts;

import java.util.Objects;

/**
 * Class that represents the adducts and ISF found in my features.
 */
public class FoundAdduct {
    /**
     * Complete name of the adduct (eg: "[M+H]+").
     */
    private String adductName;
    /**
     * m/z value of the feature the adduct is found in.
     */
    private double mzFeature;
    /**
     * Intensity value of the feature the adduct is found in.
     */
    private double intensity;
    /**
     * RT of the feature the adduct is found at.
     */
    private double rt;
    /**
     * Charge of the adduct.
     * Positive = +1, Negative = -1.
     */
    private int charge;

    /**
     * Creates a new FoundAdduct object with the specified info.
     * @param adductName The name of the found adduct.
     * @param mzFeature The m/z value of the feature.
     * @param intensity The intensity value of the feature.
     * @param rt The RT the feature is at.
     * @param charge The charge of the found adduct.
     */
    public FoundAdduct(String adductName, double mzFeature, double intensity, double rt, int charge) {
        this.adductName = adductName;
        this.mzFeature = mzFeature;
        this.intensity = intensity;
        this.rt = rt;
        this.charge = charge;
    }

    /**
     * Gets the complete adduct name.
     * @return The adduct name.
     */
    public String getAdductName() {
        return adductName;
    }

    /**
     * Gets the m/z value.
     * @return The m/z value.
     */

    public double getMzFeature() {
        return mzFeature;
    }

    /**
     * Gets the intensity value.
     * @return The intensity value.
     */
    public double getIntensity() {
        return intensity;
    }

    /**
     * Gets the RT.
     * @return The RT.
     */
    public double getRt() {
        return rt;
    }

    /**
     * Gets the charge.
     * @return The charge.
     */
    public int getCharge() {return charge;}

    /**
     * Indicates whether an object is equal to this one.
     * @param o Reference object we want to compare with.
     * @return True = it IS equal, False = it iS NOT equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FoundAdduct adduct = (FoundAdduct) o;
        return Double.compare(adduct.mzFeature, mzFeature) == 0 && Double.compare(adduct.intensity, intensity) == 0 && Double.compare(adduct.rt, rt) == 0 && charge == adduct.charge && Objects.equals(adductName, adduct.adductName);
    }

    /**
     * Creates a hash code value fot his object.
     * @return Hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(adductName, mzFeature, intensity, rt, charge);
    }

    /**
     * String representation of the object.
     * It only includes the adduct name.
     * @return String with the name of the adduct.
     */
    @Override
    public String toString() {
        return "" + adductName +
                ';';
    }
}
