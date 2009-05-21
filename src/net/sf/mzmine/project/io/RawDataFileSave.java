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
package net.sf.mzmine.project.io;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.project.impl.RawDataFileImpl;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class RawDataFileSave {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private int numOfScans;
	private ZipOutputStream zipOutputStream;
	private SaveFileUtils saveFileUtils;

	public RawDataFileSave(ZipOutputStream zipOutputStream) {
		this.zipOutputStream = zipOutputStream;
	}

	/**
	 * Copy the scan file of the raw data file from the temporal folder to the
	 * zip file.
	 * Create an XML file which contains the description of the same raw data file
	 * an copy it into the same zip file.
	 *
	 * @param rawDataFile raw data file to be copied
	 * @param rawDataSavedName name of the raw data inside the zip file
	 * @throws java.io.IOException
	 */
	public void writeRawDataFiles(RawDataFile rawDataFile, String rawDataSavedName) throws Exception {
		// step 1 - save scan file
		logger.info("Saving scan file of: " + rawDataFile.getName());

		zipOutputStream.putNextEntry(new ZipEntry(rawDataSavedName + ".scans"));
		FileInputStream fileStream = new FileInputStream(((RawDataFileImpl) rawDataFile).getScanDataFileasFile());
		saveFileUtils = new SaveFileUtils();
		saveFileUtils.saveFile(fileStream, zipOutputStream, ((RawDataFileImpl) rawDataFile).getScanDataFileasFile().length(), SaveFileUtilsMode.CLOSE_IN);


		// step 2 - save raw data description
		logger.info("Saving raw data description of: " + rawDataFile.getName());

		zipOutputStream.putNextEntry(new ZipEntry(rawDataSavedName + ".xml"));
		OutputStream finalStream = zipOutputStream;

		StreamResult streamResult = new StreamResult(finalStream);
		SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

		TransformerHandler hd = tf.newTransformerHandler();
		Transformer serializer = hd.getTransformer();
		serializer.setOutputProperty(OutputKeys.INDENT,"yes");
		serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

		hd.setResult(streamResult);
		hd.startDocument();
		saveRawDataInformation(rawDataFile, hd);
		hd.endDocument();
	}

	/**
	 * Function which creates an XML file with the descripcion of the raw data
	 * @param rawDataFile
	 * @param hd
	 * @throws java.lang.Exception
	 */
	public void saveRawDataInformation(RawDataFile rawDataFile, TransformerHandler hd) throws Exception {
		AttributesImpl atts = new AttributesImpl();
		numOfScans = rawDataFile.getNumOfScans();

		hd.startElement("", "", RawDataElementName.RAWDATA.getElementName(), atts);
		atts.clear();

		// <NAME>
		hd.startElement("", "", RawDataElementName.NAME.getElementName(), atts);
		hd.characters(rawDataFile.getName().toCharArray(), 0, rawDataFile.getName().length());
		hd.endElement("", "", RawDataElementName.NAME.getElementName());

		// <QUANTITY>
		hd.startElement("", "", RawDataElementName.QUANTITY_SCAN.getElementName(), atts);
		hd.characters(String.valueOf(numOfScans).toCharArray(), 0, String.valueOf(numOfScans).length());
		hd.endElement("", "", RawDataElementName.QUANTITY_SCAN.getElementName());

		// <SCAN>
		for (int scanNumber : rawDataFile.getScanNumbers()) {
			hd.startElement("", "", RawDataElementName.SCAN.getElementName(), atts);
			Scan scan = rawDataFile.getScan(scanNumber);
			fillScanElement(scan, hd);
			hd.endElement("", "", RawDataElementName.SCAN.getElementName());
		}

		hd.endElement("", "", RawDataElementName.RAWDATA.getElementName());

	}

	/**
	 * Create the part of the XML document related to the scans
	 * @param scan 
	 * @param element 
	 */
	private void fillScanElement(Scan scan, TransformerHandler hd) throws SAXException {
		// <SCAN_ID>
		AttributesImpl atts = new AttributesImpl();
		hd.startElement("", "", RawDataElementName.SCAN_ID.getElementName(), atts);
		hd.characters(String.valueOf(scan.getScanNumber()).toCharArray(), 0, String.valueOf(scan.getScanNumber()).length());
		hd.endElement("", "", RawDataElementName.SCAN_ID.getElementName());

		// <MS_LEVEL>
		hd.startElement("", "", RawDataElementName.MS_LEVEL.getElementName(), atts);
		hd.characters(String.valueOf(scan.getMSLevel()).toCharArray(), 0, String.valueOf(scan.getMSLevel()).length());
		hd.endElement("", "", RawDataElementName.MS_LEVEL.getElementName());

		// <PARENT_SCAN>
		if (scan.getParentScanNumber() > 0) {
			hd.startElement("", "", RawDataElementName.PARENT_SCAN.getElementName(), atts);
			hd.characters(String.valueOf(scan.getParentScanNumber()).toCharArray(), 0, String.valueOf(scan.getParentScanNumber()).length());
			hd.endElement("", "", RawDataElementName.PARENT_SCAN.getElementName());
		}


		if (scan.getMSLevel() >= 2) {
			// <PRECURSOR_MZ>
			hd.startElement("", "", RawDataElementName.PRECURSOR_MZ.getElementName(), atts);
			hd.characters(String.valueOf(scan.getPrecursorMZ()).toCharArray(), 0, String.valueOf(scan.getPrecursorMZ()).length());
			hd.endElement("", "", RawDataElementName.PRECURSOR_MZ.getElementName());

			// <PRECURSOR_CHARGE>
			hd.startElement("", "", RawDataElementName.PRECURSOR_CHARGE.getElementName(), atts);
			hd.characters(String.valueOf(scan.getPrecursorCharge()).toCharArray(), 0, String.valueOf(scan.getPrecursorCharge()).length());
			hd.endElement("", "", RawDataElementName.PRECURSOR_CHARGE.getElementName());
		}

		// <RETENTION_TIME>
		hd.startElement("", "", RawDataElementName.RETENTION_TIME.getElementName(), atts);
		hd.characters(String.valueOf(scan.getRetentionTime()).toCharArray(), 0, String.valueOf(scan.getRetentionTime()).length());
		hd.endElement("", "", RawDataElementName.RETENTION_TIME.getElementName());

		// <CENTROIDED>
		hd.startElement("", "", RawDataElementName.CENTROIDED.getElementName(), atts);
		hd.characters(String.valueOf(scan.isCentroided()).toCharArray(), 0, String.valueOf(scan.isCentroided()).length());
		hd.endElement("", "", RawDataElementName.CENTROIDED.getElementName());

		// <QUANTITY_DATAPOINTS>
		hd.startElement("", "", RawDataElementName.QUANTITY_DATAPOINTS.getElementName(), atts);
		hd.characters(String.valueOf((scan.getNumberOfDataPoints())).toCharArray(), 0, String.valueOf((scan.getNumberOfDataPoints())).length());
		hd.endElement("", "", RawDataElementName.QUANTITY_DATAPOINTS.getElementName());

		// <FRAGMENT_SCAN>
		if (scan.getFragmentScanNumbers() != null) {
			int[] fragmentScans = scan.getFragmentScanNumbers();
			atts.addAttribute("", "", RawDataElementName.QUANTITY.getElementName(), "CDATA", String.valueOf(fragmentScans.length));
			hd.startElement("", "", RawDataElementName.QUANTITY_FRAGMENT_SCAN.getElementName(), atts);
			atts.clear();
			for (int i : fragmentScans) {
				hd.startElement("", "", RawDataElementName.FRAGMENT_SCAN.getElementName(), atts);
				hd.characters( String.valueOf(i).toCharArray(), 0,  String.valueOf(i).length());
				hd.endElement("", "", RawDataElementName.FRAGMENT_SCAN.getElementName());
			}
			hd.endElement("", "", RawDataElementName.QUANTITY_FRAGMENT_SCAN.getElementName());

		}
	}

	/**
	 * 
	 * @return the progress of these functions saving the raw data information to the zip file.
	 */
	public double getProgress() {
		return saveFileUtils.getProgress();
	}
}
