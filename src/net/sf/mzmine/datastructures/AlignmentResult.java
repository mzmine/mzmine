/*
    Copyright 2005-2006 VTT Biotechnology

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


import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.distributionframework.*;
import net.sf.mzmine.miscellaneous.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;

import java.util.Vector;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Hashtable;

import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.Serializable;

import javax.swing.JInternalFrame;


/**
 * This class represents an alignment result
 *
 * @version	30 March 2006
 */
public class AlignmentResult implements Serializable {




	// VARIABLES

	// Constants for different peak statuses
	public static final int PEAKSTATUS_DETECTED = 1;
	public static final int PEAKSTATUS_ESTIMATED = 2;
	public static final int PEAKSTATUS_NOTFOUND = 0;

	// General information about the alignment
	private int alignmentResultID;
	private String niceName;
	private String description;

	// These variables are only used for imported alignment results
	// (FIX: Ability to import alignment results is basically a debug feature, so these might be removed some day)
	private boolean isImported = false;
	private Hashtable<Integer, String> importedRawDataNames;

	// Data for common columns (not specific for a raw data file)
	private boolean[] standardCompounds;	// Standard compound flags for every row of the alignment (every peak)
	private int[] isotopePatternIDs;			// Unique ID for every isotope pattern
	private int[] isotopePeakNumbers;		// Numbering of peaks inside isotope patterns
	private int[] chargeStates;

	// Data for raw data specific columns ('key' is rawDataID, 'value' is data for a column)
	private Vector<Integer> rawDataIDs;
	private Hashtable<Integer, int[]> peakStatuses;
	private Hashtable<Integer, int[]> peakIDs;
	private Hashtable<Integer, double[]> peakMZs;
	private Hashtable<Integer, double[]> peakRTs;
	private Hashtable<Integer, double[]> peakHeights;
	private Hashtable<Integer, double[]> peakAreas;

	// These are precalculated from raw data specific columns
	private double[] averageMZs;
	private double[] averageRTs;
	private double[] averageHeights;
	private double[] averageAreas;

	// This is used to store currently selected row of the alignment result
	// (FIX: This information should be stored by the visualizers)
	private int selectedRow = -1;




	/**
	 * Initializes a new alignment result with given data
	 *
	 * @param	_rawDataIDs				IDs of raw data files whose peak lists participate in this alignment
	 * @param	_standardCompounds		Standard compound flags for peaks (true=peak is a standard compound)
	 * @param	_isotopePatternIDs		Isotope pattern IDs of peaks (all peaks in one pattern have same, unique ID)
	 * @param	_isotopePeakNumber		Isotope peak numbers of peaks (0=monoisotopic peak, 1,2,3=following peaks in a pattern)
	 * @param	_chargeStates			Charge states of peaks (1=+1 charge, ...)
	 * @param	_peakStatuses			Statuses of peaks (peak status must be represented using constants PEAKSTATUS_DETECTED, PEAKSTATUS_ESTIMATED, PEAKSTATUS_NOTFOUND)
	 * @param	_peakIDs				IDs of peaks (ID is the same ID that is used in original peak lists)
	 * @param	_peakMZs				M/Z values of peaks
	 * @param	_peakRTs				RT values of peaks
	 * @param	_peakHeights			Heights of peaks
	 * @param	_peakAreas				Areas of peaks
	 * @param	_description			Free text description of the alignment result
	 */
	public AlignmentResult(	Vector<Integer> _rawDataIDs,
							boolean[] _standardCompounds,
							int[] _isotopePatternIDs,
							int[] _isotopePeakNumbers,
							int[] _chargeStates,
							Hashtable<Integer, int[]> _peakStatuses,
							Hashtable<Integer, int[]> _peakIDs,
							Hashtable<Integer, double[]> _peakMZs,
							Hashtable<Integer, double[]> _peakRTs,
							Hashtable<Integer, double[]> _peakHeights,
							Hashtable<Integer, double[]> _peakAreas,
							String _description) {

		// Set name and description
		niceName = new String("Alignment (ID unassigned)");
		description = _description;

		// Store data for common columns
		standardCompounds = _standardCompounds;
		isotopePatternIDs = _isotopePatternIDs;
		isotopePeakNumbers = _isotopePeakNumbers;
		chargeStates = _chargeStates;

		// Store raw data specific data
		rawDataIDs = _rawDataIDs;
		peakStatuses = _peakStatuses;
		peakIDs = _peakIDs;
		peakMZs = _peakMZs;
		peakRTs = _peakRTs;
		peakHeights = _peakHeights;
		peakAreas = _peakAreas;

		// Do precalculation
		calculateAverages();
	}



