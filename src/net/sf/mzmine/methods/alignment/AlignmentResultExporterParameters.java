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

import java.util.HashSet;

import org.xml.sax.Attributes;



/**
 * This class stores parameter settings for AlignmentResultExporter.
 * These parameters define the columns to be included in exported alignment result.
 *
 * @version 30 March 2006
 */
public class AlignmentResultExporterParameters {

	// CONSTANTS FOR STORING PARAMETERS TO XML FILE

	private static final String myTagName = "AlignmentResultExporterParameters";

	private static final String exportTypeAttributeName = "ExportType";

	private static final String commonColsStandardAttributeName = "StandardColumn";
	private static final String commonColsIsotopePatternIDAttributeName = "IsotopePatternIDColumn";
	private static final String commonColsIsotopePeakNumberAttributeName = "IsotopePeakNumberColumn";
	private static final String commonColsChargeStateAttributeName = "ChargeStateColumn";
	private static final String commonColsAverageMZAttributeName = "AverageMZColumn";
	private static final String commonColsAverageRTAttributeName = "AverageRTColumn";
	private static final String commonColsNumFoundAttributeName = "NumFound";

	private static final String rawDataColsMZAttributeName = "PeakMZColumn";
	private static final String rawDataColsRTAttributeName = "PeakRTColumn";
	private static final String rawDataColsHeightAttributeName = "PeakHeightColumn";
	private static final String rawDataColsAreaAttributeName = "PeakAreaColumn";
	private static final String rawDataColsStatusAttributeName = "PeakStatusColumn";



	// CONSTANT FOR WRITING EXPORTED FILE

	public static final String commonColsStandardColumnHeader = "Std";
	public static final String commonColsIsotopePatternIDColumnHeader = "Isotope ID";
	public static final String commonColsIsotopePeakNumberColumnHeader = "Isotope Peak Number";
	public static final String commonColsChargeStateColumnHeader = "Charge State";
	public static final String commonColsIsotopeAverageMZColumnHeader = "Average M/Z";
	public static final String commonColsIsotopeAverageRTColumnHeader = "Average RT";
	public static final String commonColsNumFoundColumnHeader = "Num Found";

	public static final String rawDataColsMZColumnHeader = "M/Z";
	public static final String rawDataColsRTColumnHeader = "RT";
	public static final String rawDataColsHeightColumnHeader = "Height";
	public static final String rawDataColsAreaColumnHeader = "Area";
	public static final String rawDataColsStatusColumnHeader = "Status";



	// CONSTANTS FOR EXPORT TYPE SETTINGS AND ACCESSING COMMON COLUMNS AND RAW DATA SPECIFIC COLUMNS

	// Export type
	public static final Integer EXPORTTYPE_COMPACT = 1;
	public static final Integer EXPORTTYPE_WIDE = 2;
	public static final Integer EXPORTTYPE_CUSTOM = 3;

	// Common columns
	public static final Integer COMMONCOLS_STANDARD = 1;
	public static final Integer COMMONCOLS_ISOTOPEPATTERNID = 2;
	public static final Integer COMMONCOLS_ISOTOPEPEAKNUMBER = 3;
	public static final Integer COMMONCOLS_CHARGESTATE = 4;
	public static final Integer COMMONCOLS_AVERAGEMZ = 5;
	public static final Integer COMMONCOLS_AVERAGERT = 6;
	public static final Integer COMMONCOLS_NUMFOUND = 7;

	// Raw data specific columns
	public static final Integer RAWDATACOLS_MZ = 1;
	public static final Integer RAWDATACOLS_RT = 2 ;
	public static final Integer RAWDATACOLS_HEIGHT = 3;
	public static final Integer RAWDATACOLS_AREA = 4;
	public static final Integer RAWDATACOLS_STATUS = 5;



	// DATA STRUCTURES FOR STORING PARAMETER SETTINGS

	private Integer exportType;
	private HashSet<Integer> selectedCommonCols;
	private HashSet<Integer> selectedRawDataCols;



	/**
	 * Initializes parameter object with COMPACT column selection
	 */
	public AlignmentResultExporterParameters() {

		// Initialize data structures
		selectedCommonCols = new HashSet<Integer>();
		selectedRawDataCols = new HashSet<Integer>();

		// Setup default parameter values
		setExportType(EXPORTTYPE_COMPACT);

	}




