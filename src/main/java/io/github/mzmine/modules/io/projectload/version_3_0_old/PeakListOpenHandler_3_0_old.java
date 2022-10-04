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

package io.github.mzmine.modules.io.projectload.version_3_0_old;

import com.Ostermiller.util.Base64;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.modules.io.projectload.PeakListOpenHandler;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PeakListOpenHandler_3_0_old extends DefaultHandler implements PeakListOpenHandler {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private ModularFeatureListRow buildingRow;
  private ModularFeatureList buildingPeakList;

  private int numOfMZpeaks;
  private Integer representativeScan;
  private Integer fragmentScan;
  private String peakColumnID;
  private double mass;
  private float rt, area;
  private Integer[] scanNumbers;
  private Integer[] allMS2FragmentScanNumbers;
  private Vector<Integer> currentAllMS2FragmentScans;
  private float height;
  private double[] masses, intensities;
  private String peakStatus, peakListName, name, identityPropertyName, rawDataFileID;
  private Hashtable<String, String> identityProperties;
  private boolean preferred;
  private String dateCreated;

  private Map<String, String> informationProperties;
  private String infoPropertyName;

  private StringBuffer charBuffer;

  private Vector<String> appliedMethods, appliedMethodParameters;
  private Vector<RawDataFile> currentPeakListDataFiles;

  private Vector<DataPoint> currentIsotopes;
  private IsotopePatternStatus currentIsotopePatternStatus;
  private int currentPeakCharge;
  private String currentIsotopePatternDescription;

  private Integer parentChromatogramRowID = null;

  private Hashtable<String, RawDataFile> dataFilesIDMap;

  private int parsedRows, totalRows;

  private boolean canceled = false;

  private final MemoryMapStorage flistStorage = MemoryMapStorage.forFeatureList();
  private final MemoryMapStorage rawStorage = MemoryMapStorage.forRawDataFile();
  private final MemoryMapStorage massListStorage = MemoryMapStorage.forMassList();

  public PeakListOpenHandler_3_0_old(Hashtable<String, RawDataFile> dataFilesIDMap) {
    this.dataFilesIDMap = dataFilesIDMap;
  }

  /**
   * Load the feature list from the zip file reading the XML feature list file
   */
  @Override
  public FeatureList readPeakList(InputStream peakListStream)
      throws IOException, ParserConfigurationException, SAXException {

    totalRows = 0;
    parsedRows = 0;

    charBuffer = new StringBuffer();
    appliedMethods = new Vector<String>();
    appliedMethodParameters = new Vector<String>();
    currentPeakListDataFiles = new Vector<RawDataFile>();
    currentIsotopes = new Vector<DataPoint>();
    currentAllMS2FragmentScans = new Vector<Integer>();

    buildingPeakList = null;

    // Parse the XML file
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser = factory.newSAXParser();
    saxParser.parse(peakListStream, this);

    // If there were no rows in the peaklist, it is still not initialized
    if (buildingPeakList == null) {
      initializePeakList();
    }

    return buildingPeakList;

  }

  /**
   * @return the progress of these functions loading the feature list from the zip file.
   */
  public double getProgress() {
    if (totalRows == 0)
      return 0;
    return (double) parsedRows / totalRows;
  }

  @Override
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

    if (canceled)
      throw new SAXException("Parsing canceled");

    // This will remove any remaining characters from previous elements
    getTextOfElement();

    // <ROW>
    if (qName.equals(PeakListElementName_3_0_old.ROW.getElementName())) {

      if (buildingPeakList == null) {
        initializePeakList();
      }
      int rowID = Integer.parseInt(attrs.getValue(PeakListElementName_3_0_old.ID.getElementName()));
      buildingRow = new ModularFeatureListRow(buildingPeakList, rowID);
      String comment = attrs.getValue(PeakListElementName_3_0_old.COMMENT.getElementName());
      if (comment != null && !comment.isEmpty())
        buildingRow.setComment(comment);
    }

    // <PEAK_IDENTITY>
    if (qName.equals(PeakListElementName_3_0_old.PEAK_IDENTITY.getElementName())) {
      identityProperties = new Hashtable<String, String>();
      preferred =
          Boolean.parseBoolean(attrs.getValue(PeakListElementName_3_0_old.PREFERRED.getElementName()));
    }

    // <IDENTITY_PROPERTY>
    if (qName.equals(PeakListElementName_3_0_old.IDPROPERTY.getElementName())) {
      identityPropertyName = attrs.getValue(PeakListElementName_3_0_old.NAME.getElementName());
    }

    // <PEAK_INFORMATION>
    if (qName.equals(PeakListElementName_3_0_old.PEAK_INFORMATION.getElementName())) {
      informationProperties = new HashMap<>();
    }

    // <INFO_PROPERTY>
    if (qName.equals(PeakListElementName_3_0_old.INFO_PROPERTY.getElementName())) {
      infoPropertyName = attrs.getValue(PeakListElementName_3_0_old.NAME.getElementName());
    }

    // <PEAK>
    if (qName.equals(PeakListElementName_3_0_old.PEAK.getElementName())) {

      peakColumnID = attrs.getValue(PeakListElementName_3_0_old.COLUMN.getElementName());
      mass = Double.parseDouble(attrs.getValue(PeakListElementName_3_0_old.MZ.getElementName()));
      // Before MZmine.6 retention time was saved in seconds, but now we
      // use minutes, so we need to divide by 60
      rt = (float) (Double.parseDouble(attrs.getValue(PeakListElementName_3_0_old.RT.getElementName()))
          / 60d);
      height = (float) Double
          .parseDouble(attrs.getValue(PeakListElementName_3_0_old.HEIGHT.getElementName()));
      area =
          (float) Double.parseDouble(attrs.getValue(PeakListElementName_3_0_old.AREA.getElementName()));
      peakStatus = attrs.getValue(PeakListElementName_3_0_old.STATUS.getElementName());
      String chargeString = attrs.getValue(PeakListElementName_3_0_old.CHARGE.getElementName());
      if (chargeString != null)
        currentPeakCharge = Integer.valueOf(chargeString);
      else
        currentPeakCharge = 0;
      try {
        parentChromatogramRowID = Integer.parseInt(
            attrs.getValue(PeakListElementName_3_0_old.PARENT_CHROMATOGRAM_ROW_ID.getElementName()));
      } catch (NumberFormatException e) {
        parentChromatogramRowID = null;
      }
    }

    // <MZPEAK>
    if (qName.equals(PeakListElementName_3_0_old.MZPEAKS.getElementName())) {
      numOfMZpeaks =
          Integer.parseInt(attrs.getValue(PeakListElementName_3_0_old.QUANTITY.getElementName()));
    }

    // <ISOTOPE_PATTERN>
    if (qName.equals(PeakListElementName_3_0_old.ISOTOPE_PATTERN.getElementName())) {
      currentIsotopes.clear();
      currentIsotopePatternStatus = IsotopePatternStatus
          .valueOf(attrs.getValue(PeakListElementName_3_0_old.STATUS.getElementName()));
      currentIsotopePatternDescription =
          attrs.getValue(PeakListElementName_3_0_old.DESCRIPTION.getElementName());
    }

  }

  /**
   * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public void endElement(String namespaceURI, String sName, String qName) throws SAXException {
    if (canceled)
      throw new SAXException("Parsing canceled");

    // <NAME>
    if (qName.equals(PeakListElementName_3_0_old.PEAKLIST_NAME.getElementName())) {
      name = getTextOfElement();
      logger.info("Loading feature list: " + name);
      peakListName = name;
    }

    // <PEAKLIST_DATE>
    if (qName.equals(PeakListElementName_3_0_old.PEAKLIST_DATE.getElementName())) {
      dateCreated = getTextOfElement();
    }

    // <QUANTITY>
    if (qName.equals(PeakListElementName_3_0_old.QUANTITY.getElementName())) {
      String text = getTextOfElement();
      totalRows = Integer.parseInt(text);
    }

    // <RAW_FILE>
    if (qName.equals(PeakListElementName_3_0_old.RAWFILE.getElementName())) {
      rawDataFileID = getTextOfElement();
      RawDataFile dataFile = dataFilesIDMap.get(rawDataFileID);
      if (dataFile == null) {
        throw new SAXException(
            "Cannot open feature list, because raw data file " + rawDataFileID + " is missing.");
      }
      currentPeakListDataFiles.add(dataFile);
    }

    // <SCAN_ID>
    if (qName.equals(PeakListElementName_3_0_old.SCAN_ID.getElementName())) {

      byte[] bytes = Base64.decodeToBytes(getTextOfElement());
      // make a data input stream
      DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
      scanNumbers = new Integer[numOfMZpeaks];
      for (int i = 0; i < numOfMZpeaks; i++) {
        try {
          scanNumbers[i] = dataInputStream.readInt();
        } catch (IOException ex) {
          throw new SAXException(ex);
        }
      }
    }

    // <REPRESENTATIVE_SCAN>
    if (qName.equals(PeakListElementName_3_0_old.REPRESENTATIVE_SCAN.getElementName())) {
      representativeScan = Integer.valueOf(getTextOfElement());
    }

    // <FRAGMENT_SCAN>
    if (qName.equals(PeakListElementName_3_0_old.FRAGMENT_SCAN.getElementName())) {
      fragmentScan = Integer.valueOf(getTextOfElement());
    }

    // <All_MS2_FRAGMENT_SCANS>
    if (qName.equals(PeakListElementName_3_0_old.ALL_MS2_FRAGMENT_SCANS.getElementName())) {
      Integer fragmentNumber = Integer.valueOf(getTextOfElement());
      currentAllMS2FragmentScans.add(fragmentNumber);
    }

    // <MASS>
    if (qName.equals(PeakListElementName_3_0_old.MZ.getElementName())) {

      byte[] bytes = Base64.decodeToBytes(getTextOfElement());
      // make a data input stream
      DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
      masses = new double[numOfMZpeaks];
      for (int i = 0; i < numOfMZpeaks; i++) {
        try {
          masses[i] = dataInputStream.readFloat();
        } catch (IOException ex) {
          throw new SAXException(ex);
        }
      }
    }

    // <HEIGHT>
    if (qName.equals(PeakListElementName_3_0_old.HEIGHT.getElementName())) {

      byte[] bytes = Base64.decodeToBytes(getTextOfElement());
      // make a data input stream
      DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
      intensities = new double[numOfMZpeaks];
      for (int i = 0; i < numOfMZpeaks; i++) {
        try {
          intensities[i] = dataInputStream.readFloat();
        } catch (IOException ex) {
          throw new SAXException(ex);
        }
      }
    }

    // <PEAK>
    if (qName.equals(PeakListElementName_3_0_old.PEAK.getElementName())) {

      DataPoint[] mzPeaks = new DataPoint[numOfMZpeaks];
      Range<Double> peakMZRange = null;
      Range<Float> peakRTRange = null, peakIntensityRange = null;
      RawDataFile dataFile = dataFilesIDMap.get(peakColumnID);

      if (dataFile == null) {
        throw new SAXException("Error in project: data file " + peakColumnID + " not found");
      }

      Scan[] scans = Arrays.stream(scanNumbers).map(s -> dataFile.getScanAtNumber(s))
          .toArray(Scan[]::new);
      List<Scan> fragmentScans = currentAllMS2FragmentScans.stream()
          .map(s -> dataFile.getScanAtNumber(s)).toList();
      Scan bestMS1 = dataFile.getScanAtNumber(representativeScan);
      Scan bestMS2 = dataFile.getScanAtNumber(fragmentScan);

      if (fragmentScans.isEmpty() && bestMS2 != null) {
        fragmentScans = List.of(bestMS2);
      }

      for (int i = 0; i < numOfMZpeaks; i++) {

        Scan sc = scans[i];
        float retentionTime = sc.getRetentionTime();

        double mz = masses[i];
        float intensity = (float) intensities[i];

        if (peakIntensityRange == null) {
          peakIntensityRange = Range.singleton(intensity);
        } else {
          peakIntensityRange = peakIntensityRange.span(Range.singleton(intensity));
        }
        if (intensity > 0) {
          if (peakRTRange == null) {
            peakRTRange = Range.singleton(retentionTime);
          } else {
            peakRTRange = peakRTRange.span(Range.singleton(retentionTime));
          }
        }

        if (mz > 0.0) {
          mzPeaks[i] = new SimpleDataPoint(mz, intensity);
          if (peakMZRange == null)
            peakMZRange = Range.singleton(mz);
          else
            peakMZRange = peakMZRange.span(Range.singleton(mz));
        }
      }

      FeatureStatus status = FeatureStatus.valueOf(peakStatus);

      // clear all MS2 fragment scan numbers list for next peak
      currentAllMS2FragmentScans.clear();

      // peakRTRange could be null if the peak consists only of 0 intensity data points
      if (peakRTRange == null)
        peakRTRange = Range.singleton(rt);

      // SimpleFeatureOld peak = new SimpleFeatureOld(dataFile, mass, rt, height, area, scanNumbers,
      // mzPeaks,
      // status, representativeScan, fragmentScan, allMS2FragmentScanNumbers, peakRTRange,
      // peakMZRange, peakIntensityRange);
      ModularFeature peak = new ModularFeature(buildingPeakList, dataFile, mass, rt, height, area,
          scans, mzPeaks, status, bestMS1, fragmentScans, peakRTRange, peakMZRange,
          peakIntensityRange);
      // SimpleFeatureOld peak = new SimpleFeatureOld(dataFile, mass, rt, height, area, scanNumbers,
      // mzPeaks,
      // status, representativeScan, fragmentScan, allMS2FragmentScanNumbers, peakRTRange,
      // peakMZRange, peakIntensityRange);

      peak.setCharge(currentPeakCharge);

      if (currentIsotopes.size() > 0) {
        SimpleIsotopePattern newPattern = new SimpleIsotopePattern(null, null, -1,
            currentIsotopePatternStatus, currentIsotopePatternDescription);
        peak.setIsotopePattern(newPattern);
        currentIsotopes.clear();
      }

      // TODO:
      // peak.setParentChromatogramRowID(parentChromatogramRowID);

      buildingRow.addFeature(dataFile, peak);

    }

    // <IDENTITY_PROPERTY>
    if (qName.equals(PeakListElementName_3_0_old.IDPROPERTY.getElementName())) {
      identityProperties.put(identityPropertyName, getTextOfElement());
    }

    // <INFO_PROPERTY>
    if (qName.equals(PeakListElementName_3_0_old.INFO_PROPERTY.getElementName())) {
      informationProperties.put(infoPropertyName, getTextOfElement());
    }

    // <PEAK_IDENTITY>
    if (qName.equals(PeakListElementName_3_0_old.PEAK_IDENTITY.getElementName())) {
      SimpleFeatureIdentity identity = new SimpleFeatureIdentity(identityProperties);
      buildingRow.addFeatureIdentity(identity, preferred);
    }

    if (qName.equals(PeakListElementName_3_0_old.PEAK_INFORMATION.getElementName())) {
      FeatureInformation information = new SimpleFeatureInformation(informationProperties);

      buildingRow.setFeatureInformation(information);
    }

    // <ROW>
    if (qName.equals(PeakListElementName_3_0_old.ROW.getElementName())) {
      buildingPeakList.addRow(buildingRow);
      buildingRow = null;
      parsedRows++;
    }

    // <ISOTOPE>
    if (qName.equals(PeakListElementName_3_0_old.ISOTOPE.getElementName())) {
      String text = getTextOfElement();
      String items[] = text.split(":");
      double mz = Double.valueOf(items[0]);
      double intensity = Double.valueOf(items[1]);
      DataPoint isotope = new SimpleDataPoint(mz, intensity);
      currentIsotopes.add(isotope);
    }

    if (qName.equals(PeakListElementName_3_0_old.METHOD_NAME.getElementName())) {
      String appliedMethod = getTextOfElement();
      appliedMethods.add(appliedMethod);
    }

    if (qName.equals(PeakListElementName_3_0_old.METHOD_PARAMETERS.getElementName())) {
      String appliedMethodParam = getTextOfElement();
      appliedMethodParameters.add(appliedMethodParam);
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
    charBuffer.setLength(0);
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

  /**
   * Initializes the feature list
   */
  private void initializePeakList() {

    RawDataFile[] dataFiles = currentPeakListDataFiles.toArray(new RawDataFile[0]);

    buildingPeakList = new ModularFeatureList(peakListName, flistStorage, dataFiles);

    // just add all columns that we used in MZmine 2
    // TODO create new method to save and load projects with modular data model
    DataTypeUtils.addDefaultChromatographicTypeColumns(buildingPeakList);

    for (int i = 0; i < appliedMethods.size(); i++) {
      String methodName = appliedMethods.elementAt(i);
      String methodParams = appliedMethodParameters.elementAt(i);
      /*SimpleFeatureListAppliedMethod pam =
          new SimpleFeatureListAppliedMethod(methodName, methodParams);
      buildingPeakList.addDescriptionOfAppliedTask(pam);*/
    }
    buildingPeakList.setDateCreated(dateCreated);
  }
}
