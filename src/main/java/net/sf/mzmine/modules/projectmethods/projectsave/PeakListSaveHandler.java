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

package net.sf.mzmine.modules.projectmethods.projectsave;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimplePeakList;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.Ostermiller.util.Base64;
import net.sf.mzmine.datamodel.PeakInformation;

public class PeakListSaveHandler {

    public static DateFormat dateFormat = new SimpleDateFormat(
	    "yyyy/MM/dd HH:mm:ss");

    private Hashtable<RawDataFile, String> dataFilesIDMap;

    private int numberOfRows, finishedRows;
    private boolean canceled = false;

    private OutputStream finalStream;

    public PeakListSaveHandler(OutputStream finalStream,
	    Hashtable<RawDataFile, String> dataFilesIDMap) {
	this.finalStream = finalStream;
	this.dataFilesIDMap = dataFilesIDMap;
    }

    /**
     * Create an XML document with the peak list information an save it into the
     * project zip file
     * 
     * @param peakList
     * @param peakListSavedName
     *            name of the peak list
     * @throws java.io.IOException
     */
    public void savePeakList(PeakList peakList) throws IOException,
	    TransformerConfigurationException, SAXException {

	numberOfRows = peakList.getNumberOfRows();
	finishedRows = 0;

	StreamResult streamResult = new StreamResult(finalStream);
	SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory
		.newInstance();

	TransformerHandler hd = tf.newTransformerHandler();

	Transformer serializer = hd.getTransformer();
	serializer.setOutputProperty(OutputKeys.INDENT, "yes");
	serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

	hd.setResult(streamResult);
	hd.startDocument();
	AttributesImpl atts = new AttributesImpl();

	hd.startElement("", "", PeakListElementName.PEAKLIST.getElementName(),
		atts);
	atts.clear();

	// <NAME>
	hd.startElement("", "",
		PeakListElementName.PEAKLIST_NAME.getElementName(), atts);
	hd.characters(peakList.getName().toCharArray(), 0, peakList.getName()
		.length());
	hd.endElement("", "",
		PeakListElementName.PEAKLIST_NAME.getElementName());

	// <PEAKLIST_DATE>
	String dateText = "";
	if (((SimplePeakList) peakList).getDateCreated() == null) {
	    dateText = ((SimplePeakList) peakList).getDateCreated();
	} else {
	    Date date = new Date();
	    dateText = dateFormat.format(date);
	}
	hd.startElement("", "",
		PeakListElementName.PEAKLIST_DATE.getElementName(), atts);
	hd.characters(dateText.toCharArray(), 0, dateText.length());
	hd.endElement("", "",
		PeakListElementName.PEAKLIST_DATE.getElementName());

	// <QUANTITY>
	hd.startElement("", "", PeakListElementName.QUANTITY.getElementName(),
		atts);
	hd.characters(String.valueOf(numberOfRows).toCharArray(), 0, String
		.valueOf(numberOfRows).length());
	hd.endElement("", "", PeakListElementName.QUANTITY.getElementName());

	// <PROCESS>
	PeakListAppliedMethod[] processes = peakList.getAppliedMethods();
	for (PeakListAppliedMethod proc : processes) {

	    hd.startElement("", "",
		    PeakListElementName.METHOD.getElementName(), atts);

	    hd.startElement("", "",
		    PeakListElementName.METHOD_NAME.getElementName(), atts);
	    String methodName = proc.getDescription();
	    hd.characters(methodName.toCharArray(), 0, methodName.length());
	    hd.endElement("", "",
		    PeakListElementName.METHOD_NAME.getElementName());

	    hd.startElement("", "",
		    PeakListElementName.METHOD_PARAMETERS.getElementName(),
		    atts);
	    String methodParameters = proc.getParameters();
	    hd.characters(methodParameters.toCharArray(), 0,
		    methodParameters.length());
	    hd.endElement("", "",
		    PeakListElementName.METHOD_PARAMETERS.getElementName());

	    hd.endElement("", "", PeakListElementName.METHOD.getElementName());

	}
	atts.clear();

	// <RAWFILE>
	RawDataFile[] dataFiles = peakList.getRawDataFiles();

	for (int i = 0; i < dataFiles.length; i++) {

	    String ID = dataFilesIDMap.get(dataFiles[i]);

	    hd.startElement("", "",
		    PeakListElementName.RAWFILE.getElementName(), atts);
	    char idChars[] = ID.toCharArray();
	    hd.characters(idChars, 0, idChars.length);

	    hd.endElement("", "", PeakListElementName.RAWFILE.getElementName());
	}

	// <ROW>
	PeakListRow row;
	for (int i = 0; i < numberOfRows; i++) {

	    if (canceled)
		return;

	    atts.clear();
	    row = peakList.getRow(i);
	    atts.addAttribute("", "", PeakListElementName.ID.getElementName(),
		    "CDATA", String.valueOf(row.getID()));
	    if (row.getComment() != null) {
		atts.addAttribute("", "",
			PeakListElementName.COMMENT.getElementName(), "CDATA",
			row.getComment());
	    }

	    hd.startElement("", "", PeakListElementName.ROW.getElementName(),
		    atts);
	    fillRowElement(row, hd);
	    hd.endElement("", "", PeakListElementName.ROW.getElementName());

	    finishedRows++;
	}

	hd.endElement("", "", PeakListElementName.PEAKLIST.getElementName());
	hd.endDocument();
    }

