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
package net.sf.mzmine.rawdatamethods;
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.distributionframework.*;
import net.sf.mzmine.miscellaneous.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;


import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;
import java.util.Date;
import java.util.Hashtable;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import net.iharder.xmlizable.Base64;

public class MZXMLHandlerForRetrieve extends DefaultHandler {

	private File mzXMLFile;
	private Vector<Integer> ms1Scans;		// This vector stores all scan number of MS^1 scans
	private Hashtable<Integer, Long> scanFilePositions;

	private String charBuffer;

	private boolean in_mzXML;
	private boolean in_msRun;
	private boolean in_scan;
	private boolean in_peaks;
	private boolean in_scanIndex;
	private boolean in_offset;

	private int scanIndexID;		// While reading <offset> tag for a scan index, this is used to store ID attribute of the tag
	private Integer lookingForScanNumber;	// mzXML-scan number for scan that we are reading
	private int peakDataPrecision;		// Precision used in representing peak data
	private Scan buildingScan;		// Scan object that we are currently building
	private boolean lowHighGiven;


	public MZXMLHandlerForRetrieve(File _mzXMLFile, Vector<Integer> _ms1Scans, Hashtable<Integer, Long>_scanFilePositions) {
		mzXMLFile = _mzXMLFile;
		ms1Scans = _ms1Scans;
		scanFilePositions = _scanFilePositions;
	}


	public Scan getScan(int scanNumber) {

		//Logger.put("getScan " + scanNumber);

		// Convert MZmine scan numbering (MS^1-scans only) to scan numbering (includes all MS^n scans)
		if (scanNumber>(ms1Scans.size()-1)) { return null; }
		lookingForScanNumber = ms1Scans.get(scanNumber);
		buildingScan = new Scan(scanNumber);

		// Skip mzXML file to right position
		long filePos = scanFilePositions.get(lookingForScanNumber).longValue();

		//Logger.put("Skip mzXML file to position " + filePos);

		FileInputStream fileIN = null;
		try
		{
			fileIN = new FileInputStream(mzXMLFile);
			fileIN.skip(filePos);
		} catch (Exception e)
		{
			Logger.putFatal("ERROR while seeking for scan " + scanNumber + " from mzXML file " + mzXMLFile);
			Logger.putFatal(e.toString());
		}


		// Logger.put("Start parse");

		// Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {

            // Parse the file
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse( new InputSource(fileIN), this);

        } catch (SAXParseException e) {
            // Ignore
            // Logger.put("SAXParseException");
            // Logger.put(e.toString());

        } catch (Exception e) {
			// Logger.put("Exception");
			// Logger.put(e.toString());
			if (!e.getMessage().equals("ScanReadDone")) {
				Logger.putFatal("Couldn't retrieve scan " + lookingForScanNumber + " from file mzXML file = " + mzXMLFile.getPath());
				Logger.putFatal(e.toString());
				return null;
			}
		}