	// METHODS FOR CONTROLLING EXPORT TYPE

	/**
	 * Returns the current export type
	 */
	public Integer getExportType() { return exportType; }



	/**
	 * Sets current export type and also selects columns accordingly
	 * @param	generalParameters	Access to general parameters is required because compact mode stores only peak height or area according to current peak measurement setting. If this is null, then height is used for peak measuring.
	 */
	public void setExportType(Integer type) {
		exportType = type;

		if (exportType==EXPORTTYPE_COMPACT) {

			selectedCommonCols.clear();
			addCommonCol(COMMONCOLS_STANDARD);
			addCommonCol(COMMONCOLS_ISOTOPEPATTERNID);
			addCommonCol(COMMONCOLS_ISOTOPEPEAKNUMBER);
			addCommonCol(COMMONCOLS_CHARGESTATE);
			addCommonCol(COMMONCOLS_AVERAGEMZ);
			addCommonCol(COMMONCOLS_AVERAGERT);
			addCommonCol(COMMONCOLS_NUMFOUND);

			selectedRawDataCols.clear();
/*			if (generalParameters==null) {
				addRawDataCol(RAWDATACOLS_HEIGHT);
			} else {
				if (generalParameters.getPeakMeasuringType()==GeneralParameters.PARAMETERVALUE_PEAKMEASURING_HEIGHT) {
					addRawDataCol(RAWDATACOLS_HEIGHT);
				}
				if (generalParameters.getPeakMeasuringType()==GeneralParameters.PARAMETERVALUE_PEAKMEASURING_AREA) {
					addRawDataCol(RAWDATACOLS_AREA);
				}
			}*/
		}

		if (exportType==EXPORTTYPE_WIDE) {

			selectedCommonCols.clear();
			addCommonCol(COMMONCOLS_STANDARD);
			addCommonCol(COMMONCOLS_ISOTOPEPATTERNID);
			addCommonCol(COMMONCOLS_ISOTOPEPEAKNUMBER);
			addCommonCol(COMMONCOLS_CHARGESTATE);

			selectedRawDataCols.clear();
			addRawDataCol(RAWDATACOLS_MZ);
			addRawDataCol(RAWDATACOLS_RT);
			addRawDataCol(RAWDATACOLS_HEIGHT);
			addRawDataCol(RAWDATACOLS_AREA);
			addRawDataCol(RAWDATACOLS_STATUS);

		}

		if (exportType==EXPORTTYPE_CUSTOM) {
			// When switching to custom settings, maintain previous column selections
		}

	}




	// METHODS FOR (UN)SELECTING COLUMNS

	/**
	 * Clears all column selections
	 */
	public void clearAllSelections() {
		selectedCommonCols.clear();
		selectedRawDataCols.clear();
	}



	/**
	 * Selects a common column
	 */
	public void addCommonCol(Integer selection) { selectedCommonCols.add(selection); }

	/**
	 * Unselects a common column
	 */
	public void removeCommonCol(Integer selection) { selectedCommonCols.remove(selection); }

	/**
	 * Checks if a common column is currently selected
	 */
	public boolean isSelectedCommonCol(Integer selection) { return selectedCommonCols.contains(selection); }



	/**
	 * Selects a raw data specific column
	 */
	public void addRawDataCol(Integer selection) { selectedRawDataCols.add(selection); }

	/**
	 * Unselects a raw data specific column
	 */
	public void removeRawDataCol(Integer selection) { selectedRawDataCols.remove(selection); }

	/**
	 * Checks if a raw data specific column is currently selected
	 */
	public boolean isSelectedRawDataCol(Integer selection) { return selectedRawDataCols.contains(selection); }




	// METHODS FOR WRITING/READING PARAMETERS FROM/TO XML FILE

