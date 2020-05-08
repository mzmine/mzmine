package io.github.mzmine;

import io.github.mzmine.datamodel.*;

public interface MobilogramListRow {

    /**
     * Return raw data with Mobilograms on this row
     */
    public RawDataFile[] getRawDataFiles();

    /**
     * Returns ID of this row
     */
    public int getID();

    /**
     * Returns number of Mobilograms assigned to this row
     */
    public int getNumberOfMobilograms();

    /**
     * Return Mobilograms assigned to this row
     */
    public IMSFeature[] getMobilograms();

    /**
     * Returns Mobilogram for given raw data file
     */
    public IMSFeature getMobilogram(RawDataFile rawData);

    /**
     * Add a Mobilogram
     */
    public void addMobilogram(RawDataFile rawData, IMSFeature Mobilogram);

    /**
     * D Remove a Mobilogram
     */
    public void removeMobilogram(RawDataFile file);

    /**
     * Has a Mobilogram?
     */
    public boolean hasMobilogram(IMSFeature Mobilogram);

    /**
     * Has a Mobilogram?
     */
    public boolean hasMobilogram(RawDataFile rawData);

    /**
     * Returns average M/Z for Mobilograms on this row
     */
    public double getAverageMZ();

    /**
     * Returns average RT for Mobilograms on this row
     */
    public double getAverageRT();

    /**
     * Returns average mobility for Mobilograms on this row
     */
    public double getAverageMobility();

    /**
     * Returns average ccs for Mobilograms on this row
     */
    public double getAverageCcs();

    /**
     * Returns average height for Mobilograms on this row
     */
    public double getAverageHeight();

    /**
     * Returns the charge for Mobilogram on this row. If more charges are found 0 is returned
     */
    public int getRowCharge();

    /**
     * Returns average area for Mobilograms on this row
     */
    public double getAverageArea();

    /**
     * Returns comment for this row
     */
    public String getComment();

    /**
     * Sets comment for this row
     */
    public void setComment(String comment);

    /**
     * Sets average mz for this row
     */
    public void setAverageMZ(double mz);

    /**
     * Sets average rt for this row
     */
    public void setAverageRT(double rt);

    /**
     * Sets average mobility for this row
     */
    public void setAverageMobility(double mobility);

    /**
     * Add a new identity candidate (result of identification method)
     *
     * @param identity New Mobilogram identity
     * @param preffered boolean value to define this identity as preferred identity
     */
    public void addMobilogramIdentity(MobilogramIdentity identity, boolean preffered);

    /**
     * Remove identity candidate
     *
     * @param identity Mobilogram identity
     */
    public void removeMobilogramIdentity(MobilogramIdentity identity);

    /**
     * Returns all candidates for this Mobilogram's identity
     *
     * @return Identity candidates
     */
    public MobilogramIdentity[] getMobilogramIdentities();

    /**
     * Returns preferred Mobilogram identity among candidates
     *
     * @return Preferred identity
     */
    public MobilogramIdentity getPreferredMobilogramIdentity();

    /**
     * Sets a preferred Mobilogram identity among candidates
     *
     * @param identity Preferred identity
     */
    public void setPreferredMobilogramIdentity(MobilogramIdentity identity);


    /**
     * Adds a new MobilogramInformation object.
     *
     * MobilogramInformation is used to keep extra information about Mobilograms in the form of a map
     * <propertyName, propertyValue>
     *
     * @param information object
     */

    public void setMobilogramInformation(MobilogramInformation information);


    /**
     * Returns MobilogramInformation
     *
     * @return
     */

    public MobilogramInformation getMobilogramInformation();

    /**
     * Returns maximum raw data point intensity among all Mobilograms in this row
     *
     * @return Maximum intensity
     */
    public double getDataPointMaxIntensity();

    /**
     * Returns the most intense Mobilogram in this row
     */
    public IMSFeature getBestMobilogram();

    /**
     * Returns the most intense fragmentation scan in this row
     */
    public Scan getBestFragmentation();

    /**
     * Returns the most intense isotope pattern in this row. If there are no isotope patterns present
     * in the row, returns null.
     */
    public IsotopePattern getBestIsotopePattern();

    // DorresteinLaB edit
    /**
     * reset the rowID
     */
    public void setID(int id);

    // End DorresteinLab edit

}
