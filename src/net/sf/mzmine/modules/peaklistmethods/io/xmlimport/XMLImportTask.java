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

package net.sf.mzmine.modules.peaklistmethods.io.xmlimport;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;
import java.util.LinkedList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleChromatographicPeak;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimplePeakIdentity;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.io.xmlexport.PeakListElementName;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.TaskEvent;
import net.sf.mzmine.util.Range;

import org.jfree.xml.util.Base64;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLImportTask extends DefaultHandler implements Task {

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
	private String peakStatus, peakListName, name, identityPropertyName;
	private Hashtable<String, String> identityProperties;
	private boolean preferred;
	private String dateCreated;
	private Range rtRange, mzRange;
	private Vector<String> appliedProcess;
	private LinkedList <TaskListener> taskListeners = new LinkedList<TaskListener>( );

	/**
	 * 
	 * @param parameters
	 */
	public XMLImportTask(XMLImporterParameters parameters) {

		fileName = (String) parameters
				.getParameterValue(XMLImporterParameters.filename);

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		logger.info("Cancelling loading of MZmine peak list " + fileName);
		setStatus( TaskStatus.CANCELED );
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

		setStatus( TaskStatus.PROCESSING );
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
				setStatus( TaskStatus.ERROR );
			errorMessage = e.toString();
			e.printStackTrace();
			return;
		}

		if (parsedRows == 0) {
			setStatus( TaskStatus.ERROR );
			errorMessage = "No peaks found";
			return;
		}

		// Add new peaklist to the project or MZviewer.desktop
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		currentProject.addPeakList(buildingPeakList);

		logger.info("Finished parsing " + fileName + ", parsed " + parsedRows
				+ " rows");
		setStatus( TaskStatus.FINISHED );

	}

	/**
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
	 *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
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
				setStatus( TaskStatus.ERROR );
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
				setStatus( TaskStatus.ERROR );
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
				setStatus( TaskStatus.ERROR );
				errorMessage = "This file does not have MZmine peak list file format";
				throw new SAXException(
						"Could not read row attributes information");
			}
		}

		// <PEAK_IDENTITY>
		if (qName.equals(PeakListElementName.PEAK_IDENTITY.getElementName())) {
			identityProperties = new Hashtable<String, String>();
			preferred = Boolean.parseBoolean(attrs
					.getValue(PeakListElementName.PREFERRED.getElementName()));
		}

		// <IDENTITY_PROPERTY>
		if (qName.equals(PeakListElementName.IDPROPERTY.getElementName())) {
			identityPropertyName = attrs.getValue(PeakListElementName.NAME
					.getElementName());
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
				setStatus( TaskStatus.ERROR );
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
				setStatus( TaskStatus.ERROR );
				errorMessage = "This file does not have MZmine peak list file format";
				throw new SAXException(
						"Could not read mzPeak attributes information");
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
				// String text = getTextOfElement();
				dateCreated = getTextOfElement();
			} catch (Exception e) {
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
					String values[] = valueText
							.split(PeakListElementName.SEPARATOR
									.getElementName());
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
				throw new SAXException("Could not read list of scan numbers");
			}
		}

		// <RT>
		if (qName.equals(PeakListElementName.RT.getElementName())) {
			try {
				String valueText = getTextOfElement();
				String values[] = valueText.split(PeakListElementName.SEPARATOR
						.getElementName());
				retentionTimes = new double[quantity];
				for (int i = 0; i < quantity; i++) {
					retentionTimes[i] = Double.parseDouble(values[i]);
				}
			} catch (Exception e) {
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
				throw new SAXException("Could not read list of intensities");
			}

		}

		// <RTRANGE>
		if (qName.equals(PeakListElementName.RTRANGE.getElementName())) {
			try {
				String valueText = getTextOfElement();
				rtRange = new Range(valueText);
			} catch (Exception e) {
				throw new SAXException(
						"Could not read retention time range form raw data file");
			}
		}

		// <MZRANGE>
		if (qName.equals(PeakListElementName.MZRANGE.getElementName())) {
			try {
				String valueText = getTextOfElement();
				mzRange = new Range(valueText);
			} catch (Exception e) {
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

			DataPoint[] mzPeaks = new DataPoint[quantity];
			Range peakRTRange = null, peakMZRange = null, peakIntensityRange = null;
			for (int i = 0; i < quantity; i++) {
				double rt = buildingArrayRawDataFiles.get(peakColumnID)
						.getScan(scanNumbers[i]).getRetentionTime();
				double mz = masses[i];
				double intensity = intensities[i];
				if (i == 0) {
					peakRTRange = new Range(rt);
					peakMZRange = new Range(mz);
					peakIntensityRange = new Range(intensity);
				} else {
					peakRTRange.extendRange(rt);
					peakMZRange.extendRange(mz);
					peakIntensityRange.extendRange(intensity);
				}

				mzPeaks[i] = new SimpleDataPoint(mz, intensity);
			}

			SimpleChromatographicPeak peak = new SimpleChromatographicPeak(
					buildingArrayRawDataFiles.get(peakColumnID), mass, rt,
					height, area, scanNumbers, mzPeaks, PeakStatus.valueOf(
							PeakStatus.class, peakStatus), -1, -1, peakRTRange,
					peakMZRange, peakIntensityRange);

			buildingRow.addPeak(buildingArrayRawDataFiles.get(peakColumnID),
					peak);
		}

		// <IDENTITY_PROPERTY>
		if (qName.equals(PeakListElementName.IDPROPERTY.getElementName())) {
			identityProperties.put(identityPropertyName, getTextOfElement());
		}

		// <PEAK_IDENTITY>
		if (qName.equals(PeakListElementName.PEAK_IDENTITY.getElementName())) {
			SimplePeakIdentity identity = new SimplePeakIdentity(
					identityProperties);
			buildingRow.addPeakIdentity(identity, preferred);
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
					SimpleScan newScan = new SimpleScan(buildingRawDataFile,
							scanNumbers[i], 1, retentionTimes[i], -1, 0f, 0,
							null, new DataPoint[0], false);
					buildingRawDataFile.addScan(newScan);
				}
				scanFlag = false;

			} catch (Exception e) {
				setStatus( TaskStatus.ERROR );
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

	private void initializePeakList() {
		RawDataFile[] dataFiles = buildingArrayRawDataFiles.values().toArray(
				new RawDataFile[0]);
		buildingPeakList = new SimplePeakList(peakListName, dataFiles);
		String[] process = appliedProcess.toArray(new String[0]);
		for (String description : process) {
			((SimplePeakList) buildingPeakList)
					.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
							description));
		}
		// Add task description to peakList
		((SimplePeakList) buildingPeakList)
				.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
						getTaskDescription()));

		((SimplePeakList) buildingPeakList).setDateCreated(dateCreated);
	}

	public Object[] getCreatedObjects() {
		return new Object[] { buildingPeakList };
	}
	
	/**
	 * Adds a TaskListener to this Task
	 * 
	 * @param t The TaskListener to add
	 */
	public void addTaskListener( TaskListener t ) {
		this.taskListeners.add( t );
	}

	/**
	 * Returns all of the TaskListeners which are listening to this task.
	 * 
	 * @return An array containing the TaskListeners
	 */
	public TaskListener[] getTaskListeners( ) {
		return this.taskListeners.toArray( new TaskListener[ this.taskListeners.size( )]);
	}

	private void fireTaskEvent( ) {
		TaskEvent event = new TaskEvent( this );
		for( TaskListener t : this.taskListeners ) {
			t.statusChanged( event );
		}
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#setStatus()
	 */
	public void setStatus( TaskStatus newStatus ) {
		this.status = newStatus;
		this.fireTaskEvent( );
	}

	public boolean isCanceled( ) {
		return status == TaskStatus.CANCELED;
	}

	public boolean isFinished( ) {
		return status == TaskStatus.FINISHED;
	}
}
