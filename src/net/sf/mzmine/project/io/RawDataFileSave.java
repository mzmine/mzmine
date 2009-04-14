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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
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
import org.jfree.xml.util.Base64;

public class RawDataFileSave {

	private double progress;
	private int numOfScans;
	private ZipOutputStream zipOutputStream;

	public RawDataFileSave(ZipOutputStream zipOutputStream){
		this.zipOutputStream = zipOutputStream;
	}

	public void writeRawDataFiles(RawDataFile rawDataFile, String rawDataSavedName) {
		try {
			progress = 0.0;
			zipOutputStream.putNextEntry(new ZipEntry(rawDataSavedName));
			copyFile(((RawDataFileImpl) rawDataFile).getScanDataFileasFile(), zipOutputStream);
			
			Document document = this.saveRawDataInformation(rawDataFile);

			zipOutputStream.putNextEntry(new ZipEntry(rawDataSavedName + ".description"));
			OutputStream finalStream = zipOutputStream;
			OutputFormat format = OutputFormat.createPrettyPrint();
			XMLWriter writer = new XMLWriter(finalStream, format);
			writer.write(document);

		} catch (Exception ex) {
			Logger.getLogger(RawDataFileSave.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void copyFile(File in, ZipOutputStream zipStream) throws Exception {
		FileInputStream fis = new FileInputStream(in);
		int totalBytes = (int) in.length();
		try {
			byte[] buffer = new byte[4096];
			int bytesRead;
			int len = 0;
			while ((bytesRead = fis.read(buffer)) != -1) {
				zipStream.write(buffer, 0, bytesRead);
				len += bytesRead;
				progress = (double) len / totalBytes;
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
	}
	public Document saveRawDataInformation(RawDataFile rawDataFile) throws IOException {		
		numOfScans = rawDataFile.getNumOfScans();

		Element newElement;
		Document document = DocumentFactory.getInstance().createDocument();
		Element saveRoot = document.addElement(RawDataElementName.RAWDATA.getElementName());

		// <NAME>
		newElement = saveRoot.addElement(RawDataElementName.NAME.getElementName());
		newElement.addText(rawDataFile.getName());

		// <QUANTITY>
		newElement = saveRoot.addElement(RawDataElementName.QUANTITY_SCAN.getElementName());
		newElement.addText(String.valueOf(numOfScans));

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

	public double getProgress() {
		return progress;
	}
}
