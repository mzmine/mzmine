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

package net.sf.mzmine.alignmentresultmethods;

import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.distributionframework.*;
import net.sf.mzmine.miscellaneous.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;


import java.util.Enumeration;
import java.io.FileWriter;


public class PeakListExporter {

	/**
	 * Exports peak data to a text file
	 */
	public static boolean writePeakListToFile(RawDataAtClient rawData, String fname) {

		double mz,rt,height,area, duration, stdevMZ;
		int charge, isotopePatternID, isotopePeakNumber;

		// Open file

		FileWriter fw;
		try {
			fw = new FileWriter(fname);
		} catch (Exception e) {
			return false;
		}


		// Write column headers

		String s = "" 	+ "M/Z" + "\t"
						+ "RT" + "\t"
						+ "Height" + "\t"
						+ "Area" + "\t"
						+ "Charge" + "\t"
						+ "Isotope Pattern ID" + "\t"
						+ "Isotope Peak Number" + "\t"
						+ "Duration" + "\t"
						+ "M/Z stdev" + "\n";
		try {
			fw.write(s);
		} catch (Exception e) {
			return false;
		}



		// Loop through peaks

		Enumeration<Peak> pe = rawData.getPeakList().getPeaks().elements();
		Peak p;

		while (pe.hasMoreElements()) {
			p = pe.nextElement();

			mz = p.getMZ();
			rt = p.getRT();
			height = p.getHeight();
			area = p.getArea();
			charge = p.getChargeState();
			isotopePatternID = p.getIsotopePatternID();
			isotopePeakNumber = p.getIsotopePeakNumber();
			duration = rawData.getScanTime(p.getStopScanNumber()) - rawData.getScanTime(p.getStartScanNumber());
			stdevMZ = p.getMZStdev();

			s = ""	+ mz + "\t"
					+ rt + "\t"
					+ height + "\t"
					+ area + "\t"
					+ charge + "\t"
					+ isotopePatternID + "\t"
					+ isotopePeakNumber + "\t"
					+ duration + "\t"
					+ stdevMZ + "\n";

			try {
				fw.write(s);
			} catch (Exception e) {
				return false;
			}
		}

		// Close file

		try {
			fw.close();
		} catch (Exception e) {
			return false;
		}

		return true;

	}

}