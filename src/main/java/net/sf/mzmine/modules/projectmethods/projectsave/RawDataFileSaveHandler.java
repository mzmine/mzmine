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

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.project.impl.StorableMassList;
import net.sf.mzmine.project.impl.StorableScan;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.google.common.collect.Range;

class RawDataFileSaveHandler {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private int numOfScans, completedScans;
    private ZipOutputStream zipOutputStream;
    private boolean canceled = false;
    private Map<Integer, Long> dataPointsOffsets;
    private Map<Integer, Long> consolidatedDataPointsOffsets;
    private Map<Integer, Integer> dataPointsLengths;
    private double progress = 0;

    RawDataFileSaveHandler(ZipOutputStream zipOutputStream) {
	this.zipOutputStream = zipOutputStream;
    }

    /**
     * Copy the data points file of the raw data file from the temporary folder
     * to the zip file. Create an XML file which contains the description of the
     * same raw data file an copy it into the same zip file.
     * 
     * @param rawDataFile
     *            raw data file to be copied
     * @param rawDataSavedName
     *            name of the raw data inside the zip file
     * @throws java.io.IOException
     * @throws TransformerConfigurationException
     * @throws SAXException
     */
    void writeRawDataFile(RawDataFileImpl rawDataFile, int number)
	    throws IOException, TransformerConfigurationException, SAXException {

	numOfScans = rawDataFile.getNumOfScans();

	// Get the structure of the data points file
	dataPointsOffsets = rawDataFile.getDataPointsOffsets();
	dataPointsLengths = rawDataFile.getDataPointsLengths();
	consolidatedDataPointsOffsets = new TreeMap<Integer, Long>();

	// step 1 - save data file
	logger.info("Saving data points of: " + rawDataFile.getName());

	String rawDataSavedName = "Raw data file #" + number + " "
		+ rawDataFile.getName();

	zipOutputStream.putNextEntry(new ZipEntry(rawDataSavedName + ".scans"));

	// We save only those data points that still have a reference in the
	// dataPointsOffset table. Some deleted mass lists may still be present
	// in the data points file, we don't want to copy those.
	long newOffset = 0;
	byte buffer[] = new byte[1 << 20];
	RandomAccessFile dataPointsFile = rawDataFile.getDataPointsFile();
	for (Integer storageID : dataPointsOffsets.keySet()) {

	    if (canceled)
		return;

	    final long offset = dataPointsOffsets.get(storageID);
	    dataPointsFile.seek(offset);

	    final int bytes = dataPointsLengths.get(storageID) * 4 * 2;
	    consolidatedDataPointsOffsets.put(storageID, newOffset);
	    if (buffer.length < bytes) {
		buffer = new byte[bytes * 2];
	    }
	    dataPointsFile.read(buffer, 0, bytes);
	    zipOutputStream.write(buffer, 0, bytes);
	    newOffset += bytes;
	    progress = 0.9 * ((double) offset / dataPointsFile.length());
	}

	if (canceled)
	    return;

	// step 2 - save raw data description
	logger.info("Saving raw data description of: " + rawDataFile.getName());

	zipOutputStream.putNextEntry(new ZipEntry(rawDataSavedName + ".xml"));
	OutputStream finalStream = zipOutputStream;

	StreamResult streamResult = new StreamResult(finalStream);
	SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory
		.newInstance();

	TransformerHandler hd = tf.newTransformerHandler();
	Transformer serializer = hd.getTransformer();
	serializer.setOutputProperty(OutputKeys.INDENT, "yes");
	serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

	hd.setResult(streamResult);
	hd.startDocument();
	saveRawDataInformation(rawDataFile, hd);
	hd.endDocument();
    }

