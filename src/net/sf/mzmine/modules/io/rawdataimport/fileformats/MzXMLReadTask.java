/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.io.rawdataimport.fileformats;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.zip.Inflater;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PreloadLevel;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.io.rawdataimport.RawDataFileImpl;
import net.sf.mzmine.taskcontrol.Task;

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
	private RawDataFileImpl newMZmineFile;
	private PreloadLevel preloadLevel;
	private TaskStatus status;
	private int totalScans = -1;
	private int parsedScans;
	private int peaksCount = 0;
	private String errorMessage;
	private StringBuilder charBuffer;
	private Boolean compressFlag = false;
	private int compressedLen;

	/*
	 * This variables are used to set the number of fragments that one single
	 * scan can have. The initial size of array is set to 10, but it depends of 
	 * fragmentation level. 
	 */
	private int parentTreeValue[] = new int[10];
	private int msLevelTree = 0;

	/*
	 * This stack stores the current scan and all his fragments until all the information
	 * is recover. The logic is FIFO at the moment of write into the MZmineFile
	 */
	private LinkedList<SimpleScan> parentStack;
	
	/*
	 * This variable hold the present scan or fragment, it is send to the stack when
	 * another scan/fragment appears as a parser.startElement
	 */
	private SimpleScan buildingScan;

	public MzXMLReadTask(File fileToOpen, PreloadLevel preloadLevel) {
		originalFile = fileToOpen;
		status = TaskStatus.WAITING;
		this.preloadLevel = preloadLevel;
		charBuffer = new StringBuilder(2048);
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
	public float getFinishedPercentage() {
		return totalScans == 0 ? 0 : (float) parsedScans / totalScans;
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

			newMZmineFile = new RawDataFileImpl(originalFile.getName(),
					"mzxml", preloadLevel);

			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(originalFile, this);

			// Close file
			newMZmineFile.finishWriting();
			MZmineCore.getCurrentProject().addFile(newMZmineFile);

		} catch (Throwable e) {
			/* we may already have set the status to CANCELED */
			e.printStackTrace();
			if (status == TaskStatus.PROCESSING)
				status = TaskStatus.ERROR;
			errorMessage = e.toString();
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
			 * Only num, msLevel & peaksCount values are required according with mzxml standard,
			 * the others are optional
			 */
			int scanNumber = Integer.parseInt(attrs.getValue("num"));
			int msLevel = Integer.parseInt(attrs.getValue("msLevel"));
			peaksCount = Integer.parseInt(attrs.getValue("peaksCount"));

			// Parse retention time
			float retentionTime = 0;
			String retentionTimeStr = attrs.getValue("retentionTime");
			if (retentionTimeStr != null){
				Date currentDate = new Date();
				try {
					DatatypeFactory dataTypeFactory = DatatypeFactory.newInstance();
					Duration dur = dataTypeFactory.newDuration(retentionTimeStr);
					retentionTime = dur.getTimeInMillis(currentDate) / 1000f;
				} catch (DatatypeConfigurationException e) {
					throw new SAXException("Could not read retention time: " + e);
				}
			}else {
				status = TaskStatus.ERROR;
				errorMessage = "This file does not contain retentionTime for scans";
				throw new SAXException("Could not read retention time");
			}

			int parentScan = -1;
			if (msLevelTree > 0) {
				if (msLevel > 9) {
					status = TaskStatus.ERROR;
					errorMessage = "msLevel value bigger than 10";
					throw new SAXException("The value of msLevel is bigger than 10");
				}
				parentScan = parentTreeValue[msLevel - 1];
			}

			
			// Setting the level of fragment of scan and parent scan number
				msLevelTree++;
				parentTreeValue[msLevel] = scanNumber;

			buildingScan = new SimpleScan(scanNumber, msLevel, retentionTime,
					parentScan, 0f, null, new DataPoint[0], false);

		}

		// <peak>
		if (qName.equalsIgnoreCase("peaks")) {
			// clean the current char buffer for the new element
			charBuffer.setLength(0);
			compressFlag = false;
			compressedLen = 0;
			if ((attrs.getValue("compressionType"))!= null){
				compressFlag = true;
			    compressedLen = Integer.parseInt(attrs.getValue("compressedLen"));
			}
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
			 * to take elements from them (FIFO) for the MZmineFile. 
			 */
			logger.info("Level of msLevelTree " + msLevelTree);

			if (msLevelTree == 0) {
				parentStack.addFirst(buildingScan);
				buildingScan = null;
				while (!parentStack.isEmpty()) {
					SimpleScan s = parentStack.removeLast();
					newMZmineFile.addScan(s);
					parsedScans++;
				}
				
				/* 
				 * The scan with all his fragments is in the MzmineFile, now we clean
				 * the stack for the next scan and fragments.
				 */
				parentStack.clear();
				
			} 
			
			/*
			 * If there are some scan/fragments still open, we update the reference of 
			 * fragments of each element in the stack with the current scan/fragment.
			 */
			
			else {
				for (SimpleScan s : parentStack) {
					if (s.getScanNumber() == buildingScan.getParentScanNumber()) {

						int[] b = s.getFragmentScanNumbers();
						if (b != null) {
							int[] temp = b;
							b = new int[temp.length + 1];
							System.arraycopy(temp, 0, b, 0, temp.length);
							b[temp.length] = buildingScan.getScanNumber();
							s.setFragmentScanNumbers(b);
						} else {
							b = new int[1];
							b[0] = s.getScanNumber();
							s.setFragmentScanNumbers(b);
						}
					}
				}
			}
			return;
		}

		// <precursorMz>
		if (qName.equalsIgnoreCase("precursorMz")) {
			float precursorMz = Float.parseFloat(charBuffer.toString());
			buildingScan.setPrecursorMZ(precursorMz);
			return;
		}

		// <peak>
		if (qName.equalsIgnoreCase("peaks")) {

			//
			DataPoint completeDataPoints[] = new DataPoint[peaksCount];
			DataPoint tempDataPoints[] = new DataPoint[peaksCount];
			byte[] peakBytes = Base64.decode(charBuffer.toString()
					.toCharArray());
			
			/*
			 * This section provides support for decompression ZLIB compression library. 
			 */
			
			if (compressFlag){
				// Decompress the bytes
				Inflater decompresser = new Inflater();
				decompresser.setInput(peakBytes, 0, compressedLen);
				byte[] result = new byte[1024];
				byte[] resultTotal = new byte [0];
				
				try {
					int resultLength = decompresser.inflate(result);
					while (!decompresser.finished()){
						byte temp[] = resultTotal;
						resultTotal = new byte[resultTotal.length + resultLength];
						System.arraycopy(temp, 0, resultTotal, 0, temp.length);
						System.arraycopy(result, 0, resultTotal, temp.length, resultLength);
						resultLength = decompresser.inflate(result);
					}
					byte temp[] = resultTotal;
					resultTotal = new byte[resultTotal.length + resultLength];
					System.arraycopy(temp, 0, resultTotal, 0, temp.length);
					System.arraycopy(result, 0, resultTotal, temp.length, resultLength);
					decompresser.end();
					peakBytes = new byte[resultTotal.length];
					peakBytes = resultTotal;
				} 
				catch (Exception eof) {
					status = TaskStatus.ERROR;
							errorMessage = "Corrupt compressed peak";
							throw new SAXException("Parsing Cancelled");
				 }
			}

			// make a data input stream
			DataInputStream peakStream = new DataInputStream(
					new ByteArrayInputStream(peakBytes));
			try {
				for (int i = 0; i < completeDataPoints.length; i++) {
					
					//Always respect this order pairOrder="m/z-int"
					float massOverCharge = peakStream.readFloat();
					float intensity = peakStream.readFloat();

					// Copy m/z and intensity data
					completeDataPoints[i] = new SimpleDataPoint(massOverCharge,
							intensity);

				}
			} catch (IOException eof) {
				status = TaskStatus.ERROR;
				errorMessage = "Corrupt mzXML file";
				throw new SAXException("Parsing Cancelled");
			}

			/*
			 * This section verifies DataPoints with intensity="0" and exclude
			 * them from tempDataPoints array. Only accept some of these points
			 * because they are part the left/right part of the peak.
			 */

			int i, j;
			for (i = 0, j = 0; i < completeDataPoints.length; i++) {
				float intensity = completeDataPoints[i].getIntensity();
				float mz = completeDataPoints[i].getMZ();
				if (completeDataPoints[i].getIntensity() > 0) {
					tempDataPoints[j] = new SimpleDataPoint(mz, intensity);
					j++;
					continue;
				}
				if ((i > 0) && (completeDataPoints[i - 1].getIntensity() > 0)) {
					tempDataPoints[j] = new SimpleDataPoint(mz, intensity);
					j++;
					continue;
				}
				if ((i < completeDataPoints.length - 1)
						&& (completeDataPoints[i + 1].getIntensity() > 0)) {
					tempDataPoints[j] = new SimpleDataPoint(mz, intensity);
					j++;
					continue;
				}
			}

			// If we have no peaks with intensity of 0, we assume the scan is
			// centroided
			if (i == j) {
				buildingScan.setCentroided(true);
				buildingScan.setDataPoints(tempDataPoints);
			} else {
				int sizeArray = j;
				DataPoint[] dataPoints = new DataPoint[j];

				System.arraycopy(tempDataPoints, 0, dataPoints, 0, sizeArray);
				/*
				 * for (int i = 0; i < j; i++){ dataPoints[i]=tempDataPoints[i]; }
				 */
				buildingScan.setDataPoints(dataPoints);
			}

			return;
		}
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
