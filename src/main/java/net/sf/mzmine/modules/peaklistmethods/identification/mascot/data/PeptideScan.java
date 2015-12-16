/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.mascot.data;

import java.util.Arrays;
import java.util.TreeSet;
import java.util.Vector;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.util.PeptideSorter;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.SortingDirection;

import com.google.common.collect.Range;
import com.google.common.primitives.Ints;

public class PeptideScan implements Scan {

    private PeptideIdentityDataFile dataFile;
    private RawDataFile rawDataFile;
    private int rawScanNumber;
    private Vector<Peptide> peptides;
    private Peptide[] alterPeptides;
    private int queryNumber;
    private int msLevel;
    private int parentScan;
    private int fragmentScans[];
    private DataPoint dataPoints[];
    private double precursorMZ;
    private int precursorCharge;
    private double retentionTime;
    private Range<Double> mzRange;
    private DataPoint basePeak;
    private double totalIonCurrent;

    /**
     * This class represents the scan (collection of DataPoints) with MS level 2
     * or more, which contains peaks with masses equal to the calculated
     * fragment ion's masses for one or many peptides.
     */
    public PeptideScan(PeptideIdentityDataFile dataFile, String rawDataFile,
	    int queryNumber, int rawScanNumber) {

	this.dataFile = dataFile;
	this.peptides = new Vector<Peptide>();
	this.queryNumber = queryNumber;
	this.rawScanNumber = rawScanNumber;

    }

    /**
     * Sets the original raw data file
     * 
     * @param rawDataFile
     */
    public void setRawDataFile(RawDataFile rawDataFile) {
	this.rawDataFile = rawDataFile;
    }

    /**
     * @return Returns scan datapoints
     */
    public @Nonnull DataPoint[] getDataPoints() {
	return dataPoints;
    }

    /**
     * @return Returns scan datapoints within a given range
     */
    public @Nonnull DataPoint[] getDataPointsByMass(
	    @Nonnull Range<Double> mzRange) {

	int startIndex, endIndex;
	for (startIndex = 0; startIndex < dataPoints.length; startIndex++) {
	    if (dataPoints[startIndex].getMZ() >= mzRange.lowerEndpoint())
		break;
	}

	for (endIndex = startIndex; endIndex < dataPoints.length; endIndex++) {
	    if (dataPoints[endIndex].getMZ() > mzRange.upperEndpoint())
		break;
	}

	DataPoint pointsWithinRange[] = new DataPoint[endIndex - startIndex];

	// Copy the relevant points
	System.arraycopy(dataPoints, startIndex, pointsWithinRange, 0, endIndex
		- startIndex);

	return pointsWithinRange;
    }

    /**
     * @return Returns scan datapoints over certain intensity
     */
    public @Nonnull DataPoint[] getDataPointsOverIntensity(double intensity) {
	int index;
	Vector<DataPoint> points = new Vector<DataPoint>();

	for (index = 0; index < dataPoints.length; index++) {
	    if (dataPoints[index].getIntensity() >= intensity)
		points.add(dataPoints[index]);
	}

	DataPoint pointsOverIntensity[] = points.toArray(new DataPoint[0]);

	return pointsOverIntensity;
    }

    /**
     * @param mzValues
     *            m/z values to set
     * @param intensityValues
     *            Intensity values to set
     */
    public void setDataPoints(DataPoint[] dataPoints) {

	this.dataPoints = dataPoints;
	mzRange = Range.singleton(0.0);
	basePeak = null;
	totalIonCurrent = 0;

	// find m/z range and base peak
	if (dataPoints.length > 0) {

	    basePeak = dataPoints[0];
	    mzRange = Range.singleton(dataPoints[0].getMZ());

	    for (DataPoint dp : dataPoints) {

		if (dp.getIntensity() > basePeak.getIntensity())
		    basePeak = dp;

		mzRange = mzRange.span(Range.singleton(dp.getMZ()));
		totalIonCurrent += dp.getIntensity();

	    }

	}

    }

    /**
     * @see net.sf.mzmine.datamodel.Scan#getNumberOfDataPoints()
     */
    public int getNumberOfDataPoints() {
	return dataPoints.length;
    }

    /**
     * @see net.sf.mzmine.datamodel.Scan#getScanNumber()
     */
    public int getScanNumber() {
	return rawScanNumber;
    }

    /**
     * @param scanNumber
     *            The scanNumber to set.
     */
    public void setScanNumber(int scanNumber) {
	this.rawScanNumber = scanNumber;
    }

    /**
     * @see net.sf.mzmine.datamodel.Scan#getMSLevel()
     */
    public int getMSLevel() {
	return msLevel;
    }

    /**
     * @param msLevel
     *            The msLevel to set.
     */
    public void setMSLevel(int msLevel) {
	this.msLevel = msLevel;
    }

    /**
     * @see net.sf.mzmine.datamodel.Scan#getPrecursorMZ()
     */
    public double getPrecursorMZ() {
	return precursorMZ;
    }

