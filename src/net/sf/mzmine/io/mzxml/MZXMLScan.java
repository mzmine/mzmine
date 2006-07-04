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

/**
 *
 */
package net.sf.mzmine.io.mzxml;

import java.util.Date;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import net.iharder.xmlizable.Base64;
import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.util.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
class MZXMLScan extends DefaultHandler implements Scan {

    private int scanNumber;
    private int msLevel;
    private double mzValues[], intensityValues[];
    private double precursorMZ;
    private double retentionTime;
    private double mzRangeMin, mzRangeMax;
    private double basePeakMZ, basePeakIntensity;

    /*
     * Variables used for parsing
     */
    private StringBuffer charBuffer;
    private int peakDataPrecision;
    private boolean lowHighGiven;

    /**
     * Used for parsing XML time values
     */
    private static final Date currentDate = new Date();

    /**
     * Constructor for empty scan that which will be parsed from XML document
     */
    MZXMLScan() {
        charBuffer = new StringBuffer(256);
    }


    /**
     * @return Returns the intensityValues.
     */
    public double[] getIntensityValues() {
        return intensityValues;
    }

    /**
     * @return Returns the mZValues.
     */
    public double[] getMZValues() {
        return mzValues;
    }

    /**
     * @see net.sf.mzmine.interfaces.Scan#getNumberOfDataPoints()
     */
    public int getNumberOfDataPoints() {
        return mzValues.length;
    }

    /**
     * @see net.sf.mzmine.interfaces.Scan#getScanNumber()
     */
    public int getScanNumber() {
        return scanNumber;
    }

    /**
     * @see net.sf.mzmine.interfaces.Scan#getMSLevel()
     */
    public int getMSLevel() {
        return msLevel;
    }

    /**
     * @see net.sf.mzmine.interfaces.Scan#getPrecursorMZ()
     */
    public double getPrecursorMZ() {
        return precursorMZ;
    }

    /**
     * @see net.sf.mzmine.interfaces.Scan#getScanAcquisitionTime()
     */
    public double getRetentionTime() {
        return retentionTime;
    }

    /**
     * @see net.sf.mzmine.interfaces.Scan#getMZRangeMin()
     */
    public double getMZRangeMin() {
        return mzRangeMin;
    }

    /**
     * @see net.sf.mzmine.interfaces.Scan#getMZRangeMax()
     */
    public double getMZRangeMax() {
        return mzRangeMax;
    }

    /**
     * @see net.sf.mzmine.interfaces.Scan#getBasePeakMZ()
     */
    public double getBasePeakMZ() {
        return basePeakMZ;
    }

    /**
     * @see net.sf.mzmine.interfaces.Scan#getBasePeakIntensity()
     */
    public double getBasePeakIntensity() {
        return basePeakIntensity;
    }

    /**
     * @see net.sf.mzmine.interfaces.Scan#isCentroided()
     */
    public boolean isCentroided() {
		return false;
	}

    public void startElement(String namespaceURI, String lName, // local name
            String qName, // qualified name
            Attributes attrs) throws SAXException {

        charBuffer.setLength(0);

        // <scan>
        if (qName.equalsIgnoreCase("scan")) {

            /*
             * check if we have already read the scan data, in such case this
             * <scan> is inner scan and we want to ignore it
             */
            if (mzValues != null) {
                // free memory
                charBuffer = null;
                // Stop parsing by throwing an exception
                throw (new SAXException("Scan reading finished"));
            }

            // Get number of the scan
            scanNumber = Integer.parseInt(attrs.getValue("num"));
            msLevel = Integer.parseInt(attrs.getValue("msLevel"));

            /* Allocate memory for data */
            int peaksCount = Integer.parseInt(attrs.getValue("peaksCount"));
            
            /* Workaround for a case of empty scan */
            if (peaksCount <= 0) peaksCount = 1;

            mzValues = new double[peaksCount];
            intensityValues = new double[peaksCount];

            String retentionTimeStr = attrs.getValue("retentionTime");
            if (retentionTimeStr != null) {
                // Create a DataTypeFactory for converting XML datetypes to
                // seconds
                DatatypeFactory dataTypeFactory;
                try {
                    dataTypeFactory = DatatypeFactory.newInstance();
                    Duration dur = dataTypeFactory
                            .newDuration(retentionTimeStr);
                    retentionTime = dur.getTimeInMillis(currentDate) / 1000.0;
                } catch (Exception e) {
                    Logger.put(e.toString());
                    throw (new SAXException(
                            "Could not instantiate DatatypeFactory"));

                }
            }

            // Set MZ range minimum and maximum parameters if available
            String scanLowMZStr = attrs.getValue("lowMz");
            if (scanLowMZStr == null)
                scanLowMZStr = attrs.getValue("startMz");

            String scanHighMZStr = attrs.getValue("highMz");
            if (scanHighMZStr == null)
                scanHighMZStr = attrs.getValue("endMz");

            lowHighGiven = false;
            if ((scanLowMZStr != null) && (scanHighMZStr != null)) {
                lowHighGiven = true;
                try {
                    mzRangeMin = Double.parseDouble(scanLowMZStr);
                    mzRangeMax = Double.parseDouble(scanHighMZStr);

                } catch (NumberFormatException e) {
                    Logger.put("Can't interpret scan lowest/highest mz value");
                    lowHighGiven = false;
                }

            }

        }

        // <peaks>
        if (qName.equalsIgnoreCase("peaks")) {
            // Get precision of peak data
            peakDataPrecision = Integer.parseInt(attrs.getValue("precision"));
        }

    }