	/**
	 * Initialies a new alignment result using data from an existing alignment result
	 *
	 * Because alignmentResultID must be unique for every alignment result, it is not copied from ar
	 * but it is expected to be set by somebody else
	 *
	 * @param	ar		Alignment result to be cloned
	 * @param	_description			Free text description of the aligment result
	 */
	public AlignmentResult(AlignmentResult ar, String _description) {

		// Set name and description
		niceName = new String("Alignment (ID unassigned)");
		description = _description;

		// FIX: Copy special information for imported alignment results
		isImported = ar.isImported();
		importedRawDataNames = ar.getImportedRawDataNames();

		// Assume that not any row has been selected
		selectedRow = -1;

		// Copy common columns
		standardCompounds = new boolean[ar.getNumOfRows()];
		isotopePatternIDs = new int[ar.getNumOfRows()];
		isotopePeakNumbers = new int[ar.getNumOfRows()];
		chargeStates = new int[ar.getNumOfRows()];
		for (int rowInd=0; rowInd<standardCompounds.length; rowInd++) {
			standardCompounds[rowInd] = ar.getStandardCompoundFlag(rowInd);
			isotopePatternIDs[rowInd] = ar.getIsotopePatternID(rowInd);
			isotopePeakNumbers[rowInd] = ar.getIsotopePeakNumber(rowInd);
			chargeStates[rowInd] = ar.getChargeState(rowInd);

		}

		// Copy raw data IDs
		rawDataIDs = new Vector<Integer>();
		int[] originalRawDataIDs = ar.getRawDataIDs();
		for (int originalRawDataID : originalRawDataIDs) {
			rawDataIDs.add(new Integer(originalRawDataID));
		}

		// Copy raw data specific data
		peakStatuses = new Hashtable<Integer, int[]>();
		peakIDs = new Hashtable<Integer, int[]>();
		peakMZs = new Hashtable<Integer, double[]>();
		peakRTs = new Hashtable<Integer, double[]>();
		peakHeights = new Hashtable<Integer, double[]>();
		peakAreas = new Hashtable<Integer, double[]>();

		for (int originalRawDataID : originalRawDataIDs) {
			Integer orgRawDataID = new Integer(originalRawDataID);

			// Fetch old arrays
			int[] orgStatuses = ar.getPeakStatuses(orgRawDataID);
			int[] orgIDs = ar.getPeakIDs(orgRawDataID);
			double[] orgMZs = ar.getPeakMZs(orgRawDataID);
			double[] orgRTs = ar.getPeakRTs(orgRawDataID);
			double[] orgHeights = ar.getPeakHeights(orgRawDataID);
			double[] orgAreas = ar.getPeakAreas(orgRawDataID);

			// Create new arrays
			int[] newStatuses = new int[orgStatuses.length];
			int[] newIDs = new int[orgStatuses.length];
			double[] newMZs = new double[orgStatuses.length];
			double[] newRTs = new double[orgStatuses.length];
			double[] newHeights = new double[orgStatuses.length];
			double[] newAreas = new double[orgStatuses.length];

			// Copy array contents from old to new
			System.arraycopy(orgStatuses,0, newStatuses,0, orgStatuses.length);
			System.arraycopy(orgIDs,0, newIDs,0, orgIDs.length);
			System.arraycopy(orgMZs,0, newMZs,0, orgMZs.length);
			System.arraycopy(orgRTs,0, newRTs,0, orgRTs.length);
			System.arraycopy(orgHeights,0, newHeights,0, orgHeights.length);
			System.arraycopy(orgAreas,0, newAreas,0, orgAreas.length);

			// Put new arrays to hashtables
			peakStatuses.put(orgRawDataID, newStatuses);
			peakIDs.put(orgRawDataID, newIDs);
			peakMZs.put(orgRawDataID, newMZs);
			peakRTs.put(orgRawDataID, newRTs);
			peakHeights.put(orgRawDataID, newHeights);
			peakAreas.put(orgRawDataID, newAreas);

		}

		// Do precalculation
		calculateAverages();

	}