    /**
     * Add the row information into the XML document
     * 
     * @param row
     * @param element
     * @throws IOException
     */
    private void fillRowElement(PeakListRow row, TransformerHandler hd)
	    throws SAXException, IOException {

	// <PEAK_IDENTITY>
	PeakIdentity preferredIdentity = row.getPreferredPeakIdentity();
	PeakIdentity[] identities = row.getPeakIdentities();
	AttributesImpl atts = new AttributesImpl();

	for (int i = 0; i < identities.length; i++) {

	    if (canceled)
		return;

	    atts.addAttribute("", "", PeakListElementName.ID.getElementName(),
		    "CDATA", String.valueOf(i));
	    atts.addAttribute("", "",
		    PeakListElementName.PREFERRED.getElementName(), "CDATA",
		    String.valueOf(identities[i] == preferredIdentity));
	    hd.startElement("", "",
		    PeakListElementName.PEAK_IDENTITY.getElementName(), atts);
	    fillIdentityElement(identities[i], hd);
	    hd.endElement("", "",
		    PeakListElementName.PEAK_IDENTITY.getElementName());
	}

        // <PEAK_INFORMATION>
        
        //atts.clear();
        
        if (canceled) return;
        
        //atts.addAttribute("", "", PeakListElementName.ID.getElementName(), 
        //        "CDATA", "INFORMATION");
        hd.startElement("", "", 
                PeakListElementName.PEAK_INFORMATION.getElementName(), atts);
        fillInformationElement(row.getPeakInformation(), hd);
        hd.endElement("", "", 
                PeakListElementName.PEAK_INFORMATION.getElementName());
        
        	// <PEAK>
	Feature[] peaks = row.getPeaks();
	for (Feature p : peaks) {
	    if (canceled)
		return;

	    atts.clear();
	    String dataFileID = dataFilesIDMap.get(p.getDataFile());
	    atts.addAttribute("", "",
		    PeakListElementName.COLUMN.getElementName(), "CDATA",
		    dataFileID);
	    atts.addAttribute("", "", PeakListElementName.MZ.getElementName(),
		    "CDATA", String.valueOf(p.getMZ()));
	    // In the project file, retention time is represented in seconds,
	    // for historical reasons
	    double rt = p.getRT() * 60d;
	    atts.addAttribute("", "", PeakListElementName.RT.getElementName(),
		    "CDATA", String.valueOf(rt));
	    atts.addAttribute("", "",
		    PeakListElementName.HEIGHT.getElementName(), "CDATA",
		    String.valueOf(p.getHeight()));
	    atts.addAttribute("", "",
		    PeakListElementName.AREA.getElementName(), "CDATA",
		    String.valueOf(p.getArea()));
	    atts.addAttribute("", "", PeakListElementName.STATUS
		    .getElementName(), "CDATA", p.getFeatureStatus().toString());
	    atts.addAttribute("", "",
		    PeakListElementName.CHARGE.getElementName(), "CDATA",
		    String.valueOf(p.getCharge()));
	    hd.startElement("", "", PeakListElementName.PEAK.getElementName(),
		    atts);

	    fillPeakElement(p, hd);
	    hd.endElement("", "", PeakListElementName.PEAK.getElementName());
	}

    }