    /**
     * endElement()
     */
    public void endElement(String namespaceURI, String sName, // simple name
            String qName // qualified name
    ) throws SAXException {

        // </scan>
        if (qName.equalsIgnoreCase("scan")) {

            // free memory
            charBuffer = null;
            // Stop parsing by throwing an exception
            throw (new SAXException("Scan reading finished"));
        }

        // </precursorMz>
        if (qName.equals("precursorMz")) {
            precursorMZ = Double.parseDouble(charBuffer.toString());
        }

        // </peaks>
        if (qName.equals("peaks")) {

            // Decode base64 data

            byte[] tmpArr = Base64.decode(charBuffer.toString());

            int floatBytes = peakDataPrecision / 8;

            int peakIndex = 0;
            int fieldIndex = 0;

            for (int i = 0; i <= tmpArr.length - floatBytes; i += floatBytes) {

                int intBits = 0;
                intBits |= (((int) tmpArr[i]) & 0xff);
                intBits <<= 8;
                intBits |= (((int) tmpArr[i + 1]) & 0xff);
                intBits <<= 8;
                intBits |= (((int) tmpArr[i + 2]) & 0xff);
                intBits <<= 8;
                intBits |= (((int) tmpArr[i + 3]) & 0xff);
                // Must be in IEEE 754 encoding!
                switch (fieldIndex) {

                case 0:

                    // mass values
                    mzValues[peakIndex] = (double) (Float
                            .intBitsToFloat(intBits));
                    break;

                case 1:
                    // intensity values
                    intensityValues[peakIndex] = (double) (Float
                            .intBitsToFloat(intBits));
                    break;

                }

                fieldIndex++;

                if (fieldIndex == 2) {
                    fieldIndex = 0;
                    peakIndex++;
                }

            }

        }

        // If lowMZ and highMZ were not defined as attributes, we must pick
        // values from MZ data points
        if ((!lowHighGiven) && (mzValues.length > 0)) {
            mzRangeMin = mzValues[0];
            mzRangeMax = mzValues[mzValues.length - 1];
        }

        // find the base peak ourselves (the "basePeakIntensity" cannot be
        // trusted)
        for (int i = 0; i < intensityValues.length; i++) {
            if (intensityValues[i] > basePeakIntensity) {
                basePeakIntensity = intensityValues[i];
                basePeakMZ = mzValues[i];
            }
        }

    }

    /**
     * characters()
     */
    public void characters(char buf[], int offset, int len) throws SAXException {
        charBuffer = charBuffer.append(buf, offset, len);
    }


    /**
     * @see net.sf.mzmine.interfaces.Scan#getParentScanNumber()
     */
    public int getParentScanNumber() {
        
        // TODO: temporary for testing
        if (scanNumber > 100) return scanNumber - 5;

        return 0;
    }


    /**
     * @see net.sf.mzmine.interfaces.Scan#getFragmentScanNumbers()
     */
    public int[] getFragmentScanNumbers() {

        // TODO: temporary for testing
        if (scanNumber > 500) return new int[] { scanNumber + 1, scanNumber + 2, scanNumber + 3 };
        return null;
    }

}
