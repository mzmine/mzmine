/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.io.rawdataimport.fileformats;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ScanUtils;

import org.jfree.xml.util.Base64;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class read 1.04 and 1.05 MZDATA files.
 */
public class MzDataReadTask extends DefaultHandler implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private File originalFile;
    private RawDataFileWriter newMZmineFile;
    private TaskStatus status;
    private int totalScans = -1;
    private int parsedScans;
    private int peaksCount = 0;
    private String errorMessage;
    private StringBuilder charBuffer;
    private Boolean precursorFlag = false;
    private Boolean spectrumInstrumentFlag = false;
    private Boolean mzArrayBinaryFlag = false;
    private Boolean intenArrayBinaryFlag = false;
    private String precision;
    private String endian;
    private int scanNumber;
    private int msLevel;
    private int parentScan;
    private double retentionTime;
    private double precursorMz;

    /*
     * The information of "m/z" & "int" is content in two arrays because the
     * mzData standard manages this information in two different tags.
     */
    private double[] mzDataPoints = new double[0];
    private double[] intensityDataPoints = new double[0];

    /*
     * This variable hold the current scan or fragment, it is send to the stack
     * when another scan/fragment appears as a parser.startElement
     */
    private SimpleScan buildingScan;

    /*
     * This stack stores at most 10 consecutive scans. This window serves to
     * find possible fragments (current scan) that belongs to any of the stored
     * scans in the stack. The reason of the size follows the concept of
     * neighborhood of scans and all his fragments. These solution is
     * implemented because exists the possibility to find fragments of one scan
     * after one or more full scans. The file
     * myo_full_1.05cv.mzdata/myo_full_1.04cv.mzdata, provided by Proteomics
     * Standards Initiative as example, shows this condition in the order of
     * scans and fragments.
     * 
     * http://sourceforge.net/projects/psidev/
     */
    private LinkedList<SimpleScan> parentStack;

    public MzDataReadTask(File fileToOpen) {
        originalFile = fileToOpen;
        status = TaskStatus.WAITING;
        charBuffer = new StringBuilder(2048);
        parentStack = new LinkedList<SimpleScan>();
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Opening file " + originalFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;
        logger.info("Started parsing file " + originalFile);

        // Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();

        try {

            newMZmineFile = MZmineCore.createNewFile(originalFile.getName());

            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(originalFile, this);

            // Close file
            RawDataFile finalRawDataFile = newMZmineFile.finishWriting();
            MZmineCore.getCurrentProject().addFile(finalRawDataFile);

        } catch (Throwable e) {
            /* we may already have set the status to CANCELED */
            if (status == TaskStatus.PROCESSING) {
                status = TaskStatus.ERROR;
                errorMessage = e.toString();
            }
            return;
        }

        if (parsedScans == 0) {
            status = TaskStatus.ERROR;
            errorMessage = "No scans found";
            return;
        }

        logger.info("Finished parsing " + originalFile + ", parsed "
                + parsedScans + " scans");
        status = TaskStatus.FINISHED;

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        logger.info("Cancelling opening of MZDATA file " + originalFile);
        status = TaskStatus.CANCELED;
    }

    public void startElement(String namespaceURI, String lName, // local name
            String qName, // qualified name
            Attributes attrs) throws SAXException {

        if (status == TaskStatus.CANCELED)
            throw new SAXException("Parsing Cancelled");

        // <spectrumList>
        if (qName.equals("spectrumList")) {
            String s = attrs.getValue("count");
            if (s != null)
                totalScans = Integer.parseInt(s);
        }

        // <spectrum>
        if (qName.equalsIgnoreCase("spectrum")) {
            msLevel = 0;
            retentionTime = 0f;
            parentScan = -1;
            precursorMz = 0f;
            scanNumber = Integer.parseInt(attrs.getValue("id"));
        }

        // <spectrumInstrument> 1.05 version, <acqInstrument> 1.04 version
        if ((qName.equalsIgnoreCase("spectrumInstrument"))
                || (qName.equalsIgnoreCase("acqInstrument"))) {
            msLevel = Integer.parseInt(attrs.getValue("msLevel"));
            spectrumInstrumentFlag = true;
        }

        // <cvParam>
        /*
         * The terms time.min, time.sec & mz belongs to mzData 1.04 standard.
         */
        if (qName.equalsIgnoreCase("cvParam")) {
            if (spectrumInstrumentFlag) {
                if ((attrs.getValue("accession").equals("PSI:1000038"))
                        || (attrs.getValue("name").equals("time.min"))) {
                    retentionTime = Double.parseDouble(attrs.getValue("value")) * 60f;
                }
                if ((attrs.getValue("accession").equals("PSI:1000039"))
                        || (attrs.getValue("name").equals("time.sec"))) {
                    retentionTime = Double.parseDouble(attrs.getValue("value"));
                }
            }
            if (precursorFlag) {
                if ((attrs.getValue("accession").equals("PSI:1000040"))
                        || (attrs.getValue("name").equals("mz"))) {
                    precursorMz = Double.parseDouble(attrs.getValue("value"));
                }
            }
        }

        // <mzArrayBinary>
        if (qName.equalsIgnoreCase("mzArrayBinary")) {
            // clean the current char buffer for the new element
            mzArrayBinaryFlag = true;
        }

        // <intenArrayBinary>
        if (qName.equalsIgnoreCase("intenArrayBinary")) {
            // clean the current char buffer for the new element
            intenArrayBinaryFlag = true;
        }

        // <data>
        if (qName.equalsIgnoreCase("data")) {
            // clean the current char buffer for the new element
            charBuffer.setLength(0);
            if (mzArrayBinaryFlag) {
                endian = attrs.getValue("endian");
                precision = attrs.getValue("precision");
                String len = attrs.getValue("length");
                if (len != null)
                    peaksCount = Integer.parseInt(len);
            }
            if (intenArrayBinaryFlag) {
                endian = attrs.getValue("endian");
                precision = attrs.getValue("precision");
                String len = attrs.getValue("length");
                if (len != null)
                    peaksCount = Integer.parseInt(len);
            }
        }

        // <precursor>
        if (qName.equalsIgnoreCase("precursor")) {
            String parent = attrs.getValue("spectrumRef");
            if (parent != null)
                parentScan = Integer.parseInt(parent);
            else
                parentScan = -1;
            precursorFlag = true;
        }
    }

    /**
     * endElement()
     * 
     * @throws IOException
     */
    public void endElement(String namespaceURI, String sName, // simple name
            String qName // qualified name
    ) throws SAXException {

        // <spectrumInstrument>
        if (qName.equalsIgnoreCase("spectrumInstrument")) {
            spectrumInstrumentFlag = false;
        }

        // <precursor>
        if (qName.equalsIgnoreCase("precursor")) {
            precursorFlag = false;
        }

        // <spectrum>
        if (qName.equalsIgnoreCase("spectrum")) {

            MzDataPoint completeDataPoints[] = new MzDataPoint[peaksCount];
            MzDataPoint tempDataPoints[] = new MzDataPoint[peaksCount];
            spectrumInstrumentFlag = false;

            // Copy m/z and intensity data
            for (int i = 0; i < completeDataPoints.length; i++) {
                completeDataPoints[i] = new SimpleDataPoint(
                        (double) mzDataPoints[i],
                        (double) intensityDataPoints[i]);
            }
            /*
             * This section verifies DataPoints with intensity="0" and exclude
             * them from tempDataPoints array. Only accept some of these points
             * because they are part the left/right part of the peak.
             */

            int i, j;
            for (i = 0, j = 0; i < completeDataPoints.length; i++) {
                double intensity = completeDataPoints[i].getIntensity();
                double mz = completeDataPoints[i].getMZ();
                if (completeDataPoints[i].getIntensity() > 0) {
                    tempDataPoints[j] = new SimpleDataPoint(mz, intensity);
                    j++;
                    continue;
                }
                if ((i > 0) && (completeDataPoints[i - 1].getIntensity() > 0)) {
                    tempDataPoints[j] = new SimpleDataPoint(mz, intensity);
                    j++;
                    continue;
                }
                if ((i < completeDataPoints.length - 1)
                        && (completeDataPoints[i + 1].getIntensity() > 0)) {
                    tempDataPoints[j] = new SimpleDataPoint(mz, intensity);
                    j++;
                    continue;
                }
            }

            // If we have no peaks with intensity of 0, we assume the scan is
            // centroided

            buildingScan = new SimpleScan(null, scanNumber, msLevel,
                    retentionTime, parentScan, precursorMz, null,
                    new MzDataPoint[0], false);

            if (ScanUtils.isCentroided(completeDataPoints)) {
                buildingScan.setCentroided(true);
                buildingScan.setDataPoints(tempDataPoints);
            } else {
                int sizeArray = j;
                MzDataPoint[] dataPoints = new MzDataPoint[j];

                System.arraycopy(tempDataPoints, 0, dataPoints, 0, sizeArray);
                buildingScan.setDataPoints(dataPoints);
            }

            /*
             * Update of fragmentScanNumbers of each Scan in the parentStack
             */

            for (SimpleScan s : parentStack) {
                if (s.getScanNumber() == buildingScan.getParentScanNumber()) {
                    s.addFragmentScan(buildingScan.getScanNumber());
                }
            }

            /*
             * Verify the size of parentStack. The actual size of the window to
             * cover possible candidates for fragmentScanNumber update is 10
             * elements.
             */
            if (parentStack.size() > 10) {
                SimpleScan scan = parentStack.removeLast();
                try {
                    newMZmineFile.addScan(scan);
                } catch (IOException e) {
                    status = TaskStatus.ERROR;
                    errorMessage = "IO error: " + e;
                    throw new SAXException("Parsing cancelled");
                }
                parsedScans++;
            }

            parentStack.addFirst(buildingScan);
            buildingScan = null;

        }

        // <mzArrayBinary>
        if (qName.equalsIgnoreCase("mzArrayBinary")) {

            mzArrayBinaryFlag = false;
            mzDataPoints = new double[peaksCount];

            byte[] peakBytes = Base64.decode(charBuffer.toString().toCharArray());

            ByteBuffer currentMzBytes = ByteBuffer.wrap(peakBytes);

            if (endian.equals("big")) {
                currentMzBytes = currentMzBytes.order(ByteOrder.BIG_ENDIAN);
            } else {
                currentMzBytes = currentMzBytes.order(ByteOrder.LITTLE_ENDIAN);
            }

            try {
                for (int i = 0; i < mzDataPoints.length; i++) {
                    if (precision == null || precision.equals("32"))
                        mzDataPoints[i] = (double) currentMzBytes.getFloat();
                    else
                        mzDataPoints[i] = currentMzBytes.getDouble();
                }
            } catch (Exception eof) {
                status = TaskStatus.ERROR;
                errorMessage = "Corrupt compressed peak";
                throw new SAXException("Parsing Cancelled");
            }
        }

        // <intenArrayBinary>
        if (qName.equalsIgnoreCase("intenArrayBinary")) {

            intenArrayBinaryFlag = false;
            intensityDataPoints = new double[peaksCount];

            byte[] peakBytes = Base64.decode(charBuffer.toString().toCharArray());
            ByteBuffer currentIntensityBytes = ByteBuffer.wrap(peakBytes);

            if (endian.equals("big")) {
                currentIntensityBytes = currentIntensityBytes.order(ByteOrder.BIG_ENDIAN);
            } else {
                currentIntensityBytes = currentIntensityBytes.order(ByteOrder.LITTLE_ENDIAN);
            }

            for (int i = 0; i < intensityDataPoints.length; i++) {
                if (precision == null || precision.equals("32"))
                    intensityDataPoints[i] = (double) currentIntensityBytes.getFloat();
                else
                    intensityDataPoints[i] = currentIntensityBytes.getDouble();
            }
        }
    }

    /**
     * characters()
     * 
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char buf[], int offset, int len) throws SAXException {
        charBuffer = charBuffer.append(buf, offset, len);
    }

    public void endDocument() throws SAXException {
        while (!parentStack.isEmpty()) {
            SimpleScan scan = parentStack.removeLast();
            try {
                newMZmineFile.addScan(scan);
            } catch (IOException e) {
                status = TaskStatus.ERROR;
                errorMessage = "IO error: " + e;
                throw new SAXException("Parsing cancelled");
            }
            parsedScans++;
        }
    }
}
