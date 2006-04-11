/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/


package net.sf.mzmine.datastructures;
import java.util.Vector;

import net.sf.mzmine.util.Logger;
import net.sf.mzmine.visualizers.rawdata.RawDataVisualizer;

public class RawDataAtClient {


	private static final int paramRangeFoldMZ = 100;		// These two parameters define the shape of selection when it is set around a specific mz,rt location
	private static final int paramRangeFoldScanInd = 5;

	private static final double paramMZvsRTBalance = 10;		// When searching for nearest peak to cursor location, how many times more important is closeness in MZ direction than RT


	// Identity
	private int rawDataID;
	private String niceName;

	// Properties of the raw data
	private int numberOfScans;						// Number of MS-scans in the raw data file
	private double[] scanTimes;						// Times of scans (in secs)
	private double minMZValue;						// Minimum and maximum m/z value in the raw data file
	private double maxMZValue;

	private double totalRawSignal;

	// Peak list for raw data file
	private PeakList peakList;

	// Current cursor position in the raw data
    private int cursorPositionScan;
    private double cursorPositionMZ;

	// Selected area of this raw data
    private int selectionScanStart;
    private int selectionScanEnd;
    private double selectionMZStart;
    private double selectionMZEnd;

    // IDs of alignment results where this raw data is participating
    Vector<Integer> alignmentResultIDs;

	// This flag is on when raw data has been updated (after filtering), and it is not healthy for visualizers to paint anything until a refresh has been done
	// This flag is set when updateFromRawDataOnTransit. Caller should also initiate a visualizer refresh, and afterwards unset this flag using setRawDataUpdatedFlag method
	private boolean rawDataUpdatedFlag;


	/**
	 * Constructor
	 * Creates a new object on basis of all available information in RawDataOnTransit object.
	 */
	public RawDataAtClient(RawDataOnTransit rawDataOnTransit) {
		rawDataID = rawDataOnTransit.rawDataID;
		niceName = rawDataOnTransit.niceName;
		numberOfScans = rawDataOnTransit.numberOfScans;
		scanTimes = rawDataOnTransit.scanTimes;
		minMZValue = rawDataOnTransit.minMZValue;
		maxMZValue = rawDataOnTransit.maxMZValue;
		alignmentResultIDs = new Vector<Integer>();
		clearSelection();
	}

	/**
	 * Constructor
	 * Used to create fake raw data objects when importing alignment result
	 * "Fake" means that object has only niceName and rawDataID.
	 */
	public RawDataAtClient(int _rawDataID, String _niceName) {
		rawDataID = _rawDataID;
		niceName = _niceName;
		numberOfScans = 0;
		scanTimes = null;
		minMZValue = 0;
		maxMZValue = 0;
		alignmentResultIDs = new Vector<Integer>();
		clearSelection();

	}


	/**
	 * This method updates current settings of RawDataOnClient object with data
	 * available on RawDataOnTransit. This is used when major change (filtering)
	 * has been made to raw data on node-side.
	 */
	public void updateFromRawDataOnTransit(RawDataOnTransit rawDataOnTransit) {

		Logger.put("!!! updateFromRawDataOnTransit");
		rawDataUpdatedFlag = true;

		// Record RT of previous cursor position
		double previousCursorPositionRT = scanTimes[cursorPositionScan];

		numberOfScans = rawDataOnTransit.numberOfScans;
		scanTimes = rawDataOnTransit.scanTimes;
		minMZValue = rawDataOnTransit.minMZValue;
		maxMZValue = rawDataOnTransit.maxMZValue;

		// Restore cursor position to scan which has nearest RT value after change
		double smallestDifference = Double.MAX_VALUE;
		for (int ind=0; ind<scanTimes.length; ind++) {
			if (java.lang.Math.abs(previousCursorPositionRT-scanTimes[ind])<smallestDifference) {
				cursorPositionScan = ind;
				smallestDifference = java.lang.Math.abs(previousCursorPositionRT-scanTimes[ind]);
			}
		}

		if ( (cursorPositionMZ>=minMZValue) && (cursorPositionMZ<=maxMZValue) ) {
			// If MZ of previous cursor position is still within the M/Z range, then use it.
		} else {
			// Else set cursor to minMZValue
			cursorPositionMZ = minMZValue;
		}

		// Clear selection to avoid more mess.
    	clearSelection();

	}

