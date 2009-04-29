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
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListAppliedMethod;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.data.impl.SimplePeakIdentity;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.Range;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class PeakListSave {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	public static DateFormat dateFormat = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm:ss");
	private Hashtable<RawDataFile, Integer> dataFilesIDMap;
	private int numberOfRows;
	private double progress;
	private ZipOutputStream zipOutputStream;

	public PeakListSave(ZipOutputStream zipStream) {
		dataFilesIDMap = new Hashtable<RawDataFile, Integer>();
		this.zipOutputStream = zipStream;
	}

	/**
	 * Create an XML document with the peak list information an save it into
	 * the project zip file
	 * @param peakList
	 * @param peakListSavedName name of the peak list
	 * @throws java.io.IOException
	 */
	public void savePeakList(PeakList peakList, String peakListSavedName) throws IOException, TransformerConfigurationException, SAXException {
		logger.info("Saving peak list: " + peakList.getName());

		progress = 0.0;
		numberOfRows = peakList.getNumberOfRows();

		zipOutputStream.putNextEntry(new ZipEntry(peakListSavedName + ".xml"));
		OutputStream finalStream = zipOutputStream;
		StreamResult streamResult = new StreamResult(finalStream);
		SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
		
		TransformerHandler hd = tf.newTransformerHandler();
		Transformer serializer = hd.getTransformer();

		serializer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
		
		hd.setResult(streamResult);
		hd.startDocument();
		AttributesImpl atts = new AttributesImpl();

		hd.startElement("", "", PeakListElementName.PEAKLIST.getElementName(), atts);
		atts.clear();

		// <NAME>
		hd.startElement("", "", PeakListElementName.NAME.getElementName(), atts);
		hd.characters(peakList.getName().toCharArray(), 0, peakList.getName().length());
		hd.endElement("", "", PeakListElementName.NAME.getElementName());

		// <PEAKLIST_DATE>
		String dateText = "";
		if (((SimplePeakList) peakList).getDateCreated() == null) {
			dateText = ((SimplePeakList) peakList).getDateCreated();
		} else {
			Date date = new Date();
			dateText = dateFormat.format(date);
		}
		hd.startElement("", "", PeakListElementName.PEAKLIST_DATE.getElementName(), atts);
		hd.characters(dateText.toCharArray(), 0, dateText.length());
		hd.endElement("", "", PeakListElementName.PEAKLIST_DATE.getElementName());

		// <QUANTITY>
		hd.startElement("", "", PeakListElementName.QUANTITY.getElementName(), atts);
		hd.characters(String.valueOf(numberOfRows).toCharArray(), 0, String.valueOf(numberOfRows).length());
		hd.endElement("", "", PeakListElementName.QUANTITY.getElementName());

		// <PROCESS> 
		PeakListAppliedMethod[] processes = peakList.getAppliedMethods();
		for (PeakListAppliedMethod proc : processes) {
			atts.clear();
			atts.addAttribute("", "", "description", "CDATA", proc.getDescription());
			hd.startElement("", "", PeakListElementName.PROCESS.getElementName(), atts);			
			this.fillProcessParameters((SimpleParameterSet)proc.getParameterSet(), hd);
			hd.endElement("", "", PeakListElementName.PROCESS.getElementName());
		}
		atts.clear();

		// <RAWFILE>
		RawDataFile[] dataFiles = peakList.getRawDataFiles();

		for (int i = 1; i <= dataFiles.length; i++) {
			int ID = 0;
			RawDataFile[] files = MZmineCore.getCurrentProject().getDataFiles();
			for (int e = 0; e < files.length; e++) {
				if (files[e].equals(dataFiles[i - 1])) {
					ID = e;
					break;
				}
			}
			atts.clear();
			atts.addAttribute("", "", PeakListElementName.ID.getElementName(), "CDATA", String.valueOf(ID));
			hd.startElement("", "", PeakListElementName.RAWFILE.getElementName(), atts);
			try {
				fillRawDataFileElement(dataFiles[i - 1], hd);
			} catch (Exception ex) {
				MZmineCore.getDesktop().displayErrorMessage("Error. No raw data exists for the peak list: " + peakList.getName());
				return;
			}
			dataFilesIDMap.put(dataFiles[i - 1], ID);
			hd.endElement("", "", PeakListElementName.RAWFILE.getElementName());
		}


		// <ROW>
		PeakListRow row;
		for (int i = 0; i < numberOfRows; i++) {
			atts.clear();
			row = peakList.getRow(i);
			atts.addAttribute("", "", PeakListElementName.ID.getElementName(), "CDATA", String.valueOf(row.getID()));
			hd.startElement("", "", PeakListElementName.ROW.getElementName(), atts);
			fillRowElement(row, hd);
			hd.endElement("", "", PeakListElementName.ROW.getElementName());
			progress = (double) i / numberOfRows;
		}


		hd.endElement("", "", PeakListElementName.PEAKLIST.getElementName());

		hd.endDocument();
	}

	/**
	 * Add the process parameters into the XML document
	 * @param parameterSet
	 * @param hd
	 * @throws org.xml.sax.SAXException
	 */
	private void fillProcessParameters(SimpleParameterSet parameterSet, TransformerHandler hd) throws SAXException {
		AttributesImpl atts = new AttributesImpl();

		for (Parameter p :  parameterSet.getParameters()) {
			atts.clear();
			atts.addAttribute("", "", "name", "CDATA", p.getName());
			atts.addAttribute("", "", "type", "CDATA", p.getType().toString());

			hd.startElement("", "", PeakListElementName.PARAMETER.getElementName(), atts);

			if ((p.getType() == ParameterType.MULTIPLE_SELECTION) || (p.getType() == ParameterType.ORDERED_LIST)) {
				Object[] values = p.getPossibleValues();
				if (values != null) {
					String valueAsString = "";
					for (int i = 0; i < values.length; i++) {
						if (i == values.length - 1) {
							valueAsString += String.valueOf(values[i]);
						} else {
							valueAsString += String.valueOf(values[i]) + ",";
						}
					}
					hd.characters(valueAsString.toCharArray(), 0, valueAsString.length());
				}
			} else {
				Object value = parameterSet.getParameterValue(p);
				if (value != null) {
					String valueAsString;
					if (value instanceof Range) {
						Range rangeValue = (Range) value;
						valueAsString = String.valueOf(rangeValue.getMin()) + "-" + String.valueOf(rangeValue.getMax());
					} else {
						valueAsString = value.toString();
					}
					hd.characters(valueAsString.toCharArray(), 0, valueAsString.length());
				}
			}
			hd.endElement("", "", PeakListElementName.PARAMETER.getElementName());
		}

	}

	/**
	 * Add the row information into the XML document
	 * @param row
	 * @param element
	 */
	private void fillRowElement(PeakListRow row, TransformerHandler hd) throws SAXException {

		// <PEAK_IDENTITY>
		PeakIdentity preferredIdentity = row.getPreferredPeakIdentity();
		PeakIdentity[] identities = row.getPeakIdentities();
		AttributesImpl atts = new AttributesImpl();

		for (int i = 0; i < identities.length; i++) {
			atts.addAttribute("", "", PeakListElementName.ID.getElementName(), "CDATA", String.valueOf(i));
			atts.addAttribute("", "", PeakListElementName.PREFERRED.getElementName(), "CDATA", String.valueOf(identities[i] == preferredIdentity));
			hd.startElement("", "", PeakListElementName.PEAK_IDENTITY.getElementName(), atts);
			fillIdentityElement(identities[i], hd);
			hd.endElement("", "", PeakListElementName.PEAK_IDENTITY.getElementName());
		}

		// <PEAK>
		int dataFileID = 0;
		ChromatographicPeak[] peaks = row.getPeaks();
		for (ChromatographicPeak p : peaks) {
			atts.clear();
			dataFileID = dataFilesIDMap.get(p.getDataFile());
			atts.addAttribute("", "", PeakListElementName.COLUMN.getElementName(), "CDATA", String.valueOf(dataFileID));
			atts.addAttribute("", "", PeakListElementName.MASS.getElementName(), "CDATA", String.valueOf(p.getMZ()));
			atts.addAttribute("", "", PeakListElementName.RT.getElementName(), "CDATA", String.valueOf(p.getRT()));
			atts.addAttribute("", "", PeakListElementName.HEIGHT.getElementName(), "CDATA", String.valueOf(p.getHeight()));
			atts.addAttribute("", "", PeakListElementName.AREA.getElementName(), "CDATA", String.valueOf(p.getArea()));
			atts.addAttribute("", "", PeakListElementName.STATUS.getElementName(), "CDATA", p.getPeakStatus().toString());


			hd.startElement("", "", PeakListElementName.PEAK.getElementName(), atts);

			fillPeakElement(p, hd);
			hd.endElement("", "", PeakListElementName.PEAK.getElementName());
		}

	}

	/**
	 * Add the raw data information into the XML document
	 * @param file
	 * @param element
	 */
	private void fillRawDataFileElement(RawDataFile file, TransformerHandler hd) throws Exception {
		AttributesImpl atts = new AttributesImpl();
		// <NAME>
		hd.startElement("", "", PeakListElementName.RAWDATA_NAME.getElementName(), atts);
		hd.characters(file.getName().toCharArray(), 0, file.getName().length());
		hd.endElement("", "", PeakListElementName.RAWDATA_NAME.getElementName());

		// <RTRANGE>
		Range RTRange = file.getDataRTRange(1);
		atts.clear();
		hd.startElement("", "", PeakListElementName.RTRANGE.getElementName(), atts);
		String range = String.valueOf(RTRange.getMin() + "-" + RTRange.getMax());
		hd.characters(range.toCharArray(), 0, range.length());
		hd.endElement("", "", PeakListElementName.RTRANGE.getElementName());


		// <MZRANGE>
		Range MZRange = file.getDataMZRange(1);
		atts.clear();
		hd.startElement("", "", PeakListElementName.MZRANGE.getElementName(), atts);
		range = String.valueOf(MZRange.getMin() + "-" + MZRange.getMax());
		hd.characters(range.toCharArray(), 0, range.length());
		hd.endElement("", "", PeakListElementName.MZRANGE.getElementName());
	}

	/**
	 * Add the peak identity information into the XML document
	 * @param identity
	 * @param element
	 */
	private void fillIdentityElement(PeakIdentity identity, TransformerHandler hd) throws SAXException {

		AttributesImpl atts = new AttributesImpl();
		// <NAME>
		String name = identity.getName() != null ? identity.getName() : " ";
		hd.startElement("", "", PeakListElementName.NAME.getElementName(), atts);
		hd.characters(name.toCharArray(), 0, name.length());
		hd.endElement("", "", PeakListElementName.NAME.getElementName());

		// <FORMULA>
		String formula = "";
		if (identity instanceof SimplePeakIdentity) {
			SimplePeakIdentity id = (SimplePeakIdentity) identity;
			formula = id.getCompoundFormula();
		}
		atts.clear();
		hd.startElement("", "", PeakListElementName.FORMULA.getElementName(), atts);
		hd.characters(formula.toCharArray(), 0, formula.length());
		hd.endElement("", "", PeakListElementName.FORMULA.getElementName());

		// <IDENTIFICATION>
		atts.clear();
		hd.startElement("", "", PeakListElementName.IDENTIFICATION.getElementName(), atts);
		name = identity.getIdentificationMethod() != null ? identity.getIdentificationMethod() : " ";
		hd.characters(name.toCharArray(), 0, name.length());
		hd.endElement("", "", PeakListElementName.IDENTIFICATION.getElementName());
	}

	/**
	 * Add the peaks information into the XML document
	 * @param peak
	 * @param element
	 * @param dataFileID
	 */
	private void fillPeakElement(ChromatographicPeak peak, TransformerHandler hd) throws SAXException {
		AttributesImpl atts = new AttributesImpl();

		// <MZPEAK>
		int scanNumbers[] = peak.getScanNumbers();
		atts.addAttribute("", "", PeakListElementName.QUANTITY.getElementName(), "CDATA", String.valueOf(scanNumbers.length));
		hd.startElement("", "", PeakListElementName.MZPEAK.getElementName(), atts);
		atts.clear();

		// <REPRESENTATIVE_SCAN>
		hd.startElement("", "", PeakListElementName.REPRESENTATIVE_SCAN.getElementName(), atts);
		hd.characters(String.valueOf(peak.getRepresentativeScanNumber()).toCharArray(), 0, String.valueOf(peak.getRepresentativeScanNumber()).length());
		hd.endElement("", "", PeakListElementName.REPRESENTATIVE_SCAN.getElementName());

		// <FRAGMENT_SCAN>
		hd.startElement("", "", PeakListElementName.FRAGMENT_SCAN.getElementName(), atts);
		hd.characters(String.valueOf(peak.getMostIntenseFragmentScanNumber()).toCharArray(), 0, String.valueOf(peak.getMostIntenseFragmentScanNumber()).length());
		hd.endElement("", "", PeakListElementName.FRAGMENT_SCAN.getElementName());

		// <SCAN_ID> <MASS> <HEIGHT>
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
					mass = 0f;
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
		hd.startElement("", "", PeakListElementName.SCAN_ID.getElementName(), atts);
		String sbytes = new String(bytes);
		hd.characters(sbytes.toCharArray(), 0, sbytes.length());
		hd.endElement("", "", PeakListElementName.SCAN_ID.getElementName());

		bytes = Base64.encode(byteMassStream.toByteArray());
		hd.startElement("", "", PeakListElementName.MASS.getElementName(), atts);
		sbytes = new String(bytes);
		hd.characters(sbytes.toCharArray(), 0, sbytes.length());
		hd.endElement("", "", PeakListElementName.MASS.getElementName());

		bytes = Base64.encode(byteHeightStream.toByteArray());
		hd.startElement("", "", PeakListElementName.HEIGHT.getElementName(), atts);
		sbytes = new String(bytes);
		hd.characters(sbytes.toCharArray(), 0, sbytes.length());
		hd.endElement("", "", PeakListElementName.HEIGHT.getElementName());

		hd.endElement("", "", PeakListElementName.MZPEAK.getElementName());
	}

	/**
	 * @return the progress of these functions saving the peak list to the zip file.
	 */
	public double getProgress() {
		return progress;
	}
}
