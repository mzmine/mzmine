/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.project.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.SwingUtilities;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.MassList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.desktop.impl.projecttree.ProjectTreeModel;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;

/**
 * Implementation of the Scan interface which stores raw data points in a
 * temporary file, accessed by RawDataFileImpl.readFromFloatBufferFile()
 */
public class StorableScan implements Scan {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private int scanNumber, msLevel, parentScan, fragmentScans[];
	private double precursorMZ;
	private int precursorCharge;
	private double retentionTime;
	private Range mzRange;
	private DataPoint basePeak;
	private Double totalIonCurrent;
	private boolean centroided;
	private int numberOfDataPoints;
	private RawDataFileImpl rawDataFile;
	private ArrayList<MassList> massLists = new ArrayList<MassList>();
	private int storageID;

	/**
	 * Constructor for creating a storable scan from a given scan
	 */
	public StorableScan(Scan originalScan, RawDataFileImpl rawDataFile,
			int numberOfDataPoints, int storageID) {

		// save scan data
		this.rawDataFile = rawDataFile;
		this.numberOfDataPoints = numberOfDataPoints;
		this.storageID = storageID;

		this.scanNumber = originalScan.getScanNumber();
		this.msLevel = originalScan.getMSLevel();
		this.retentionTime = originalScan.getRetentionTime();
		this.parentScan = originalScan.getParentScanNumber();
		this.precursorMZ = originalScan.getPrecursorMZ();
		this.precursorCharge = originalScan.getPrecursorCharge();
		this.fragmentScans = originalScan.getFragmentScanNumbers();
		this.centroided = originalScan.isCentroided();
		this.mzRange = originalScan.getMZRange();
		this.basePeak = originalScan.getBasePeak();
		this.totalIonCurrent = originalScan.getTIC();

	}

	public StorableScan(RawDataFileImpl rawDataFile, int storageID,
			int numberOfDataPoints, int scanNumber, int msLevel,
			double retentionTime, int parentScan, double precursorMZ,
			int precursorCharge, int fragmentScans[], boolean centroided) {

		this.rawDataFile = rawDataFile;
		this.numberOfDataPoints = numberOfDataPoints;
		this.storageID = storageID;

		this.scanNumber = scanNumber;
		this.msLevel = msLevel;
		this.retentionTime = retentionTime;
		this.parentScan = parentScan;
		this.precursorMZ = precursorMZ;
		this.precursorCharge = precursorCharge;
		this.fragmentScans = fragmentScans;
		this.centroided = centroided;
	}

	/**
	 * @return Scan's datapoints from temporary file.
	 */
	public @Nonnull DataPoint[] getDataPoints() {

		try {
			DataPoint result[] = rawDataFile.readDataPoints(storageID);
			return result;
		} catch (IOException e) {
			logger.severe("Could not read data from temporary file "
					+ e.toString());
			return new DataPoint[0];
		}

	}

