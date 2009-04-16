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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;

import net.sf.mzmine.project.impl.CachedStorableScan;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.project.impl.StorableScan;
import org.jfree.xml.util.Base64;
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
	private double retentionTime;
	private boolean centroided;
	private int dataPointsNumber;
	private int scansReaded;
	private double progress;	
	int storageFileOffset;
	ByteBuffer bbuffer;
	private ZipInputStream zipInputStream;
	private File tempConfigFile;
	ReadableByteChannel in;
	WritableByteChannel out;

	public RawDataFileOpen(ZipInputStream zipInputStream) {
		this.zipInputStream = zipInputStream;
		charBuffer = new StringBuffer();
	}

	public RawDataFile getRawDataFile() {
		return this.rawDataFileWriter;
	}

	public void readRawDataFile(String Name) throws ClassNotFoundException {
		try {

			// Writes the scan file into a temporal file
			zipInputStream.getNextEntry();
			tempConfigFile = File.createTempFile("mzmine", ".scans");
			FileOutputStream fileStream = new FileOutputStream(tempConfigFile);

			in = Channels.newChannel(zipInputStream);
			out = Channels.newChannel(fileStream);

			bbuffer = ByteBuffer.allocate(65536);

			while (in.read(bbuffer) != -1) {
				bbuffer.flip();
				out.write(bbuffer);
				bbuffer.clear();
			}

			
			this.rawDataFileWriter = new RawDataFileImpl();
			((RawDataFileImpl) rawDataFileWriter).setScanDataFile(tempConfigFile);
			this.rawDataFileWriter.setName(Name);


			zipInputStream.getNextEntry();
			InputStream InputStream = new UnclosableInputStream(zipInputStream);

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
		return progress;
	}

	public void startElement(String namespaceURI, String lName, // local name
			String qName, // qualified name
			Attributes attrs) throws SAXException {

		if (qName.equals(RawDataElementName.QUANTITY_FRANGMENT_SCAN.getElementName())) {
			this.numberOfFragments = Integer.parseInt(attrs.getValue(RawDataElementName.QUANTITY.getElementName()));
			if (this.numberOfFragments > 0) {
				this.fragmentScan = new int[this.numberOfFragments];
			}
		}
	}

	public void endElement(String namespaceURI, String sName, // simple name
			String qName // qualified name
			) throws SAXException {

		// <NAME>
		if (qName.equals(RawDataElementName.NAME.getElementName())) {
			try {
				//this.rawDataFileWriter = MZmineCore.createNewFile();
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
			progress = (double) scansReaded / numberOfScans;
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
		if (qName.equals(RawDataElementName.RETENTION_TIME.getElementName())) {
			this.retentionTime = Double.parseDouble(getTextOfElement());
		}
		if (qName.equals(RawDataElementName.CENTROIDED.getElementName())) {
			this.centroided = Boolean.parseBoolean(getTextOfElement());
		}
		if (qName.equals(RawDataElementName.QUANTITY_DATAPOINTS.getElementName())) {
			this.dataPointsNumber = Integer.parseInt(getTextOfElement());
		}
		if (qName.equals(RawDataElementName.QUANTITY_FRANGMENT_SCAN.getElementName())) {
			byte[] bytes = Base64.decode(getTextOfElement().toCharArray());
			// make a data input stream
			DataInputStream dataInputStream = new DataInputStream(
					new ByteArrayInputStream(bytes));
			if (this.fragmentScan != null) {
				for (int i = 0; i < this.fragmentScan.length; i++) {
					try {
						fragmentScan[i] = dataInputStream.readInt();
					} catch (IOException ex) {					
					}
				}
			}
		}


		if (qName.equals(RawDataElementName.SCAN.getElementName())) {
			try {
				
				int storageArrayByteLength = this.dataPointsNumber * 8 * 2;				
				StorableScan scan = new CachedStorableScan(this.ScanNumber, this.msLevel, this.retentionTime,
						this.parentScan, this.precursorMZ, this.fragmentScan,
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
