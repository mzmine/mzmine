/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.io.projectsave;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListAppliedMethod;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakIdentity;
import net.sf.mzmine.data.impl.SimplePeakList;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.Ostermiller.util.Base64;

import de.schlichtherle.util.zip.ZipEntry;
import de.schlichtherle.util.zip.ZipOutputStream;

class PeakListSaveHandler {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public static DateFormat dateFormat = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm:ss");

	private Hashtable<RawDataFile, Integer> dataFilesIDMap;

	private int numberOfRows, finishedRows;
	private boolean canceled = false;

	private ZipOutputStream zipOutputStream;

	PeakListSaveHandler(ZipOutputStream zipStream) {
		this.zipOutputStream = zipStream;
	}

	/**
	 * Create an XML document with the peak list information an save it into the
	 * project zip file
	 * 
	 * @param peakList
	 * @param peakListSavedName
	 *            name of the peak list
	 * @throws java.io.IOException
	 */
	void savePeakList(PeakList peakList, int number,
			Hashtable<RawDataFile, Integer> dataFilesIDMap) throws IOException,
			TransformerConfigurationException, SAXException {

		logger.info("Saving peak list: " + peakList.getName());

		numberOfRows = peakList.getNumberOfRows();
		finishedRows = 0;

		this.dataFilesIDMap = dataFilesIDMap;

		String peakListSavedName = "Peak list #" + number + " "
				+ peakList.getName();

		zipOutputStream.putNextEntry(new ZipEntry(peakListSavedName + ".xml"));
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
		AttributesImpl atts = new AttributesImpl();

		hd.startElement("", "", PeakListElementName.PEAKLIST.getElementName(),
				atts);
		atts.clear();

		// <NAME>
		hd.startElement("", "", PeakListElementName.PEAKLIST_NAME
				.getElementName(), atts);
		hd.characters(peakList.getName().toCharArray(), 0, peakList.getName()
				.length());
		hd.endElement("", "", PeakListElementName.PEAKLIST_NAME
				.getElementName());

		// <PEAKLIST_DATE>
		String dateText = "";
		if (((SimplePeakList) peakList).getDateCreated() == null) {
			dateText = ((SimplePeakList) peakList).getDateCreated();
		} else {
			Date date = new Date();
			dateText = dateFormat.format(date);
		}
		hd.startElement("", "", PeakListElementName.PEAKLIST_DATE
				.getElementName(), atts);
		hd.characters(dateText.toCharArray(), 0, dateText.length());
		hd.endElement("", "", PeakListElementName.PEAKLIST_DATE
				.getElementName());

		// <QUANTITY>
		hd.startElement("", "", PeakListElementName.QUANTITY.getElementName(),
				atts);
		hd.characters(String.valueOf(numberOfRows).toCharArray(), 0, String
				.valueOf(numberOfRows).length());
		hd.endElement("", "", PeakListElementName.QUANTITY.getElementName());

		// <PROCESS>
		PeakListAppliedMethod[] processes = peakList.getAppliedMethods();
		for (PeakListAppliedMethod proc : processes) {

			hd.startElement("", "",
					PeakListElementName.METHOD.getElementName(), atts);

			hd.startElement("", "", PeakListElementName.METHOD_NAME
					.getElementName(), atts);
			String methodName = proc.getDescription();
			hd.characters(methodName.toCharArray(), 0, methodName.length());
			hd.endElement("", "", PeakListElementName.METHOD_NAME
					.getElementName());

			hd.startElement("", "", PeakListElementName.METHOD_PARAMETERS
					.getElementName(), atts);
			String methodParameters = proc.getParameters();
			hd.characters(methodParameters.toCharArray(), 0, methodParameters
					.length());
			hd.endElement("", "", PeakListElementName.METHOD_PARAMETERS
					.getElementName());

			hd.endElement("", "", PeakListElementName.METHOD.getElementName());

		}
		atts.clear();

		// <RAWFILE>
		RawDataFile[] dataFiles = peakList.getRawDataFiles();

		for (int i = 0; i < dataFiles.length; i++) {

			Integer ID = dataFilesIDMap.get(dataFiles[i]);

			hd.startElement("", "", PeakListElementName.RAWFILE
					.getElementName(), atts);
			char idChars[] = String.valueOf(ID).toCharArray();
			hd.characters(idChars, 0, idChars.length);

			hd.endElement("", "", PeakListElementName.RAWFILE.getElementName());
		}

		// <ROW>
		PeakListRow row;
		for (int i = 0; i < numberOfRows; i++) {

			if (canceled)
				return;

			atts.clear();
			row = peakList.getRow(i);
			atts.addAttribute("", "", PeakListElementName.ID.getElementName(),
					"CDATA", String.valueOf(row.getID()));
			if (row.getComment() != null) {
				atts.addAttribute("", "", PeakListElementName.COMMENT.getElementName(),
						"CDATA", row.getComment());
			}
			
			hd.startElement("", "", PeakListElementName.ROW.getElementName(),
					atts);
			fillRowElement(row, hd);
			hd.endElement("", "", PeakListElementName.ROW.getElementName());

			finishedRows++;
		}

		hd.endElement("", "", PeakListElementName.PEAKLIST.getElementName());
		hd.endDocument();
	}