    /**
     * Function which creates an XML file with the descripcion of the raw data
     * 
     * @param rawDataFile
     * @param hd
     * @throws SAXException
     * @throws java.lang.Exception
     */
    private void saveRawDataInformation(RawDataFileImpl rawDataFile,
	    TransformerHandler hd) throws SAXException, IOException {

	AttributesImpl atts = new AttributesImpl();

	hd.startElement("", "", RawDataElementName.RAWDATA.getElementName(),
		atts);

	// <NAME>
	hd.startElement("", "", RawDataElementName.NAME.getElementName(), atts);
	hd.characters(rawDataFile.getName().toCharArray(), 0, rawDataFile
		.getName().length());
	hd.endElement("", "", RawDataElementName.NAME.getElementName());

	// <STORED_DATAPOINTS>
	atts.addAttribute("", "", RawDataElementName.QUANTITY.getElementName(),
		"CDATA", String.valueOf(dataPointsOffsets.size()));
	hd.startElement("", "",
		RawDataElementName.STORED_DATAPOINTS.getElementName(), atts);
	atts.clear();
	for (Integer storageID : dataPointsOffsets.keySet()) {
	    if (canceled)
		return;
	    int length = dataPointsLengths.get(storageID);
	    long offset = consolidatedDataPointsOffsets.get(storageID);
	    atts.addAttribute("", "",
		    RawDataElementName.STORAGE_ID.getElementName(), "CDATA",
		    String.valueOf(storageID));
	    atts.addAttribute("", "",
		    RawDataElementName.QUANTITY_DATAPOINTS.getElementName(),
		    "CDATA", String.valueOf(length));
	    hd.startElement("", "",
		    RawDataElementName.STORED_DATA.getElementName(), atts);
	    atts.clear();
	    hd.characters(String.valueOf(offset).toCharArray(), 0, String
		    .valueOf(offset).length());
	    hd.endElement("", "",
		    RawDataElementName.STORED_DATA.getElementName());
	}

	hd.endElement("", "",
		RawDataElementName.STORED_DATAPOINTS.getElementName());

	// <QUANTITY>
	hd.startElement("", "",
		RawDataElementName.QUANTITY_SCAN.getElementName(), atts);
	hd.characters(String.valueOf(numOfScans).toCharArray(), 0, String
		.valueOf(numOfScans).length());
	hd.endElement("", "", RawDataElementName.QUANTITY_SCAN.getElementName());

	// <SCAN>
	for (int scanNumber : rawDataFile.getScanNumbers()) {

	    if (canceled)
		return;

	    StorableScan scan = (StorableScan) rawDataFile.getScan(scanNumber);
	    int storageID = scan.getStorageID();
	    atts.addAttribute("", "",
		    RawDataElementName.STORAGE_ID.getElementName(), "CDATA",
		    String.valueOf(storageID));
	    hd.startElement("", "", RawDataElementName.SCAN.getElementName(),
		    atts);
	    fillScanElement(scan, hd);
	    hd.endElement("", "", RawDataElementName.SCAN.getElementName());
	    atts.clear();
	    completedScans++;
	    progress = 0.9 + (0.1 * ((double) completedScans / numOfScans));
	}

	hd.endElement("", "", RawDataElementName.RAWDATA.getElementName());

    }

