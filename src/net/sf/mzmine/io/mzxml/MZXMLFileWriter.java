/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.io.mzxml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Hashtable;

import javax.xml.datatype.Duration;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;

import net.iharder.xmlizable.Base64;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFile.PreloadLevel;
import net.sf.mzmine.io.RawDataFileWriter;
import net.sf.mzmine.util.Logger;

/**
 * This class represent a writer for mzXML files. TODO: Currently writes only
 * minimum amount of information in slightly ugly formatting
 */
public class MZXMLFileWriter implements RawDataFileWriter {

    private DatatypeFactory datatypeFactory;
    private File workingCopy;
    private FileWriter fileWriter;
    private long bytesWritten;
    private long scansWritten;
    private MZXMLFile filteredMZXMLFile;
    private Hashtable<Integer, Long> scanIndex; // Maps scan number to index

    public MZXMLFileWriter(RawDataFile originalFile, File workingCopy,
            PreloadLevel preloadLevel) throws IOException {

        // Create a DataTypeFactory for converting XML datetypes to seconds
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            Logger.putFatal("Could not instantiate DatatypeFactory.");
            throw new IOException("Could not instantiate DatatypeFactory.");
        }

        // Open writer for the new working copy
        fileWriter = new FileWriter(workingCopy);

        bytesWritten = 0;
        scansWritten = 0;
        scanIndex = new Hashtable<Integer, Long>();

        // Write header to XML file
        String s = new String();
        s += "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n";
        s += "<mzXML>\n";
        fileWriter.write(s);
        bytesWritten += s.length();

        // Create a new MZXMLFile
        filteredMZXMLFile = new MZXMLFile(workingCopy, preloadLevel,
                originalFile.getHistory());

    }

    /**
     * Adds a new scan to the file.
     * 
     * @param newScan
     *            Scan to add
     */
    public void addScan(Scan newScan) throws IOException {

        // Record file index of the scan
        scanIndex.put(newScan.getScanNumber(), bytesWritten);

        // Add scan to MZXML file
        filteredMZXMLFile.addIndexEntry(newScan.getScanNumber(), bytesWritten);
        filteredMZXMLFile.addScan(newScan);

        // Encode mass and intensity datapoints using Base64
        double[] mzValues = newScan.getMZValues();
        double[] intValues = newScan.getIntensityValues();
        byte[] tmpArr = new byte[mzValues.length * 4 * 2];
        int tmpArrInd = 0;
        for (int i = 0; i < mzValues.length; i++) {
            double mzVal = mzValues[i];
            int intBits = Float.floatToIntBits((float) (mzVal));
            tmpArr[tmpArrInd++] = (byte) ((intBits & 0xff000000) >> 24);
            tmpArr[tmpArrInd++] = (byte) ((intBits & 0xff0000) >> 16);
            tmpArr[tmpArrInd++] = (byte) ((intBits & 0xff00) >> 8);
            tmpArr[tmpArrInd++] = (byte) ((intBits & 0xff));

            double intVal = intValues[i];
            intBits = Float.floatToIntBits((float) (intVal));
            tmpArr[tmpArrInd++] = (byte) ((intBits & 0xff000000) >> 24);
            tmpArr[tmpArrInd++] = (byte) ((intBits & 0xff0000) >> 16);
            tmpArr[tmpArrInd++] = (byte) ((intBits & 0xff00) >> 8);
            tmpArr[tmpArrInd++] = (byte) ((intBits & 0xff));
        }
        String encodedDatapoints = Base64.encodeBytes(tmpArr);

        // Format scan duration to XML
        Duration dur = datatypeFactory.newDuration((long) (java.lang.Math
                .round(1000 * newScan.getRetentionTime())));

        // Write scan data to file
        String xmlElementString = new String();

        xmlElementString += "<scan num=\"";
        xmlElementString += "" + newScan.getScanNumber() + "\" ";
        xmlElementString += "msLevel=\"1\" ";
        xmlElementString += "lowMz=\"" + newScan.getMZRangeMin() + "\" ";
        xmlElementString += "highMz=\"" + newScan.getMZRangeMax() + "\" ";
        xmlElementString += "peaksCount=\"" + mzValues.length + "\" ";
        xmlElementString += "retentionTime=\"" + dur.toString() + "\">\n";

        xmlElementString += "<peaks precision=\"32\" byteOrder=\"network\" pairOrder=\"m/z-int\">";
        xmlElementString += encodedDatapoints;
        xmlElementString += "</peaks>\n";
        xmlElementString += "</scan>\n";

        fileWriter.write(xmlElementString);

        bytesWritten += xmlElementString.length();

    }

    /**
     * Finishes writing of the file
     * 
     * @return newly written file as RawDataFile
     */
    public RawDataFile finishWriting() throws IOException {

        // TODO: Write scan index
        // Unimplemented: index is not needed in temporary copy because offsets
        // are given to MZXMLFile already during addScan()

        // Write footer to XML file
        String xmlElementString;
        xmlElementString = new String();
        xmlElementString += "</mzXML>\n";

        fileWriter.write(xmlElementString);

        bytesWritten += xmlElementString.length();

        // Close new working copy
        fileWriter.flush();
        fileWriter.close();

        // Trash datatypeFactory
        datatypeFactory = null;

        return filteredMZXMLFile;

    }

}
