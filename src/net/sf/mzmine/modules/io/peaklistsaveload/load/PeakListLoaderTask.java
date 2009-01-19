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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.io.peaklistsaveload.load;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.mzmine.data.MzDataPoint;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleChromatographicPeak;
import net.sf.mzmine.data.impl.SimpleCompoundIdentity;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleMzPeak;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.io.peaklistsaveload.PeakListElementName;
import net.sf.mzmine.modules.lightviewer.MZviewerWindow;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.Range;

import org.jfree.xml.util.Base64;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PeakListLoaderTask extends DefaultHandler implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	// task variables
	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;
	private int parsedRows, totalRows;
	private StringBuffer charBuffer;

	// parameter values
	private String fileName;

	// flags
	private boolean peakListFlag = false;
	private boolean scanFlag = false;
	private boolean mzPeakFlag = false;

	// temporary variables
	private TreeMap<Integer, RawDataFile> buildingArrayRawDataFiles;
	private RawDataFileImpl buildingRawDataFile;
	private PeakList buildingPeakList;
	private SimplePeakListRow buildingRow;
	private int peakColumnID, rawDataFileID, quantity;
	private double mass, rt, height, area;
	private int[] scanNumbers;
	private double[] retentionTimes, masses, intensities;
	private String peakStatus, peakListName, name, formula,
			identificationMethod, identityID;
	private boolean preferred;
	private String dateCreated;
	private Range rtRange, mzRange;
	private Vector<String> appliedProcess;

	/**
	 * 
	 * @param parameters
	 */
	public PeakListLoaderTask(PeakListLoaderParameters parameters) {

		fileName = (String) parameters
				.getParameterValue(PeakListLoaderParameters.filename);

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		logger.info("Cancelling loading of MZmine peak list " + fileName);
		status = TaskStatus.CANCELED;
	}

	
	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		if (totalRows == 0) {
			return 0.0f;
		}
		return (double) parsedRows / (double) totalRows;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getStatus()
	 */
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Loading peak list from " + fileName;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		status = TaskStatus.PROCESSING;
		logger.info("Started parsing file " + fileName);

		// Use the default (non-validating) parser
		SAXParserFactory factory = SAXParserFactory.newInstance();

		try {

			File originalFile = new File(fileName);

			if ((!originalFile.exists()) || (!originalFile.canRead())) {
				throw new SAXException(
						"Parsing Cancelled, file does not exist or is not readable");
			}

			FileInputStream fis = new FileInputStream(fileName);
			InputStream finalStream = fis;
			byte b[] = new byte[32];
			fis.read(b);
			String firstLine = new String(b);
			if (!firstLine.contains("<?xml")) {
				FileChannel fc = fis.getChannel();
				fc.position(0);
				ZipInputStream zis = new ZipInputStream(fis);
				zis.getNextEntry();
				finalStream = zis;
			} else {
				FileChannel fc = fis.getChannel();
				fc.position(0);
			}

			buildingArrayRawDataFiles = new TreeMap<Integer, RawDataFile>();
			charBuffer = new StringBuffer();
			appliedProcess = new Vector<String>();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(finalStream, this);

		} catch (Throwable e) {
			/* we may already have set the status to CANCELED */
			if (status == TaskStatus.PROCESSING)
				status = TaskStatus.ERROR;
			errorMessage = e.toString();
			e.printStackTrace();
			return;
		}

		if (parsedRows == 0) {
			status = TaskStatus.ERROR;
			errorMessage = "No peaks found";
			return;
		}

		// Add new peaklist to the project or MZviewer.desktop
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		if (currentProject != null) {
			currentProject.addPeakList(buildingPeakList);
		} else {
			Desktop desktop = MZmineCore.getDesktop();
			((MZviewerWindow) desktop).getItemSelector().addPeakList(
					buildingPeakList);
		}

		logger.info("Finished parsing " + fileName + ", parsed " + parsedRows
				+ " rows");
		status = TaskStatus.FINISHED;

	}

	/**
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String namespaceURI, String lName, // local name
			String qName, // qualified name
			Attributes attrs) throws SAXException {

		if (status == TaskStatus.CANCELED)
			throw new SAXException("Parsing Cancelled");

		// <PEAKLIST>
		if (qName.equals(PeakListElementName.PEAKLIST.getElementName())) {
			peakListFlag = true;
			// clean the current char buffer for the new element
		}

		// <RAWFILE>
		if (qName.equals(PeakListElementName.RAWFILE.getElementName())) {
			try {
				rawDataFileID = Integer.parseInt(attrs
						.getValue(PeakListElementName.ID.getElementName()));
				peakListFlag = false;
			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				throw new SAXException(
						"Could not read scan attributes information");
			}
		}

		// <SCAN>
		if (qName.equals(PeakListElementName.SCAN.getElementName())) {
			try {
				quantity = Integer
						.parseInt(attrs.getValue(PeakListElementName.QUANTITY
								.getElementName()));

				scanFlag = true;

			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				throw new SAXException(
						"Could not read scan attributes information");
			}
		}

		// <ROW>
		if (qName.equals(PeakListElementName.ROW.getElementName())) {

			if (buildingPeakList == null) {
				initializePeakList();
			}

			try {
				int rowID = Integer.parseInt(attrs
						.getValue(PeakListElementName.ID.getElementName()));
				buildingRow = new SimplePeakListRow(rowID);
			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				throw new SAXException(
						"Could not read row attributes information");
			}
		}

		// <PEAK_IDENTITY>
		if (qName.equals(PeakListElementName.PEAK_IDENTITY.getElementName())) {
			try {
				identityID = attrs.getValue(PeakListElementName.ID
						.getElementName());
				preferred = Boolean.parseBoolean(attrs
						.getValue(PeakListElementName.PREFERRED
								.getElementName()));
			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				throw new SAXException(
						"Could not read identity attributes information");
			}
		}

		// <PEAK>
		if (qName.equals(PeakListElementName.PEAK.getElementName())) {
			try {

				peakColumnID = Integer.parseInt(attrs
						.getValue(PeakListElementName.COLUMN.getElementName()));
				mass = Double.parseDouble(attrs
						.getValue(PeakListElementName.MASS.getElementName()));
				rt = Double.parseDouble(attrs.getValue(PeakListElementName.RT
						.getElementName()));
				height = Double.parseDouble(attrs
						.getValue(PeakListElementName.HEIGHT.getElementName()));
				area = Double.parseDouble(attrs
						.getValue(PeakListElementName.AREA.getElementName()));
				peakStatus = attrs.getValue(PeakListElementName.STATUS
						.getElementName());
			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				throw new SAXException(
						"Could not read peak attributes information");
			}

		}

		// <MZPEAK>
		if (qName.equals(PeakListElementName.MZPEAK.getElementName())) {
			try {
				quantity = Integer
						.parseInt(attrs.getValue(PeakListElementName.QUANTITY
								.getElementName()));

				mzPeakFlag = true;

			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				throw new SAXException(
						"Could not read mzPeak attributes information");
			}

		}

	}

	/**
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String namespaceURI, String sName, // simple name
			String qName // qualified name
	) throws SAXException {

		if (status == TaskStatus.CANCELED)
			throw new SAXException("Parsing Cancelled");

		// <NAME>
		if (qName.equals(PeakListElementName.NAME.getElementName())) {
			name = getTextOfElement();
			if (peakListFlag)
				peakListName = name;

		}

		// <PEAKLIST_DATE>
		if (qName.equals(PeakListElementName.PEAKLIST_DATE.getElementName())) {
			try {
				//String text = getTextOfElement();
				dateCreated = getTextOfElement();
			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				throw new SAXException(
						"Could not read peak list date of creation");
			}
		}

		// <QUANTITY>
		if (qName.equals(PeakListElementName.QUANTITY.getElementName())) {
			try {
				String text = getTextOfElement();
				text = text.trim();
				totalRows = Integer.parseInt(text);
			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				throw new SAXException("Could not read quantity");
			}
		}

		// <PROCESS>
		if (qName.equals(PeakListElementName.PROCESS.getElementName())) {
			String text = getTextOfElement();
			if (text.length() != 0)
				appliedProcess.add(text);
		}

		// <SCAN_ID>
		if (qName.equals(PeakListElementName.SCAN_ID.getElementName())) {
			try {
				if (scanFlag) {
					String valueText = getTextOfElement();
					String values[] = valueText.split(PeakListElementName.SEPARATOR.getElementName());
					scanNumbers = new int[quantity];
					for (int i = 0; i < quantity; i++) {
						scanNumbers[i] = Integer.parseInt(values[i]);
					}
				} else if (mzPeakFlag) {
					byte[] bytes = Base64.decode(getTextOfElement()
							.toCharArray());
					// make a data input stream
					DataInputStream dataInputStream = new DataInputStream(
							new ByteArrayInputStream(bytes));
					scanNumbers = new int[quantity];
					for (int i = 0; i < quantity; i++) {
						scanNumbers[i] = dataInputStream.readInt();
					}
				}

			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				e.printStackTrace();
				throw new SAXException("Could not read list of scan numbers");
			}
		}

		// <RT>
		if (qName.equals(PeakListElementName.RT.getElementName())) {
			try {
				String valueText = getTextOfElement();
				String values[] = valueText.split(PeakListElementName.SEPARATOR.getElementName());
				retentionTimes = new double[quantity];
				for (int i = 0; i < quantity; i++) {
					retentionTimes[i] = Double.parseDouble(values[i]);
				}
			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				e.printStackTrace();
				throw new SAXException("Could not read list of retention times");
			}
		}

		// <MASS>
		if (qName.equals(PeakListElementName.MASS.getElementName())) {
			try {
				byte[] bytes = Base64.decode(getTextOfElement().toCharArray());
				// make a data input stream
				DataInputStream dataInputStream = new DataInputStream(
						new ByteArrayInputStream(bytes));
				masses = new double[quantity];
				for (int i = 0; i < quantity; i++) {
					masses[i] = (double) dataInputStream.readFloat();
				}

			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				e.printStackTrace();
				throw new SAXException("Could not read list of masses");
			}

		}

		// <HEIGHT>
		if (qName.equals(PeakListElementName.HEIGHT.getElementName())) {
			try {
				byte[] bytes = Base64.decode(getTextOfElement().toCharArray());
				// make a data input stream
				DataInputStream dataInputStream = new DataInputStream(
						new ByteArrayInputStream(bytes));
				intensities = new double[quantity];
				for (int i = 0; i < quantity; i++) {
					intensities[i] = (double) dataInputStream.readFloat();
				}

			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				e.printStackTrace();
				throw new SAXException("Could not read list of intensities");
			}

		}

		// <FORMULA>
		if (qName.equals(PeakListElementName.FORMULA.getElementName())) {
			formula = getTextOfElement();
		}

		// <IDENTIFICATION>
		if (qName.equals(PeakListElementName.IDENTIFICATION.getElementName())) {
			identificationMethod = getTextOfElement();
		}

		// <RTRANGE>
		if (qName.equals(PeakListElementName.RTRANGE.getElementName())) {
			try {
				String valueText = getTextOfElement();
				String values[] = valueText.split("-");
				double min = Double.parseDouble(values[0]);
				double max = Double.parseDouble(values[1]);
				rtRange = new Range(min, max);
			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				throw new SAXException(
						"Could not read retention time range form raw data file");
			}
		}

		// <MZRANGE>
		if (qName.equals(PeakListElementName.MZRANGE.getElementName())) {
			try {
				String valueText = getTextOfElement();
				String values[] = valueText.split("-");
				double min = Double.parseDouble(values[0]);
				double max = Double.parseDouble(values[1]);
				mzRange = new Range(min, max);
			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				throw new SAXException(
						"Could not read m/z range from raw data file");
			}
		}

		// <MZPEAK>
		if (qName.equals(PeakListElementName.MZPEAK.getElementName())) {
			mzPeakFlag = false;
		}

		// <PEAK>
		if (qName.equals(PeakListElementName.PEAK.getElementName())) {

			MzPeak[] mzPeaks = new MzPeak[quantity];
			for (int i = 0; i < quantity; i++) {
				mzPeaks[i] = new SimpleMzPeak(new SimpleDataPoint(masses[i],
						intensities[i]));
			}

			SimpleChromatographicPeak peak = new SimpleChromatographicPeak(
					buildingArrayRawDataFiles.get(peakColumnID), mass, rt,
					height, area, scanNumbers, mzPeaks, PeakStatus.valueOf(
							PeakStatus.class, peakStatus), -1);

			buildingRow.addPeak(buildingArrayRawDataFiles.get(peakColumnID),
					peak);
		}

		// <PEAK_IDENTITY>
		if (qName.equals(PeakListElementName.PEAK_IDENTITY.getElementName())) {
			SimpleCompoundIdentity identity = new SimpleCompoundIdentity(
					identityID, name, new String[0], formula, null,
					identificationMethod, null);
			buildingRow.addCompoundIdentity(identity, preferred);
		}

		// <ROW>
		if (qName.equals(PeakListElementName.ROW.getElementName())) {
			buildingPeakList.addRow(buildingRow);
			buildingRow = null;
			parsedRows++;
		}

		// <SCAN>
		if (qName.equals(PeakListElementName.SCAN.getElementName())) {
			try {
				if (buildingRawDataFile == null) {
					buildingRawDataFile = new RawDataFileImpl(name);
				}

				for (int i = 0; i < quantity; i++) {
					SimpleScan newScan = new SimpleScan(scanNumbers[i], 1,
							retentionTimes[i], -1, 0f, null,
							new MzDataPoint[0], false);
					buildingRawDataFile.addScan(newScan);
				}
				scanFlag = false;

			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "Error trying to create temporary RawDataFile";
				throw new SAXException(
						"Could not create scans for temporary raw data file");
			}
		}

		// <RAWFILE>
		if (qName.equals(PeakListElementName.RAWFILE.getElementName())) {
			buildingRawDataFile.setRTRange(1, rtRange);
			buildingRawDataFile.setMZRange(1, mzRange);
			buildingArrayRawDataFiles.put(rawDataFileID, buildingRawDataFile);
			buildingRawDataFile = null;
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
	
	private void initializePeakList(){
		RawDataFile[] dataFiles = buildingArrayRawDataFiles.values()
		.toArray(new RawDataFile[0]);
		buildingPeakList = new SimplePeakList(peakListName, dataFiles);
		String[] process = appliedProcess.toArray(new String[0]);
		for (String description: process){
			((SimplePeakList)buildingPeakList).addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(description));
		}
        // Add task description to peakList
		((SimplePeakList)buildingPeakList).addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(getTaskDescription()));

		((SimplePeakList)buildingPeakList).setDateCreated(dateCreated);
	}

}
