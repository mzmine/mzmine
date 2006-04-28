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


package net.sf.mzmine.methods.alignment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.Logger;


/**
 * This class contains static methods for exporting/importing alignment results to/from tab-delimitted text files.
 *
 * @version	30 March 2006
 */
public class AlignmentResultExporter {


	/**
	 * Writes an alignment result to tab-delimitted text file.
	 *
	 * @param	alignmentResult		The alignment result to be exported
	 * @param	fileName			Name of the file
	 * @param	parameters			User-definable parameters (defines which columns to export)
	 *
	 * @return	true if successfully exported, otherwise false
	 */
	public static boolean exportAlignmentResultToFile(AlignmentResult alignmentResult, String fileName, AlignmentResultExporterParameters parameters, MainWindow mainWin) {

		// Open file

		FileWriter fw;
		try {
			fw = new FileWriter(fileName);
		} catch (Exception e) {
			Logger.putFatal("Could not open file " + fileName + "for writing.");
			return false;
		}


		// Define headers for common columns

		String s = "";
		if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_STANDARD)) 			{ s += parameters.commonColsStandardColumnHeader			+ "\t"; }
		if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_ISOTOPEPATTERNID)) 	{ s += parameters.commonColsIsotopePatternIDColumnHeader	+ "\t"; }
		if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_ISOTOPEPEAKNUMBER)) { s += parameters.commonColsIsotopePeakNumberColumnHeader	+ "\t"; }
		if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_CHARGESTATE)) 		{ s += parameters.commonColsChargeStateColumnHeader	+ "\t"; }
		if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_AVERAGEMZ)) 		{ s += parameters.commonColsIsotopeAverageMZColumnHeader	+ "\t"; }
		if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_AVERAGERT)) 		{ s += parameters.commonColsIsotopeAverageRTColumnHeader	+ "\t"; }
		if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_NUMFOUND)) 			{ s += parameters.commonColsNumFoundColumnHeader	+ "\t"; }




		// Define headers for raw data specific columns

		int[] rawDataIDs = alignmentResult.getRawDataIDs();
		for (int rawDataID : rawDataIDs) {


			if (alignmentResult.isImported()) {

				if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_MZ)) 		{ s += alignmentResult.getImportedRawDataName(rawDataID) + ": " + parameters.rawDataColsMZColumnHeader		+ "\t"; }
				if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_RT)) 		{ s += alignmentResult.getImportedRawDataName(rawDataID) + ": " + parameters.rawDataColsRTColumnHeader		+ "\t"; }
				if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_HEIGHT)) 	{ s += alignmentResult.getImportedRawDataName(rawDataID) + ": " + parameters.rawDataColsHeightColumnHeader	+ "\t"; }
				if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_AREA)) 		{ s += alignmentResult.getImportedRawDataName(rawDataID) + ": " + parameters.rawDataColsAreaColumnHeader	+ "\t"; }
				if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_STATUS)) 	{ s += alignmentResult.getImportedRawDataName(rawDataID) + ": " + parameters.rawDataColsStatusColumnHeader	+ "\t"; }

			} else {

				/*RawDataAtClient rawData = mainWin.getItemSelector().getRawDataByID(rawDataID);
				if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_MZ)) 		{ s += rawData.getNiceName() + ": " + parameters.rawDataColsMZColumnHeader 		+ "\t"; }
				if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_RT)) 		{ s += rawData.getNiceName() + ": " + parameters.rawDataColsRTColumnHeader 		+ "\t"; }
				if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_HEIGHT)) 	{ s += rawData.getNiceName() + ": " + parameters.rawDataColsHeightColumnHeader 	+ "\t"; }
				if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_AREA)) 		{ s += rawData.getNiceName() + ": " + parameters.rawDataColsAreaColumnHeader 		+ "\t"; }
				if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_STATUS)) 	{ s += rawData.getNiceName() + ": " + parameters.rawDataColsStatusColumnHeader 	+ "\t"; }
*/
			}

		}
		s = s + "\n";




		// Write column headers to file

		try {
			fw.write(s);
		} catch (Exception e) {
			Logger.putFatal("Could not open file " + fileName + "for writing.");
			return false;
		}




		// Loop through alignment rows

		for (int rowInd=0; rowInd<alignmentResult.getNumOfRows(); rowInd++) {

			s = "";

			// Pickup data from common columns

			if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_STANDARD)) 				{ if (alignmentResult.getStandardCompoundFlag(rowInd)) { s += "1\t"; } else { s += "0\t"; } }
			if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_ISOTOPEPATTERNID)) 		{ s += "" + alignmentResult.getIsotopePatternID(rowInd) 	+ "\t"; }
			if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_ISOTOPEPEAKNUMBER)) 	{ s += "" + alignmentResult.getIsotopePeakNumber(rowInd) 	+ "\t"; }
			if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_CHARGESTATE)) 			{ s += alignmentResult.getChargeState(rowInd) 				+ "\t"; }
			if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_AVERAGEMZ)) 			{ s += "" + alignmentResult.getAverageMZ(rowInd) 			+ "\t"; }
			if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_AVERAGERT)) 			{ s += "" + alignmentResult.getAverageRT(rowInd) 			+ "\t"; }
			if (parameters.isSelectedCommonCol(AlignmentResultExporterParameters.COMMONCOLS_NUMFOUND)) {

				// Calculate number of raw data where the status of this row is DETECTED
				int numFound = 0;
				for (Integer rawDataID : rawDataIDs) {
					if (alignmentResult.getPeakStatus(rawDataID.intValue(), rowInd) == AlignmentResult.PEAKSTATUS_DETECTED) { numFound++; }
				}
				s += "" + numFound + "\t";

			}

			// Raw data specific columns

			for (Integer rawDataID : rawDataIDs) {

				int peakStatus = alignmentResult.getPeakStatus(rawDataID.intValue(), rowInd);

				if ((peakStatus == AlignmentResult.PEAKSTATUS_DETECTED) || (peakStatus == AlignmentResult.PEAKSTATUS_ESTIMATED)) {
					if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_MZ)) 		{ s += "" + alignmentResult.getPeakMZ(rawDataID.intValue(), rowInd)		+ "\t"; }
					if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_RT)) 		{ s += "" + alignmentResult.getPeakRT(rawDataID.intValue(), rowInd) 	+ "\t"; }
					if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_HEIGHT)) 	{ s += "" + alignmentResult.getPeakHeight(rawDataID.intValue(), rowInd)	+ "\t"; }
					if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_AREA)) 	{ s += "" + alignmentResult.getPeakArea(rawDataID.intValue(), rowInd)	+ "\t"; }
				} else {
					if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_MZ)) 		{ s += "" + "N/A"	+ "\t"; }
					if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_RT)) 		{ s += "" + "N/A"	+ "\t"; }
					if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_HEIGHT)) 	{ s += "" + "N/A"	+ "\t"; }
					if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_AREA)) 	{ s += "" + "N/A"	+ "\t"; }
				}

				if (parameters.isSelectedRawDataCol(AlignmentResultExporterParameters.RAWDATACOLS_STATUS)) 	{ s += "" + peakStatus + "\t"; }

			}
			s = s + "\n";

			// Write row to the file

			try {
				fw.write(s);
			} catch (Exception e) {
				Logger.putFatal("Could not open file " + fileName + " for writing.");
				return false;
			}
		}


		// Close file

		try {
			fw.close();
		} catch (Exception e) {
			Logger.putFatal("Could not close file " + fileName + "after writing.");
			return false;
		}

		return true;

	}


	/**
	 * Reads an alignment result from tab-delimitted text file
	 * The file must be in format be EXPORTTYPE_WIDE
	 *
	 * @param	filePath	Path to the file (excluding file name)
	 * @param	fileName	Name of the file
	 * @return	Alignment result with data from the file
	 */
	public static AlignmentResult importAlignmentResultFromFile(String filePath, String fileName) {

		String oneLine;
		String oneLine2;


		// Open file for reading

		BufferedReader buffRead;
		try {
			buffRead = new BufferedReader(new FileReader(filePath+fileName));
		} catch (Exception e) {
			return null;
		}



		// Read first line

		try {
			oneLine = buffRead.readLine();
		} catch (Exception e) {
			return null;
		}



		// Count number of columns in file and calculate number of raw data files

		StringTokenizer st = new StringTokenizer(oneLine,"\t");

		// If not possible to determine number of raw data files exactly, then the number of columns must be incorrect for WIDE mode exported alignment result
		if ( (((float)(st.countTokens()-4))/5.0) != java.lang.Math.floor(((float)(st.countTokens()-4))/5.0) ) {
			return null;
		}
		int numOfRawDatas = (int)java.lang.Math.floor(((float)(st.countTokens()-3))/5.0);



		// Skip common columns columns (standard compound, isotope pattern id, isotope peak number and charge state)

		oneLine2 = st.nextToken();
		oneLine2 = st.nextToken();
		oneLine2 = st.nextToken();
		oneLine2 = st.nextToken();



		// Create fake raw data IDs for each raw data in this alignment

		int importedStartingRawDataID = 10000;
		Hashtable<Integer, String> importedRawDataNames = new Hashtable<Integer, String>();
		Vector<Integer> importedRawDataIDs = new Vector<Integer>();
		for (int ri=0; ri<numOfRawDatas; ri++) {

			// Parse name for fake raw data using header of M/Z column

			oneLine2 = st.nextToken();
			StringTokenizer st2 = new StringTokenizer(oneLine2, ":");
			String tmpRawDataName = st2.nextToken();



			// Skip other raw data specific columns (RT, height, area, status)

			oneLine2 = st.nextToken();
			oneLine2 = st.nextToken();
			oneLine2 = st.nextToken();
			oneLine2 = st.nextToken();



			// Give ID to fake raw data

			Integer tmpRawDataID = new Integer(ri+importedStartingRawDataID);
			importedRawDataIDs.add(new Integer(tmpRawDataID));

			importedRawDataNames.put(new Integer(tmpRawDataID), tmpRawDataName);

		}



		// Count number of data rows

		int numOfRows = 0;
		while (true) {

			// Read new row

			try {
				oneLine = buffRead.readLine();
			} catch (Exception e) {
				return null;
			}
			if (oneLine == null) { break; }
			numOfRows++;
		}



		// Reopen file

		try {
			buffRead.close();
			buffRead = new BufferedReader(new FileReader(filePath+fileName));

		} catch (Exception e) {
			return null;
		}



		// Skip column headers on the first line

		try {
			oneLine = buffRead.readLine();
		} catch (Exception e) {
			return null;
		}



		// Initialize arrays for common columns

		boolean[] standardCompounds = new boolean[numOfRows];
		int[] isotopePatternIDs = new int[numOfRows];
		int[] isotopePeakNumbers = new int[numOfRows];
		int[] chargeStates = new int[numOfRows];



		// Initialize hashtables for raw data specific columns

		Hashtable<Integer, int[]> peakStatuses = new Hashtable<Integer, int[]>();
		Hashtable<Integer, int[]> peakIDs = new Hashtable<Integer, int[]>();
		Hashtable<Integer, double[]> peakMZs = new Hashtable<Integer, double[]>();
		Hashtable<Integer, double[]> peakRTs = new Hashtable<Integer, double[]>();
		Hashtable<Integer, double[]> peakHeights = new Hashtable<Integer, double[]>();
		Hashtable<Integer, double[]> peakAreas = new Hashtable<Integer, double[]>();

		for (Integer rawDataID : importedRawDataIDs) {
			peakStatuses.put(rawDataID, new int[numOfRows]);
			peakIDs.put(rawDataID, new int[numOfRows]);
			peakMZs.put(rawDataID, new double[numOfRows]);
			peakRTs.put(rawDataID, new double[numOfRows]);
			peakHeights.put(rawDataID, new double[numOfRows]);
			peakAreas.put(rawDataID, new double[numOfRows]);
		}



		// Start reading the file

		int rowNum=0;
		double tmpDouble;
		int tmpInt;
		while (true) {



			// Read new row

			try {
				oneLine = buffRead.readLine();
			} catch (Exception e) {
				return null;
			}
			if (oneLine == null) { break; }

			st = new StringTokenizer(oneLine,"\t");



			// Processes common columns

			oneLine2 = st.nextToken();	// Standard column
			if (oneLine2.equals("1")) {	standardCompounds[rowNum] = true; } else { standardCompounds[rowNum] = false; }

			oneLine2 = st.nextToken();	// Isotope pattern ID
			try { tmpInt = Integer.parseInt(oneLine2); }
				catch (Exception e) { tmpInt = -1; }
			isotopePatternIDs[rowNum] = tmpInt;

			oneLine2 = st.nextToken();	// Isotope peak number
			try { tmpInt = Integer.parseInt(oneLine2); }
				catch (Exception e) { tmpInt = -1; }
			isotopePeakNumbers[rowNum] = tmpInt;

			oneLine2 = st.nextToken();	// Charge state
			try { tmpInt = Integer.parseInt(oneLine2); }
				catch (Exception e) { tmpInt = -1; }
			chargeStates[rowNum] = tmpInt;



			// Raw data specific columns

			for (Integer rawDataID : importedRawDataIDs) {

				oneLine2 = st.nextToken(); // M/Z
				try { tmpDouble = Double.parseDouble(oneLine2);	}
					catch (Exception e) { tmpDouble = -1; }
				peakMZs.get(rawDataID)[rowNum] = tmpDouble;

				oneLine2 = st.nextToken(); // RT
				try { tmpDouble = Double.parseDouble(oneLine2);	}
					catch (Exception e) { tmpDouble = -1; }
				peakRTs.get(rawDataID)[rowNum] = tmpDouble;

				oneLine2 = st.nextToken(); // height
				try { tmpDouble = Double.parseDouble(oneLine2);	}
					catch (Exception e) { tmpDouble = -1; }
				peakHeights.get(rawDataID)[rowNum] = tmpDouble;

				oneLine2 = st.nextToken(); // area
				try { tmpDouble = Double.parseDouble(oneLine2);	}
					catch (Exception e) { tmpDouble = -1; }
				peakAreas.get(rawDataID)[rowNum] = tmpDouble;

				oneLine2 = st.nextToken();	// status
				try { tmpInt = Integer.parseInt(oneLine2); }
					catch (Exception e) { tmpInt = -1; }
				peakStatuses.get(rawDataID)[rowNum] = tmpInt;

			}

			rowNum++;

		}



		// Close file

		try {
			buffRead.close();
		} catch (Exception e) {
			return null;
		}



		// Create alignment	result

		AlignmentResult nar = new AlignmentResult(	importedRawDataIDs,
													standardCompounds,
													isotopePatternIDs,
													isotopePeakNumbers,
													chargeStates,
													peakStatuses,
													peakIDs,
													peakMZs,
													peakRTs,
													peakHeights,
													peakAreas,
													new String("imported from " + fileName)
									);



		// Set imported flag and raw data names

		nar.setImported(true);
		nar.setImportedRawDataNames(importedRawDataNames);


		return nar;

	}


}