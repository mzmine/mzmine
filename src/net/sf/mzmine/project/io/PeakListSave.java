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

import com.Ostermiller.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListAppliedMethod;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakIdentity;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.util.Range;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class PeakListSave {

	public static DateFormat dateFormat = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm:ss");
	private Hashtable<RawDataFile, Integer> dataFilesIDMap;
	private Document document;
	private int numberOfRows;
	private double progress;
	private ZipOutputStream zipOutputStream;

	public PeakListSave(ZipOutputStream zipStream) {
		dataFilesIDMap = new Hashtable<RawDataFile, Integer>();
		this.zipOutputStream = zipStream;
	}

	public void savePeakList(PeakList peakList, String peakListSavedName) throws IOException {
		progress = 0.0;
		numberOfRows = peakList.getNumberOfRows();

		Element newElement;
		document = DocumentFactory.getInstance().createDocument();
		Element saveRoot = document.addElement(PeakListElementName.PEAKLIST.getElementName());

		// <NAME>
		XMLUtils.fillXMLValues(saveRoot, PeakListElementName.NAME.getElementName(), null, null, peakList.getName());

		// <PEAKLIST_DATE>
		String dateText = "";
		if (((SimplePeakList) peakList).getDateCreated() == null) {
			dateText = ((SimplePeakList) peakList).getDateCreated();
		} else {
			Date date = new Date();
			dateText = dateFormat.format(date);
		}
		XMLUtils.fillXMLValues(saveRoot, PeakListElementName.PEAKLIST_DATE.getElementName(), null, null, dateText);


		// <QUANTITY>
		XMLUtils.fillXMLValues(saveRoot, PeakListElementName.QUANTITY.getElementName(), null, null, String.valueOf(numberOfRows));


		// <PROCESS>
		PeakListAppliedMethod[] processes = peakList.getAppliedMethods();
		for (PeakListAppliedMethod proc : processes) {
			XMLUtils.fillXMLValues(saveRoot, PeakListElementName.PROCESS.getElementName(), null, null, proc.getDescription());
		}

		// <RAWFILE>
		RawDataFile[] dataFiles = peakList.getRawDataFiles();

		for (int i = 1; i <= dataFiles.length; i++) {
			newElement = XMLUtils.fillXMLValues(saveRoot, PeakListElementName.RAWFILE.getElementName(), PeakListElementName.ID.getElementName(), String.valueOf(i), null);
			fillRawDataFileElement(dataFiles[i - 1], newElement);
			dataFilesIDMap.put(dataFiles[i - 1], i);
		}

		// <ROW>		
		PeakListRow row;
		for (int i = 0; i < numberOfRows; i++) {
			row = peakList.getRow(i);
			newElement = XMLUtils.fillXMLValues(saveRoot, PeakListElementName.ROW.getElementName(), PeakListElementName.ID.getElementName(), String.valueOf(row.getID()), null);
			fillRowElement(row, newElement);
			progress = (double) i / numberOfRows;
		}

		zipOutputStream.putNextEntry(new ZipEntry(peakListSavedName + ".xml"));
		OutputStream finalStream = zipOutputStream;
		OutputFormat format = OutputFormat.createPrettyPrint();
		XMLWriter writer = new XMLWriter(finalStream, format);
		writer.write(document);
	}

	/**
	 * @param row
	 * @param element
	 */
	private void fillRowElement(PeakListRow row, Element element) {
		Element newElement;

		// <PEAK_IDENTITY>
		PeakIdentity preferredIdentity = row.getPreferredPeakIdentity();
		PeakIdentity[] identities = row.getPeakIdentities();

		for (int i = 0; i < identities.length; i++) {
			newElement = XMLUtils.fillXMLValues(element, PeakListElementName.PEAK_IDENTITY.getElementName(), PeakListElementName.ID.getElementName(), String.valueOf(i), null);
			newElement.addAttribute(PeakListElementName.PREFERRED.getElementName(), String.valueOf(identities[i] == preferredIdentity));
			fillIdentityElement(identities[i], newElement);
		}

		// <PEAK>
		int dataFileID = 0;
		ChromatographicPeak[] peaks = row.getPeaks();
		for (ChromatographicPeak p : peaks) {
			newElement = XMLUtils.fillXMLValues(element, PeakListElementName.PEAK.getElementName(), null, null, null);

			dataFileID = dataFilesIDMap.get(p.getDataFile());
			fillPeakElement(p, newElement, dataFileID);
		}

	}

	/**
	 * @param file
	 * @param element
	 */
	private void fillRawDataFileElement(RawDataFile file, Element element) {

		// <NAME>
		XMLUtils.fillXMLValues(element, "rawdata_name", null, null, file.getName());


		// <RTRANGE>
		Range RTRange = file.getDataRTRange(1);
		XMLUtils.fillXMLValues(element, PeakListElementName.RTRANGE.getElementName(), null, null, String.valueOf(RTRange.getMin()+"-"+RTRange.getMax()));

		// <MZRANGE>
		Range MZRange = file.getDataMZRange(1);
		XMLUtils.fillXMLValues(element, PeakListElementName.MZRANGE.getElementName(), null, null, String.valueOf(MZRange.getMin()+"-"+MZRange.getMax()));

	}

	/**
	 * @param identity
	 * @param element
	 */
	private void fillIdentityElement(PeakIdentity identity, Element element) {


		// <NAME>
		XMLUtils.fillXMLValues(element, PeakListElementName.NAME.getElementName(), null, null, identity.getName() != null ? identity.getName() : " ");

		
		// <FORMULA>
		String formula = "";
		if (identity instanceof SimplePeakIdentity) {
			SimplePeakIdentity id = (SimplePeakIdentity) identity;
			formula = id.getCompoundFormula();
		}
		XMLUtils.fillXMLValues(element, PeakListElementName.FORMULA.getElementName(), null, null, formula);

		// <IDENTIFICATION>
		XMLUtils.fillXMLValues(element, PeakListElementName.IDENTIFICATION.getElementName(), null, null, identity.getIdentificationMethod() != null ? identity.getIdentificationMethod() : " ");
	}

	/**
	 * @param peak
	 * @param element
	 * @param dataFileID
	 */
	private void fillPeakElement(ChromatographicPeak peak, Element element,
			int dataFileID) {

		element.addAttribute(PeakListElementName.COLUMN.getElementName(),
				String.valueOf(dataFileID));
		element.addAttribute(PeakListElementName.MASS.getElementName(), String.valueOf(peak.getMZ()));
		element.addAttribute(PeakListElementName.RT.getElementName(), String.valueOf(peak.getRT()));
		element.addAttribute(PeakListElementName.HEIGHT.getElementName(),
				String.valueOf(peak.getHeight()));
		element.addAttribute(PeakListElementName.AREA.getElementName(), String.valueOf(peak.getArea()));
		element.addAttribute(PeakListElementName.STATUS.getElementName(), peak.getPeakStatus().toString());

		// <MZPEAK>
		int scanNumbers[] = peak.getScanNumbers();
		Element newElement = XMLUtils.fillXMLValues(element, PeakListElementName.MZPEAK.getElementName(), PeakListElementName.QUANTITY.getElementName(), String.valueOf(scanNumbers.length), null);

		ByteArrayOutputStream byteScanStream = new ByteArrayOutputStream();
		DataOutputStream dataScanStream = new DataOutputStream(byteScanStream);

		ByteArrayOutputStream byteMassStream = new ByteArrayOutputStream();
		DataOutputStream dataMassStream = new DataOutputStream(byteMassStream);

		ByteArrayOutputStream byteHeightStream = new ByteArrayOutputStream();
		DataOutputStream dataHeightStream = new DataOutputStream(
				byteHeightStream);

		float mass, height;
		for (int scan : scanNumbers) {
			try {
				dataScanStream.writeInt(scan);
				dataScanStream.flush();
				DataPoint mzPeak = peak.getDataPoint(scan);
				if (mzPeak != null) {
					mass = (float) mzPeak.getMZ();
					height = (float) mzPeak.getIntensity();
				} else {
					mass = (float) peak.getMZ();
					height = 0f;
				}
				dataMassStream.writeFloat(mass);
				dataMassStream.flush();
				dataHeightStream.writeFloat(height);
				dataHeightStream.flush();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		byte[] bytes = Base64.encode(byteScanStream.toByteArray());
		XMLUtils.fillXMLValues(newElement, PeakListElementName.SCAN_ID.getElementName(), null, null, new String(bytes));

		
		bytes = Base64.encode(byteMassStream.toByteArray());
		XMLUtils.fillXMLValues(newElement, PeakListElementName.MASS.getElementName(), null, null, new String(bytes));

		
		bytes = Base64.encode(byteHeightStream.toByteArray());
		XMLUtils.fillXMLValues(newElement, PeakListElementName.HEIGHT.getElementName(), null, null, new String(bytes));


	}

	public double getProgress() {
		return progress;
	}
}
