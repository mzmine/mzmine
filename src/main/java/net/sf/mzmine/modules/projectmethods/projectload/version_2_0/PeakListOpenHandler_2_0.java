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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.projectmethods.projectload.version_2_0;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.projectmethods.projectload.PeakListOpenHandler;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.Ostermiller.util.Base64;
import com.google.common.collect.Range;

public class PeakListOpenHandler_2_0 extends DefaultHandler implements
	PeakListOpenHandler {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private SimplePeakListRow buildingRow;
    private SimplePeakList buildingPeakList;

    private int numOfMZpeaks, representativeScan, fragmentScan;
    private String peakColumnID;
    private double mass, rt, area;
    private int[] scanNumbers;
    private double height;
    private double[] masses, intensities;
    private String peakStatus, peakListName, name, identityPropertyName,
	    rawDataFileID;
    private Hashtable<String, String> identityProperties;
    private boolean preferred;
    private String dateCreated;

    private StringBuffer charBuffer;

    private Vector<String> appliedMethods, appliedMethodParameters;
    private Vector<RawDataFile> currentPeakListDataFiles;

    private Vector<DataPoint> currentIsotopes;
    private IsotopePatternStatus currentIsotopePatternStatus;
    private int currentPeakCharge;
    private String currentIsotopePatternDescription;

    private Hashtable<String, RawDataFile> dataFilesIDMap;

    private int parsedRows, totalRows;

    private boolean canceled = false;

    public PeakListOpenHandler_2_0(Hashtable<String, RawDataFile> dataFilesIDMap) {
	this.dataFilesIDMap = dataFilesIDMap;
    }

    /**
     * Load the peak list from the zip file reading the XML peak list file
     */
    public PeakList readPeakList(InputStream peakListStream)
	    throws IOException, ParserConfigurationException, SAXException {

	totalRows = 0;
	parsedRows = 0;

	charBuffer = new StringBuffer();
	appliedMethods = new Vector<String>();
	appliedMethodParameters = new Vector<String>();
	currentPeakListDataFiles = new Vector<RawDataFile>();
	currentIsotopes = new Vector<DataPoint>();

	buildingPeakList = null;

	// Parse the XML file
	SAXParserFactory factory = SAXParserFactory.newInstance();
	SAXParser saxParser = factory.newSAXParser();
	saxParser.parse(peakListStream, this);

	return buildingPeakList;

    }

    /**
     * @return the progress of these functions loading the peak list from the
     *         zip file.
     */
    public double getProgress() {
	if (totalRows == 0)
	    return 0;
	return (double) parsedRows / totalRows;
    }

    public void cancel() {
	canceled = true;
    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String namespaceURI, String lName, String qName,
	    Attributes attrs) throws SAXException {

	if (canceled)
	    throw new SAXException("Parsing canceled");

	// This will remove any remaining characters from previous elements
	getTextOfElement();
	
	// <ROW>
	if (qName.equals(PeakListElementName_2_0.ROW.getElementName())) {

	    if (buildingPeakList == null) {
		initializePeakList();
	    }
	    int rowID = Integer.parseInt(attrs
		    .getValue(PeakListElementName_2_0.ID.getElementName()));
	    buildingRow = new SimplePeakListRow(rowID);
	    String comment = attrs.getValue(PeakListElementName_2_0.COMMENT
		    .getElementName());
	    buildingRow.setComment(comment);
	}

	// <PEAK_IDENTITY>
	if (qName
		.equals(PeakListElementName_2_0.PEAK_IDENTITY.getElementName())) {
	    identityProperties = new Hashtable<String, String>();
	    preferred = Boolean.parseBoolean(attrs
		    .getValue(PeakListElementName_2_0.PREFERRED
			    .getElementName()));
	}

	// <IDENTITY_PROPERTY>
	if (qName.equals(PeakListElementName_2_0.IDPROPERTY.getElementName())) {
	    identityPropertyName = attrs.getValue(PeakListElementName_2_0.NAME
		    .getElementName());
	}

	// <PEAK>
	if (qName.equals(PeakListElementName_2_0.PEAK.getElementName())) {

	    peakColumnID = attrs.getValue(PeakListElementName_2_0.COLUMN
		    .getElementName());
	    mass = Double.parseDouble(attrs.getValue(PeakListElementName_2_0.MZ
		    .getElementName()));
	    // Before MZmine 2.6 retention time was saved in seconds, but now we
	    // use minutes, so we need to divide by 60
	    rt = Double.parseDouble(attrs.getValue(PeakListElementName_2_0.RT
		    .getElementName())) / 60d;
	    height = Double.parseDouble(attrs
		    .getValue(PeakListElementName_2_0.HEIGHT.getElementName()));
	    area = Double.parseDouble(attrs
		    .getValue(PeakListElementName_2_0.AREA.getElementName()));
	    peakStatus = attrs.getValue(PeakListElementName_2_0.STATUS
		    .getElementName());
	    String chargeString = attrs.getValue(PeakListElementName_2_0.CHARGE
		    .getElementName());
	    if (chargeString != null)
		currentPeakCharge = Integer.valueOf(chargeString);
	    else
		currentPeakCharge = 0;

	}

	// <MZPEAK>
	if (qName.equals(PeakListElementName_2_0.MZPEAKS.getElementName())) {
	    numOfMZpeaks = Integer
		    .parseInt(attrs.getValue(PeakListElementName_2_0.QUANTITY
			    .getElementName()));
	}

	// <ISOTOPE_PATTERN>
	if (qName.equals(PeakListElementName_2_0.ISOTOPE_PATTERN
		.getElementName())) {
	    currentIsotopes.clear();
	    currentIsotopePatternStatus = IsotopePatternStatus.valueOf(attrs
		    .getValue(PeakListElementName_2_0.STATUS.getElementName()));
	    currentIsotopePatternDescription = attrs
		    .getValue(PeakListElementName_2_0.DESCRIPTION
			    .getElementName());
	}

    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public void endElement(String namespaceURI, String sName, String qName)
	    throws SAXException {

	if (canceled)
	    throw new SAXException("Parsing canceled");

	// <NAME>
	if (qName
		.equals(PeakListElementName_2_0.PEAKLIST_NAME.getElementName())) {
	    name = getTextOfElement();
	    logger.info("Loading peak list: " + name);
	    peakListName = name;
	}

	// <PEAKLIST_DATE>
	if (qName
		.equals(PeakListElementName_2_0.PEAKLIST_DATE.getElementName())) {
	    dateCreated = getTextOfElement();
	}

	// <QUANTITY>
	if (qName.equals(PeakListElementName_2_0.QUANTITY.getElementName())) {
	    String text = getTextOfElement();
	    totalRows = Integer.parseInt(text);
	}

	// <RAW_FILE>
	if (qName.equals(PeakListElementName_2_0.RAWFILE.getElementName())) {
	    rawDataFileID = getTextOfElement();
	    RawDataFile dataFile = dataFilesIDMap.get(rawDataFileID);
	    if (dataFile == null) {
		throw new SAXException(
			"Cannot open peak list, because raw data file "
				+ rawDataFileID + " is missing.");
	    }
	    currentPeakListDataFiles.add(dataFile);
	}

	// <SCAN_ID>
	if (qName.equals(PeakListElementName_2_0.SCAN_ID.getElementName())) {

	    byte[] bytes = Base64.decodeToBytes(getTextOfElement());
	    // make a data input stream
	    DataInputStream dataInputStream = new DataInputStream(
		    new ByteArrayInputStream(bytes));
	    scanNumbers = new int[numOfMZpeaks];
	    for (int i = 0; i < numOfMZpeaks; i++) {
		try {
		    scanNumbers[i] = dataInputStream.readInt();
		} catch (IOException ex) {
		    throw new SAXException(ex);
		}
	    }
	}

	// <REPRESENTATIVE_SCAN>
	if (qName.equals(PeakListElementName_2_0.REPRESENTATIVE_SCAN
		.getElementName())) {
	    representativeScan = Integer.valueOf(getTextOfElement());
	}

	// <FRAGMENT_SCAN>

	if (qName
		.equals(PeakListElementName_2_0.FRAGMENT_SCAN.getElementName())) {
	    fragmentScan = Integer.valueOf(getTextOfElement());
	}

	// <MASS>
	if (qName.equals(PeakListElementName_2_0.MZ.getElementName())) {

	    byte[] bytes = Base64.decodeToBytes(getTextOfElement());
	    // make a data input stream
	    DataInputStream dataInputStream = new DataInputStream(
		    new ByteArrayInputStream(bytes));
	    masses = new double[numOfMZpeaks];
	    for (int i = 0; i < numOfMZpeaks; i++) {
		try {
		    masses[i] = (double) dataInputStream.readFloat();
		} catch (IOException ex) {
		    throw new SAXException(ex);
		}
	    }
	}

	// <HEIGHT>
	if (qName.equals(PeakListElementName_2_0.HEIGHT.getElementName())) {

	    byte[] bytes = Base64.decodeToBytes(getTextOfElement());
	    // make a data input stream
	    DataInputStream dataInputStream = new DataInputStream(
		    new ByteArrayInputStream(bytes));
	    intensities = new double[numOfMZpeaks];
	    for (int i = 0; i < numOfMZpeaks; i++) {
		try {
		    intensities[i] = (double) dataInputStream.readFloat();
		} catch (IOException ex) {
		    throw new SAXException(ex);
		}
	    }
	}

	// <PEAK>
	if (qName.equals(PeakListElementName_2_0.PEAK.getElementName())) {

	    DataPoint[] mzPeaks = new DataPoint[numOfMZpeaks];
	    Range<Double> peakRTRange = null, peakMZRange = null, peakIntensityRange = null;
	    RawDataFile dataFile = dataFilesIDMap.get(peakColumnID);

	    if (dataFile == null)
		throw new SAXException("Error in project: data file "
			+ peakColumnID + " not found");

	    for (int i = 0; i < numOfMZpeaks; i++) {

		Scan sc = dataFile.getScan(scanNumbers[i]);
		double retentionTime = sc.getRetentionTime();

		double mz = masses[i];
		double intensity = intensities[i];

		if ((peakRTRange == null) || (peakIntensityRange == null)) {
		    peakRTRange = Range.singleton(retentionTime);
		    peakIntensityRange = Range.singleton(intensity);
		} else {
		    peakRTRange = peakRTRange.span(Range
			    .singleton(retentionTime));
		    peakIntensityRange = peakIntensityRange.span(Range
			    .singleton(intensity));
		}
		if (mz > 0.0) {
		    mzPeaks[i] = new SimpleDataPoint(mz, intensity);
		    if (peakMZRange == null)
			peakMZRange = Range.singleton(mz);
		    else
			peakMZRange = peakMZRange.span(Range.singleton(mz));
		}
	    }

	    FeatureStatus status = FeatureStatus.valueOf(peakStatus);

	    SimpleFeature peak = new SimpleFeature(dataFile, mass, rt, height,
		    area, scanNumbers, mzPeaks, status, representativeScan,
		    fragmentScan, peakRTRange, peakMZRange, peakIntensityRange);

	    peak.setCharge(currentPeakCharge);

	    if (currentIsotopes.size() > 0) {
		SimpleIsotopePattern newPattern = new SimpleIsotopePattern(
			currentIsotopes.toArray(new DataPoint[0]),
			currentIsotopePatternStatus,
			currentIsotopePatternDescription);
		peak.setIsotopePattern(newPattern);
		currentIsotopes.clear();
	    }

	    buildingRow.addPeak(dataFile, peak);

	}

	// <IDENTITY_PROPERTY>
	if (qName.equals(PeakListElementName_2_0.IDPROPERTY.getElementName())) {
	    identityProperties.put(identityPropertyName, getTextOfElement());
	}

	// <PEAK_IDENTITY>
	if (qName
		.equals(PeakListElementName_2_0.PEAK_IDENTITY.getElementName())) {
	    String content = getTextOfElement();
	    if (identityProperties.get(PeakIdentity.PROPERTY_NAME) == null)
		identityProperties.put(PeakIdentity.PROPERTY_NAME, content);
	    SimplePeakIdentity identity = new SimplePeakIdentity(
		    identityProperties);
	    buildingRow.addPeakIdentity(identity, preferred);
	}

	// <ROW>
	if (qName.equals(PeakListElementName_2_0.ROW.getElementName())) {
	    buildingPeakList.addRow(buildingRow);
	    buildingRow = null;
	    parsedRows++;
	}

	// <ISOTOPE>
	if (qName.equals(PeakListElementName_2_0.ISOTOPE.getElementName())) {
	    String text = getTextOfElement();
	    String items[] = text.split(":");
	    double mz = Double.valueOf(items[0]);
	    double intensity = Double.valueOf(items[1]);
	    DataPoint isotope = new SimpleDataPoint(mz, intensity);
	    currentIsotopes.add(isotope);
	}

	if (qName.equals(PeakListElementName_2_0.METHOD_NAME.getElementName())) {
	    String appliedMethod = getTextOfElement();
	    appliedMethods.add(appliedMethod);
	}

	if (qName.equals(PeakListElementName_2_0.METHOD_PARAMETERS
		.getElementName())) {
	    String appliedMethodParam = getTextOfElement();
	    appliedMethodParameters.add(appliedMethodParam);
	}

    }

    /**
     * Return a string without tab an EOF characters
     * 
     * @return String element text
     */
    private String getTextOfElement() {
	String text = charBuffer.toString();
	text = text.replaceAll("[\n\r\t]+", "");
	text = text.replaceAll("^\\s+", "");
	charBuffer.setLength(0);
	return text;
    }

    /**
     * characters()
     * 
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char buf[], int offset, int len) throws SAXException {
	charBuffer = charBuffer.append(buf, offset, len);
    }

    /**
     * Initializes the peak list
     */
    private void initializePeakList() {

	RawDataFile[] dataFiles = currentPeakListDataFiles
		.toArray(new RawDataFile[0]);

	buildingPeakList = new SimplePeakList(peakListName, dataFiles);

	for (int i = 0; i < appliedMethods.size(); i++) {
	    String methodName = appliedMethods.elementAt(i);
	    String methodParams = appliedMethodParameters.elementAt(i);
	    PeakListAppliedMethod pam = new SimplePeakListAppliedMethod(
		    methodName, methodParams);
	    buildingPeakList.addDescriptionOfAppliedTask(pam);
	}
	buildingPeakList.setDateCreated(dateCreated);
    }
}