    /**
     * Add the peak identity information into the XML document
     * 
     * @param identity
     * @param element
     */
    private void fillIdentityElement(PeakIdentity identity,
	    TransformerHandler hd) throws SAXException {

	AttributesImpl atts = new AttributesImpl();

	Map<String, String> idProperties = identity.getAllProperties();

	for (Entry<String, String> property : idProperties.entrySet()) {
	    String propertyValue = property.getValue();
	    atts.clear();
	    atts.addAttribute("", "",
		    PeakListElementName.NAME.getElementName(), "CDATA",
		    property.getKey());

	    hd.startElement("", "",
		    PeakListElementName.IDPROPERTY.getElementName(), atts);
	    hd.characters(propertyValue.toCharArray(), 0,
		    propertyValue.length());
	    hd.endElement("", "",
		    PeakListElementName.IDPROPERTY.getElementName());
	}

    }

    private void fillInformationElement(PeakInformation information,
            TransformerHandler hd) throws SAXException 
    {
        if (information == null) return;
        
        AttributesImpl atts = new AttributesImpl();
        
        for (Entry <String, String> property : 
                information.getAllProperties().entrySet())
        {
            String value = property.getValue();
            
            atts.clear();
            atts.addAttribute("", "", PeakListElementName.NAME.getElementName(), 
                    "CDATA", property.getKey());
            
            hd.startElement("", "", 
                    PeakListElementName.INFO_PROPERTY.getElementName(), atts);
            hd.characters(property.getValue().toCharArray(), 0, value.length());
            hd.endElement("", "", 
                    PeakListElementName.INFO_PROPERTY.getElementName());
        }
    }
    