    /**
     * Create the part of the XML document related to the scans
     * 
     * @param scan
     * @param element
     */
    private void fillScanElement(Scan scan, TransformerHandler hd)
	    throws SAXException, IOException {
	// <SCAN_ID>
	AttributesImpl atts = new AttributesImpl();
	hd.startElement("", "", RawDataElementName.SCAN_ID.getElementName(),
		atts);
	hd.characters(String.valueOf(scan.getScanNumber()).toCharArray(), 0,
		String.valueOf(scan.getScanNumber()).length());
	hd.endElement("", "", RawDataElementName.SCAN_ID.getElementName());

	// <MS_LEVEL>
	hd.startElement("", "", RawDataElementName.MS_LEVEL.getElementName(),
		atts);
	hd.characters(String.valueOf(scan.getMSLevel()).toCharArray(), 0,
		String.valueOf(scan.getMSLevel()).length());
	hd.endElement("", "", RawDataElementName.MS_LEVEL.getElementName());

	if (scan.getMSLevel() >= 2) {
	    // <PRECURSOR_MZ>
	    hd.startElement("", "",
		    RawDataElementName.PRECURSOR_MZ.getElementName(), atts);
	    hd.characters(String.valueOf(scan.getPrecursorMZ()).toCharArray(),
		    0, String.valueOf(scan.getPrecursorMZ()).length());
	    hd.endElement("", "",
		    RawDataElementName.PRECURSOR_MZ.getElementName());

	    // <PRECURSOR_CHARGE>
	    hd.startElement("", "",
		    RawDataElementName.PRECURSOR_CHARGE.getElementName(), atts);
	    hd.characters(String.valueOf(scan.getPrecursorCharge())
		    .toCharArray(), 0, String
		    .valueOf(scan.getPrecursorCharge()).length());
	    hd.endElement("", "",
		    RawDataElementName.PRECURSOR_CHARGE.getElementName());
	}

	// <RETENTION_TIME>
	hd.startElement("", "",
		RawDataElementName.RETENTION_TIME.getElementName(), atts);
	// In the project file, retention time is represented in seconds, for
	// historical reasons
	double rt = scan.getRetentionTime() * 60d;
	hd.characters(String.valueOf(rt).toCharArray(), 0, String.valueOf(rt)
		.length());
	hd.endElement("", "",
		RawDataElementName.RETENTION_TIME.getElementName());

	// <CENTROIDED>
	hd.startElement("", "", RawDataElementName.CENTROIDED.getElementName(),
		atts);
	hd.characters(String.valueOf(scan.getSpectrumType()).toCharArray(), 0,
		String.valueOf(scan.getSpectrumType()).length());
	hd.endElement("", "", RawDataElementName.CENTROIDED.getElementName());

	// <QUANTITY_DATAPOINTS>
	hd.startElement("", "",
		RawDataElementName.QUANTITY_DATAPOINTS.getElementName(), atts);
	hd.characters(String.valueOf((scan.getNumberOfDataPoints()))
		.toCharArray(), 0,
		String.valueOf((scan.getNumberOfDataPoints())).length());
	hd.endElement("", "",
		RawDataElementName.QUANTITY_DATAPOINTS.getElementName());

	// <FRAGMENT_SCAN>
	if (scan.getFragmentScanNumbers() != null) {
	    int[] fragmentScans = scan.getFragmentScanNumbers();
	    atts.addAttribute("", "",
		    RawDataElementName.QUANTITY.getElementName(), "CDATA",
		    String.valueOf(fragmentScans.length));
	    hd.startElement("", "",
		    RawDataElementName.QUANTITY_FRAGMENT_SCAN.getElementName(),
		    atts);
	    atts.clear();
	    for (int i : fragmentScans) {
		hd.startElement("", "",
			RawDataElementName.FRAGMENT_SCAN.getElementName(), atts);
		hd.characters(String.valueOf(i).toCharArray(), 0, String
			.valueOf(i).length());
		hd.endElement("", "",
			RawDataElementName.FRAGMENT_SCAN.getElementName());
	    }
	    hd.endElement("", "",
		    RawDataElementName.QUANTITY_FRAGMENT_SCAN.getElementName());

	}

	// <MASS_LIST>
	MassList massLists[] = scan.getMassLists();
	for (MassList massList : massLists) {
	    StorableMassList stMassList = (StorableMassList) massList;
	    atts.addAttribute("", "", RawDataElementName.NAME.getElementName(),
		    "CDATA", stMassList.getName());
	    atts.addAttribute("", "",
		    RawDataElementName.STORAGE_ID.getElementName(), "CDATA",
		    String.valueOf(stMassList.getStorageID()));
	    hd.startElement("", "",
		    RawDataElementName.MASS_LIST.getElementName(), atts);
	    atts.clear();
	    hd.endElement("", "", RawDataElementName.MASS_LIST.getElementName());
	}

	// <POLARITY>
	hd.startElement("", "", RawDataElementName.POLARITY.getElementName(),
		atts);
	String pol = scan.getPolarity().toString();
	hd.characters(pol.toCharArray(), 0, pol.length());
	hd.endElement("", "", RawDataElementName.POLARITY.getElementName());

	// <SCAN_DESCRIPTION>
	hd.startElement("", "",
		RawDataElementName.SCAN_DESCRIPTION.getElementName(), atts);
	String scanDesc = scan.getScanDefinition();
	hd.characters(scanDesc.toCharArray(), 0, scanDesc.length());
	hd.endElement("", "",
		RawDataElementName.SCAN_DESCRIPTION.getElementName());
	
	// <SCAN_MZ_RANGE>
	hd.startElement("", "",
		RawDataElementName.SCAN_MZ_RANGE.getElementName(), atts);
	Range<Double> mzRange = scan.getScanningMZRange();
	String mzRangeStr = mzRange.lowerEndpoint() + "-" + mzRange.upperEndpoint();
	hd.characters(mzRangeStr.toCharArray(), 0, mzRangeStr.length());
	hd.endElement("", "",
		RawDataElementName.SCAN_MZ_RANGE.getElementName());

    }

    /**
     * 
     * @return the progress of these functions saving the raw data information
     *         to the zip file.
     */
    double getProgress() {
	return progress;
    }

    void cancel() {
	canceled = true;
    }
}
