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

package io.github.mzmine.modules.io.projectload.version_3_0;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.projectload.RawDataFileOpenHandler;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.project.impl.StorableFrame;
import io.github.mzmine.project.impl.StorableMassList;
import io.github.mzmine.project.impl.StorableScan;
import io.github.mzmine.util.RangeUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.scene.paint.Color;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RawDataFileOpenHandler_3_0 extends DefaultHandler implements RawDataFileOpenHandler {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private StringBuffer charBuffer;
  private RawDataFileImpl newRawDataFile;
  private int scanNumber;
  private int msLevel;
//  private int[] fragmentScan;
  private int numberOfFragments;
  private double precursorMZ;
  private int precursorCharge;
  private float retentionTime;
  private double mobility;
  private int dataPointsNumber;
  private int fragmentCount;
  private int currentStorageID;
  private int storedDataID;
  private int storedDataNumDP;
  private TreeMap<Integer, Long> dataPointsOffsets;
  private TreeMap<Integer, Integer> dataPointsLengths;
  private ArrayList<StorableMassList> massLists;
  private PolarityType polarity = PolarityType.UNKNOWN;
  private String scanDescription = "";
  private Range<Double> scanMZRange = null;

  private int[] mobilityScans;
  private int numberMoblityScans;
  private int mobilityScanCount;
  private MobilityType mobilityType;
  private int frameId = -1;
  private double lowerMobilityRange;
  private double upperMobilityRange;


  private boolean canceled = false;

  /**
   * Extract the scan file and copies it into the temporary folder. Create a new raw data file using
   * the information from the XML raw data description file
   *
   * @param is
   * @param scansFile
   * @param isIMSRawDataFile
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public RawDataFile readRawDataFile(InputStream is, File scansFile, boolean isIMSRawDataFile)
      throws IOException, ParserConfigurationException, SAXException {

    charBuffer = new StringBuffer();
    massLists = new ArrayList<StorableMassList>();

    if (isIMSRawDataFile) {
      newRawDataFile = (IMSRawDataFileImpl) MZmineCore.createNewIMSFile(null);
    } else {
      newRawDataFile = (RawDataFileImpl) MZmineCore.createNewFile(null);
    }
    newRawDataFile.openDataPointsFile(scansFile);

    dataPointsOffsets = newRawDataFile.getDataPointsOffsets();
    dataPointsLengths = newRawDataFile.getDataPointsLengths();

    // Reads the XML file (raw data description)
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser = factory.newSAXParser();
    saxParser.parse(is, this);

    // Adds the raw data file to MZmine
    RawDataFile rawDataFile = newRawDataFile.finishWriting();
    return rawDataFile;

  }

  public void cancel() {
    canceled = true;
  }

  /**
   * @see DefaultHandler#startElement(String, String, String, Attributes)
   */
  @Override
  public void startElement(String namespaceURI, String lName, String qName, Attributes attrs)
      throws SAXException {

    if (canceled) {
      throw new SAXException("Parsing canceled");
    }

    // This will remove any remaining characters from previous elements
    getTextOfElement();

    /*if (qName.equals(RawDataElementName_3_0.QUANTITY_FRAGMENT_SCAN.getElementName())) {
      numberOfFragments =
          Integer.parseInt(attrs.getValue(
              RawDataElementName_3_0.QUANTITY.getElementName()));
      if (numberOfFragments > 0) {
        fragmentScan = new int[numberOfFragments];
        fragmentCount = 0;
      }
    }*/

    if (qName.equals(RawDataElementName_3_0.SCAN.getElementName())) {
      currentStorageID =
          Integer.parseInt(attrs.getValue(
              RawDataElementName_3_0.STORAGE_ID.getElementName()));
    }

    if (qName.equals(RawDataElementName_3_0.STORED_DATA.getElementName())) {
      storedDataID =
          Integer.parseInt(attrs.getValue(
              RawDataElementName_3_0.STORAGE_ID.getElementName()));
      storedDataNumDP = Integer
          .parseInt(attrs.getValue(
              RawDataElementName_3_0.QUANTITY_DATAPOINTS.getElementName()));
    }

    if (qName.equals(RawDataElementName_3_0.MASS_LIST.getElementName())) {
      String name = attrs.getValue(
          RawDataElementName_3_0.NAME.getElementName());
      int storageID =
          Integer.parseInt(attrs.getValue(
              RawDataElementName_3_0.STORAGE_ID.getElementName()));
      StorableMassList newML = new StorableMassList(newRawDataFile, storageID, name, null);
      massLists.add(newML);
    }

    if (qName.equals(RawDataElementName_3_0.FRAME.getElementName())) {
      currentStorageID = Integer
          .parseInt(attrs.getValue(RawDataElementName_3_0.STORAGE_ID.getElementName()));
    }

    if (qName.equals(RawDataElementName_3_0.QUANTITY_MOBILITY_SCANS.getElementName())) {
      numberMoblityScans =
          Integer.parseInt(attrs.getValue(
              RawDataElementName_3_0.QUANTITY.getElementName()));
      if (numberMoblityScans > 0) {
        mobilityScans = new int[numberMoblityScans];
        mobilityScanCount = 0;
      }
    }

  }

  /**
   * @see DefaultHandler#endElement(String, String, String)
   */
  @Override
  public void endElement(String namespaceURI, String sName, String qName) throws SAXException {

    if (canceled) {
      throw new SAXException("Parsing canceled");
    }

    // <NAME>
    if (qName.equals(RawDataElementName_3_0.NAME.getElementName())) {

      // Adds the scan file and the name to the new raw data file
      String name = getTextOfElement();
      logger.info("Loading raw data file: " + name);
      newRawDataFile.setName(name);
    }

    if (qName.equals(RawDataElementName_3_0.QUANTITY_SCAN.getElementName())) {
      // number of scans - actually not used for anything
      Integer.parseInt(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_3_0.SCAN_ID.getElementName())) {
      scanNumber = Integer.parseInt(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_3_0.COLOR.getElementName())) {
      String color = getTextOfElement();
      newRawDataFile.setColor(Color.valueOf(color));
    }

    if (qName.equals(RawDataElementName_3_0.STORED_DATA.getElementName())) {
      long offset = Long.parseLong(getTextOfElement());
      dataPointsOffsets.put(storedDataID, offset);
      dataPointsLengths.put(storedDataID, storedDataNumDP);
    }

    if (qName.equals(RawDataElementName_3_0.MS_LEVEL.getElementName())) {
      msLevel = Integer.parseInt(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_3_0.PARENT_SCAN.getElementName())) {
      Integer.parseInt(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_3_0.POLARITY.getElementName())) {
      String txt = getTextOfElement();
      try {
        polarity = PolarityType.valueOf(txt);
      } catch (Exception e) {
        polarity = PolarityType.fromSingleChar(txt);
      }
    }

    if (qName.equals(RawDataElementName_3_0.SCAN_DESCRIPTION.getElementName())) {
      scanDescription = getTextOfElement();
    }

    if (qName.equals(RawDataElementName_3_0.SCAN_MZ_RANGE.getElementName())) {
      final String text = getTextOfElement();
      scanMZRange = RangeUtils.parseDoubleRange(text);
    }

    if (qName.equals(RawDataElementName_3_0.PRECURSOR_CHARGE.getElementName())) {
      precursorCharge = Integer.parseInt(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_3_0.PRECURSOR_MZ.getElementName())) {
      precursorMZ = Double.parseDouble(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_3_0.RETENTION_TIME.getElementName())) {
      // Before MZmine.6 retention time was saved in seconds, but now we
      // use minutes, so we need to divide by 60
      retentionTime = (float) (Double.parseDouble(getTextOfElement()) / 60d);
    }

    if (qName.equals(RawDataElementName_3_0.QUANTITY_DATAPOINTS.getElementName())) {
      dataPointsNumber = Integer.parseInt(getTextOfElement());
    }

    /*if (qName.equals(RawDataElementName_3_0.FRAGMENT_SCAN.getElementName())) {
      fragmentScan[fragmentCount++] = Integer.parseInt(getTextOfElement());
    }*/

    if (qName.equals(RawDataElementName_3_0.MOBILITY.getElementName())) {
      mobility = Double.parseDouble(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_3_0.MOBILITY_TYPE.getElementName())) {
      mobilityType = MobilityType.valueOf(getTextOfElement());
      mobilityType = (mobilityType == null) ? MobilityType.NONE : mobilityType;
    }

    if (qName.equals(RawDataElementName_3_0.FRAME_ID.getElementName())) {
      frameId = Integer.parseInt(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_3_0.MOBILITY_SCANNUM.getElementName())) {
      mobilityScans[mobilityScanCount++] = Integer.parseInt(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_3_0.LOWER_MOBILITY_RANGE.getElementName())) {
      lowerMobilityRange = Double.parseDouble(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_3_0.UPPER_MOBILITY_RANGE.getElementName())) {
      upperMobilityRange = Double.parseDouble(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_3_0.SCAN.getElementName())) {

      final StorableScan storableScan = new StorableScan(newRawDataFile, currentStorageID,
          dataPointsNumber, scanNumber, msLevel, retentionTime, precursorMZ,
          precursorCharge, /*fragmentScan,*/ null, polarity, scanDescription, scanMZRange,
          mobility, mobilityType);

      try {
        newRawDataFile.addScan(storableScan);
      } catch (IOException e) {
        throw new SAXException(e);
      }

      for (StorableMassList newML : massLists) {
        newML.setScan(storableScan);
        storableScan.addMassList(newML);
      }

      // Cleanup
      resetReadValues();
    }

    if (qName.equals(RawDataElementName_3_0.FRAME.getElementName())) {

      /*final StorableFrame storableScan = new StorableFrame(newRawDataFile, currentStorageID,
          dataPointsNumber, scanNumber, msLevel, retentionTime, precursorMZ, precursorCharge,
          *//*fragmentScan,*//* null, polarity, scanDescription, scanMZRange, frameId,
          mobilityType, Range.closed(lowerMobilityRange, upperMobilityRange),
          Arrays.stream(mobilityScans).boxed().collect(Collectors.toList()));

      try {
        newRawDataFile.addScan(storableScan);
      } catch (IOException e) {
        throw new SAXException(e);
      }

      for (StorableMassList newML : massLists) {
        newML.setScan(storableScan);
        storableScan.addMassList(newML);
      }*/

      resetReadValues();
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

  private void resetReadValues() {
    massLists.clear();
    currentStorageID = -1;
    dataPointsNumber = -1;
    scanNumber = -1;
    msLevel = -1;
    retentionTime = -1;
    mobility = -1;
    precursorMZ = -1;
    precursorCharge = -1;
//    fragmentScan = null;
    polarity = PolarityType.UNKNOWN;
    scanDescription = "";
    scanMZRange = null;

    mobilityScans = null;
    numberMoblityScans = 0;
    mobilityScanCount = 0;
    mobilityType = MobilityType.NONE;
    frameId = -1;
    lowerMobilityRange = 0.0d;
    upperMobilityRange = 0.0d;
  }
}