		// Logger.put("Parse done, returning buildingScan");
		return buildingScan;

	}


    //===========================================================
    // SAX DocumentHandler methods
    //===========================================================


	/**
	 * startDocument()
	 */
    public void startDocument() throws SAXException {
    }


	/**
	 * endDocument()
	 */
    public void endDocument() throws SAXException {
    }


	/**
	 * startElement()
	 */
    public void startElement(String namespaceURI,
                             String lName, // local name
                             String qName, // qualified name
                             Attributes attrs)
    throws SAXException
    {

		// Logger.put("startElement " + qName);

		charBuffer = new String();

		// <scan>
		if (qName.equalsIgnoreCase("scan")) {

			// Get number of the scan
			String scanNumStr = attrs.getValue("num");
			int scanNum = Integer.parseInt(scanNumStr);

			// Set MZ range minimum and maximum parameters if available
			String scanLowMZStr;
			scanLowMZStr = attrs.getValue("lowMz");
			if (scanLowMZStr==null) {
				scanLowMZStr = attrs.getValue("startMz");
				if (scanLowMZStr==null) { lowHighGiven = false; }
			}
			double scanLowMZ = 50;
			if (scanLowMZStr!=null) {
				try { scanLowMZ = Double.parseDouble(scanLowMZStr); } catch (NumberFormatException e) { Logger.put("Can't interpret lowMz/startMz value " + scanLowMZStr); }
				lowHighGiven = true;
			} else {
				lowHighGiven = false;
			}

			String scanHighMZStr;
			scanHighMZStr = attrs.getValue("highMz");
			if (scanHighMZStr==null) {
				scanHighMZStr = attrs.getValue("endMz");
				if (scanHighMZStr==null) { lowHighGiven = false; }
			}
			double scanHighMZ = 2000;
			if (scanHighMZStr!=null) {
				try { scanHighMZ = Double.parseDouble(scanHighMZStr); } catch (NumberFormatException e) { Logger.put("Can't interpret highMz/stopMz value " + scanHighMZStr); }
				lowHighGiven = true;
			} else {
				lowHighGiven = false;
			}

			buildingScan.setMZRangeMin(scanLowMZ);
			buildingScan.setMZRangeMax(scanHighMZ);



			// Check that number is right
			if (scanNum != lookingForScanNumber.intValue()) {
				Logger.putFatal("ERROR while reading from mzXML file: Retrieving incorrect scan #" + scanNum + " supposed-to-be #" + lookingForScanNumber);
			}

			in_scan = true;

		}

		// <peaks>
		if (qName.equalsIgnoreCase("peaks")) {
			// Get precision of peak data
			String peakDataPrecisionStr = attrs.getValue("precision");
			peakDataPrecision = Integer.parseInt(peakDataPrecisionStr);

			in_peaks = true;
		}


    }



	/**
	 * endElement()
	 */
    public void endElement(String namespaceURI,
                           String sName, // simple name
                           String qName  // qualified name
                          )
    throws SAXException
    {
		// Logger.put("endElement " + qName);

		// </scan>
		if (qName.equalsIgnoreCase("scan")) {
			in_scan = false;

			// Stop parsing by throwing an exception
			throw (new SAXException("ScanReadDone"));
		}


		// </peaks>
		if (qName.equalsIgnoreCase("peaks")) {
			in_peaks = false;

			// Decode base64 data
			//Logger.put("Calling Base64.decode");
			byte[] tmpArr = Base64.decode(charBuffer);
			//Logger.put("Called Base64.decode");




			int floatBytes = peakDataPrecision / 8;

			if (floatBytes <= 0)
				Logger.putFatal("Problems while reading mzXML file: FLOATBYTES <= 0!!!");


			double[] tmpMZValues = new double[tmpArr.length / floatBytes / 2];
			double[] tmpIntValues = new double[tmpArr.length / floatBytes / 2];
			int peakIndex = 0;
			int fieldIndex = 0;

			/*
			Logger.put("tmpArr.length = " + tmpArr.length);
			Logger.put("floatBytes = " + floatBytes);
			Logger.put("peakDataPrecision = " + peakDataPrecision);
			*/


			if (tmpMZValues.length>0) {

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
					if (fieldIndex == 0) {
						// mass values
						tmpMZValues[peakIndex] = (double)(Float.intBitsToFloat(intBits));
						fieldIndex++;
					} else if (fieldIndex == 1) {
						// intensity values
						tmpIntValues[peakIndex] = (double)(Float.intBitsToFloat(intBits));
						fieldIndex++;
					}

					if (fieldIndex == 2) {
						fieldIndex = 0;
						peakIndex++;
					}

					//Logger.put("i=" + i + " peakIndex="  + peakIndex);

				}
				tmpArr = null;

			}

			// Set m/z and intensity values to Scan

			/*
			Logger.put("Setting MZ and int values to scan");
			Logger.put("Setting MZ and int values to scan");
			Logger.put("xxx tmpIntValues.length = " + tmpIntValues.length);
			*/


			buildingScan.setMZValues(tmpMZValues);
			buildingScan.setIntensityValues(tmpIntValues);



			// If lowMZ and highMZ were not defined as attributes, we must pick values from MZ data points
			if (!lowHighGiven) {
				if (tmpMZValues.length>0) {
					buildingScan.setMZRangeMin(tmpMZValues[0]);
					buildingScan.setMZRangeMax(tmpMZValues[tmpMZValues.length-1]);
				} else {
					buildingScan.setMZRangeMin(0.0);
					buildingScan.setMZRangeMax(0.0);
				}
			}


			tmpMZValues = null;
			tmpIntValues = null;

			//Logger.put("All done, throwing ScanReadDone");

			// Stop parsing by throwing an exception
			throw (new SAXException("ScanReadDone"));

		}


    }


	/**
	 * characters()
	 */
    public void characters(char buf[], int offset, int len)
    throws SAXException
    {

        String s = new String(buf, offset, len);
        charBuffer = charBuffer.concat(new String(buf, offset, len));
        // Logger.put("characters, charBuffer is now: " + charBuffer);

    }





}