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

package net.sf.mzmine.modules.io.rawdataimport.fileformats;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.CompressionUtils;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.ScanUtils;

import org.jfree.xml.util.Base64;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 */
public class MzXMLReadTask extends DefaultHandler implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private File originalFile;
	private RawDataFileWriter newRawDataFile;
	private RawDataFile finalRawDataFile;
	private TaskStatus status = TaskStatus.WAITING;
	private int totalScans = 0, parsedScans;
	private int peaksCount = 0;
	private String errorMessage;
	private StringBuilder charBuffer;
	private boolean compressFlag = false;

	// Retention time parser
	private DatatypeFactory dataTypeFactory;

	/*
	 * This variables are used to set the number of fragments that one single
	 * scan can have. The initial size of array is set to 10, but it depends of
	 * fragmentation level.
	 */
	private int parentTreeValue[] = new int[10];
	private int msLevelTree = 0;

	/*
	 * This stack stores the current scan and all his fragments until all the
	 * information is recover. The logic is FIFO at the moment of write into the
	 * RawDataFile
	 */
	private LinkedList<SimpleScan> parentStack;

	/*
	 * This variable hold the present scan or fragment, it is send to the stack
	 * when another scan/fragment appears as a parser.startElement
	 */
	private SimpleScan buildingScan;

	public MzXMLReadTask(File fileToOpen) {
		originalFile = fileToOpen;
		// 256 kilo-chars buffer
		charBuffer = new StringBuilder(1 << 18);
		parentStack = new LinkedList<SimpleScan>();
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Opening file " + originalFile;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getStatus()
	 */
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		status = TaskStatus.PROCESSING;
		logger.info("Started parsing file " + originalFile);

		// Use the default (non-validating) parser
		SAXParserFactory factory = SAXParserFactory.newInstance();

		try {

			dataTypeFactory = DatatypeFactory.newInstance();

			newRawDataFile = MZmineCore.createNewFile(originalFile.getName());

			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(originalFile, this);

			// Close file
			finalRawDataFile = newRawDataFile.finishWriting();
			MZmineCore.getCurrentProject().addFile(finalRawDataFile);

		} catch (Throwable e) {
			/* we may already have set the status to CANCELED */
			if (status == TaskStatus.PROCESSING) {
				status = TaskStatus.ERROR;
				errorMessage = ExceptionUtils.exceptionToString(e);
			}
			return;
		}

		if (parsedScans == 0) {
			status = TaskStatus.ERROR;
			errorMessage = "No scans found";
			return;
		}

		logger.info("Finished parsing " + originalFile + ", parsed "
				+ parsedScans + " scans");
		status = TaskStatus.FINISHED;

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		logger.info("Cancelling opening of MZXML file " + originalFile);
		status = TaskStatus.CANCELED;
	}

	public void startElement(String namespaceURI, String lName, // local name
			String qName, // qualified name
			Attributes attrs) throws SAXException {

		if (status == TaskStatus.CANCELED)
			throw new SAXException("Parsing Cancelled");

		// <msRun>
		if (qName.equals("msRun")) {
			String s = attrs.getValue("scanCount");
			if (s != null)
				totalScans = Integer.parseInt(s);
		}

		// <scan>
		if (qName.equalsIgnoreCase("scan")) {

			if (buildingScan != null) {
				parentStack.addFirst(buildingScan);
				buildingScan = null;
			}

			/*
			 * Only num, msLevel & peaksCount values are required according with
			 * mzxml standard, the others are optional
			 */
			int scanNumber = Integer.parseInt(attrs.getValue("num"));
			int msLevel = Integer.parseInt(attrs.getValue("msLevel"));
			peaksCount = Integer.parseInt(attrs.getValue("peaksCount"));

			// Parse retention time
			double retentionTime = 0;
			String retentionTimeStr = attrs.getValue("retentionTime");
			if (retentionTimeStr != null) {
				Date currentDate = new Date();
				Duration dur = dataTypeFactory.newDuration(retentionTimeStr);
				retentionTime = dur.getTimeInMillis(currentDate) / 1000d;
			} else {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not contain retentionTime for scans";
				throw new SAXException("Could not read retention time");
			}

			int parentScan = -1;

			if (msLevel > 9) {
				status = TaskStatus.ERROR;
				errorMessage = "msLevel value bigger than 10";
				throw new SAXException("The value of msLevel is bigger than 10");
			}

			if (msLevel > 1) {
				parentScan = parentTreeValue[msLevel - 1];
				for (SimpleScan p : parentStack) {
					if (p.getScanNumber() == parentScan) {
						p.addFragmentScan(scanNumber);
					}
				}
			}

			// Setting the level of fragment of scan and parent scan number
			msLevelTree++;
			parentTreeValue[msLevel] = scanNumber;

			buildingScan = new SimpleScan(null, scanNumber, msLevel,
					retentionTime, parentScan, 0, 0, null, new DataPoint[0],
					false);

		}

		// <peaks>
		if (qName.equalsIgnoreCase("peaks")) {
			// clean the current char buffer for the new element
			charBuffer.setLength(0);
			compressFlag = false;
			String compressionType = attrs.getValue("compressionType");
			if ((compressionType == null) || (compressionType.equals("none")))
				compressFlag = false;
			else
				compressFlag = true;

		}

		// <precursorMz>
		if (qName.equalsIgnoreCase("precursorMz")) {
			// clean the current char buffer for the new element
			charBuffer.setLength(0);
			String precursorCharge = attrs.getValue("precursorCharge");
			if (precursorCharge != null)
				buildingScan.setPrecursorCharge(Integer
						.parseInt(precursorCharge));
		}

	}

	/**
	 * endElement()
	 */
	public void endElement(String namespaceURI, String sName, // simple name
			String qName // qualified name
	) throws SAXException {

		// </scan>
		if (qName.equalsIgnoreCase("scan")) {

			msLevelTree--;

			/*
			 * At this point we verify if the scan and his fragments are closed,
			 * so we include the present scan/fragment into the stack and start
			 * to take elements from them (FIFO) for the RawDataFile.
			 */

			if (msLevelTree == 0) {
				parentStack.addFirst(buildingScan);
				buildingScan = null;
				while (!parentStack.isEmpty()) {
					SimpleScan currentScan = parentStack.removeLast();
					try {
						newRawDataFile.addScan(currentScan);
					} catch (IOException e) {
						status = TaskStatus.ERROR;
						errorMessage = "IO error: " + e;
						throw new SAXException("Parsing cancelled");
					}
					parsedScans++;
				}

				/*
				 * The scan with all his fragments is in the RawDataFile, now we
				 * clean the stack for the next scan and fragments.
				 */
				parentStack.clear();

			}

			return;
		}

		// <precursorMz>
		if (qName.equalsIgnoreCase("precursorMz")) {
			double precursorMz = Double.parseDouble(charBuffer.toString());
			buildingScan.setPrecursorMZ(precursorMz);
			return;
		}

		// <peaks>
		if (qName.equalsIgnoreCase("peaks")) {

			byte[] peakBytes = Base64.decode(charBuffer.toString()
					.toCharArray());

			if (compressFlag) {
				try {
					peakBytes = CompressionUtils.decompress(peakBytes);
				} catch (DataFormatException e) {
					status = TaskStatus.ERROR;
					errorMessage = "Corrupt compressed peak: " + e.toString();
					throw new SAXException("Parsing Cancelled");
				}
			}

			// make a data input stream
			DataInputStream peakStream = new DataInputStream(
					new ByteArrayInputStream(peakBytes));

			DataPoint completeDataPoints[] = new DataPoint[peaksCount];

			try {
				for (int i = 0; i < completeDataPoints.length; i++) {

					// Always respect this order pairOrder="m/z-int"
					double massOverCharge = (double) peakStream.readFloat();
					double intensity = (double) peakStream.readFloat();

					// Copy m/z and intensity data
					completeDataPoints[i] = new SimpleDataPoint(massOverCharge,
							intensity);

				}
			} catch (IOException eof) {
				status = TaskStatus.ERROR;
				errorMessage = "Corrupt mzXML file";
				throw new SAXException("Parsing Cancelled");
			}

			// Auto-detect whether this scan is centroided
			boolean centroided = ScanUtils.isCentroided(completeDataPoints);

			// Set the centroided tag
			buildingScan.setCentroided(centroided);

			// Remove zero data points
			DataPoint optimizedDataPoints[] = ScanUtils.removeZeroDataPoints(
					completeDataPoints, centroided);

			// Set the final data points to the scan
			buildingScan.setDataPoints(optimizedDataPoints);

			return;
		}
	}

	/**
	 * characters()
	 * 
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char buf[], int offset, int len) throws SAXException {
		charBuffer.append(buf, offset, len);
	}

	public Object[] getCreatedObjects() {
		return new Object[] { finalRawDataFile };
	}

}