	/**
	 * Initializes an empty alignment result object
	 *
	 * @param	_description			Free text description of the alignment result
	 */
	public AlignmentResult(String _description) {

		// Set name and description
		niceName = new String("Alignment (ID unassigned)");
		description = _description;

		// Initialize common columns
		standardCompounds = null;
		isotopePatternIDs = null;
		isotopePeakNumbers = null;
		chargeStates = null;

		// Initialize raw data specific columns
		rawDataIDs = new Vector<Integer>();
		peakStatuses = new Hashtable<Integer, int[]>();
		peakIDs = new Hashtable<Integer, int[]>();
		peakMZs = new Hashtable<Integer, double[]>();
		peakRTs = new Hashtable<Integer, double[]>();
		peakHeights = new Hashtable<Integer, double[]>();
		peakAreas = new Hashtable<Integer, double[]>();

	}




	// METHODS FOR GENERAL INFORMATION OF THE ALIGNMENT RESULT

	/**
	 * Returns the ID of the alignment result
	 */
	public int getAlignmentResultID() { return alignmentResultID; }

	/**
	 * Sets the ID of the alignment result and name of the alignment result accordingly.
	 */
	public void setAlignmentResultID(int id) {
		alignmentResultID = id;
		niceName = new String("Alignment " + alignmentResultID);
	}

	/**
	 * Returns the number of rows in the alignment result
	 */
	public int getNumOfRows() {
		if (standardCompounds == null) return 0;
		return standardCompounds.length;
	}

	/**
	 * Returns the number of peak lists aligned together in this alignment result
	 */
	public int getNumOfRawDatas() {
		if (rawDataIDs==null) return 0;
		return rawDataIDs.size();
	}

	/**
	 * This function checks if all heights&areas on a single alignment table row are detected / estimated (i.e. no gaps on this row)
	 */
	public boolean isFullRow(int rowInd) {

		int tmpPeakStatus;
		for (Integer rawDataID : rawDataIDs) {
			// Get peak status
			tmpPeakStatus = getPeakStatus(rawDataID.intValue(), rowInd);
			if (tmpPeakStatus == PEAKSTATUS_NOTFOUND) { return false;}
		}

		return true;
	}

	/**
	 * This function returns the number of full rows
	 */
	public int getNumOfFullRows() {
		int num=0;
		for (int rowInd=0; rowInd<getNumOfRows(); rowInd++) {
			if (isFullRow(rowInd)) { num++; }
		}
		return num;
	}

	/**
	 * Returns the name of the alignment result
	 */
	public String getNiceName() { return niceName; }

	public String toString() { return getNiceName(); }


	/**
	 * Returns the description of the alignment result
	 */
	public String getDescription() { return description; }




	// METHODS FOR COMMON COLUMNS' DATA

	/**
	 *	Returns all standard compound flags
	 */
	public boolean[] getStandardCompoundFlags() { return standardCompounds; }

	/**
	 * Sets all standard compound flags
	 */
	public void setStandardCompoundFlags(boolean[] flagValues) { standardCompounds = flagValues; }

	/**
	 * Returns standard compound flag of a row
	 */
	public boolean getStandardCompoundFlag(int rowNum) { return standardCompounds[rowNum]; }

	/**
	 * Sets standard compound flag of a row
	 */
	public void setStandardCompoundFlag(int rowNum, boolean flagValue) { standardCompounds[rowNum] = flagValue; }

	/**
	 * Counts number of standard compounds
	 */
	public int getNumOfStandardCompounds() {
		int num=0;
		for (int i=0; i<standardCompounds.length; i++) { if (standardCompounds[i]==true) { num++; }}
		return num;
	}




	/**
	 * Returns all isotope pattern IDs
	 */
	public int[] getIsotopePatternIDs() { return isotopePatternIDs; }

	/**
	 * Sets all isotope pattern IDs
	 */
	public void setIsotopePatternIDs(int[] patternIDs) {
		isotopePatternIDs = patternIDs;
	}

	/**
	 * Returns isotope pattern ID of one row
	 */
	public int getIsotopePatternID(int rowNum) { return isotopePatternIDs[rowNum]; }

	/**
	 * Sets isotope pattern ID of one row
	 */
	public void setIsotopePatternID(int rowNum, int patternID) { isotopePatternIDs[rowNum] = patternID; }




	/**
	 * Returns all isotope peak numbers
	 */
	public int[] getIsotopePeakNumbers() { return isotopePeakNumbers; }