	/**
	 * Add the row information into the XML document
	 * 
	 * @param row
	 * @param element
	 * @throws IOException
	 */
	private void fillRowElement(PeakListRow row, TransformerHandler hd)
			throws SAXException, IOException {

		// <PEAK_IDENTITY>
		PeakIdentity preferredIdentity = row.getPreferredPeakIdentity();
		PeakIdentity[] identities = row.getPeakIdentities();
		AttributesImpl atts = new AttributesImpl();

		for (int i = 0; i < identities.length; i++) {

			if (canceled)
				return;

			atts.addAttribute("", "", PeakListElementName.ID.getElementName(),
					"CDATA", String.valueOf(i));
			atts.addAttribute("", "", PeakListElementName.PREFERRED
					.getElementName(), "CDATA", String
					.valueOf(identities[i] == preferredIdentity));
			hd.startElement("", "", PeakListElementName.PEAK_IDENTITY
					.getElementName(), atts);
			fillIdentityElement(identities[i], hd);
			hd.endElement("", "", PeakListElementName.PEAK_IDENTITY
					.getElementName());
		}

		// <PEAK>
		int dataFileID = 0;
		ChromatographicPeak[] peaks = row.getPeaks();
		for (ChromatographicPeak p : peaks) {
			if (canceled)
				return;

			atts.clear();
			dataFileID = dataFilesIDMap.get(p.getDataFile());
			atts.addAttribute("", "", PeakListElementName.COLUMN
					.getElementName(), "CDATA", String.valueOf(dataFileID));
			atts.addAttribute("", "", PeakListElementName.MZ.getElementName(),
					"CDATA", String.valueOf(p.getMZ()));
			atts.addAttribute("", "", PeakListElementName.RT.getElementName(),
					"CDATA", String.valueOf(p.getRT()));
			atts.addAttribute("", "", PeakListElementName.HEIGHT
					.getElementName(), "CDATA", String.valueOf(p.getHeight()));
			atts.addAttribute("", "",
					PeakListElementName.AREA.getElementName(), "CDATA", String
							.valueOf(p.getArea()));
			atts.addAttribute("", "", PeakListElementName.STATUS
					.getElementName(), "CDATA", p.getPeakStatus().toString());

			hd.startElement("", "", PeakListElementName.PEAK.getElementName(),
					atts);

			fillPeakElement(p, hd);
			hd.endElement("", "", PeakListElementName.PEAK.getElementName());
		}

	}

	/**
	 * Add the peak identity information into the XML document
	 * 
	 * @param identity
	 * @param element
	 */
	private void fillIdentityElement(PeakIdentity identity,
			TransformerHandler hd) throws SAXException {

		AttributesImpl atts = new AttributesImpl();
		// <NAME>
		String name = (identity.getName() != null) ? identity.getName() : "";
		hd.startElement("", "", PeakListElementName.IDENTITY_NAME
				.getElementName(), atts);
		hd.characters(name.toCharArray(), 0, name.length());
		hd.endElement("", "", PeakListElementName.IDENTITY_NAME
				.getElementName());

		// <FORMULA>
		String formula = null;
		if (identity instanceof SimplePeakIdentity) {
			SimplePeakIdentity id = (SimplePeakIdentity) identity;
			formula = id.getCompoundFormula();
		}
		if (formula != null) {
			atts.clear();
			hd.startElement("", "", PeakListElementName.FORMULA
					.getElementName(), atts);
			hd.characters(formula.toCharArray(), 0, formula.length());
			hd.endElement("", "", PeakListElementName.FORMULA.getElementName());
		}

		// <IDENTIFICATION>
		if (identity.getIdentificationMethod() != null) {
			atts.clear();
			hd.startElement("", "", PeakListElementName.IDENTIFICATION_METHOD
					.getElementName(), atts);
			String idMethod = identity.getIdentificationMethod();
			hd.characters(idMethod.toCharArray(), 0, idMethod.length());
			hd.endElement("", "", PeakListElementName.IDENTIFICATION_METHOD
					.getElementName());
		}
	}

