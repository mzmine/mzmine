/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.projectload.version_2_5;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.RangeUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RawDataFileOpenHandler_2_5 extends DefaultHandler  {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private StringBuffer charBuffer;
  private RawDataFileImpl newRawDataFile;
  private int scanNumber;
  private int msLevel;
  private int[] fragmentScan;
  private int numberOfFragments;
  private double precursorMZ;
  private int precursorCharge;
  private float retentionTime;
  private int dataPointsNumber;
  private int fragmentCount;
  private int currentStorageID;
  private int storedDataID;
  private int storedDataNumDP;
  private TreeMap<Integer, Long> dataPointsOffsets;
  private TreeMap<Integer, Integer> dataPointsLengths;
  private ArrayList<MassList> massLists;
  private PolarityType polarity = PolarityType.UNKNOWN;
  private String scanDescription = "";
  private Range<Double> scanMZRange = null;

  private boolean canceled = false;

  /**
   * Extract the scan file and copies it into the temporary folder. Create a new raw data file using
   * the information from the XML raw data description file
   *
   * @param is
   * @param scansFile
   * @param isIMSRawDataFile this parameter is ignored in project version 2.3
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public RawDataFile readRawDataFile(InputStream is, File scansFile, boolean isIMSRawDataFile,
      boolean isImagingRawDataFile) throws IOException, ParserConfigurationException, SAXException,
      UnsupportedOperationException {

    if (isIMSRawDataFile) {
      throw new UnsupportedOperationException(
          "Ion mobility is not supported in projects created before MZmine 3.0");
    }
    if (isImagingRawDataFile) {
      throw new UnsupportedOperationException(
          "Imaging is not supported in projects created before MZmine 3.0");
    }

    charBuffer = new StringBuffer();
    massLists = new ArrayList<>();

    newRawDataFile = (RawDataFileImpl) MZmineCore.createNewFile("DUMMYNAME", null, null);
    // newRawDataFile.openDataPointsFile(scansFile);

    // dataPointsOffsets = newRawDataFile.getDataPointsOffsets();
    // dataPointsLengths = newRawDataFile.getDataPointsLengths();

    // Reads the XML file (raw data description)
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser = factory.newSAXParser();
    saxParser.parse(is, this);

    // Adds the raw data file to MZmine
    // RawDataFile rawDataFile = newRawDataFile.finishWriting();
    return newRawDataFile;

  }

  public void cancel() {
    canceled = true;
  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String,
   *      java.lang.String, org.xml.sax.Attributes)
   */
  @Override
  public void startElement(String namespaceURI, String lName, String qName, Attributes attrs)
      throws SAXException {

    if (canceled) {
      throw new SAXException("Parsing canceled");
    }

    // This will remove any remaining characters from previous elements
    getTextOfElement();

    if (qName.equals(RawDataElementName_2_5.QUANTITY_FRAGMENT_SCAN.getElementName())) {
      numberOfFragments =
          Integer.parseInt(attrs.getValue(RawDataElementName_2_5.QUANTITY.getElementName()));
      if (numberOfFragments > 0) {
        fragmentScan = new int[numberOfFragments];
        fragmentCount = 0;
      }
    }

    if (qName.equals(RawDataElementName_2_5.SCAN.getElementName())) {
      currentStorageID =
          Integer.parseInt(attrs.getValue(RawDataElementName_2_5.STORAGE_ID.getElementName()));
    }

    if (qName.equals(RawDataElementName_2_5.STORED_DATA.getElementName())) {
      storedDataID =
          Integer.parseInt(attrs.getValue(RawDataElementName_2_5.STORAGE_ID.getElementName()));
      storedDataNumDP = Integer
          .parseInt(attrs.getValue(RawDataElementName_2_5.QUANTITY_DATAPOINTS.getElementName()));
    }

    if (qName.equals(RawDataElementName_2_5.MASS_LIST.getElementName())) {
      String name = attrs.getValue(RawDataElementName_2_5.NAME.getElementName());
      int storageID =
          Integer.parseInt(attrs.getValue(RawDataElementName_2_5.STORAGE_ID.getElementName()));
      // todo: what was this supposed to do?
//      MassList newML = new SimpleMassList(null, null, null, null);
//      massLists.add(newML);
    }
  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public void endElement(String namespaceURI, String sName, String qName) throws SAXException {

    if (canceled) {
      throw new SAXException("Parsing canceled");
    }

    // <NAME>
    if (qName.equals(RawDataElementName_2_5.NAME.getElementName())) {

      // Adds the scan file and the name to the new raw data file
      String name = getTextOfElement();
      logger.info("Loading raw data file: " + name);
      newRawDataFile.setName(name);
    }

    if (qName.equals(RawDataElementName_2_5.QUANTITY_SCAN.getElementName())) {
      // number of scans - actually not used for anything
      Integer.parseInt(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_2_5.SCAN_ID.getElementName())) {
      scanNumber = Integer.parseInt(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_2_5.STORED_DATA.getElementName())) {
      // need to multiply the offsets by 2 to account for the fact that the old projects used floats
      // but now we use doubles
      // TODO is this still necessary? @tomas
      long offset = Long.parseLong(getTextOfElement()) * 2;
      dataPointsOffsets.put(storedDataID, offset);
      dataPointsLengths.put(storedDataID, storedDataNumDP);
    }

    if (qName.equals(RawDataElementName_2_5.MS_LEVEL.getElementName())) {
      msLevel = Integer.parseInt(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_2_5.PARENT_SCAN.getElementName())) {
      Integer.parseInt(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_2_5.POLARITY.getElementName())) {
      String txt = getTextOfElement();
      try {
        polarity = PolarityType.valueOf(txt);
      } catch (Exception e) {
        polarity = PolarityType.fromSingleChar(txt);
      }
    }

    if (qName.equals(RawDataElementName_2_5.SCAN_DESCRIPTION.getElementName())) {
      scanDescription = getTextOfElement();
    }

    if (qName.equals(RawDataElementName_2_5.SCAN_MZ_RANGE.getElementName())) {
      final String text = getTextOfElement();
      scanMZRange = RangeUtils.parseDoubleRange(text);
    }

    if (qName.equals(RawDataElementName_2_5.PRECURSOR_CHARGE.getElementName())) {
      precursorCharge = Integer.parseInt(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_2_5.PRECURSOR_MZ.getElementName())) {
      precursorMZ = Double.parseDouble(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_2_5.RETENTION_TIME.getElementName())) {
      // Before MZmine.6 retention time was saved in seconds, but now we
      // use minutes, so we need to divide by 60
      retentionTime = (float) (Double.parseDouble(getTextOfElement()) / 60d);
    }

    // if (qName.equals(RawDataElementName_2_5.ION_MOBILITY.getElementName())) {
    // mobility = Double.parseDouble(getTextOfElement());
    // }

    if (qName.equals(RawDataElementName_2_5.QUANTITY_DATAPOINTS.getElementName())) {
      dataPointsNumber = Integer.parseInt(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_2_5.FRAGMENT_SCAN.getElementName())) {
      fragmentScan[fragmentCount++] = Integer.parseInt(getTextOfElement());
    }

    if (qName.equals(RawDataElementName_2_5.SCAN.getElementName())) {

      final DDAMsMsInfo info =
          msLevel != 1 && precursorMZ != 0d ? new DDAMsMsInfoImpl(precursorMZ, precursorCharge,
              null, null, null, msLevel, ActivationMethod.UNKNOWN, null) : null;

      Scan storableScan = new SimpleScan(newRawDataFile, scanNumber, msLevel, retentionTime,
          info, /* fragmentScan, */ null, null, null, polarity,
          scanDescription, scanMZRange);

      try {
        newRawDataFile.addScan(storableScan);
      } catch (IOException e) {
        throw new SAXException(e);
      }

      for (MassList newML : massLists) {
        // newML.setScan(storableScan);
        storableScan.addMassList(newML);
      }

      // Cleanup
      massLists.clear();
      currentStorageID = -1;
      dataPointsNumber = -1;
      scanNumber = -1;
      msLevel = -1;
      retentionTime = -1;
      precursorMZ = -1;
      precursorCharge = -1;
      fragmentScan = null;
      polarity = PolarityType.UNKNOWN;
      scanDescription = "";
      scanMZRange = null;

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
  @Override
  public void characters(char buf[], int offset, int len) throws SAXException {
    charBuffer = charBuffer.append(buf, offset, len);
  }
}