	/**
	 * Sets all isotope peak numbers
	 */
	public void setIsotopePeakNumbers(int[] peakNumbers) { isotopePeakNumbers = peakNumbers; }

	/**
	 * Returns isotope peak number of one row
	 */
	public int getIsotopePeakNumber(int rowNum) { return isotopePeakNumbers[rowNum]; }

	/**
	 * Sets isotope peak number of one row
	 */
	public void setIsotopePeakNumber(int rowNum, int peakNumber) { isotopePeakNumbers[rowNum] = peakNumber; }




	/**
	 * Returns all charge states
	 */
	public int[] getChargeStates() { return chargeStates; }

	/**
	 * Sets all charge states
	 */
	public void setChargeStates(int[] charges) { chargeStates = charges; }

	/**
	 * Returns charge state of one row
	 */
	public int getChargeState(int rowNum) { return chargeStates[rowNum]; }

	/**
	 * Sets charge state of one row
	 */
	public void setChargeState(int rowNum, int charge) { chargeStates[rowNum] = charge; }




	// METHODS FOR RAW DATA SPECIFIC COLUMNS' DATA

	/**
	 * Returns ID of the n:th raw data from left in the table
	 *
	 * @param	columnGroupNumber	0=first raw data from left, 1=second, ...
	 */
	public int getRawDataID(int columnGroupNumber) {
		return rawDataIDs.get(columnGroupNumber);
	}

	/**
	 * Returns the position of raw data from left
	 *
	 * @param	rawDataID	ID of the raw data whose position is returned
	 * @return	0=first from left, 1=second, 2=...
	 */
	public int getColumnGroupNumber(int rawDataID) {
		return rawDataIDs.indexOf(new Integer(rawDataID));
	}

	/**
	 * Returns all IDs of raw data participating in this alignment
	 */
	public int[] getRawDataIDs() {
		int[] res = new int[rawDataIDs.size()];
		for (int i=0; i<rawDataIDs.size(); i++) { res[i] = rawDataIDs.get(i).intValue(); }
		return res;
	}




	/**
	 * Returns all statuses of peaks for one raw data
	 */
	public int[] getPeakStatuses(int rawDataID) { return peakStatuses.get(new Integer(rawDataID)); }

	/**
	 * Sets all statuses of peaks for one raw data
	 */
	public void setPeakStatuses(int rawDataID, int[] _peakStatuses) { peakStatuses.put(new Integer(rawDataID), _peakStatuses); }

	/**
	 * Returns all IDs of peaks for one raw data
	 */
	public int[] getPeakIDs(int rawDataID) { return peakIDs.get(new Integer(rawDataID)); }

	/**
	 * Sets all IDs of peaks for one raw data
	 */
	public void setPeakIDs(int rawDataID, int[] _peakIDs) { peakIDs.put(new Integer(rawDataID), _peakIDs); }

	/**
	 * Returns all M/Z values for one raw data
	 */
	public double[] getPeakMZs(int rawDataID) { return peakMZs.get(new Integer(rawDataID)); }

	/**
	 * Sets all M/Z values for one raw data
	 */
	public void setPeakMZs(int rawDataID, double[] _peakMZs) {
		peakMZs.put(new Integer(rawDataID), _peakMZs);
		calculateAverages();
	}

	/**
	 * Returns all RT values for one raw data
	 */
	public double[] getPeakRTs(int rawDataID) { return peakRTs.get(new Integer(rawDataID)); }

	/**
	 * Sets all RT values for one raw data
	 */
	public void setPeakRTs(int rawDataID, double[] _peakRTs) {
		peakRTs.put(new Integer(rawDataID), _peakRTs);
		calculateAverages();
	}

	/**
	 * Returns all heights for one raw data
	 */
	public double[] getPeakHeights(int rawDataID) { return peakHeights.get(new Integer(rawDataID)); }

	/**
	 * Sets all heights for one raw data
	 */
	public void setPeakHeights(int rawDataID, double[] _peakHeights) {
		peakHeights.put(new Integer(rawDataID), _peakHeights);
		calculateAverages();
	}

	/**
	 * Returns all areas for one raw data
	 */
	public double[] getPeakAreas(int rawDataID) { return peakAreas.get(new Integer(rawDataID)); }

	/**
	 * Sets all areas for one raw data
	 */
	public void setPeakAreas(int rawDataID, double[] _peakAreas) {
		peakAreas.put(new Integer(rawDataID), _peakAreas);
		calculateAverages();
	}