    /**
     * Add the peaks information into the XML document
     * 
     * @param peak
     * @param element
     * @param dataFileID
     * @throws IOException
     */
    private void fillPeakElement(Feature peak, TransformerHandler hd)
	    throws SAXException, IOException {
	AttributesImpl atts = new AttributesImpl();

	// <REPRESENTATIVE_SCAN>
	hd.startElement("", "",
		PeakListElementName.REPRESENTATIVE_SCAN.getElementName(), atts);
	hd.characters(String.valueOf(peak.getRepresentativeScanNumber())
		.toCharArray(), 0,
		String.valueOf(peak.getRepresentativeScanNumber()).length());
	hd.endElement("", "",
		PeakListElementName.REPRESENTATIVE_SCAN.getElementName());

	// <FRAGMENT_SCAN>
	hd.startElement("", "",
		PeakListElementName.FRAGMENT_SCAN.getElementName(), atts);
	hd.characters(String.valueOf(peak.getMostIntenseFragmentScanNumber())
		.toCharArray(), 0,
		String.valueOf(peak.getMostIntenseFragmentScanNumber())
			.length());
	hd.endElement("", "",
		PeakListElementName.FRAGMENT_SCAN.getElementName());

	int scanNumbers[] = peak.getScanNumbers();

	// <ISOTOPE_PATTERN>
	IsotopePattern isotopePattern = peak.getIsotopePattern();
	if (isotopePattern != null) {
	    atts.addAttribute("", "",
		    PeakListElementName.STATUS.getElementName(), "CDATA",
		    String.valueOf(isotopePattern.getStatus()));
	    atts.addAttribute("", "",
		    PeakListElementName.DESCRIPTION.getElementName(), "CDATA",
		    isotopePattern.getDescription());
	    hd.startElement("", "",
		    PeakListElementName.ISOTOPE_PATTERN.getElementName(), atts);
	    atts.clear();

	    fillIsotopePatternElement(isotopePattern, hd);

	    hd.endElement("", "",
		    PeakListElementName.ISOTOPE_PATTERN.getElementName());

	}

	// <MZPEAK>
	atts.addAttribute("", "",
		PeakListElementName.QUANTITY.getElementName(), "CDATA",
		String.valueOf(scanNumbers.length));
	hd.startElement("", "", PeakListElementName.MZPEAKS.getElementName(),
		atts);
	atts.clear();

	// <SCAN_ID> <MASS> <HEIGHT>
	ByteArrayOutputStream byteScanStream = new ByteArrayOutputStream();
	DataOutputStream dataScanStream = new DataOutputStream(byteScanStream);

	ByteArrayOutputStream byteMassStream = new ByteArrayOutputStream();
	DataOutputStream dataMassStream = new DataOutputStream(byteMassStream);

	ByteArrayOutputStream byteHeightStream = new ByteArrayOutputStream();
	DataOutputStream dataHeightStream = new DataOutputStream(
		byteHeightStream);

	float mass, height;
	for (int scan : scanNumbers) {
	    dataScanStream.writeInt(scan);
	    dataScanStream.flush();
	    DataPoint mzPeak = peak.getDataPoint(scan);
	    if (mzPeak != null) {
		mass = (float) mzPeak.getMZ();
		height = (float) mzPeak.getIntensity();
	    } else {
		mass = 0f;
		height = 0f;
	    }
	    dataMassStream.writeFloat(mass);
	    dataMassStream.flush();
	    dataHeightStream.writeFloat(height);
	    dataHeightStream.flush();
	}

	byte[] bytes = Base64.encode(byteScanStream.toByteArray());
	hd.startElement("", "", PeakListElementName.SCAN_ID.getElementName(),
		atts);
	String sbytes = new String(bytes);
	hd.characters(sbytes.toCharArray(), 0, sbytes.length());
	hd.endElement("", "", PeakListElementName.SCAN_ID.getElementName());

	bytes = Base64.encode(byteMassStream.toByteArray());
	hd.startElement("", "", PeakListElementName.MZ.getElementName(), atts);
	sbytes = new String(bytes);
	hd.characters(sbytes.toCharArray(), 0, sbytes.length());
	hd.endElement("", "", PeakListElementName.MZ.getElementName());

	bytes = Base64.encode(byteHeightStream.toByteArray());
	hd.startElement("", "", PeakListElementName.HEIGHT.getElementName(),
		atts);
	sbytes = new String(bytes);
	hd.characters(sbytes.toCharArray(), 0, sbytes.length());
	hd.endElement("", "", PeakListElementName.HEIGHT.getElementName());

	hd.endElement("", "", PeakListElementName.MZPEAKS.getElementName());
    }

    private void fillIsotopePatternElement(IsotopePattern isotopePattern,
	    TransformerHandler hd) throws SAXException, IOException {

	AttributesImpl atts = new AttributesImpl();

	DataPoint isotopes[] = isotopePattern.getDataPoints();

	for (DataPoint isotope : isotopes) {
	    hd.startElement("", "",
		    PeakListElementName.ISOTOPE.getElementName(), atts);
	    String isotopeString = isotope.getMZ() + ":"
		    + isotope.getIntensity();
	    hd.characters(isotopeString.toCharArray(), 0,
		    isotopeString.length());
	    hd.endElement("", "", PeakListElementName.ISOTOPE.getElementName());
	}
    }

    /**
     * @return the progress of these functions saving the peak list to the zip
     *         file.
     */
    public double getProgress() {
	if (numberOfRows == 0)
	    return 0;
	return (double) finishedRows / numberOfRows;
    }

    public void cancel() {
	canceled = true;
    }

}
