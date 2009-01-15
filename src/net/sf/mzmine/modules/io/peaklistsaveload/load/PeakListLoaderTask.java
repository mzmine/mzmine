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

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.logging.Logger;

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
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.io.peaklistsaveload.PeakListElementName;
import net.sf.mzmine.modules.io.peaklistsaveload.save.PeakListSaverTask;
import net.sf.mzmine.modules.lightviewer.MZviewerWindow;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.Range;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PeakListLoaderTask extends DefaultHandler implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	// task variables
	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;
	private int parsedRows, totalRows;

	// parameter values
	private String fileName;
	private File originalFile;

	// flags
	private boolean peakListFlag = false;

	// temporary variables
	private TreeMap<Integer, RawDataFile> buildingArrayRawDataFiles;
	private TreeMap<Integer, MzPeak> buildingArrayMzPeaks;
	private RawDataFileImpl buildingRawDataFile;
	private PeakList buildingPeakList;
	private SimplePeakListRow buildingRow;
	private int peakColumnID, rawDataFileID;
	private double mass, rt, height, area;
	private String peakStatus, peakListName, name, formula,
			identificationMethod, identityID;
	private boolean preferred;
	private Date date;
	private Range rtRange, mzRange;

	private String elementText;

	public PeakListLoaderTask(PeakListLoaderParameters parameters) {

		fileName = (String) parameters
				.getParameterValue(PeakListLoaderParameters.filename);

		originalFile = new File(fileName);

	}

	public void cancel() {
		logger.info("Cancelling loading of MZmine peak list " + fileName);
		status = TaskStatus.CANCELED;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public double getFinishedPercentage() {
		if (totalRows == 0) {
			return 0.0f;
		}
		return (double) parsedRows / (double) totalRows;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getTaskDescription() {
		return "Loading peak list from " + fileName;
	}

	public void run() {

		status = TaskStatus.PROCESSING;
		logger.info("Started parsing file " + originalFile);

		// Use the default (non-validating) parser
		SAXParserFactory factory = SAXParserFactory.newInstance();

		try {

			if ((!originalFile.exists()) || (!originalFile.canRead())) {
				throw new SAXException(
						"Parsing Cancelled, file does not exist or is not readable");
			}

			buildingArrayRawDataFiles = new TreeMap<Integer, RawDataFile>();
			buildingArrayMzPeaks = new TreeMap<Integer, MzPeak>();
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(originalFile, this);

		} catch (Throwable e) {
			/* we may already have set the status to CANCELED */
			if (status == TaskStatus.PROCESSING)
				status = TaskStatus.ERROR;
			errorMessage = e.toString();
			return;
		}

		if (parsedRows == 0) {
			status = TaskStatus.ERROR;
			errorMessage = "No peaks found";
			return;
		}

		// Add new peaklist to the project
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		if (currentProject != null){
			currentProject.addPeakList(buildingPeakList);
		}
		else{
			Desktop desktop = MZmineCore.getDesktop();
			((MZviewerWindow) desktop).getItemSelector().addPeakList(buildingPeakList);
		}

		logger.info("Finished parsing " + originalFile + ", parsed "
				+ parsedRows + " rows");
		status = TaskStatus.FINISHED;

	}

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
				if (buildingRawDataFile == null) {
					buildingRawDataFile = new RawDataFileImpl(name);
				}
				int scanNumber = Integer.parseInt(attrs
						.getValue(PeakListElementName.ID.getElementName()));
				double retentionTime = Double.parseDouble(attrs
						.getValue(PeakListElementName.RT.getElementName()));
				
				SimpleScan newScan = new SimpleScan(scanNumber, 1,
						retentionTime, -1, 0f, null, new MzDataPoint[0], false);
				
				buildingRawDataFile.addScan(newScan);
				
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
				RawDataFile[] dataFiles = buildingArrayRawDataFiles.values()
						.toArray(new RawDataFile[0]);
				buildingPeakList = new SimplePeakList(peakListName, dataFiles);
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
				int mzPeakScanID = Integer.parseInt(attrs
						.getValue(PeakListElementName.SCAN.getElementName()));
				double mz = Double.parseDouble(attrs
						.getValue(PeakListElementName.MASS.getElementName()));
				double intensity = Double.parseDouble(attrs
						.getValue(PeakListElementName.HEIGHT.getElementName()));
				SimpleMzPeak mzPeak = new SimpleMzPeak(new SimpleDataPoint(
						mz, intensity));
				
				buildingArrayMzPeaks.put(mzPeakScanID, mzPeak);
				
			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				throw new SAXException(
						"Could not read mzPeak attributes information");
			}

		}

	}

	/**
	 * endElement()
	 */
	public void endElement(String namespaceURI, String sName, // simple name
			String qName // qualified name
	) throws SAXException {

		if (status == TaskStatus.CANCELED)
			throw new SAXException("Parsing Cancelled");

		// <NAME>
		if (qName.equals(PeakListElementName.NAME.getElementName())) {
			name = elementText;
			if (peakListFlag)
				peakListName = name;
		}

		// <PEAKLIST_DATE>
		if (qName.equals(PeakListElementName.PEAKLIST_DATE.getElementName())) {
			try {
				String text = elementText;
				date = PeakListSaverTask.dateFormat.parse(text);
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
				String text = elementText;
				text = text.trim();
				totalRows = Integer.parseInt(text);
			} catch (Exception e) {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not have MZmine peak list file format";
				throw new SAXException("Could not read number of rows");
			}
		}

		// <FORMULA>
		if (qName.equals(PeakListElementName.FORMULA.getElementName())) {
			formula = elementText;
		}

		// <IDENTIFICATION>
		if (qName.equals(PeakListElementName.IDENTIFICATION.getElementName())) {
			identificationMethod = elementText;
		}

		// <RTRANGE>
		if (qName.equals(PeakListElementName.RTRANGE.getElementName())) {
			try {
				String valueText = elementText;
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
				String valueText = elementText;
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

		// <PEAK>
		if (qName.equals(PeakListElementName.PEAK.getElementName())) {

			int index = 0;
			int length = buildingArrayMzPeaks.size();
			int[] scanNumbers = new int[length];
			MzPeak[] mzPeaks = new MzPeak[length];

			Iterator<Integer> itr = buildingArrayMzPeaks.keySet().iterator();
			while (itr.hasNext()) {
				int scan = itr.next();
				scanNumbers[index] = scan;
				mzPeaks[index] = buildingArrayMzPeaks.get(scan);
				index++;
			}
			buildingArrayMzPeaks.clear();

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

		// <RAWFILE>
		if (qName.equals(PeakListElementName.RAWFILE.getElementName())) {
			buildingRawDataFile.setRTRange(1, rtRange);
			buildingRawDataFile.setMZRange(1, mzRange);
			buildingArrayRawDataFiles.put(rawDataFileID, buildingRawDataFile);
			buildingRawDataFile = null;
		}

	}

	/**
	 * characters()
	 * 
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char buf[], int offset, int len) throws SAXException {
		elementText = new String(buf, offset, len);
		elementText = elementText.replaceAll("[\n\r\t]+", "");
	}

}
