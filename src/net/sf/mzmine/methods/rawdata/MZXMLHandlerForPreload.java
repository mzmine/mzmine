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
package net.sf.mzmine.methods.rawdata;
import java.io.File;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.mzmine.util.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



/**
 * This class is used to parse a mzXML file during preload time.
 * Preloading requires determing..
 * - number of scans (MS^1 scans only)
 * - scan times
 * - low and high m/z of file
 * For mzXML-files, also locations of scans are required for faster random access. (scan index)
 */
public class MZXMLHandlerForPreload extends DefaultHandler {

	private Vector<Double> scanTimes;
	private double lowMZ;
	private double highMZ;
	private Vector<Integer> ms1Scans;		// This vector stores all scan number of MS^1 scans
	private Hashtable<Integer, Long> scanFilePositions;

	private String charBuffer;

	private int scanIndexID;		// While reading <offset> tag for a scan index, this is used to store ID attribute of the tag

	private DatatypeFactory dataTypeFactory;
	private Date currentDate;

	private boolean in_mzXML;
	private boolean in_msRun;
	private boolean in_scan;
	private boolean in_peaks;
	private boolean in_scanIndex;
	private boolean in_offset;




	public MZXMLHandlerForPreload() {

		// Create a DataTypeFactory for converting XML datetypes to seconds
		try {
			dataTypeFactory = DatatypeFactory.newInstance();
		} catch (Exception e) {
			Logger.put("Could not instantiate DatatypeFactory");
			Logger.put(e.toString());
		}

		// Get current date which is also required in conversions
		currentDate = new Date();

	}

	/**
	 * Preloads an mzXML file
	 * @return	1=success, -1=failed
	 */
	public int preloadFile(File peakListFile) {

		// Initialize variables
		scanTimes = new Vector<Double>();
		lowMZ = Double.MAX_VALUE;
		highMZ = Double.MIN_VALUE;

		ms1Scans = new Vector<Integer>();
		scanFilePositions = new Hashtable<Integer, Long>();

		// Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {

            // Parse the file
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse( peakListFile, this);

        } catch (Throwable t) {
            t.printStackTrace();
            return -1;
        }

		return 1;

	}


	/**
	 * This method returns starting positions of MS^1 scans in mzXML file
	 * This method returns file offsets to <scan> locations (only for MS^1 scans)
	 */
	public Hashtable<Integer, Long> getScanStartPositions() {
		return scanFilePositions;
	}

	public int getNumberOfScans() {
		return ms1Scans.size();
	}

	/**
	 * This method returns the lowest m/z value of the file
	 */
	public double getLowMZ() {
		return lowMZ;
	}

	/**
	 * This method returns the highest m/z value of the file
	 */
	public double getHighMZ() {
		return highMZ;
	}

	/**
	 * This method returns times (in seconds) of all MS^1 scans
	 */
	public double[] getScanTimes() {
		double[] secs = new double[scanTimes.size()];


		for (int i=0; i<scanTimes.size(); i++) {
			secs[i] = scanTimes.get(i).doubleValue();
		}
		return secs;
	}

	/**
	 * This method returns scan numbers of all MS^1 scans
	 */
	public Vector<Integer> getScanNumbers() {
		return ms1Scans;
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

		charBuffer = new String();

		// <scan>
		if (qName.equalsIgnoreCase("scan")) {

			// Get MS-level of the scan
			int msLevel = Integer.parseInt(attrs.getValue("msLevel"));

			// Only process MS^1 scans
			if (msLevel==1) {

				// Get number of the scan
				String scanNumStr = attrs.getValue("num");
				int scanNum = Integer.parseInt(scanNumStr);

				// Get time of the scan
				String rtStr = attrs.getValue("retentionTime");
				Duration dur = dataTypeFactory.newDuration(rtStr);
				double rt = dur.getTimeInMillis(currentDate) / 1000.0;

				// Try to get low and high MZ of the scan
				boolean lowHighGiven;

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


				// Since lowMz and highMz are optional in mzXML, we must be prepared to interpret the peak information to find them when not given as attributes
				if (!lowHighGiven) {
					// Should interpret peak data and determine real lowMz & highMz, but this is not yet implemented
					Logger.put("lowMz/startMz or highMz/stopMz value is missing from scan");
				}


				if (lowMZ > scanLowMZ) { lowMZ = scanLowMZ; }
				if (highMZ < scanHighMZ) { highMZ = scanHighMZ; }

				scanTimes.add(new Double(rt));
				ms1Scans.add(new Integer(scanNum));

			}

			in_scan = true;

		}

		// <index>
		if (qName.equalsIgnoreCase("index")) {

			String indexName = attrs.getValue("name");
			if (indexName == null) { return; }

			if (indexName.equalsIgnoreCase("scan")) {
				in_scanIndex = true;
			}
		}

		// <offset>
		if (qName.equalsIgnoreCase("offset")) {

			String indexID = attrs.getValue("id");
			if (indexID == null) { scanIndexID=-1; return; }

			scanIndexID = Integer.parseInt(indexID);

			in_offset = true;

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

		// </scan>
		if (qName.equalsIgnoreCase("scan")) {
			in_scan = false;
		}

		// </index>
		if (qName.equalsIgnoreCase("index")) {
			if (in_scanIndex) {	in_scanIndex = false; }
		}

		// </offset>
		if (qName.equalsIgnoreCase("offset")) {
			if (scanIndexID!=-1) {


				// Check if this is MS^1 scan
				if (ms1Scans.indexOf(new Integer(scanIndexID))!=-1) {

					// It is MS^1 scan, put its file offset to hashtable
					Long offsetInt = Long.parseLong(charBuffer);
					scanFilePositions.put(new Integer(scanIndexID), offsetInt);

				} else {}

			}

			in_offset = false;
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

    }



}