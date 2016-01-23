/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.rawdataimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * Detector of raw data file format
 */
public class RawDataFileTypeDetector {

    /*
     * See
     * "http://www.unidata.ucar.edu/software/netcdf/docs/netcdf/File-Format-Specification.html"
     */
    private static final String CDF_HEADER = "CDF";

    /*
     * mzML files with index start with <indexedmzML><mzML>tags, but files with
     * no index contain only the <mzML> tag. See
     * "http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/psi-ms/mzML/schema/mzML1.1.0.xsd"
     */
    private static final String MZML_HEADER = "<mzML";

    /*
     * mzXML files with index start with <mzXML><msRun> tags, but files with no
     * index contain only the <msRun> tag. See
     * "http://sashimi.sourceforge.net/schema_revision/mzXML_3.2/mzXML_3.2.xsd"
     */
    private static final String MZXML_HEADER = "<msRun";

    // See "http://www.psidev.info/sites/default/files/mzdata.xsd.txt"
    private static final String MZDATA_HEADER = "<mzData";

    // See "https://code.google.com/p/unfinnigan/wiki/FileHeader"
    private static final String THERMO_HEADER = String
            .valueOf(new char[] { 0x01, 0xA1, 'F', 0, 'i', 0, 'n', 0, 'n', 0,
                    'i', 0, 'g', 0, 'a', 0, 'n', 0 });

    private static final String GZIP_HEADER = String
            .valueOf(new char[] { 0x1f, 0x8b });

    private static final String ZIP_HEADER = String
            .valueOf(new char[] { 'P', 'K', 0x03, 0x04 });

    /**
     * 
     * @return Detected file type or null if the file is not of any supported
     *         type
     */
    public static RawDataFileType detectDataFileType(File fileName) {

        if (fileName.isDirectory()) {
            // To check for Waters .raw directory, we look for _FUNC[0-9]{3}.DAT
            for (File f : fileName.listFiles()) {
                if (f.isFile() && f.getName().toUpperCase()
                        .matches("_FUNC[0-9]{3}.DAT"))
                    return RawDataFileType.WATERS_RAW;
            }
            // We don't recognize any other directory type than Waters
            return null;
        }

        if (fileName.getName().toLowerCase().endsWith(".csv")) {
            return RawDataFileType.AGILENT_CSV;
        }

        try {

            // Read the first 1kB of the file into a String
            InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(fileName), "ISO-8859-1");
            char buffer[] = new char[1024];
            reader.read(buffer);
            reader.close();
            String fileHeader = new String(buffer);

            if (fileHeader.startsWith(THERMO_HEADER)) {
                return RawDataFileType.THERMO_RAW;
            }

            if (fileHeader.startsWith(GZIP_HEADER)) {
                return RawDataFileType.GZIP;
            }

            if (fileHeader.startsWith(ZIP_HEADER)) {
                return RawDataFileType.ZIP;
            }

            if (fileHeader.startsWith(CDF_HEADER)) {
                return RawDataFileType.NETCDF;
            }

            if (fileHeader.contains(MZML_HEADER))
                return RawDataFileType.MZML;

            if (fileHeader.contains(MZDATA_HEADER))
                return RawDataFileType.MZDATA;

            if (fileHeader.contains(MZXML_HEADER))
                return RawDataFileType.MZXML;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

}
