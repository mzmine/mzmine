package io.github.mzmine.datamodel;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.impl.SimpleMobilogramInformation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IMSFeature {
    public enum IMSFeatureStatus {

        /**
         * Mobilogram was not found
         */
        UNKNOWN,

        /**
         * Mobilogram was found in primary Mobilogram picking
         */
        DETECTED,

        /**
         * Mobilogram was estimated in secondary Mobilogram picking
         */
        ESTIMATED,

        /**
         * Mobilogram was defined manually
         */
        MANUAL

    }

    /**
     * This method returns the status of the Mobilogram
     */
    public @Nonnull IMSFeatureStatus getIMSFeatureStatus();

    /**
     * This method returns raw M/Z value of the Mobilogram
     */
    public double getMZ();

    /**
     * This method returns raw retention time of the Mobilogram in minutes
     */
    public double getRT();

    /**
     * This method returns raw mobility of the Mobilogram
     */
    public double getMobility();

    /**
     * This method returns ccs of the Mobilogram
     */
    public double getCcs();

    /**
     * This method returns the raw height of the Mobilogram
     */
    public double getHeight();

    /**
     * This method returns the raw area of the Mobilogram
     */
    public double getArea();

    /**
     * Returns raw data file where this Mobilogram is present
     */
    public @Nonnull RawDataFile getDataFile();

    /**
     * This method returns numbers of scans that contain this Mobilogram
     */
    public @Nonnull int[] getScanNumbers();

    /**
     * This method returns number of most representative scan of this Mobilogram
     */
    public int getRepresentativeScanNumber();

    /**
     * This method returns m/z and intensity of this Mobilogram in a given scan. This m/z and
     * intensity does not need to match any actual raw data point. May return null, if there is no
     * data point in given scan.
     */
    public @Nullable IMSDataPoint getIMSDataPoint(int scanNumber);

    /**
     * Returns the retention time range of all raw data points used to detect this Mobilogram
     */
    public @Nonnull Range<Double> getRawIMSDataPointsRTRange();

    /**
     * Returns the range of m/z values of all raw data points used to detect this Mobilogram
     */
    public @Nonnull Range<Double> getRawIMSDataPointsMZRange();

    /**
     * Returns the range of intensity values of all raw data points used to detect this Mobilogram
     */
    public @Nonnull Range<Double> getRawIMSDataPointsIntensityRange();

    /**
     * Returns the number of scan that represents the fragmentation of this Mobilogram in MS2 level.
     */
    public int getMostIntenseFragmentScanNumber();

    /**
     * Returns all scan numbers that represent fragmentations of this peak in MS2 level.
     */
    public int[] getAllMS2FragmentScanNumbers();

    /**
     * Returns the isotope pattern of this Mobilogram or null if no pattern is attached
     */
    public @Nullable IsotopePattern getIsotopePattern();

    /**
     * Sets the isotope pattern of this Mobilogram
     */
    public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern);

    /**
     * Returns the charge of this ion. If the charge is unknown, returns 0.
     */
    public int getCharge();

    /**
     * Sets the charge of this ion
     */
    public void setCharge(int charge);

    /**
     * This method returns the full width at half maximum (FWHM) of the Mobilogram
     */
    public Double getFWHM();

    /**
     * This method returns the tailing factor of the Mobilogram
     */
    public Double getTailingFactor();

    /**
     * This method returns the asymmetry factor of the Mobilogram
     */
    public Double getAsymmetryFactor();

    /**
     * Sets the full width at half maximum (FWHM)
     */
    public void setFWHM(Double fwhm);

    /**
     * Sets the tailing factor
     */
    public void setTailingFactor(Double tf);

    /**
     * Sets the asymmetry factor
     */
    public void setAsymmetryFactor(Double af);

    // dulab Edit
    public void outputChromToFile();
    public void setMobilogramInformation(SimpleMobilogramInformation MobilogramInfoIn);

    public SimpleMobilogramInformation getMobilogramInformation();

}