	public boolean getRawDataUpdatedFlag() {
		return rawDataUpdatedFlag;
	}

	public void setRawDataUpdatedFlag(boolean flag) {
		rawDataUpdatedFlag = flag;
	}

	public int getRawDataID() {
		return rawDataID;
	}

	public String getNiceName() {
		return niceName;
	}

	/**
	 * This function returns the time (in seconds) of a scan
	 * @param	Scan number for which the time should be given
	 * @return	Time of the scan in seconds
	 */
	public double getScanTime(int scanNumber) {
		return scanTimes[scanNumber];
	}

	public int getNumOfScans() {
		return scanTimes.length;
	}

	/**
	 * Returns the number of scan that is closest to given rt
	 */
	public int getScanNumberByTime(double rt) {
		for (int scanNum=0; scanNum<scanTimes.length; scanNum++) {
			if (scanTimes[scanNum]>=rt) { return scanNum; }
		}
		return scanTimes.length-1;
	}

	public double getDataMinMZ() {
		return minMZValue;
	}

	public double getDataMaxMZ() {
		return maxMZValue;
	}


	public double getTotalRawSignal() {
		return totalRawSignal;
	}

	public void setTotalRawSignal(double totSig) {
		totalRawSignal = totSig;
	}


	// Methods for handling cursor position

    public void setCursorPositionScan(int _cursorPositionScan) {
		cursorPositionScan = _cursorPositionScan;
	}

	public int getCursorPositionScan() {
		return cursorPositionScan;
	}

    public void setCursorPositionMZ(double _cursorPositionMZ) {
		cursorPositionMZ = _cursorPositionMZ;
	}

	public double getCursorPositionMZ() {
		return cursorPositionMZ;
	}

	public void setCursorPosition(int _cursorPositionScan, double _cursorPositionMZ) {
		cursorPositionScan = _cursorPositionScan;
		cursorPositionMZ = _cursorPositionMZ;
	}


	public void setCursorPositionByPeakID(int peakID) {

		if (peakList==null) { return; }

		// Find this peak
		Peak p = peakList.getPeak(peakID);

		// If peak was not found with this peakID
		if (p == null) { return; }

		// Set cursor on median mz and maximum intensity scan of this peak
		double mz = p.getMZ();
		int scannum = p.getMaxIntensityScanNumber();
		setCursorPosition(scannum, mz);

	}


	// Methods for handling area selection

  	public void clearSelection() {
		selectionScanStart = -1;
		selectionScanEnd = -1;
		selectionMZStart = -1;
		selectionMZEnd = -1;
	}

  	public void clearSelectionScan() {
		selectionScanStart = -1;
		selectionScanEnd = -1;
	}

  	public void clearSelectionMZ() {
		selectionMZStart = -1;
		selectionMZEnd = -1;
	}

	public void setSelection(int scanStart, int scanEnd, double mzStart, double mzEnd) {
		selectionScanStart = scanStart;
		selectionScanEnd = scanEnd;
		selectionMZStart = mzStart;
		selectionMZEnd = mzEnd;
	}

	public void setSelectionScan(int scanStart, int scanEnd) {
		selectionScanStart = scanStart;
		selectionScanEnd = scanEnd;
	}

	public void setSelectionMZ(double mzStart, double mzEnd) {
		selectionMZStart = mzStart;
		selectionMZEnd = mzEnd;
	}

	public int getSelectionScanStart() {
		return selectionScanStart;
	}

	public int getSelectionScanEnd() {
			return selectionScanEnd;
	}

	public double getSelectionMZStart() {
			return selectionMZStart;
	}

	public double getSelectionMZEnd() {
			return selectionMZEnd;
	}


	// Methods for dealing with peaks


	/**
	 * Returns true if the raw data file has been assigned some peak data
	 */
	public boolean hasPeakData() {
		if (peakList!=null) { return true; } else { return false; }
		//if (peaks!=null) { return true; } else { return false; }
	}

	/**
	 * Returns the peak list of the raw data
	 */
	public PeakList getPeakList() {
		return peakList;
	}

	/**
	 * Sets the peak list of the raw data
	 */
	public void setPeakList(PeakList _peakList) {
		peakList = _peakList;
	}


