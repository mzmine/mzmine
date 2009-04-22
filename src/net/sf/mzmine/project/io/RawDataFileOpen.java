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
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.impl.CachedStorableScan;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.project.impl.StorableScan;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RawDataFileOpen extends DefaultHandler {

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
	private double progress;
	private int stepNumber;
	private int storageFileOffset;
	private ZipInputStream zipInputStream;
	private ZipFile zipFile;
	private int fragmentCount;
	private SaveFileUtils saveFileUtils;

	public RawDataFileOpen(ZipInputStream zipInputStream, ZipFile zipFile) {
		this.zipInputStream = zipInputStream;
		this.zipFile = zipFile;
		charBuffer = new StringBuffer();
	}

	public void readRawDataFile(String Name) throws ClassNotFoundException {
		try {
			stepNumber = 0;
			// Writes the scan file into a temporal file
			String fileName = zipInputStream.getNextEntry().getName();

			File tempConfigFile = File.createTempFile("mzmine", ".scans");
			FileOutputStream fileStream = new FileOutputStream(tempConfigFile);
			stepNumber++;
			this.saveFileUtils = new SaveFileUtils();
			saveFileUtils.saveFile(zipInputStream, fileStream, this.zipFile.getEntry(fileName).getSize(), SaveFileUtilsMode.CLOSE_OUT);
			fileStream.close();

			this.rawDataFileWriter = new RawDataFileImpl();
			((RawDataFileImpl) rawDataFileWriter).setScanDataFile(tempConfigFile);
			this.rawDataFileWriter.setName(Name);

			stepNumber++;
			InputStream InputStream = zipFile.getInputStream(zipInputStream.getNextEntry());

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(InputStream, this);


			RawDataFile rawDataFile = rawDataFileWriter.finishWriting();
			MZmineCore.getCurrentProject().addFile(rawDataFile);

		} catch (Exception ex) {

			Logger.getLogger(RawDataFileOpen.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public double getProgress() {
		switch (stepNumber) {
			case 1:
				return saveFileUtils.progress * 0.5;
			case 2:
				return progress;
			default:
				return 0.0;
		}
	}

	public void startElement(String namespaceURI, String lName, // local name
			String qName, // qualified name
			Attributes attrs) throws SAXException {

		if (qName.equals(RawDataElementName.QUANTITY_FRAGMENT_SCAN.getElementName())) {
			this.numberOfFragments = Integer.parseInt(attrs.getValue(RawDataElementName.QUANTITY.getElementName()));
			if (this.numberOfFragments > 0) {
				this.fragmentScan = new int[this.numberOfFragments];
				this.fragmentCount = 0;
			}
		}
	}

	public void endElement(String namespaceURI, String sName, // simple name
			String qName // qualified name
			) throws SAXException {

		// <NAME>
		if (qName.equals(RawDataElementName.NAME.getElementName())) {
			try {
				getTextOfElement();
				this.scansReaded = 0;
			} catch (Exception ex) {
				Logger.getLogger(RawDataFileOpen.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		if (qName.equals(RawDataElementName.QUANTITY_SCAN.getElementName())) {
			numberOfScans = Integer.parseInt(getTextOfElement());
		}

		if (qName.equals(RawDataElementName.SCAN_ID.getElementName())) {

			this.ScanNumber = Integer.parseInt(getTextOfElement());
			progress = ((double) scansReaded / numberOfScans) * 0.5 + 0.5;
			scansReaded++;
		}

		if (qName.equals(RawDataElementName.MS_LEVEL.getElementName())) {
			this.msLevel = Integer.parseInt(getTextOfElement());
		}

		if (qName.equals(RawDataElementName.PARENT_SCAN.getElementName())) {
			this.parentScan = Integer.parseInt(getTextOfElement());
		}

		if (qName.equals(RawDataElementName.PRECURSOR_MZ.getElementName())) {
			this.precursorMZ = Double.parseDouble(getTextOfElement());
		}
		if (qName.equals(RawDataElementName.PRECURSOR_CHARGE.getElementName())) {
			this.precursorCharge = Integer.parseInt(getTextOfElement());
		}
		if (qName.equals(RawDataElementName.RETENTION_TIME.getElementName())) {
			this.retentionTime = Double.parseDouble(getTextOfElement());
		}
		if (qName.equals(RawDataElementName.CENTROIDED.getElementName())) {
			this.centroided = Boolean.parseBoolean(getTextOfElement());
		}
		if (qName.equals(RawDataElementName.QUANTITY_DATAPOINTS.getElementName())) {
			this.dataPointsNumber = Integer.parseInt(getTextOfElement());
		}
		if (qName.equals(RawDataElementName.FRAGMENT_SCAN.getElementName())) {
			fragmentScan[fragmentCount++] = Integer.parseInt(getTextOfElement());
		}


		if (qName.equals(RawDataElementName.SCAN.getElementName())) {
			try {

				int storageArrayByteLength = this.dataPointsNumber * 8 * 2;
				StorableScan scan = new CachedStorableScan(this.ScanNumber, this.msLevel, this.retentionTime,
						this.parentScan, this.precursorMZ, this.precursorCharge, this.fragmentScan,
						null, this.centroided, this.rawDataFileWriter);
				scan.setParameters(storageFileOffset, storageArrayByteLength, this.dataPointsNumber);
				this.rawDataFileWriter.addScan(scan);
				storageFileOffset += storageArrayByteLength;

			} catch (Exception ex) {
				Logger.getLogger(RawDataFileOpen.class.getName()).log(Level.SEVERE, null, ex);
			}

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
