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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.impl.CachedStorableScan;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RawDataFileOpen extends DefaultHandler {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private StringBuffer charBuffer;
	private RawDataFileImpl rawDataFileWriter;
	private int numberOfScans;
	private int ScanNumber;
	private int msLevel;
	private int parentScan;
	private int[] fragmentScan;
	private int numberOfFragments;
	private double precursorMZ;
	private int precursorCharge;
	private double retentionTime;
	private boolean centroided;
	private int dataPointsNumber;
	private int scansReaded;
	private double progress = 0.5;
	private int stepNumber;
	private int storageFileOffset;
	private int fragmentCount;
	private SaveFileUtils saveFileUtils;
	private String fileName;

	public RawDataFileOpen() {		
		charBuffer = new StringBuffer();
	}

	/**
	 * Extracts the scan file and copies it into the temporal folder.
	 * Creates a new raw data file using the information
	 * from the XML raw data description file
	 * @param Name raw data file name
	 * @throws java.lang.ClassNotFoundException
	 */
	public void readRawDataFile(ZipFile zipFile, ZipEntry entry, ZipInputStream zipInputStream) throws Exception {
		stepNumber = 0;
		// Writes the scan file into a temporal file
		fileName = entry.getName();
		logger.info("Moving scan file : " + fileName + " to the temporal folder");
		stepNumber++;
		File tempConfigFile = File.createTempFile("mzmine", ".scans");
		FileOutputStream fileStream = new FileOutputStream(tempConfigFile);
		
		// Extracts the scan file from the zip project file to the temporal folder
		saveFileUtils = new SaveFileUtils();
		saveFileUtils.saveFile(zipFile.getInputStream(entry), fileStream, zipFile.getEntry(fileName).getSize(), SaveFileUtilsMode.CLOSE_OUT);
		fileStream.close();

		rawDataFileWriter = new RawDataFileImpl();
		((RawDataFileImpl) rawDataFileWriter).setScanDataFile(tempConfigFile);


		stepNumber++;
		// Extracts the raw data description file from the zip project file
		InputStream InputStream = zipFile.getInputStream(zipInputStream.getNextEntry());

		// Reads the XML file (raw data description)
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		saxParser.parse(InputStream, this);

		// Adds the raw data file to MZmine
		RawDataFile rawDataFile = rawDataFileWriter.finishWriting();
		MZmineCore.getCurrentProject().addFile(rawDataFile);
	}

	/**
	 * @return the progress of these functions loading the raw data from the zip file
	 */
	public double getProgress() {
		switch (stepNumber) {
			case 1:
				return saveFileUtils.getProgress() * 0.5;
			case 2:
				return progress;
			default:
				return 0.0;
		}
	}

	/**
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
	 *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String namespaceURI, String lName, // local name
			String qName, // qualified name
			Attributes attrs) throws SAXException {

		if (qName.equals(RawDataElementName.QUANTITY_FRAGMENT_SCAN.getElementName())) {
			numberOfFragments = Integer.parseInt(attrs.getValue(RawDataElementName.QUANTITY.getElementName()));
			if (numberOfFragments > 0) {
				fragmentScan = new int[numberOfFragments];
				fragmentCount = 0;
			}
		}
	}

	/**
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public void endElement(String namespaceURI, String sName, // simple name
			String qName // qualified name
			) throws SAXException {

		// <NAME>
		if (qName.equals(RawDataElementName.NAME.getElementName())) {
			try {
				// Adds the scan file and the name to the new raw data file
				String name = getTextOfElement();
				logger.info("Loading raw data file: " + name);
				rawDataFileWriter.setName(name);

				scansReaded = 0;
			} catch (Exception ex) {
				Logger.getLogger(RawDataFileOpen.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		if (qName.equals(RawDataElementName.QUANTITY_SCAN.getElementName())) {
			numberOfScans = Integer.parseInt(getTextOfElement());
		}

		if (qName.equals(RawDataElementName.SCAN_ID.getElementName())) {
			ScanNumber = Integer.parseInt(getTextOfElement());
			progress = ((double) scansReaded / numberOfScans) * 0.5 + 0.5;
			scansReaded++;
		}

		if (qName.equals(RawDataElementName.MS_LEVEL.getElementName())) {
			msLevel = Integer.parseInt(getTextOfElement());
		}

		if (qName.equals(RawDataElementName.PARENT_SCAN.getElementName())) {
			parentScan = Integer.parseInt(getTextOfElement());
		}

		if (qName.equals(RawDataElementName.PRECURSOR_MZ.getElementName())) {
			precursorMZ = Double.parseDouble(getTextOfElement());
		}
		if (qName.equals(RawDataElementName.PRECURSOR_CHARGE.getElementName())) {
			precursorCharge = Integer.parseInt(getTextOfElement());
		}
		if (qName.equals(RawDataElementName.RETENTION_TIME.getElementName())) {
			retentionTime = Double.parseDouble(getTextOfElement());
		}
		if (qName.equals(RawDataElementName.CENTROIDED.getElementName())) {
			centroided = Boolean.parseBoolean(getTextOfElement());
		}
		if (qName.equals(RawDataElementName.QUANTITY_DATAPOINTS.getElementName())) {
			dataPointsNumber = Integer.parseInt(getTextOfElement());
		}
		if (qName.equals(RawDataElementName.FRAGMENT_SCAN.getElementName())) {
			fragmentScan[fragmentCount++] = Integer.parseInt(getTextOfElement());
		}
		if (qName.equals(RawDataElementName.SCAN.getElementName())) {
			try {

				int storageArrayByteLength = dataPointsNumber * 8 * 2;
				CachedStorableScan scan = new CachedStorableScan(ScanNumber, msLevel, retentionTime,
						parentScan, precursorMZ, precursorCharge, fragmentScan,
						null, centroided, rawDataFileWriter);
				scan.setParameters(storageFileOffset, storageArrayByteLength, dataPointsNumber);
				rawDataFileWriter.addScan(scan);
				storageFileOffset += storageArrayByteLength;

			} catch (Exception ex) {
				Logger.getLogger(RawDataFileOpen.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public String getRawDataName() {
		return fileName;
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
		charBuffer.delete(0, charBuffer.length());
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
}
