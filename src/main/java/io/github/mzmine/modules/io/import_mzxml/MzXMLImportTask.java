/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.import_mzxml;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CompressionUtils;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class MzXMLImportTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private File file;
  private MZmineProject project;
  private RawDataFile newMZmineFile;
  private int totalScans = 0, parsedScans;
  private int peaksCount = 0;
  private StringBuilder charBuffer;
  private boolean compressFlag = false;
  private DefaultHandler handler = new MzXMLHandler();
  private String precision;

  // Retention time parser
  private DatatypeFactory dataTypeFactory;

  /*
   * This variables are used to set the number of fragments that one single scan can have. The
   * initial size of array is set to 10, but it depends of fragmentation level.
   */
  private int parentTreeValue[] = new int[10];
  private int msLevelTree = 0;

  /*
   * This stack stores the current scan and all his fragments until all the information is recover.
   * The logic is FIFO at the moment of write into the RawDataFile
   */
  private LinkedList<SimpleScan> parentStack;

  /*
   * This variable hold the present scan or fragment, it is send to the stack when another
   * scan/fragment appears as a parser.startElement
   */
  private SimpleScan buildingScan;

  public MzXMLImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile) {
    // 256 kilo-chars buffer
    charBuffer = new StringBuilder(1 << 18);
    parentStack = new LinkedList<SimpleScan>();
    this.project = project;
    this.file = fileToOpen;
    this.newMZmineFile = newMZmineFile;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Started parsing file " + file);

    // Use the default (non-validating) parser
    SAXParserFactory factory = SAXParserFactory.newInstance();

    try {

      dataTypeFactory = DatatypeFactory.newInstance();

      SAXParser saxParser = factory.newSAXParser();
      saxParser.parse(file, handler);

      project.addFile(newMZmineFile);

    } catch (Throwable e) {
      e.printStackTrace();
      /* we may already have set the status to CANCELED */
      if (getStatus() == TaskStatus.PROCESSING) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(ExceptionUtils.exceptionToString(e));
      }
      return;
    }

    if (isCanceled()) {
      return;
    }

    if (parsedScans == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No scans found");
      return;
    }

    logger.info("Finished parsing " + file + ", parsed " + parsedScans + " scans");
    setStatus(TaskStatus.FINISHED);

  }

  @Override
  public String getTaskDescription() {
    return "Opening file " + file;
  }

  private class MzXMLHandler extends DefaultHandler {

    @Override
    public void startElement(String namespaceURI, String lName, // local
        // name
        String qName, // qualified name
        Attributes attrs) throws SAXException {

      if (isCanceled()) {
        throw new SAXException("Parsing Cancelled");
      }

      // <msRun>
      if (qName.equals("msRun")) {
        String s = attrs.getValue("scanCount");
        if (s != null) {
          totalScans = Integer.parseInt(s);
        }
      }

      // <scan>
      if (qName.equalsIgnoreCase("scan")) {

        if (buildingScan != null) {
          parentStack.addFirst(buildingScan);
          buildingScan = null;
        }

        /*
         * Only num, msLevel & peaksCount values are required according with mzxml standard, the
         * others are optional
         */
        int scanNumber = Integer.parseInt(attrs.getValue("num"));

        // mzXML files with empty msLevel attribute do exist, so we use
        // 1 as default
        int msLevel = 1;
        if (!Strings.isNullOrEmpty(attrs.getValue("msLevel"))) {
          msLevel = Integer.parseInt(attrs.getValue("msLevel"));
        }

        String scanType = attrs.getValue("scanType");
        String filterLine = attrs.getValue("filterLine");
        String scanId = filterLine;
        if (Strings.isNullOrEmpty(scanId)) {
          scanId = scanType;
        }

        PolarityType polarity;
        String polarityAttr = attrs.getValue("polarity");
        if ((polarityAttr != null) && (polarityAttr.length() == 1)) {
          polarity = PolarityType.fromSingleChar(polarityAttr);
        } else {
          polarity = PolarityType.UNKNOWN;
        }
        peaksCount = Integer.parseInt(attrs.getValue("peaksCount"));

        // Parse retention time
        float retentionTime = 0;
        String retentionTimeStr = attrs.getValue("retentionTime");
        if (retentionTimeStr != null) {
          Date currentDate = new Date();
          Duration dur = dataTypeFactory.newDuration(retentionTimeStr);
          retentionTime = (float) (dur.getTimeInMillis(currentDate) / 1000d / 60d);
        } else {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("This file does not contain retentionTime for scans");
          throw new SAXException("Could not read retention time");
        }

        int parentScan = -1;

        if (msLevel > 9) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("msLevel value bigger than 10");
          throw new SAXException("The value of msLevel is bigger than 10");
        }

        /*
         * if (msLevel > 1) { parentScan = parentTreeValue[msLevel - 1]; for (SimpleScan p :
         * parentStack) { if (p.getScanNumber() == parentScan) { p.addFragmentScan(scanNumber); } }
         * }
         */

        // Setting the level of fragment of scan and parent scan number
        msLevelTree++;
        parentTreeValue[msLevel] = scanNumber;

        buildingScan = new SimpleScan(newMZmineFile, scanNumber, msLevel, retentionTime, 0, 0,
            new double[0], new double[0], null, polarity, scanId, null);

      }

      // <peaks>
      if (qName.equalsIgnoreCase("peaks")) {
        // clean the current char buffer for the new element
        charBuffer.setLength(0);
        compressFlag = false;
        String compressionType = attrs.getValue("compressionType");
        if ((compressionType == null) || (compressionType.equals("none"))) {
          compressFlag = false;
        } else {
          compressFlag = true;
        }
        precision = attrs.getValue("precision");

      }

      // <precursorMz>
      if (qName.equalsIgnoreCase("precursorMz")) {
        // clean the current char buffer for the new element
        charBuffer.setLength(0);
        String precursorCharge = attrs.getValue("precursorCharge");
        if (precursorCharge != null) {
          buildingScan.setPrecursorCharge(Integer.parseInt(precursorCharge));
        }
      }

    }

    /**
     * endElement()
     */
    @Override
    public void endElement(String namespaceURI, String sName, // simple name
        String qName // qualified name
    ) throws SAXException {

      // </scan>
      if (qName.equalsIgnoreCase("scan")) {

        msLevelTree--;

        /*
         * At this point we verify if the scan and his fragments are closed, so we include the
         * present scan/fragment into the stack and start to take elements from them (FIFO) for the
         * RawDataFile.
         */

        if (msLevelTree == 0) {
          parentStack.addFirst(buildingScan);
          buildingScan = null;
          while (!parentStack.isEmpty()) {
            SimpleScan currentScan = parentStack.removeLast();
            try {
              newMZmineFile.addScan(currentScan);
            } catch (IOException e) {
              e.printStackTrace();
              setStatus(TaskStatus.ERROR);
              setErrorMessage("IO error: " + e);
              throw new SAXException("Parsing error: " + e);
            }
            parsedScans++;
          }

          /*
           * The scan with all his fragments is in the RawDataFile, now we clean the stack for the
           * next scan and fragments.
           */
          parentStack.clear();

        }

        return;
      }

      // <precursorMz>
      if (qName.equalsIgnoreCase("precursorMz")) {
        final String textContent = charBuffer.toString();
        double precursorMz = 0d;
        if (!textContent.isEmpty()) {
          precursorMz = Double.parseDouble(textContent);
        }
        buildingScan.setPrecursorMZ(precursorMz);
        return;
      }

      // <peaks>
      if (qName.equalsIgnoreCase("peaks")) {

        byte[] peakBytes = Base64.getDecoder().decode(charBuffer.toString());

        if (compressFlag) {
          try {
            peakBytes = CompressionUtils.decompress(peakBytes);
          } catch (DataFormatException e) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Corrupt compressed peak: " + e.toString());
            throw new SAXException("Parsing Cancelled");
          }
        }

        // make a data input stream
        DataInputStream peakStream = new DataInputStream(new ByteArrayInputStream(peakBytes));

        double mzValues[] = new double[peaksCount];
        double intensityValues[] = new double[peaksCount];

        try {
          for (int i = 0; i < peaksCount; i++) {

            // Always respect this order pairOrder="m/z-int"
            double mz;
            double intensity;
            if ("64".equals(precision)) {
              mz = peakStream.readDouble();
              intensity = peakStream.readDouble();
            } else {
              mz = peakStream.readFloat();
              intensity = peakStream.readFloat();
            }

            // Copy m/z and intensity data
            mzValues[i] = mz;
            intensityValues[i] = intensity;

          }
        } catch (IOException eof) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Corrupt mzXML file");
          throw new SAXException("Parsing Cancelled");
        }

        // Auto-detect whether this scan is centroided
        MassSpectrumType spectrumType = ScanUtils.detectSpectrumType(mzValues, intensityValues);

        // Set the centroided tag
        buildingScan.setSpectrumType(spectrumType);

        // Set the final data points to the scan
        // This line awaits you, Robin: ~SteffenHeu :)
//        buildingScan.setDataPoints(mzValues, intensityValues);

        return;
      }
    }

    /**
     * characters()
     *
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char buf[], int offset, int len) throws SAXException {
      charBuffer.append(buf, offset, len);
    }
  }

}
