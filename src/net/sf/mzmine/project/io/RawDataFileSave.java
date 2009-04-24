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
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class RawDataFileSave {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private int numOfScans;
	private ZipOutputStream zipOutputStream;
	private SaveFileUtils saveFileUtils;

	public RawDataFileSave(ZipOutputStream zipOutputStream) {
		this.zipOutputStream = zipOutputStream;
	}

	/**
	 * Copies the scan file of the raw data file from the temporal folder to the
	 * zip file.
	 * Creates an XML file which contains the description of the same raw data file
	 * an copies it into the same zip file.
	 *
	 * @param rawDataFile raw data file to be copied
	 * @param rawDataSavedName name of the raw data inside the zip file
	 * @throws java.io.IOException
	 */
	public void writeRawDataFiles(RawDataFile rawDataFile, String rawDataSavedName) throws IOException {
		// step 1 - save scan file
		logger.info("Saving scan file of: " + rawDataFile.getName());

		zipOutputStream.putNextEntry(new ZipEntry(rawDataSavedName + ".scans"));
		FileInputStream fileStream = new FileInputStream(((RawDataFileImpl) rawDataFile).getScanDataFileasFile());
		saveFileUtils = new SaveFileUtils();
		saveFileUtils.saveFile(fileStream, zipOutputStream, ((RawDataFileImpl) rawDataFile).getScanDataFileasFile().length(), SaveFileUtilsMode.CLOSE_IN);
		Document document = saveRawDataInformation(rawDataFile);

		// step 2 - save raw data description
		logger.info("Saving raw data description of: " + rawDataFile.getName());

		zipOutputStream.putNextEntry(new ZipEntry(rawDataSavedName + ".xml"));
		OutputStream finalStream = zipOutputStream;
		OutputFormat format = OutputFormat.createPrettyPrint();
		XMLWriter writer = new XMLWriter(finalStream, format);
		writer.write(document);


	}

	/**
	 * Function which creates an XML file with the descripcion of the raw data
	 * @param rawDataFile raw data file
	 * @return XML document
	 * @throws java.io.IOException
	 */
	public Document saveRawDataInformation(RawDataFile rawDataFile) throws IOException {
		numOfScans = rawDataFile.getNumOfScans();

		Document document = DocumentFactory.getInstance().createDocument();
		Element saveRoot = document.addElement(RawDataElementName.RAWDATA.getElementName());

		// <NAME>
		XMLUtils.fillXMLValues(saveRoot, RawDataElementName.NAME.getElementName(), null, null, rawDataFile.getName());

		// <QUANTITY>
		XMLUtils.fillXMLValues(saveRoot, RawDataElementName.QUANTITY_SCAN.getElementName(), null, null, String.valueOf(numOfScans));

		for (int scanNumber : rawDataFile.getScanNumbers()) {
			Element newElement = XMLUtils.fillXMLValues(saveRoot, RawDataElementName.SCAN.getElementName(), null, null, null);
			Scan scan = rawDataFile.getScan(scanNumber);
			fillScanElement(scan, newElement);
		}
		return document;
	}

	/**
	 * Creates the part of the XML document related to the scans
	 * @param scan 
	 * @param element 
	 */
	private void fillScanElement(Scan scan, Element element) {

		XMLUtils.fillXMLValues(element, RawDataElementName.SCAN_ID.getElementName(), null, null, String.valueOf(scan.getScanNumber()));
		XMLUtils.fillXMLValues(element, RawDataElementName.MS_LEVEL.getElementName(), null, null, String.valueOf(scan.getMSLevel()));

		if (scan.getParentScanNumber() > 0) {
			XMLUtils.fillXMLValues(element, RawDataElementName.PARENT_SCAN.getElementName(), null, null, String.valueOf(scan.getParentScanNumber()));
		}

		if (scan.getMSLevel() >= 2) {
			XMLUtils.fillXMLValues(element, RawDataElementName.PRECURSOR_MZ.getElementName(), null, null, String.valueOf(scan.getPrecursorMZ()));
		
			XMLUtils.fillXMLValues(element, RawDataElementName.PRECURSOR_CHARGE.getElementName(), null, null, String.valueOf(scan.getPrecursorCharge()));
		}
		
		XMLUtils.fillXMLValues(element, RawDataElementName.RETENTION_TIME.getElementName(), null, null, String.valueOf(scan.getRetentionTime()));
		XMLUtils.fillXMLValues(element, RawDataElementName.CENTROIDED.getElementName(), null, null, String.valueOf(scan.isCentroided()));
		XMLUtils.fillXMLValues(element, RawDataElementName.QUANTITY_DATAPOINTS.getElementName(), null, null, String.valueOf((scan.getNumberOfDataPoints())));

		if (scan.getFragmentScanNumbers() != null) {
			int[] fragmentScans = scan.getFragmentScanNumbers();
			Element newElement = XMLUtils.fillXMLValues(element, RawDataElementName.QUANTITY_FRAGMENT_SCAN.getElementName(), RawDataElementName.QUANTITY.getElementName(), String.valueOf(fragmentScans.length), null);
			for (int i : fragmentScans) {
				XMLUtils.fillXMLValues(newElement, RawDataElementName.FRAGMENT_SCAN.getElementName(), null, null, String.valueOf(i));
			}
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
