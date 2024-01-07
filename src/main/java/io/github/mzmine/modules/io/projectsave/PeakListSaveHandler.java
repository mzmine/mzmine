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

package io.github.mzmine.modules.io.projectsave;


import com.Ostermiller.util.Base64;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class PeakListSaveHandler {

  public static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  private Hashtable<RawDataFile, String> dataFilesIDMap;

  private int numberOfRows, finishedRows;
  private boolean canceled = false;

  private OutputStream finalStream;

  public PeakListSaveHandler(OutputStream finalStream,
      Hashtable<RawDataFile, String> dataFilesIDMap) {
    this.finalStream = finalStream;
    this.dataFilesIDMap = dataFilesIDMap;
  }

  /**
   * Create an XML document with the feature list information an save it into the project zip file
   *
   * @param featureList
   * @throws java.io.IOException
   */
  public void savePeakList(FeatureList featureList)
      throws IOException, TransformerConfigurationException, SAXException {

    numberOfRows = featureList.getNumberOfRows();
    finishedRows = 0;

    StreamResult streamResult = new StreamResult(finalStream);
    SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

    TransformerHandler hd = tf.newTransformerHandler();

    Transformer serializer = hd.getTransformer();
    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
    serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

    hd.setResult(streamResult);
    hd.startDocument();
    AttributesImpl atts = new AttributesImpl();

    hd.startElement("", "", PeakListElementName.PEAKLIST.getElementName(), atts);
    atts.clear();

    // <NAME>
    hd.startElement("", "", PeakListElementName.PEAKLIST_NAME.getElementName(), atts);
    hd.characters(featureList.getName().toCharArray(), 0, featureList.getName().length());
    hd.endElement("", "", PeakListElementName.PEAKLIST_NAME.getElementName());

    // <PEAKLIST_DATE>
    String dateText = "";
    if (featureList.getDateCreated() != null) {
      dateText = featureList.getDateCreated();
    } else {
      Date date = new Date();
      dateText = dateFormat.format(date);
    }
    hd.startElement("", "", PeakListElementName.PEAKLIST_DATE.getElementName(), atts);
    hd.characters(dateText.toCharArray(), 0, dateText.length());
    hd.endElement("", "", PeakListElementName.PEAKLIST_DATE.getElementName());

    // <QUANTITY>
    hd.startElement("", "", PeakListElementName.QUANTITY.getElementName(), atts);
    hd.characters(String.valueOf(numberOfRows).toCharArray(), 0,
        String.valueOf(numberOfRows).length());
    hd.endElement("", "", PeakListElementName.QUANTITY.getElementName());

    // <PROCESS>
    List<FeatureListAppliedMethod> processes = featureList.getAppliedMethods();
    for (FeatureListAppliedMethod proc : processes) {

      hd.startElement("", "", PeakListElementName.METHOD.getElementName(), atts);

      hd.startElement("", "", PeakListElementName.METHOD_NAME.getElementName(), atts);
      String methodName = proc.getDescription();
      hd.characters(methodName.toCharArray(), 0, methodName.length());
      hd.endElement("", "", PeakListElementName.METHOD_NAME.getElementName());

      /*hd.startElement("", "", PeakListElementName.METHOD_PARAMETERS.getElementName(), atts);
      String methodParameters = proc.getParameters();
      hd.characters(methodParameters.toCharArray(), 0, methodParameters.length());
      hd.endElement("", "", PeakListElementName.METHOD_PARAMETERS.getElementName());

      hd.endElement("", "", PeakListElementName.METHOD.getElementName());*/

    }
    atts.clear();

    // <RAWFILE>
    RawDataFile[] dataFiles = featureList.getRawDataFiles().toArray(RawDataFile[]::new);

    for (int i = 0; i < dataFiles.length; i++) {

      String ID = dataFilesIDMap.get(dataFiles[i]);

      hd.startElement("", "", PeakListElementName.RAWFILE.getElementName(), atts);
      char idChars[] = ID.toCharArray();
      hd.characters(idChars, 0, idChars.length);

      hd.endElement("", "", PeakListElementName.RAWFILE.getElementName());
    }

    // <ROW>
    FeatureListRow row;
    for (int i = 0; i < numberOfRows; i++) {

      if (canceled) {
        return;
      }

      atts.clear();
      row = featureList.getRow(i);
      atts.addAttribute("", "", PeakListElementName.ID.getElementName(), "CDATA",
          String.valueOf(row.getID()));
      if (row.getComment() != null) {
        atts.addAttribute("", "", PeakListElementName.COMMENT.getElementName(), "CDATA",
            row.getComment());
      }

      hd.startElement("", "", PeakListElementName.ROW.getElementName(), atts);
      fillRowElement(row, hd);
      hd.endElement("", "", PeakListElementName.ROW.getElementName());

      finishedRows++;
    }

    hd.endElement("", "", PeakListElementName.PEAKLIST.getElementName());
    hd.endDocument();
  }

  /**
   * Add the row information into the XML document
   *
   * @param row
   * @throws IOException
   */
  private void fillRowElement(FeatureListRow row, TransformerHandler hd)
      throws SAXException, IOException {

    // <PEAK_IDENTITY>
    FeatureIdentity preferredIdentity = row.getPreferredFeatureIdentity();
    List<FeatureIdentity> identities = row.getPeakIdentities();
    AttributesImpl atts = new AttributesImpl();

    for (int i = 0; i < identities.size(); i++) {

      if (canceled) {
        return;
      }

      atts.addAttribute("", "", PeakListElementName.ID.getElementName(), "CDATA",
          String.valueOf(i));
      atts.addAttribute("", "", PeakListElementName.PREFERRED.getElementName(), "CDATA",
          String.valueOf(identities.get(i) == preferredIdentity));
      hd.startElement("", "", PeakListElementName.PEAK_IDENTITY.getElementName(), atts);
      fillIdentityElement(identities.get(i), hd);
      hd.endElement("", "", PeakListElementName.PEAK_IDENTITY.getElementName());
    }

    // <PEAK_INFORMATION>

    // atts.clear();

    if (canceled) {
      return;
    }

    // atts.addAttribute("", "", PeakListElementName.ID.getElementName(),
    // "CDATA", "INFORMATION");
    hd.startElement("", "", PeakListElementName.PEAK_INFORMATION.getElementName(), atts);
    fillInformationElement(row.getFeatureInformation(), hd);
    hd.endElement("", "", PeakListElementName.PEAK_INFORMATION.getElementName());

    // <PEAK>
    for (Feature feature : row.getFeatures()) {
      if (canceled) {
        return;
      }

      atts.clear();
      String dataFileID = dataFilesIDMap.get(feature.getRawDataFile());
      atts.addAttribute("", "", PeakListElementName.COLUMN.getElementName(), "CDATA", dataFileID);
      atts.addAttribute("", "", PeakListElementName.MZ.getElementName(), "CDATA",
          String.valueOf(feature.getMZ()));
      // In the project file, retention time is represented in seconds,
      // for historical reasons
      double rt = feature.getRT() * 60d;
      atts.addAttribute("", "", PeakListElementName.RT.getElementName(), "CDATA",
          String.valueOf(rt));
      atts.addAttribute("", "", PeakListElementName.HEIGHT.getElementName(), "CDATA",
          String.valueOf(feature.getHeight()));
      atts.addAttribute("", "", PeakListElementName.AREA.getElementName(), "CDATA",
          String.valueOf(feature.getArea()));
      atts.addAttribute("", "", PeakListElementName.STATUS.getElementName(), "CDATA",
          feature.getFeatureStatus().toString());
      atts.addAttribute("", "", PeakListElementName.CHARGE.getElementName(), "CDATA",
          String.valueOf(feature.getCharge()));
      atts.addAttribute("", "", PeakListElementName.PARENT_CHROMATOGRAM_ROW_ID.getElementName(),
          "CDATA",
          feature.getParentChromatogramRowID() != null
              ? String.valueOf(feature.getParentChromatogramRowID())
              : "");
      hd.startElement("", "", PeakListElementName.PEAK.getElementName(), atts);

      fillPeakElement(feature, hd);
      hd.endElement("", "", PeakListElementName.PEAK.getElementName());
    }

  }

  /**
   * Add the peak identity information into the XML document
   *
   * @param identity
   */
  private void fillIdentityElement(FeatureIdentity identity, TransformerHandler hd)
      throws SAXException {

    AttributesImpl atts = new AttributesImpl();

    Map<String, String> idProperties = identity.getAllProperties();

    for (Entry<String, String> property : idProperties.entrySet()) {
      String propertyValue = property.getValue();
      atts.clear();
      atts.addAttribute("", "", PeakListElementName.NAME.getElementName(), "CDATA",
          property.getKey());

      hd.startElement("", "", PeakListElementName.IDPROPERTY.getElementName(), atts);
      hd.characters(propertyValue.toCharArray(), 0, propertyValue.length());
      hd.endElement("", "", PeakListElementName.IDPROPERTY.getElementName());
    }

  }

  private void fillInformationElement(FeatureInformation information, TransformerHandler hd)
      throws SAXException {
    if (information == null) {
      return;
    }

    AttributesImpl atts = new AttributesImpl();

    for (Entry<String, String> property : information.getAllProperties().entrySet()) {
      String value = property.getValue();

      atts.clear();
      atts.addAttribute("", "", PeakListElementName.NAME.getElementName(), "CDATA",
          property.getKey());

      hd.startElement("", "", PeakListElementName.INFO_PROPERTY.getElementName(), atts);
      hd.characters(property.getValue().toCharArray(), 0, value.length());
      hd.endElement("", "", PeakListElementName.INFO_PROPERTY.getElementName());
    }
  }

  /**
   * Add the peaks information into the XML document
   *
   * @param feature
   * @throws IOException
   */
  private void fillPeakElement(Feature feature, TransformerHandler hd)
      throws SAXException, IOException {
    AttributesImpl atts = new AttributesImpl();

    // <REPRESENTATIVE_SCAN>
    hd.startElement("", "", PeakListElementName.REPRESENTATIVE_SCAN.getElementName(), atts);
    hd.characters(String.valueOf(feature.getRepresentativeScan().getScanNumber()).toCharArray(), 0,
        String.valueOf(feature.getRepresentativeScan().getScanNumber()).length());
    hd.endElement("", "", PeakListElementName.REPRESENTATIVE_SCAN.getElementName());

    // <FRAGMENT_SCAN>
    hd.startElement("", "", PeakListElementName.FRAGMENT_SCAN.getElementName(), atts);
    hd.characters(
        String.valueOf(feature.getMostIntenseFragmentScan().getScanNumber()).toCharArray(), 0,
        String.valueOf(feature.getMostIntenseFragmentScan().getScanNumber()).length());
    hd.endElement("", "", PeakListElementName.FRAGMENT_SCAN.getElementName());

    // <ALL_MS2_FRAGMENT_SCANS>
    fillAllMS2FragmentScanNumbers(feature.getAllMS2FragmentScans().stream().map(Scan::getScanNumber)
        .collect(Collectors.toList()), hd);

    // <ISOTOPE_PATTERN>
    IsotopePattern isotopePattern = feature.getIsotopePattern();
    if (isotopePattern != null) {
      atts.addAttribute("", "", PeakListElementName.STATUS.getElementName(), "CDATA",
          String.valueOf(isotopePattern.getStatus()));
      atts.addAttribute("", "", PeakListElementName.DESCRIPTION.getElementName(), "CDATA",
          isotopePattern.getDescription());
      hd.startElement("", "", PeakListElementName.ISOTOPE_PATTERN.getElementName(), atts);
      atts.clear();

      fillIsotopePatternElement(isotopePattern, hd);

      hd.endElement("", "", PeakListElementName.ISOTOPE_PATTERN.getElementName());

    }

    // <MZPEAK>
    atts.addAttribute("", "", PeakListElementName.QUANTITY.getElementName(), "CDATA",
        String.valueOf(feature.getScanNumbers().size()));
    hd.startElement("", "", PeakListElementName.MZPEAKS.getElementName(), atts);
    atts.clear();

    // <SCAN_ID> <MASS> <HEIGHT>
    ByteArrayOutputStream byteScanStream = new ByteArrayOutputStream();
    DataOutputStream dataScanStream = new DataOutputStream(byteScanStream);

    ByteArrayOutputStream byteMassStream = new ByteArrayOutputStream();
    DataOutputStream dataMassStream = new DataOutputStream(byteMassStream);

    ByteArrayOutputStream byteHeightStream = new ByteArrayOutputStream();
    DataOutputStream dataHeightStream = new DataOutputStream(byteHeightStream);

    float mass, height;
    for (int i = 0; i < feature.getNumberOfDataPoints(); i++) {
      Scan scan = feature.getScanAtIndex(i);
      dataScanStream.writeInt(scan.getScanNumber());
      dataScanStream.flush();
      DataPoint mzPeak = feature.getDataPointAtIndex(i);
      if (mzPeak != null) {
        mass = (float) mzPeak.getMZ();
        height = (float) mzPeak.getIntensity();
      } else {
        mass = 0f;
        height = 0f;
      }
      dataMassStream.writeFloat(mass);
      dataMassStream.flush();
      dataHeightStream.writeFloat(height);
      dataHeightStream.flush();
    }

    byte[] bytes = Base64.encode(byteScanStream.toByteArray());
    hd.startElement("", "", PeakListElementName.SCAN_ID.getElementName(), atts);
    String sbytes = new String(bytes);
    hd.characters(sbytes.toCharArray(), 0, sbytes.length());
    hd.endElement("", "", PeakListElementName.SCAN_ID.getElementName());

    bytes = Base64.encode(byteMassStream.toByteArray());
    hd.startElement("", "", PeakListElementName.MZ.getElementName(), atts);
    sbytes = new String(bytes);
    hd.characters(sbytes.toCharArray(), 0, sbytes.length());
    hd.endElement("", "", PeakListElementName.MZ.getElementName());

    bytes = Base64.encode(byteHeightStream.toByteArray());
    hd.startElement("", "", PeakListElementName.HEIGHT.getElementName(), atts);
    sbytes = new String(bytes);
    hd.characters(sbytes.toCharArray(), 0, sbytes.length());
    hd.endElement("", "", PeakListElementName.HEIGHT.getElementName());

    hd.endElement("", "", PeakListElementName.MZPEAKS.getElementName());
  }

  private void fillIsotopePatternElement(IsotopePattern isotopePattern, TransformerHandler hd)
      throws SAXException, IOException {

    AttributesImpl atts = new AttributesImpl();

    DataPoint isotopes[] = new DataPoint[0]; // TODO isotopePattern.getDataPoints();

    for (DataPoint isotope : isotopes) {
      hd.startElement("", "", PeakListElementName.ISOTOPE.getElementName(), atts);
      String isotopeString = isotope.getMZ() + ":" + isotope.getIntensity();
      hd.characters(isotopeString.toCharArray(), 0, isotopeString.length());
      hd.endElement("", "", PeakListElementName.ISOTOPE.getElementName());
    }
  }

  private void fillAllMS2FragmentScanNumbers(List<Integer> scanNumbers, TransformerHandler hd)
      throws SAXException, IOException {
    AttributesImpl atts = new AttributesImpl();
    if (scanNumbers != null) {
      for (int scan : scanNumbers) {
        hd.startElement("", "", PeakListElementName.ALL_MS2_FRAGMENT_SCANS.getElementName(), atts);
        hd.characters(String.valueOf(scan).toCharArray(), 0, String.valueOf(scan).length());
        hd.endElement("", "", PeakListElementName.ALL_MS2_FRAGMENT_SCANS.getElementName());
      }
    }
  }

  /**
   * @return the progress of these functions saving the feature list to the zip file.
   */
  public double getProgress() {
    if (numberOfRows == 0) {
      return 0;
    }
    return (double) finishedRows / numberOfRows;
  }

  public void cancel() {
    canceled = true;
  }

}