	/**
	 * @return Returns scan datapoints within a given range
	 */
	public @Nonnull DataPoint[] getDataPointsByMass(@Nonnull Range mzRange) {

		DataPoint dataPoints[] = getDataPoints();

		int startIndex, endIndex;
		for (startIndex = 0; startIndex < dataPoints.length; startIndex++) {
			if (dataPoints[startIndex].getMZ() >= mzRange.getMin()) {
				break;
			}
		}

		for (endIndex = startIndex; endIndex < dataPoints.length; endIndex++) {
			if (dataPoints[endIndex].getMZ() > mzRange.getMax()) {
				break;
			}
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
		DataPoint dataPoints[] = getDataPoints();

		for (index = 0; index < dataPoints.length; index++) {
			if (dataPoints[index].getIntensity() >= intensity) {
				points.add(dataPoints[index]);
			}
		}

		DataPoint pointsOverIntensity[] = points.toArray(new DataPoint[0]);

		return pointsOverIntensity;
	}

	public @Nonnull RawDataFile getDataFile() {
		return rawDataFile;
	}
	
	public int getStorageID() {
		return storageID;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getNumberOfDataPoints()
	 */
	public int getNumberOfDataPoints() {
		return numberOfDataPoints;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getScanNumber()
	 */
	public int getScanNumber() {
		return scanNumber;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getMSLevel()
	 */
	public int getMSLevel() {
		return msLevel;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getPrecursorMZ()
	 */
	public double getPrecursorMZ() {
		return precursorMZ;
	}

	/**
	 * @return Returns the precursorCharge.
	 */
	public int getPrecursorCharge() {
		return precursorCharge;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getScanAcquisitionTime()
	 */
	public double getRetentionTime() {
		return retentionTime;
	}

	void updateValues() {
		DataPoint dataPoints[] = getDataPoints();

		// find m/z range and base peak
		if (dataPoints.length > 0) {

			basePeak = dataPoints[0];
			mzRange = new Range(dataPoints[0].getMZ(), dataPoints[0].getMZ());
			double tic = 0;

			for (DataPoint dp : dataPoints) {

				if (dp.getIntensity() > basePeak.getIntensity())
					basePeak = dp;

				mzRange.extendRange(dp.getMZ());

				tic += dp.getIntensity();

			}

			totalIonCurrent = new Double(tic);

		} else {
			mzRange = new Range(0);
			totalIonCurrent = new Double(0);
		}
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getMZRangeMax()
	 */
	public @Nonnull Range getMZRange() {
		if (mzRange == null)
			updateValues();
		return mzRange;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getBasePeakMZ()
	 */
	public DataPoint getBasePeak() {
		if ((basePeak == null) && (numberOfDataPoints > 0))
			updateValues();
		return basePeak;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getParentScanNumber()
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
	 * @see net.sf.mzmine.data.Scan#getFragmentScanNumbers()
	 */
	public int[] getFragmentScanNumbers() {
		return fragmentScans;
	}

	/**
	 * @param fragmentScans
	 *            The fragmentScans to set.
	 */
	void setFragmentScanNumbers(int[] fragmentScans) {
		this.fragmentScans = fragmentScans;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#isCentroided()
	 */
	public boolean isCentroided() {
		return centroided;
	}

	public double getTIC() {
		if (totalIonCurrent == null)
			updateValues();
		return totalIonCurrent;
	}
	
	@Override
	public String toString() {
		return ScanUtils.scanToString(this);
	}

	@Override
	public synchronized void addMassList(final @Nonnull MassList massList) {

		// Remove all mass lists with same name, if there are any
		MassList currentMassLists[] = massLists.toArray(new MassList[0]);
		for (MassList ml : currentMassLists) {
			if (ml.getName().equals(massList.getName()))
				removeMassList(ml);
		}

		StorableMassList storedMassList;
		if (massList instanceof StorableMassList) {
			storedMassList = (StorableMassList) massList;
		} else {
			DataPoint massListDataPoints[] = massList.getDataPoints();
			try {
				int mlStorageID = rawDataFile
						.storeDataPoints(massListDataPoints);
				storedMassList = new StorableMassList(rawDataFile, mlStorageID,
						massList.getName(), this);
			} catch (IOException e) {
				logger.severe("Could not write data to temporary file "
						+ e.toString());
				return;
			}
		}

		// Add the new mass list
		massLists.add(storedMassList);

		// Add the mass list to the tree model
		MZmineProjectImpl project = (MZmineProjectImpl) MZmineCore
				.getCurrentProject();

		// Check if we are adding to the current project
		if (Arrays.asList(project.getDataFiles()).contains(rawDataFile)) {
			final ProjectTreeModel treeModel = project.getTreeModel();
			final MassList newMassList = storedMassList;
			Runnable swingCode = new Runnable() {
				@Override
				public void run() {
					treeModel.addObject(newMassList);
				}
			};

			try {
				if (SwingUtilities.isEventDispatchThread())
					swingCode.run();
				else
					SwingUtilities.invokeAndWait(swingCode);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	@Override
	public synchronized void removeMassList(final @Nonnull MassList massList) {

		// Remove the mass list
		massLists.remove(massList);
		if (massList instanceof StorableMassList) {
			StorableMassList storableMassList = (StorableMassList) massList;
			storableMassList.removeStoredData();
		}

		// Remove from the tree model
		MZmineProjectImpl project = (MZmineProjectImpl) MZmineCore
				.getCurrentProject();

		// Check if we are using the current project
		if (Arrays.asList(project.getDataFiles()).contains(rawDataFile)) {
			final ProjectTreeModel treeModel = project.getTreeModel();
			Runnable swingCode = new Runnable() {
				@Override
				public void run() {
					treeModel.removeObject(massList);
				}
			};

			SwingUtilities.invokeLater(swingCode);

		}

	}

	@Override
	public @Nonnull MassList[] getMassLists() {
		return massLists.toArray(new MassList[0]);
	}

	@Override
	public MassList getMassList(@Nonnull String name) {
		for (MassList ml : massLists) {
			if (ml.getName().equals(name))
				return ml;
		}
		return null;
	}

}