	/**
	 * Returns a string containing XML tag with all parameter values
	 */
	public String writeParameterTag() {
		String s = "<";
		s = s.concat(myTagName);

		s = s.concat(" " + exportTypeAttributeName + "=\"" + exportType + "\"");

		if (isSelectedCommonCol(COMMONCOLS_STANDARD)) { s = s.concat(" " + commonColsStandardAttributeName + "=\"1\"" ); }
		if (isSelectedCommonCol(COMMONCOLS_ISOTOPEPATTERNID)) { s = s.concat(" " + commonColsIsotopePatternIDAttributeName + "=\"1\""); }
		if (isSelectedCommonCol(COMMONCOLS_ISOTOPEPEAKNUMBER)) { s = s.concat(" " + commonColsIsotopePeakNumberAttributeName + "=\"1\""); }
		if (isSelectedCommonCol(COMMONCOLS_CHARGESTATE)) { s = s.concat(" " + commonColsChargeStateAttributeName + "=\"1\""); }
		if (isSelectedCommonCol(COMMONCOLS_AVERAGEMZ)) { s = s.concat(" " + commonColsAverageMZAttributeName + "=\"1\""); }
		if (isSelectedCommonCol(COMMONCOLS_AVERAGERT)) { s = s.concat(" " + commonColsAverageRTAttributeName + "=\"1\""); }
		if (isSelectedCommonCol(COMMONCOLS_NUMFOUND)) { s = s.concat(" " + commonColsNumFoundAttributeName + "=\"1\""); }

		if (isSelectedRawDataCol(RAWDATACOLS_MZ)) { s = s.concat(" " + rawDataColsMZAttributeName + "=\"1\""); }
		if (isSelectedRawDataCol(RAWDATACOLS_RT)) { s = s.concat(" " + rawDataColsRTAttributeName + "=\"1\""); }
		if (isSelectedRawDataCol(RAWDATACOLS_HEIGHT)) { s = s.concat(" " + rawDataColsHeightAttributeName + "=\"1\""); }
		if (isSelectedRawDataCol(RAWDATACOLS_AREA)) { s = s.concat(" " + rawDataColsAreaAttributeName + "=\"1\""); }
		if (isSelectedRawDataCol(RAWDATACOLS_STATUS)) { s = s.concat(" " + rawDataColsStatusAttributeName + "=\"1\""); }

		s = s.concat("/>");
		return s;
	}



	/**
	 * Returns the name of the parameter tag used by this class
	 */
	public String getParameterTagName() { return myTagName; }



	/**
	 * Interprets XML attributes and sets the parameter values accordingly.
	 */
	public boolean loadXMLAttributes(Attributes atr) {


		try { exportType = Integer.parseInt(atr.getValue(exportTypeAttributeName)); } catch (NumberFormatException e) {	return false; }

		clearAllSelections();

		if (atr.getValue(commonColsStandardAttributeName)!=null) 			{ addCommonCol(COMMONCOLS_STANDARD); }
		if (atr.getValue(commonColsIsotopePatternIDAttributeName)!=null)	{ addCommonCol(COMMONCOLS_ISOTOPEPATTERNID); }
		if (atr.getValue(commonColsIsotopePeakNumberAttributeName)!=null)	{ addCommonCol(COMMONCOLS_ISOTOPEPEAKNUMBER); }
		if (atr.getValue(commonColsChargeStateAttributeName)!=null)			{ addCommonCol(COMMONCOLS_CHARGESTATE); }
		if (atr.getValue(commonColsAverageMZAttributeName)!=null)			{ addCommonCol(COMMONCOLS_AVERAGEMZ); }
		if (atr.getValue(commonColsAverageRTAttributeName)!=null)			{ addCommonCol(COMMONCOLS_AVERAGERT); }
		if (atr.getValue(commonColsNumFoundAttributeName)!=null)			{ addCommonCol(COMMONCOLS_NUMFOUND); }

		if (atr.getValue(rawDataColsMZAttributeName)!=null) 	{ addRawDataCol(RAWDATACOLS_MZ); }
		if (atr.getValue(rawDataColsRTAttributeName)!=null) 	{ addRawDataCol(RAWDATACOLS_RT); }
		if (atr.getValue(rawDataColsHeightAttributeName)!=null) { addRawDataCol(RAWDATACOLS_HEIGHT); }
		if (atr.getValue(rawDataColsAreaAttributeName)!=null) 	{ addRawDataCol(RAWDATACOLS_AREA); }
		if (atr.getValue(rawDataColsStatusAttributeName)!=null) { addRawDataCol(RAWDATACOLS_STATUS); }

		return true;
	}


}
