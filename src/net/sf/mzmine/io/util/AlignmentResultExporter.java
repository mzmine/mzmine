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


package net.sf.mzmine.io.util;


import java.io.FileWriter;
import java.io.File;
import java.util.logging.Logger;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.AlignmentResultRow;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.Peak.PeakStatus;
import net.sf.mzmine.data.impl.StandardCompoundFlag;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.methods.deisotoping.util.IsotopePatternUtility;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.AlignmentResultColumnSelection;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.AlignmentResultColumnSelection.CommonColumnType;
import net.sf.mzmine.userinterface.dialogs.alignmentresultcolumnselection.AlignmentResultColumnSelection.RawDataColumnType;


public class AlignmentResultExporter {


	private Logger logger = Logger.getLogger(this.getClass().getName());

	
	/**
	 * Writes an alignment result to tab-delimitted text file.
	 *
	 * @param	alignmentResult		The alignment result to be exported
	 * @param	outputFile			Output file
	 * @param	parameters			User-definable parameters (defines which columns to export)
	 *
	 * @return	true if successfully exported, otherwise false
	 */
	public boolean exportToFile(AlignmentResult alignmentResult, File outputFile, AlignmentResultColumnSelection columnSelection) {

		IsotopePatternUtility isoUtil = new IsotopePatternUtility(alignmentResult);
		
		// Open file
		FileWriter fw;
		try {
			fw = new FileWriter(outputFile);
		} catch (Exception e) {
			logger.warning("Could not open file " + outputFile + " for writing.");
			return false;
		}


		// Write column headers
		
		CommonColumnType[] selectedCommonColumns = columnSelection.getSelectedCommonColumns();
		String s = "";
		for (CommonColumnType c : selectedCommonColumns) {
			s += c.getColumnName() + "\t";
		}
		
		RawDataColumnType[] selectedRawDataColumns = columnSelection.getSelectedRawDataColumns();
		for (OpenedRawDataFile rawData : alignmentResult.getRawDataFiles()) {
			for (RawDataColumnType c : selectedRawDataColumns) {
				s += rawData.toString() + ": " + c.getColumnName() + "\t"; 
			}
		}
		
		s = s + "\n";

		try {
			fw.write(s);
		} catch (Exception e) {
			logger.warning("Could not write to file " + outputFile);
			return false;
		}

		// Write data rows
		
		int row=0;
		for (AlignmentResultRow alignmentRow : alignmentResult.getRows()) {
			
			s = "";
			
			// Pickup data from common columns
			selectedCommonColumns = columnSelection.getSelectedCommonColumns();
			for (CommonColumnType c : selectedCommonColumns) {
				switch (c) {
				case STDCOMPOUND:
					if (alignmentRow.hasData(StandardCompoundFlag.class)) s += "1\t"; else s+="0\t";
					break;
				case ROWNUM:
					s += "" + (row+1) + "\t";
					break;
				case AVGMZ:
					s += "" + alignmentRow.getAverageMZ() + "\t";
					break;
				case AVGRT:
					s += "" + alignmentRow.getAverageRT() + "\t";
					break;
				case ISOTOPEID:
					IsotopePattern isoPatt = (IsotopePattern)alignmentRow.getLastData(IsotopePattern.class);
					s += "" + isoUtil.getIsotopePatternNumber(isoPatt) + "\t";
					break;
				case ISOTOPEPEAK:
					isoPatt = (IsotopePattern)alignmentRow.getLastData(IsotopePattern.class);
					s += "" + isoUtil.getIsotopePatternNumber(isoPatt) + "\t";
					break;
				case CHARGE:
					isoPatt = (IsotopePattern)alignmentRow.getLastData(IsotopePattern.class);
					s += "" + isoPatt.getChargeState() + "\t";
					break;
				}
			}
			
			// Loop through raw data files
			for (OpenedRawDataFile rawData : alignmentResult.getRawDataFiles()) {
				Peak p = alignmentRow.getPeak(rawData);
				
				selectedRawDataColumns = columnSelection.getSelectedRawDataColumns();
				for (RawDataColumnType c : selectedRawDataColumns) {
					
					if (p==null) { s+= "" + "N/A" + "\t"; }
					else {
						switch(c) {
						case MZ:
							s += "" + p.getNormalizedMZ() + "\t";
							break;
						case RT:
							s += "" + p.getNormalizedRT() + "\t";
							break;
						case HEIGHT:
							s += "" + p.getNormalizedHeight() + "\t";
							break;
						case AREA:
							s += "" + p.getNormalizedArea() + "\t";
							break;
						case STATUS:
							PeakStatus ps = p.getPeakStatus();
							if (ps == null) { s+= "" + "N/A" + "\t"; }
							else {
								// TODO: Numerical values for DETECTED and ESTIMATED are hard coded here
								switch(ps) {
								case DETECTED:
									s += "1\t";
									break;
								case ESTIMATED:
									s += "2\t";
									break;
								}
							}
							break;
						}
						
					}
					
				}
				
			}
			
			s = s + "\n";

			try {
				fw.write(s);
			} catch (Exception e) {
				logger.warning("Could not write to file " + outputFile);
				return false;
			}
			
			row++;
			
		}
		
		// Close file

		try {
			fw.close();
		} catch (Exception e) {
			logger.warning("Could not close file " + outputFile);
			return false;
		}		

		return true;

	}


}