    /**
     * @param precursorMZ
     *            The precursorMZ to set.
     */
    public void setPrecursorMZ(double precursorMZ) {
	this.precursorMZ = precursorMZ;
    }

    /**
     * @return Returns the precursorCharge.
     */
    public int getPrecursorCharge() {
	return precursorCharge;
    }

    /**
     * @param precursorCharge
     *            The precursorCharge to set.
     */
    public void setPrecursorCharge(int precursorCharge) {
	this.precursorCharge = precursorCharge;
    }

    /**
     * @see net.sf.mzmine.datamodel.Scan#getScanAcquisitionTime()
     */
    public double getRetentionTime() {
	return retentionTime;
    }

    /**
     * @param retentionTime
     *            The retentionTime to set.
     */
    public void setRetentionTime(double retentionTime) {
	this.retentionTime = retentionTime;
    }

    /**
     * @see net.sf.mzmine.datamodel.Scan#getMZRangeMax()
     */
    public @Nonnull Range<Double> getDataPointMZRange() {
	return mzRange;
    }

    /**
     * @see net.sf.mzmine.datamodel.Scan#getBasePeakMZ()
     */
    public DataPoint getHighestDataPoint() {
	return basePeak;
    }

    /**
     * @see net.sf.mzmine.datamodel.Scan#getParentScanNumber()
     */
    public int getParentScanNumber() {
	return parentScan;
    }

    /**
     * @param parentScan
     *            The parentScan to set.
     */
    public void setParentScanNumber(int parentScan) {
	this.parentScan = parentScan;
    }

    /**
     * @see net.sf.mzmine.datamodel.Scan#getFragmentScanNumbers()
     */
    public int[] getFragmentScanNumbers() {
	return fragmentScans;
    }

    /**
     * @param fragmentScans
     *            The fragmentScans to set.
     */
    public void setFragmentScanNumbers(int[] fragmentScans) {
	this.fragmentScans = fragmentScans;
    }

    /**
     * Adds a fragment scan
     * 
     * @param fragmentScan
     */
    public void addFragmentScan(int fragmentScan) {
	TreeSet<Integer> fragmentsSet = new TreeSet<Integer>();
	if (fragmentScans != null) {
	    for (int frag : fragmentScans)
		fragmentsSet.add(frag);
	}
	fragmentsSet.add(fragmentScan);
	fragmentScans = Ints.toArray(fragmentsSet);
    }

    /**
     * @see net.sf.mzmine.datamodel.Scan#getSpectrumType()
     */
    public MassSpectrumType getSpectrumType() {
	return MassSpectrumType.CENTROIDED;
    }

    /**
     * Returns the total ion current
     */
    public double getTIC() {
	return totalIonCurrent;
    }

    public String getName() {
	return ScanUtils.scanToString(this);
    }

    /**
     * Returns the raw data file that this scan belongs.
     */
    public @Nonnull RawDataFile getDataFile() {
	return rawDataFile;
    }

    /**
     * Returns the PeptideDataFile from where the information of the peptide was
     * extracted.
     * 
     * @return PeptideIdentityDataFile
     */
    public PeptideIdentityDataFile getPeptideDataFile() {
	return dataFile;
    }

    /**
     * Adds a peptide. This scan can be related to different peptides.
     * 
     * @param peptide
     */
    public void addPeptide(Peptide peptide) {
	peptides.add(peptide);
    }

    /**
     * Returns all the peptides that fix into this scan (masses)
     * 
     * @return Peptide[]
     */
    public Peptide[] getPeptides() {
	return peptides.toArray(new Peptide[0]);
    }

    /**
     * 
     * Returns the most probable peptide identity of this scan (collection of
     * data points).
     * 
     * @return Peptide
     */
    public Peptide getHighScorePeptide() {
	// Sort m/z peaks by descending intensity
	Peptide[] sortedPeptides = peptides.toArray(new Peptide[0]);
	Arrays.sort(sortedPeptides, new PeptideSorter(
		SortingDirection.Descending));
	return sortedPeptides[0];
    }

    /**
     * Returns the number of query associated to this scan (number from
     * identification file).
     * 
     * @return queryNumber
     */
    public int getQueryNumber() {
	return queryNumber;
    }

    public void setAlterPeptides(Peptide[] alterPeptides) {
	this.alterPeptides = alterPeptides;

    }

    public Peptide[] getAlterPeptides() {
	return alterPeptides;

    }

    @Override
    public @Nonnull MassList[] getMassLists() {
	return new MassList[0];
    }

    @Override
    public MassList getMassList(@Nonnull String name) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public void addMassList(@Nonnull MassList massList) {
	// TODO Auto-generated method stub

    }

    @Override
    public void removeMassList(@Nonnull MassList massList) {
	// TODO Auto-generated method stub

    }

    @Override
    public @Nonnull PolarityType getPolarity() {
	// TODO Auto-generated method stub
	return PolarityType.UNKNOWN;
    }

    @Override
    public String getScanDefinition() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public @Nonnull Range<Double> getScanningMZRange() {
	// TODO Auto-generated method stub
	return Range.all();
    }

}