	/**
	 * Returns status of a peak
	 */
	public int getPeakStatus(int rawDataID, int rowNum) { return (peakStatuses.get(new Integer(rawDataID)))[rowNum]; }

	/**
	 * Sets status of a peak
	 */
	public void setPeakStatus(int rawDataID, int rowNum, int peakStatus) { (peakStatuses.get(new Integer(rawDataID)))[rowNum] = peakStatus; }

	/**
	 * Returns ID of a peak
	 */
	public int getPeakID(int rawDataID, int rowNum) { return (peakIDs.get(new Integer(rawDataID)))[rowNum]; }

	/**
	 * Sets ID of a peak
	 */
	public void setPeakID(int rawDataID, int rowNum, int peakID) { (peakIDs.get(new Integer(rawDataID)))[rowNum] = peakID; }

	/**
	 * Returns M/Z value of a peak
	 */
	public double getPeakMZ(int rawDataID, int rowNum) { return (peakMZs.get(new Integer(rawDataID)))[rowNum]; }

	/**
	 * Sets M/Z value of a peak
	 */
	public void setPeakMZ(int rawDataID, int rowNum, double peakMZ) {
		(peakMZs.get(new Integer(rawDataID)))[rowNum] = peakMZ;
		calculateAverageForRow(rowNum);
	}

	/**
	 * Returns RT value of a peak
	 */
	public double getPeakRT(int rawDataID, int rowNum) { return (peakRTs.get(new Integer(rawDataID)))[rowNum]; }

	/**
	 * Sets RT value of a peak
	 */
	public void setPeakRT(int rawDataID, int rowNum, double peakRT) {
		(peakRTs.get(new Integer(rawDataID)))[rowNum] = peakRT;
		calculateAverageForRow(rowNum);
	}

	/**
	 * Returns height of a peak
	 */
	public double getPeakHeight(int rawDataID, int rowNum) { return (peakHeights.get(new Integer(rawDataID)))[rowNum]; }

	/**
	 * Sets height of a peak
	 */
	public void setPeakHeight(int rawDataID, int rowNum, double peakHeight) {
		(peakHeights.get(new Integer(rawDataID)))[rowNum] = peakHeight;
		calculateAverageForRow(rowNum);
	}

	/**
	 * Returns area of a peak
	 */
	public double getPeakArea(int rawDataID, int rowNum) { return (peakAreas.get(new Integer(rawDataID)))[rowNum]; }

	/**
	 * Sets area of a peak
	 */
	public void setPeakArea(int rawDataID, int rowNum, double peakArea) {
		(peakAreas.get(new Integer(rawDataID)))[rowNum] = peakArea;
		calculateAverageForRow(rowNum);
	}




	// METHODS FOR PRECALCULATED COLUMNS

	/**
	 * Returns average M/Z value of a row
	 */
	public double getAverageMZ(int rowInd) { return averageMZs[rowInd]; }

	/**
	 * Returns average RT value of a row
	 */
	public double getAverageRT(int rowInd) { return averageRTs[rowInd]; }

	/**
	 * Returns average height of a row
	 */
	public double getAverageHeight(int rowInd) { return averageHeights[rowInd]; }

	/**
	 * Returns average area of a row
	 */
	public double getAverageArea(int rowInd) { return averageAreas[rowInd]; }

	/**
	 * Precalculates average values for each row
	 */
	private void calculateAverages() {

		// Initialize average columns
		averageMZs = new double[getNumOfRows()];
		averageRTs = new double[getNumOfRows()];
		averageHeights = new double[getNumOfRows()];
		averageAreas = new double[getNumOfRows()];

		// Calculate averages for each row
		for (int rowInd=0; rowInd<getNumOfRows(); rowInd++) {
			calculateAverageForRow(rowInd);
		}
	}


