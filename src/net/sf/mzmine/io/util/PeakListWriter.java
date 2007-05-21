/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.io.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.OpenedRawDataFile;

/**
 * IO for peak list data
 */
public class PeakListWriter {

    /**
     * Exports peak list to a tab-delimitted text file
     */
    public static void exportPeakListToFile(OpenedRawDataFile rawData,
            File outputfile) throws IOException {

        // Open file
        FileWriter fw = new FileWriter(outputfile);

        // Write column headers

        String s = "\"m/z\"\t\"RT\"\t\"Height\"\t\"Raw Area\"\"Duration\""
                + "\t" + "M/Z diff" + "\t" + "Isotope Pattern Number" + "\t"
                + "Isotope Peak Number" + "\t" + "Charge" + "\t" + "\n";

        fw.write(s);

        // Get peak list
        PeakList peakList = (PeakList) rawData.getCurrentFile().getData(
                PeakList.class)[0];

        // Loop through peaks

        if (peakList != null) {
            Peak[] peaks = peakList.getPeaks();

            for (Peak p : peaks) {

                double mz = p.getMZ();
                double rt = p.getRT();
                double height = p.getHeight();
                double area = p.getArea();

                double duration = p.getMaxRT() - p.getMinRT();
                double mzDiff = p.getMaxMZ() - p.getMinMZ();

                s = "" + mz + "\t" + rt + "\t" + height + "\t" + area + "\t"

                + duration + "\t" + mzDiff + "\t";

                // Is this peak assigned to some isotope pattern?

                if (p instanceof IsotopePattern) {

                    /*
                     * int isotopePatternNumber =
                     * isotopeUtility.getIsotopePatternNumber(isotopePattern);
                     * int isotopePeakNumber =
                     * isotopeUtility.getPeakNumberWithinPattern(p); int charge =
                     * isotopePattern.getChargeState();
                     * 
                     * s += "" + isotopePatternNumber + "\t" + isotopePeakNumber +
                     * "\t" + charge + "\t";
                     */

                } else {

                    // No isotope pattern assigned for the peak

                }

                s += "\n";

                // Write row
                fw.write(s);

            }

        }

        // Close file

        fw.close();

    }

}