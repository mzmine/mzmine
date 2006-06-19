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

package net.sf.mzmine.io;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.MZmineProject;
import net.sf.mzmine.interfaces.IsotopePattern;
import net.sf.mzmine.interfaces.CompoundIdentity;
import net.sf.mzmine.interfaces.Peak;
import net.sf.mzmine.interfaces.PeakList;
import net.sf.mzmine.util.IsotopePatternGrouper;




/**
 * IO for peak list data
 */
public class PeakListWriter {

	/**
	 * Exports peak list to a tab-delimitted text file
	 */
	public static void exportPeakListToFile(RawDataFile rawData, File outputfile) throws IOException {


		// Open file
		FileWriter fw = new FileWriter(outputfile);

		// Write column headers

		String s = "" 	+ "M/Z" + "\t"
						+ "RT" + "\t"
						+ "Raw Height" + "\t"
						+ "Raw Area" + "\t"

						+ "Normalized M/Z" + "\t"
						+ "Normalized RT" + "\t"
						+ "Normalized Height" + "\t"
						+ "Normalized Area" + "\t"

						+ "Duration" + "\t"
						+ "M/Z diff" + "\t"

						+ "Isotope Pattern Number" + "\t"
						+ "Charge" + "\t"

						+ "Compound" + "\t"

						+ "\n";

		fw.write(s);

		// Get peak list
		MZmineProject proj = MZmineProject.getCurrentProject();
		PeakList peakList = proj.getPeakList(rawData);

		// Group peaks by their isotope pattern
		IsotopePatternGrouper isotopeGrouper = new IsotopePatternGrouper(peakList);


		// Loop through peaks

		if (peakList!=null) {
			Peak[] peaks = peakList.getPeaks();

			for (Peak p : peaks) {

				double mz = p.getMZ();
				double rt = p.getRT();
				double height = p.getRawHeight();
				double area = p.getRawArea();

				double normalizedMZ = p.getNormalizedMZ();
				double normalizedRT = p.getNormalizedRT();
				double normalizedHeight = p.getNormalizedHeight();
				double normalizedArea = p.getNormalizedArea();

				double duration = p.getMaxRT() - p.getMinRT();
				double mzDiff = p.getMaxMZ() - p.getMinMZ();

				s = ""	+ mz + "\t"
						+ rt + "\t"
						+ height + "\t"
						+ area + "\t"

						+ normalizedMZ + "\t"
						+ normalizedRT + "\t"
						+ normalizedHeight + "\t"
						+ normalizedArea + "\t"

						+ duration + "\t"
						+ mzDiff + "\t";


				// Is this peak assigned to some isotope pattern?
				IsotopePattern isotopePattern = p.getIsotopePattern();
				if (isotopePattern!=null) {

					int isotopePatternNumber =  isotopeGrouper.getIsotopePatternNumber(isotopePattern);
					int charge = isotopePattern.getChargeState();

					s += "" + isotopePatternNumber + "\t"
							+ charge + "\t";

					// Has isotope pattern got an identification result?
					CompoundIdentity identity = isotopePattern.getIdentity();
					if (identity!=null) {
						s += "" + identity.getCompoundName() + "\t";
					} else {
						s += "" + "N/A" + "\t";
					}

				} else {

					// No isotope pattern assigned for the peak
					s += "" + "N/A" + "\t"
							+ "N/A" + "\t";

					s += "" + "N/A" + "\t";
				}

				s += "\n";

				// Write row
				fw.write(s);

			}

		}

		// Close file

		fw.close();

	}


	public Element addToXML(Document doc) {
		// TODO
		return null;
	}

	public void readFromXML(Element element) {
		// TODO
	}

}