/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.CompressionUtils;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.ScanUtils;

import org.apache.axis.encoding.Base64;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class reads mzML 1.0 and 1.1.0 files.
 * (http://www.psidev.info/index.php?q=node/257)
 */
public class MzMLReadTask extends AbstractTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private File file;
	private RawDataFileWriter newMZmineFile;
	private RawDataFile finalRawDataFile;
	private int totalScans = 0, parsedScans;
	private int peaksCount = 0;
	private StringBuilder charBuffer;
	private boolean precursorFlag = false;
	private boolean spectrumFlag = false;
	private boolean spectrumListFlag = false;
	private boolean scanFlag = false;
	private boolean ionSelectionFlag = false;
	private boolean binaryDataArrayFlag = false;
	private boolean mzArrayBinaryFlag = false;
	private boolean intenArrayBinaryFlag = false;
	private boolean compressFlag = false;
	private int precision, scanNumber, msLevel, parentScan;
	private double retentionTime, precursorMz;
	private int precursorCharge = 0;
	private DefaultHandler handler = new MzMLHandler( );

	private HashMap<String, Integer> scanId = new HashMap<String, Integer>();

	/*
	 * The information of "m/z" & "int" is content in two arrays because the
	 * mzData standard manages this information in two different tags.
	 */
	private double[] mzDataPoints;
	private double[] intensityDataPoints;

	/*
	 * This variable hold the current scan or fragment, it is send to the stack
	 * when another scan/fragment appears as a parser.startElement
	 */
	private SimpleScan buildingScan;

	/*
	 * This stack stores at most 20 consecutive scans. This window serves to
	 * find possible fragments (current scan) that belongs to any of the stored
	 * scans in the stack. The reason of the size follows the concept of
	 * neighborhood of scans and all his fragments. These solution is
	 * implemented because exists the possibility to find fragments of one scan
	 * after one or more full scans.
	 */
	private static final int limitSize = 20;
	private LinkedList<SimpleScan> parentStack;

	public MzMLReadTask(File fileToOpen, RawDataFileWriter newMZmineFile) {
		// 256 kilo-chars buffer
		charBuffer = new StringBuilder(1 << 18);
		parentStack = new LinkedList<SimpleScan>();
		this.file = fileToOpen;
		this.newMZmineFile = newMZmineFile;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		setStatus( TaskStatus.PROCESSING );
		logger.info("Started parsing file " + file);

		// Use the default (non-validating) parser
		SAXParserFactory factory = SAXParserFactory.newInstance();

		try {

			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(file, handler);

			// Close file
			finalRawDataFile = newMZmineFile.finishWriting();

		} catch (Throwable e) {
			if (getStatus( ) == TaskStatus.PROCESSING) {
				setStatus( TaskStatus.ERROR );
				errorMessage = ExceptionUtils.exceptionToString(e);
			}
			return;
		}

		if (parsedScans == 0) {
			setStatus( TaskStatus.ERROR );
			errorMessage = "No scans found";
			return;
		}

		logger.info("Finished parsing " + file + ", parsed "
				+ parsedScans + " scans");
		setStatus( TaskStatus.FINISHED );

	}

	public String getTaskDescription() {
		return "Opening file" + file;
	}

	public Object[] getCreatedObjects() {
		return new Object[] { finalRawDataFile };
	}

	private class MzMLHandler extends DefaultHandler {

		/**
		 * startElement()
		 * 
		 * @see org.xml.sax.ContentHandler#startElement(String , String , String ,
		 *      Attributes )
		 */
		public void startElement(String namespaceURI, String lName, String qName,
				Attributes attrs) throws SAXException {

			if (isCanceled( ))
				throw new SAXException("Parsing Cancelled");

			// <spectrumList>
			if (qName.equals("spectrumList")) {
				String count = attrs.getValue("count");
				if (count != null)
					totalScans = Integer.parseInt(count);
				spectrumListFlag = true;
			}

			// <spectrum>
			if (qName.equalsIgnoreCase("spectrum")) {
				retentionTime = 0;
				parentScan = -1;
				precursorMz = 0f;
				precision = 32;
				precursorCharge = 0;
				compressFlag = false;

				String id = attrs.getValue("id");
				String defaultArrayLength = attrs.getValue("defaultArrayLength");
				if ((id == null) || (defaultArrayLength == null))
					throw new SAXException(
							"Missing spectrum id or defaultArrayLength");

				Pattern pattern = Pattern.compile("scan=([0-9]+)");
				Matcher matcher = pattern.matcher(id);
				boolean scanNumberFound = matcher.find();

				if (!scanNumberFound)
					throw new SAXException("Scan number not found in scan id " + id);

				scanNumber = Integer.parseInt(matcher.group(1));
				peaksCount = Integer.parseInt(defaultArrayLength);
				scanId.put(id, scanNumber);
				spectrumFlag = true;
			}

			// <scan>
			if (qName.equalsIgnoreCase("scan")) {
				scanFlag = true;
			}

			// <selectedIon> (mzML 1.1.0) or <ionSelection> (mzML 1.0)
			if (qName.equalsIgnoreCase("ionSelection")
					|| qName.equalsIgnoreCase("selectedIon")) {
				ionSelectionFlag = true;
			}

			// <precursor>
			if (qName.equalsIgnoreCase("precursor")) {
				String parent = attrs.getValue("spectrumRef");
				if (parent != null) {
					if (scanId.containsKey(parent))
						parentScan = scanId.get(parent);
					else
						parentScan = -1;
				} else
					parentScan = -1;
				precursorFlag = true;
			}

			// <cvParam>
			if (qName.equalsIgnoreCase("cvParam")) {
				String accession = attrs.getValue("accession");

				if (spectrumFlag) {
					if (accession.equals("MS:1000511")) {
						msLevel = Integer.parseInt(attrs.getValue("value"));
					}
				}

				if (scanFlag) {
					if (accession.equals("MS:1000016")) {
						String unitAccession = attrs.getValue("unitAccession");
						String value = attrs.getValue("value");
						if ((unitAccession != null) && (value != null)) {
							// MS:1000038 is used in mzML 1.0, while UO:0000031 is
							// used in mzML 1.1.0 :-/
							if (unitAccession.equals("MS:1000038")
									|| unitAccession.equals("UO:0000031"))
								retentionTime = Double.parseDouble(value) * 60d;
							else
								retentionTime = Double.parseDouble(value);
						} else
							throw new SAXException(
									"Corrupted retention time information");
					}
				}

				if ((precursorFlag) && (ionSelectionFlag)) {
					String value = attrs.getValue("value");
					if (value != null) {
						// MS:1000040 is used in mzML 1.0, MS:1000744 is used in
						// mzML 1.1.0
						if (accession.equals("MS:1000040")
								|| accession.equals("MS:1000744"))
							precursorMz = Double.parseDouble(value);
						if (accession.equals("MS:1000041"))
							precursorCharge = Integer.parseInt(value);
					} else
						throw new SAXException("Corrupted precursor information");
				}
				if (binaryDataArrayFlag) {
					if (accession.equals("MS:1000514"))
						mzArrayBinaryFlag = true;
					if (accession.equals("MS:1000515"))
						intenArrayBinaryFlag = true;
					if (accession.equals("MS:1000521")) {
						precision = 32;
					}
					if (accession.equals("MS:1000523")) {
						precision = 64;
					}
					if (accession.equals("MS:1000574")) {
						compressFlag = true;
					}
				}
			}

			// <binaryDataArray>
			if (qName.equalsIgnoreCase("binaryDataArray")) {
				binaryDataArrayFlag = true;
			}

		}

		/**
		 * endElement()
		 * 
		 * @see org.xml.sax.ContentHandler#endElement(String , String , String )
		 */
		public void endElement(String namespaceURI, String sName, String qName)
				throws SAXException {

			// <spectrumList>
			if (qName.equals("spectrumList")) {
				spectrumListFlag = false;
			}

			// <spectrum>
			if (qName.equalsIgnoreCase("spectrum")) {
				spectrumFlag = false;
			}

			// <scan>
			if (qName.equalsIgnoreCase("scan")) {
				scanFlag = false;
			}

			// <precursor>
			if (qName.equalsIgnoreCase("precursor")) {
				precursorFlag = false;
			}

			// <selectedIon> (mzML 1.1.0) or <ionSelection> (mzML 1.0)
			if (qName.equalsIgnoreCase("ionSelection")
					|| qName.equalsIgnoreCase("selectedIon")) {
				ionSelectionFlag = false;
			}

			// <spectrum>
			if (qName.equalsIgnoreCase("spectrum")) {

				if (mzDataPoints.length != intensityDataPoints.length) {
					setStatus( TaskStatus.ERROR );
					errorMessage = "Corrupt list of peaks of scan number "
							+ scanNumber;
					throw new SAXException("Parsing Cancelled");
				}

				DataPoint completeDataPoints[] = new DataPoint[peaksCount];

				// Copy m/z and intensity data
				for (int i = 0; i < completeDataPoints.length; i++) {
					completeDataPoints[i] = new SimpleDataPoint(
							(double) mzDataPoints[i],
							(double) intensityDataPoints[i]);
				}

				// Auto-detect whether this scan is centroided
				boolean centroided = ScanUtils.isCentroided(completeDataPoints);

				// Remove zero data points
				DataPoint optimizedDataPoints[] = ScanUtils.removeZeroDataPoints(
						completeDataPoints, centroided);

				buildingScan = new SimpleScan(null, scanNumber, msLevel,
						retentionTime, parentScan, precursorMz, precursorCharge, null,
						optimizedDataPoints, centroided);

				buildingScan.setPrecursorCharge(precursorCharge);

				/*
				 * Update of fragmentScanNumbers of each Scan in the parentStack
				 */
				for (SimpleScan s : parentStack) {
					if (s.getScanNumber() == buildingScan.getParentScanNumber()) {
						s.addFragmentScan(buildingScan.getScanNumber());
					}
				}

				/*
				 * Verify the size of parentStack. The actual size of the window to
				 * cover possible candidates is defined by limitSize.
				 */
				if (parentStack.size() > limitSize) {
					SimpleScan scan = parentStack.removeLast();
					try {
						newMZmineFile.addScan(scan);
					} catch (IOException e) {
						setStatus( TaskStatus.ERROR );
						errorMessage = "IO error: " + e;
						throw new SAXException("Parsing cancelled");
					}
					parsedScans++;
				}

				parentStack.addFirst(buildingScan);
				buildingScan = null;

			}

			if (qName.equalsIgnoreCase("binaryDataArray")) {
				// clean the current char buffer for the new element
				binaryDataArrayFlag = false;
			}

			// <Binary>
			if ((qName.equalsIgnoreCase("Binary")) && (spectrumListFlag)) {

				byte[] peakBytes = Base64.decode(charBuffer.toString());

				if (compressFlag) {
					// Uncompress the bytes
					try {
						peakBytes = CompressionUtils.decompress(peakBytes);
					} catch (DataFormatException e) {
						setStatus( TaskStatus.ERROR );
						errorMessage = "Corrupt compressed peak: " + e.toString();
						throw new SAXException("Parsing Cancelled");
					}
				}

				ByteBuffer currentBytes = ByteBuffer.wrap(peakBytes);
				currentBytes = currentBytes.order(ByteOrder.LITTLE_ENDIAN);

				if (mzArrayBinaryFlag) {

					mzArrayBinaryFlag = false;
					mzDataPoints = new double[peaksCount];

					for (int i = 0; i < mzDataPoints.length; i++) {
						if (precision == 32)
							mzDataPoints[i] = (double) currentBytes.getFloat();
						else
							mzDataPoints[i] = currentBytes.getDouble();
					}
				}

				if (intenArrayBinaryFlag) {

					intenArrayBinaryFlag = false;
					intensityDataPoints = new double[peaksCount];

					for (int i = 0; i < intensityDataPoints.length; i++) {
						if (precision == 32)
							intensityDataPoints[i] = (double) currentBytes
									.getFloat();
						else
							intensityDataPoints[i] = currentBytes.getDouble();
					}
				}
				charBuffer.setLength(0);
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

		public void endDocument() throws SAXException {
			while (!parentStack.isEmpty()) {
				SimpleScan scan = parentStack.removeLast();
				try {
					newMZmineFile.addScan(scan);
				} catch (IOException e) {
					setStatus( TaskStatus.ERROR );
					errorMessage = "IO error: " + e;
					throw new SAXException("Parsing cancelled");
				}
				parsedScans++;
			}
		}

	}
}
