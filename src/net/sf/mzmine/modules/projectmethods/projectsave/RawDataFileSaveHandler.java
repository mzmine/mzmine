/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.MassList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.StreamCopy;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.schlichtherle.util.zip.ZipEntry;
import de.schlichtherle.util.zip.ZipOutputStream;

class RawDataFileSaveHandler {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private int numOfScans, completedScans;
	private ZipOutputStream zipOutputStream;
	private StreamCopy copyMachine;
	private boolean canceled = false;

	RawDataFileSaveHandler(ZipOutputStream zipOutputStream) {
		this.zipOutputStream = zipOutputStream;
	}

	/**
	 * Copy the scan file of the raw data file from the temporary folder to the
	 * zip file. Create an XML file which contains the description of the same
	 * raw data file an copy it into the same zip file.
	 * 
	 * @param rawDataFile
	 *            raw data file to be copied
	 * @param rawDataSavedName
	 *            name of the raw data inside the zip file
	 * @throws java.io.IOException
	 * @throws TransformerConfigurationException
	 * @throws SAXException
	 */
	void writeRawDataFile(RawDataFile rawDataFile, int number)
			throws IOException, TransformerConfigurationException, SAXException {

		numOfScans = rawDataFile.getNumOfScans();
		completedScans = 0;

		// step 1 - save scan file
		logger.info("Saving scan file of: " + rawDataFile.getName());

		String rawDataSavedName = "Raw data file #" + number + " "
				+ rawDataFile.getName();

		zipOutputStream.putNextEntry(new ZipEntry(rawDataSavedName + ".scans"));

		File scanFile = ((RawDataFileImpl) rawDataFile).getScanFile();
		FileInputStream fileStream = new FileInputStream(scanFile);
		copyMachine = new StreamCopy();
		copyMachine.copy(fileStream, zipOutputStream, scanFile.length());
		fileStream.close();

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
	void saveRawDataInformation(RawDataFile rawDataFile, TransformerHandler hd)
			throws SAXException, IOException {

		AttributesImpl atts = new AttributesImpl();

		hd.startElement("", "", RawDataElementName.RAWDATA.getElementName(),
				atts);
		atts.clear();

		// <NAME>
		hd.startElement("", "", RawDataElementName.NAME.getElementName(), atts);
		hd.characters(rawDataFile.getName().toCharArray(), 0, rawDataFile
				.getName().length());
		hd.endElement("", "", RawDataElementName.NAME.getElementName());

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

			hd.startElement("", "", RawDataElementName.SCAN.getElementName(),
					atts);
			Scan scan = rawDataFile.getScan(scanNumber);
			fillScanElement(scan, hd);
			hd.endElement("", "", RawDataElementName.SCAN.getElementName());
			completedScans++;
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

		// <PARENT_SCAN>
		if (scan.getParentScanNumber() > 0) {
			hd.startElement("", "",
					RawDataElementName.PARENT_SCAN.getElementName(), atts);
			hd.characters(String.valueOf(scan.getParentScanNumber())
					.toCharArray(), 0,
					String.valueOf(scan.getParentScanNumber()).length());
			hd.endElement("", "",
					RawDataElementName.PARENT_SCAN.getElementName());
		}

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
		hd.characters(String.valueOf(scan.getRetentionTime()).toCharArray(), 0,
				String.valueOf(scan.getRetentionTime()).length());
		hd.endElement("", "",
				RawDataElementName.RETENTION_TIME.getElementName());

		// <CENTROIDED>
		hd.startElement("", "", RawDataElementName.CENTROIDED.getElementName(),
				atts);
		hd.characters(String.valueOf(scan.isCentroided()).toCharArray(), 0,
				String.valueOf(scan.isCentroided()).length());
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
			atts.addAttribute("", "", RawDataElementName.NAME.getElementName(),
					"CDATA", massList.getName());
			hd.startElement("", "",
					RawDataElementName.MASS_LIST.getElementName(), atts);
			atts.clear();
			fillMassListElement(massList, hd);
			hd.endElement("", "", RawDataElementName.MASS_LIST.getElementName());

		}
	}

	/**
	 * Create the part of the XML document related to a mass list
	 * 
	 */
	private void fillMassListElement(MassList massList, TransformerHandler hd)
			throws SAXException, IOException {

		DataPoint mzPeaks[] = massList.getMzPeaks();
		char encodedDataPoints[] = ScanUtils.encodeDataPointsBase64(mzPeaks);
		hd.characters(encodedDataPoints, 0, encodedDataPoints.length);
		hd.endElement("", "", RawDataElementName.MZ_PEAK.getElementName());

	}

	/**
	 * 
	 * @return the progress of these functions saving the raw data information
	 *         to the zip file.
	 */
	double getProgress() {

		if (copyMachine == null)
			return 0;

		double progress = 0;

		if (copyMachine.finished()) {

			// We can estimate that copying the scan file takes ~75% of the time
			progress = 0.75;
			if (numOfScans != 0)
				progress += 0.25 * (double) completedScans / numOfScans;
		} else {
			progress = copyMachine.getProgress() * 0.75;
		}

		return progress;

	}

	void cancel() {
		canceled = true;
		if (copyMachine != null)
			copyMachine.cancel();
	}
}