	/**
	 * Sets cursor over peak that is closest to given mz, scan coordinates
	 * @param	mz				MZ of search start location
	 * @param	scan			Scan number of search start location
	 * @param	ratioMZvsRT		Balance between coordinate axes when measuring what is closest
	 * @return	If successful, change type (cursor position or selection changed), -1 if no change (no peak found)
	 */
	public int selectNearestPeak(double mz, int scan, double ratioMZvsRT) {
		// Find nearest peak
		if (peakList == null) { return -1; }
		Peak p = peakList.findNearestPeak(mz,getScanTime(scan),ratioMZvsRT);
		if (p==null) { return -1; } // problems?

		// Set cursor on nearest peak
		int peakID = p.getPeakID();
		peakList.setSelectedPeakID(peakID);
		double peakMZ = p.getMZ();
		int peakScan = p.getMaxIntensityScanNumber();

		setCursorPosition(peakScan, peakMZ);


		int changeType = RawDataVisualizer.CHANGETYPE_CURSORPOSITION_BOTH;

		// If cursor is outside visible range, change zoom to around the selected peak
		if  (	( (getSelectionScanStart()>=0) && (peakScan<getSelectionScanStart()) ) || // out from left edge
				( (getSelectionScanEnd()>=0) && (peakScan>getSelectionScanEnd()) ) || // out from right edge
				( (getSelectionMZStart()>=0) && (peakMZ<getSelectionMZStart()) ) || // out from bottom
				( (getSelectionMZEnd()>=0) && (peakMZ>getSelectionMZEnd()) ) ) { // out from top

			setSelectionAroundPeak(p);
			changeType = RawDataVisualizer.CHANGETYPE_SELECTION_BOTH;
		}



		return changeType;

	}





	public void setSelectionAroundPeak(double mz, double rt, double mzstdev, double duration) {

		int tmpMidScan = getScanNumberByTime(rt);

		// Calc first and last scan in the zoom region
		double tmpTime = rt - paramRangeFoldScanInd * duration; if (tmpTime<0) { tmpTime=0; }
		int tmpStartScan = getScanNumberByTime(tmpTime);
		tmpTime = rt + paramRangeFoldScanInd * duration;
		int tmpStopScan = getScanNumberByTime(tmpTime);

		int zoomStartInd = tmpStartScan;
		int zoomStopInd = tmpStopScan;

		// Check that scan indices for zooming are reasonable
		while ((zoomStopInd-zoomStartInd)<10) { zoomStartInd--; zoomStopInd++;}
		if (zoomStartInd<0) { zoomStartInd = 0;}
		if (zoomStopInd>=getNumOfScans()) { zoomStopInd = getNumOfScans()-1; }

		// Calc limit of zoom region in MZ direction
		double zoomStartMZ = mz - paramRangeFoldMZ * mzstdev;
		double zoomStopMZ = mz + paramRangeFoldMZ * mzstdev;

		// Check that mz range for zooming is reasonable
		while ((zoomStopMZ-zoomStartMZ)<8) { zoomStartMZ -= 0.2; zoomStopMZ += 0.2;}
		if (zoomStartMZ<getDataMinMZ()) { zoomStartMZ = getDataMinMZ();}
		if (zoomStopMZ>=getDataMaxMZ()) { zoomStopMZ = getDataMaxMZ(); }

		// setCursorPosition(p.getMaxIntensityScanNum(), p.getCentroidMZMedian());
		setSelection(zoomStartInd, zoomStopInd, zoomStartMZ, zoomStopMZ);


	}

	public void setSelectionAroundPeak(Peak p) {
		double dur = getScanTime(p.getStopScanNumber()) - getScanTime(p.getStartScanNumber());
		setSelectionAroundPeak(p.getMZ(), p.getRT(), p.getMZStdev(), dur);
	}


	public Vector<Integer> getAlignmentResultIDs() {
		return alignmentResultIDs;
	}

	public void addAlignmentResultID(int alignmentResultID) {
		alignmentResultIDs.add(new Integer(alignmentResultID));
	}

	public void removeAlignmentResultID(int alignmentResultID) {
		alignmentResultIDs.remove(new Integer(alignmentResultID));
	}


	public String toString() {
		return getNiceName();
	}


}