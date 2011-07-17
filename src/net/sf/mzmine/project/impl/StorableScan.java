/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.tree.DefaultMutableTreeNode;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.MassList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
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
	private double totalIonCurrent;
	private boolean centroided;
	private long scanFileOffset;
	private int numberOfDataPoints;
	private RawDataFileImpl rawDataFile;
	private ArrayList<MassList> massLists = new ArrayList<MassList>();

	/**
	 * Constructor for creating a storable scan from a given scan
	 */
	public StorableScan(Scan originalScan, RawDataFileImpl rawDataFile,
			long scanFileOffset, int numberOfDataPoints) {

		// save scan data
		this.rawDataFile = rawDataFile;
		this.scanFileOffset = scanFileOffset;
		this.numberOfDataPoints = numberOfDataPoints;

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

	public StorableScan(RawDataFileImpl rawDataFile, long scanFileOffset,
			int numberOfDataPoints, int scanNumber, int msLevel,
			double retentionTime, int parentScan, double precursorMZ,
			int precursorCharge, int fragmentScans[], boolean centroided) {

		this.rawDataFile = rawDataFile;
		this.scanFileOffset = scanFileOffset;
		this.numberOfDataPoints = numberOfDataPoints;

		this.scanNumber = scanNumber;
		this.msLevel = msLevel;
		this.retentionTime = retentionTime;
		this.parentScan = parentScan;
		this.precursorMZ = precursorMZ;
		this.precursorCharge = precursorCharge;
		this.fragmentScans = fragmentScans;
		this.centroided = centroided;

		mzRange = new Range(0, 0);
		basePeak = null;
		totalIonCurrent = 0;

		DataPoint dataPoints[] = getDataPoints();

		// find m/z range and base peak
		if (dataPoints.length > 0) {

			basePeak = dataPoints[0];
			mzRange = new Range(dataPoints[0].getMZ(), dataPoints[0].getMZ());

			for (DataPoint dp : dataPoints) {

				if (dp.getIntensity() > basePeak.getIntensity())
					basePeak = dp;

				mzRange.extendRange(dp.getMZ());

				totalIonCurrent += dp.getIntensity();

			}

		}

	}

	/**
	 * @return Scan's datapoints from temporary file.
	 */
	public DataPoint[] getDataPoints() {

		try {

			ByteBuffer bytes = rawDataFile.readFromFloatBufferFile(
					scanFileOffset, numberOfDataPoints * 2 * 4);

			FloatBuffer floatBuffer = bytes.asFloatBuffer();

			DataPoint dataPoints[] = new DataPoint[numberOfDataPoints];

			for (int i = 0; i < numberOfDataPoints; i++) {
				float mz = floatBuffer.get();
				float intensity = floatBuffer.get();
				dataPoints[i] = new SimpleDataPoint(mz, intensity);
			}

			return dataPoints;

		} catch (IOException e) {
			logger.severe("Could not read data from temporary file "
					+ e.toString());
			return new DataPoint[0];
		}

	}

	/**
	 * @return Returns scan datapoints within a given range
	 */
	public DataPoint[] getDataPointsByMass(Range mzRange) {

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
	public DataPoint[] getDataPointsOverIntensity(double intensity) {
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

	public RawDataFile getDataFile() {
		return rawDataFile;
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

	/**
	 * @see net.sf.mzmine.data.Scan#getMZRangeMax()
	 */
	public Range getMZRange() {
		return mzRange;
	}

	/**
	 * @see net.sf.mzmine.data.Scan#getBasePeakMZ()
	 */
	public DataPoint getBasePeak() {
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
		return totalIonCurrent;
	}

	public String toString() {
		return ScanUtils.scanToString(this);
	}

	@Override
	public synchronized void addMassList(MassList massList) {

		// Remove all mass lists with same name, if there are any
		MassList currentMassLists[] = massLists.toArray(new MassList[0]);
		for (MassList ml : currentMassLists) {
			if (ml.getName().equals(massList.getName()))
				removeMassList(ml);
		}

		// Add the new mass list
		massLists.add(massList);

		// Add the mass list to the tree model
		MZmineProjectImpl project = (MZmineProjectImpl) MZmineCore
				.getCurrentProject();
		ProjectTreeModel treeModel = project.getTreeModel();
		DefaultMutableTreeNode root = treeModel.getRoot();
		Enumeration nodes = root.breadthFirstEnumeration();
		while (nodes.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes
					.nextElement();
			if (node.getUserObject() == this) {
				DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(
						massList);
				int index = massLists.size() - 1;
				treeModel.insertNodeInto(newNode, node, index);
				break;
			}
		}

	}

	@Override
	public synchronized void removeMassList(MassList massList) {

		massLists.remove(massList);

		// Add the mass list to the tree model
		MZmineProjectImpl project = (MZmineProjectImpl) MZmineCore
				.getCurrentProject();
		ProjectTreeModel treeModel = project.getTreeModel();

		DefaultMutableTreeNode root = treeModel.getRoot();
		Enumeration nodes = root.breadthFirstEnumeration();
		while (nodes.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes
					.nextElement();
			if (node.getUserObject() == massList) {
				treeModel.removeNodeFromParent(node);
				break;
			}
		}

	}

	@Override
	public MassList[] getMassLists() {
		return massLists.toArray(new MassList[0]);
	}

	@Override
	public MassList getMassList(String name) {
		for (MassList ml : massLists) {
			if (ml.getName().equals(name))
				return ml;
		}
		return null;
	}

}
