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
package net.sf.mzmine.project.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.jfree.xml.util.Base64;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RawDataFileSerializer extends DefaultHandler {

	private ZipOutputStream zipOutputStream;
	private ZipInputStream zipInputStream;
	private int scanDataFileBytes;
	private StringBuffer charBuffer;
	RawDataFileWriter rawDataFileWriter;
	DoubleBuffer doubleBuffer;
	int ScanNumber;
	int msLevel;
	int parentScan;
	int[] fragmentScan;
	int fragmentScanNumber;
	double precursorMZ;
	double retentionTime;	
	boolean centroided;
	int dataPointsNumber;

	public RawDataFileSerializer(ZipOutputStream zipOutputStream) {
		this.zipOutputStream = zipOutputStream;
	}

	public RawDataFileSerializer(ZipInputStream zipInputStream) {
		this.zipInputStream = zipInputStream;
	}

	public void writeRawDataFiles(RawDataFile rawDataFile) {
		try {
			int cont = 0;
			String newName = rawDataFile.getName() + "-" + cont++;

			zipOutputStream.putNextEntry(new ZipEntry(newName));
			copyFile(((RawDataFileImpl) rawDataFile).getScanDataFileasFile(), zipOutputStream);

			Document document = this.saveRawDataInformation(rawDataFile);

			zipOutputStream.putNextEntry(new ZipEntry(newName + ".description"));
			OutputStream finalStream = zipOutputStream;
			OutputFormat format = OutputFormat.createPrettyPrint();
			XMLWriter writer = new XMLWriter(finalStream, format);
			writer.write(document);

		} catch (Exception ex) {
			Logger.getLogger(RawDataFileSerializer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void copyFile(File in, ZipOutputStream zipStream) throws Exception {
		FileInputStream fis = new FileInputStream(in);
		try {
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = fis.read(buffer)) != -1) {
				zipStream.write(buffer, 0, bytesRead);
				this.scanDataFileBytes += bytesRead;
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
	}

	public int getscansbytes() {
		return this.scanDataFileBytes;
	}

	public void readRawDataFile() throws ClassNotFoundException {
		try {

			// Writes the scan file into a temporal file
			zipInputStream.getNextEntry();
			File tempConfigFile = File.createTempFile("mzmine", ".scans");
			FileOutputStream fileStream = new FileOutputStream(tempConfigFile);
			int cont = 0;
			byte buffer[] = new byte[1 << 10]; // 1 MB buffer
			int len;
			while ((len = zipInputStream.read(buffer)) > 0) {
				fileStream.write(buffer, 0, len);
				cont += len;
			}
			fileStream.close();

			// Creates the new RawDataFile reading the scans from the temporal file and adding them
			// to the RawDataFile
			ByteBuffer bbuffer = ByteBuffer.allocate(cont);
			RandomAccessFile storageFile = new RandomAccessFile(tempConfigFile, "r");
			synchronized (storageFile) {
				try {
					storageFile.seek(0);
					storageFile.read(bbuffer.array(), 0, cont);
				} catch (IOException e) {
				}
			}
			storageFile.close();

			doubleBuffer = bbuffer.asDoubleBuffer();



			// Reads RawDataDescription
			zipInputStream.getNextEntry();
			charBuffer = new StringBuffer();
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(zipInputStream, this);





			/*RawDataFileWriter rawDataFileWriter = MZmineCore.createNewFile(rawDataDescription.getName());
			for (int numScans = 0; numScans < rawDataDescription.getNumOfScans(); numScans++) {
			DataPoint[] dataPoints = new DataPoint[rawDataDescription.getNumOfDataPoints(numScans)];
			for (int j = 0; j < rawDataDescription.getNumOfDataPoints(numScans); j++) {
			dataPoints[j] = new SimpleDataPoint(doubleBuffer.get(), doubleBuffer.get());
			}
			Scan scan = new SimpleScan((RawDataFileImpl) rawDataFileWriter, rawDataDescription.getScanNumber(numScans),
			rawDataDescription.getMsLevel(numScans), rawDataDescription.getRetentionTime(numScans),
			rawDataDescription.getParentScan(numScans), rawDataDescription.getPrecursorMZ(numScans),
			rawDataDescription.getFragmentScans(numScans), dataPoints, rawDataDescription.isCentroided(numScans));
			rawDataFileWriter.addScan(scan);
			}
			RawDataFile rawDataFile = rawDataFileWriter.finishWriting();
			MZmineCore.getCurrentProject().addFile(rawDataFile);*/

			tempConfigFile.delete();
		} catch (ParserConfigurationException ex) {
			Logger.getLogger(RawDataFileSerializer.class.getName()).log(Level.SEVERE, null, ex);
		} catch (SAXException ex) {
			Logger.getLogger(RawDataFileSerializer.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(RawDataFileSerializer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private Document saveRawDataInformation(RawDataFile rawDataFile) throws IOException {
		Element newElement;
		Document document = DocumentFactory.getInstance().createDocument();
		Element saveRoot = document.addElement(RawDataElementName.RAWDATA.getElementName());

		// <NAME>
		newElement = saveRoot.addElement(RawDataElementName.NAME.getElementName());
		newElement.addText(rawDataFile.getName());

		// <SCAN>
		//newElement = saveRoot.addElement(RawDataElementName.QUANTITY_SCAN.getElementName());
		//newElement.addText(String.valueOf(rawDataFile.getNumOfScans()));

		for (int scanNumber : rawDataFile.getScanNumbers()) {
			newElement = saveRoot.addElement(RawDataElementName.SCAN.getElementName());
			Scan scan = rawDataFile.getScan(scanNumber);
			this.fillScanElement(scan, newElement);
		}
		return document;
	}

	private void fillScanElement(Scan scan, Element element) {
		Element newElement;
		newElement = element.addElement(RawDataElementName.SCAN_ID.getElementName());
		newElement.addText(String.valueOf(scan.getScanNumber()));

		newElement = element.addElement(RawDataElementName.MS_LEVEL.getElementName());
		newElement.addText(String.valueOf(scan.getMSLevel()));

		newElement = element.addElement(RawDataElementName.PARENT_SCAN.getElementName());
		newElement.addText(String.valueOf(scan.getParentScanNumber()));

		newElement = element.addElement(RawDataElementName.PRECURSOR_MZ.getElementName());
		newElement.addText(String.valueOf(scan.getPrecursorMZ()));
		
		newElement = element.addElement(RawDataElementName.RETENTION_TIME.getElementName());
		newElement.addText(String.valueOf(scan.getRetentionTime()));

		newElement = element.addElement(RawDataElementName.CENTROIDED.getElementName());
		newElement.addText(String.valueOf(scan.isCentroided()));

		newElement = element.addElement(RawDataElementName.QUANTITY_DATAPOINTS.getElementName());
		newElement.addText(String.valueOf(scan.getNumberOfDataPoints()));

		newElement = element.addElement(RawDataElementName.QUANTITY_FRANGMENT_SCAN.getElementName());
		if (scan.getFragmentScanNumbers() == null) {
			newElement.addAttribute("quantity", "0");
			return;
		}
		newElement.addAttribute("quantity", String.valueOf(scan.getFragmentScanNumbers().length));

		ByteArrayOutputStream byteScanStream = new ByteArrayOutputStream();
		DataOutputStream dataScanStream = new DataOutputStream(byteScanStream);


		for (int fragmentNumber : scan.getFragmentScanNumbers()) {
			try {
				dataScanStream.writeInt(fragmentNumber);
				dataScanStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		char[] bytes = Base64.encode(byteScanStream.toByteArray());
		newElement = element.addElement(RawDataElementName.QUANTITY_FRANGMENT_SCAN.getElementName());
		newElement.addText(new String(bytes));
	}

	public void startElement(String namespaceURI, String lName, // local name
			String qName, // qualified name
			Attributes attrs) throws SAXException {

		if (qName.equals(RawDataElementName.QUANTITY_FRANGMENT_SCAN.getElementName())) {
			this.fragmentScan = new int[Integer.parseInt(attrs.getValue("quantity"))];
		}
	}

	public void endElement(String namespaceURI, String sName, // simple name
			String qName // qualified name
			) throws SAXException {

		// <NAME>
		if (qName.equals(RawDataElementName.NAME.getElementName())) {
			try {
				this.rawDataFileWriter = MZmineCore.createNewFile(getTextOfElement());
			} catch (IOException ex) {
				Logger.getLogger(RawDataFileSerializer.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		if (qName.equals(RawDataElementName.SCAN_ID.getElementName())) {
			this.ScanNumber = Integer.parseInt(getTextOfElement());
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
						Logger.getLogger(RawDataFileSerializer.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		}
		if (qName.equals(RawDataElementName.SCAN.getElementName())) {
			try {
				DataPoint[] dataPoints = new DataPoint[this.dataPointsNumber];
				for (int j = 0; j < this.dataPointsNumber; j++) {
					dataPoints[j] = new SimpleDataPoint(doubleBuffer.get(), doubleBuffer.get());
				}
				Scan scan = new SimpleScan((RawDataFileImpl) rawDataFileWriter, this.ScanNumber, this.msLevel, this.retentionTime, this.parentScan, this.precursorMZ, this.fragmentScan, dataPoints, this.centroided);
				rawDataFileWriter.addScan(scan);
			} catch (IOException ex) {
				Logger.getLogger(RawDataFileSerializer.class.getName()).log(Level.SEVERE, null, ex);
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