	/**
	 * Add the peaks information into the XML document
	 * 
	 * @param peak
	 * @param element
	 * @param dataFileID
	 * @throws IOException
	 */
	private void fillPeakElement(ChromatographicPeak peak, TransformerHandler hd)
			throws SAXException, IOException {
		AttributesImpl atts = new AttributesImpl();

		// <REPRESENTATIVE_SCAN>
		hd.startElement("", "", PeakListElementName.REPRESENTATIVE_SCAN
				.getElementName(), atts);
		hd.characters(String.valueOf(peak.getRepresentativeScanNumber())
				.toCharArray(), 0, String.valueOf(
				peak.getRepresentativeScanNumber()).length());
		hd.endElement("", "", PeakListElementName.REPRESENTATIVE_SCAN
				.getElementName());

		// <FRAGMENT_SCAN>
		hd.startElement("", "", PeakListElementName.FRAGMENT_SCAN
				.getElementName(), atts);
		hd.characters(String.valueOf(peak.getMostIntenseFragmentScanNumber())
				.toCharArray(), 0, String.valueOf(
				peak.getMostIntenseFragmentScanNumber()).length());
		hd.endElement("", "", PeakListElementName.FRAGMENT_SCAN
				.getElementName());

		int scanNumbers[] = peak.getScanNumbers();

		// <ISOTOPE_PATTERN>
		IsotopePattern isotopePattern = peak.getIsotopePattern();
		if (isotopePattern != null) {
			atts.addAttribute("", "", PeakListElementName.STATUS
					.getElementName(), "CDATA", String.valueOf(isotopePattern
					.getStatus()));
			atts.addAttribute("", "", PeakListElementName.CHARGE
					.getElementName(), "CDATA", String.valueOf(isotopePattern
					.getCharge()));
			atts
					.addAttribute("", "", PeakListElementName.DESCRIPTION
							.getElementName(), "CDATA", isotopePattern
							.getDescription());
			hd.startElement("", "", PeakListElementName.ISOTOPE_PATTERN
					.getElementName(), atts);
			atts.clear();

			fillIsotopePatternElement(isotopePattern, hd);

			hd.endElement("", "", PeakListElementName.ISOTOPE_PATTERN
					.getElementName());

		}

		// <MZPEAK>
		atts.addAttribute("", "",
				PeakListElementName.QUANTITY.getElementName(), "CDATA", String
						.valueOf(scanNumbers.length));
		hd.startElement("", "", PeakListElementName.MZPEAKS.getElementName(),
				atts);
		atts.clear();

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
		}

		byte[] bytes = Base64.encode(byteScanStream.toByteArray());
		hd.startElement("", "", PeakListElementName.SCAN_ID.getElementName(),
				atts);
		String sbytes = new String(bytes);
		hd.characters(sbytes.toCharArray(), 0, sbytes.length());
		hd.endElement("", "", PeakListElementName.SCAN_ID.getElementName());

		bytes = Base64.encode(byteMassStream.toByteArray());
		hd.startElement("", "", PeakListElementName.MZ.getElementName(), atts);
		sbytes = new String(bytes);
		hd.characters(sbytes.toCharArray(), 0, sbytes.length());
		hd.endElement("", "", PeakListElementName.MZ.getElementName());

		bytes = Base64.encode(byteHeightStream.toByteArray());
		hd.startElement("", "", PeakListElementName.HEIGHT.getElementName(),
				atts);
		sbytes = new String(bytes);
		hd.characters(sbytes.toCharArray(), 0, sbytes.length());
		hd.endElement("", "", PeakListElementName.HEIGHT.getElementName());

		hd.endElement("", "", PeakListElementName.MZPEAKS.getElementName());
	}

	private void fillIsotopePatternElement(IsotopePattern isotopePattern,
			TransformerHandler hd) throws SAXException, IOException {

		AttributesImpl atts = new AttributesImpl();

		DataPoint isotopes[] = isotopePattern.getDataPoints();

		for (DataPoint isotope : isotopes) {
			hd.startElement("", "", PeakListElementName.ISOTOPE
					.getElementName(), atts);
			String isotopeString = isotope.getMZ() + ":"
					+ isotope.getIntensity();
			hd.characters(isotopeString.toCharArray(), 0, isotopeString
					.length());
			hd.endElement("", "", PeakListElementName.ISOTOPE.getElementName());
		}
	}

	/**
	 * @return the progress of these functions saving the peak list to the zip
	 *         file.
	 */
	double getProgress() {
		if (numberOfRows == 0)
			return 0;
		return (double) finishedRows / numberOfRows;
	}

	void cancel() {
		canceled = true;
	}

}