	/**
	 * Calculates average values for one row
	 */
	private void calculateAverageForRow(int rowInd) {

		int rowPeakNum;
		int rowHeightNum;
		int rowAreaNum;

		double rowSumMZ;
		double rowSumRT;
		double rowSumHeight;
		double rowSumArea;

		int tmpPeakStatus;
		double tmpMZ;
		double tmpRT;
		double tmpHeight;
		double tmpArea;

		rowSumMZ=0; rowSumRT=0; rowSumHeight=0; rowSumArea=0;
		rowPeakNum=0; rowHeightNum=0; rowAreaNum=0;

		for (Integer rawDataID : rawDataIDs) {

			// Get peak status
			tmpPeakStatus = getPeakStatus(rawDataID.intValue(), rowInd);

			// Add m/z and rt values to average calculation if available
			if ( (tmpPeakStatus == PEAKSTATUS_DETECTED) || (tmpPeakStatus == PEAKSTATUS_ESTIMATED) ) {
				tmpMZ = getPeakMZ(rawDataID.intValue(), rowInd);
				tmpRT = getPeakRT(rawDataID.intValue(), rowInd);

				rowSumMZ += tmpMZ;
				rowSumRT += tmpRT;
				rowPeakNum++;
			}

			// Add height to average calculation if available
			if ( (tmpPeakStatus == PEAKSTATUS_DETECTED) || (tmpPeakStatus == PEAKSTATUS_ESTIMATED) ) {
				tmpHeight = getPeakHeight(rawDataID.intValue(), rowInd);

				rowSumHeight += tmpHeight;
				rowHeightNum++;
			}

			// Add area to average calculation if available
			if ( (tmpPeakStatus == PEAKSTATUS_DETECTED) || (tmpPeakStatus == PEAKSTATUS_ESTIMATED) ) {
				tmpArea = getPeakArea(rawDataID.intValue(), rowInd);

				rowSumArea += tmpArea;
				rowAreaNum++;
			}
		}

		if (rowPeakNum>0) {
			averageMZs[rowInd] = rowSumMZ / (double)rowPeakNum;
			averageRTs[rowInd] = rowSumRT / (double)rowPeakNum;
		} else {
			averageMZs[rowInd] = -1;
			averageRTs[rowInd] = -1;
		}

		if (rowHeightNum>0) {
			averageHeights[rowInd] = rowSumHeight / (double)rowHeightNum;
		} else {
			averageHeights[rowInd] = -1;
		}

		if (rowAreaNum>0) {
			averageAreas[rowInd] = rowSumArea / (double)rowAreaNum;
		} else {
			averageAreas[rowInd] = -1;
		}
	}




	// METHODS FOR ROW CURSOR POSITION
	// FIX: Maybe the cursor position should be handled by visualizers?


	/**
	 * Returns selected row of the table
	 */
	public int getSelectedRow() {
		return selectedRow;
	}

	/**
	 * Selects row of the table
	 */
	public void setSelectedRow(int rowNum) {
		selectedRow = rowNum;
	}

	/**
	 * Select row of the table where given peak is located
	 *
	 * @param	rawDataID	ID of the raw data whose peak is searched
	 * @param	peakID		ID of the peak to be searched
	 */
	public int selectPeak(int rawDataID, int peakID) {

		// Find ID of the row where this peak is
		int[] tmpIDs = peakIDs.get(new Integer(rawDataID));
		if (tmpIDs == null) { return -1; }

		int alignmentRow=-1;
		for (int i=0; i<tmpIDs.length; i++) {
			if (tmpIDs[i] == peakID) { alignmentRow = i; break; }
		}

		// Select this row
		if (alignmentRow!=-1) {
			setSelectedRow(alignmentRow);
		}

		return alignmentRow;

	}




	// METHODS FOR SPECIAL FIELDS OF IMPORTED ALIGNMENT RESULTS
	// FIX: Imported alignment results are mostly a debug feature


	/**
	 * Checks if this alignment result has been imported (instead of being constructed by an aligner method)
	 */
	public boolean isImported() {
		return isImported;
	}

	/**
	 * Sets the imported flag of the alignment result
	 */
	public void setImported(boolean b) {
		isImported = b;
	}

	/**
	 * Gets name of a raw data participating in an imported alignment result
	 */
	public String getImportedRawDataName(int rawDataID) {
		return importedRawDataNames.get(new Integer(rawDataID));
	}

	/**
	 * Sets name of a raw data participating in an imported alignment result
	 */
	public void setImportedRawDataName(int rawDataID, String rawDataName) {
		importedRawDataNames.put(rawDataID, rawDataName);
	}

	/**
	 * Gets names of all raw data participating in an imported alignment result
	 */
	public Hashtable<Integer, String> getImportedRawDataNames() {
		return importedRawDataNames;
	}

	/**
	 * Sets names of all raw data participating in an imported alignment result
	 */
	public void setImportedRawDataNames(Hashtable<Integer, String>_names) {
		importedRawDataNames = _names;
	}



}