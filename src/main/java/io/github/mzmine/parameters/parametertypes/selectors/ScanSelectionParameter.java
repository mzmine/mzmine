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

package io.github.mzmine.parameters.parametertypes.selectors;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.XMLUtils;
import java.util.Collection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ScanSelectionParameter
    implements UserParameter<ScanSelection, ScanSelectionComponent> {
  private final String name, description;
  private ScanSelection value;

  public ScanSelectionParameter() {
    this("Scans", "Select scans that should be included.", null);
  }

  public ScanSelectionParameter(ScanSelection defaultValue) {
    this("Scans", "Select scans that should be included.", defaultValue);
  }

  public ScanSelectionParameter(String name, String description, ScanSelection defaultValue) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;
  }

  @Override
  public ScanSelection getValue() {
    return value;
  }

  @Override
  public void setValue(ScanSelection newValue) {
    this.value = newValue;
  }

  @Override
  public ScanSelectionParameter cloneParameter() {
    ScanSelectionParameter copy = new ScanSelectionParameter(name, description, value);
    return copy;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

    Range<Integer> scanNumberRange = null;
    Integer baseFilteringInteger = null;
    Range<Double> scanMobilityRange = null;
    Range<Float> scanRTRange = null;
    PolarityType polarity = null;
    MassSpectrumType spectrumType = null;
    Integer msLevel = null;
    String scanDefinition = null;

    scanNumberRange = XMLUtils.parseIntegerRange(xmlElement, "scan_numbers");
    scanMobilityRange = XMLUtils.parseDoubleRange(xmlElement, "mobility");
    scanRTRange = XMLUtils.parseFloatRange(xmlElement, "retention_time");

    NodeList items = xmlElement.getElementsByTagName("ms_level");
    for (int i = 0; i < items.getLength(); i++) {
      msLevel = Integer.valueOf(items.item(i).getTextContent());
    }

    items = xmlElement.getElementsByTagName("polarity");
    for (int i = 0; i < items.getLength(); i++) {
      try {
        polarity = PolarityType.valueOf(items.item(i).getTextContent());
      } catch (Exception e) {
        polarity = PolarityType.fromSingleChar(items.item(i).getTextContent());
      }
    }

    items = xmlElement.getElementsByTagName("spectrum_type");
    for (int i = 0; i < items.getLength(); i++) {
      spectrumType = MassSpectrumType.valueOf(items.item(i).getTextContent());
    }

    items = xmlElement.getElementsByTagName("scan_definition");
    for (int i = 0; i < items.getLength(); i++) {
      scanDefinition = items.item(i).getTextContent();
    }

    this.value = new ScanSelection(scanNumberRange, baseFilteringInteger, scanRTRange,
        scanMobilityRange, polarity, spectrumType, msLevel, scanDefinition);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    Document parentDocument = xmlElement.getOwnerDocument();

    final Range<Integer> scanNumberRange = value.getScanNumberRange();
    final Range<Float> scanRetentionTimeRange = value.getScanRTRange();
    final Range<Double> scanMobilityRange = value.getScanMobilityRange();
    final Integer baseFilteringInteger = value.getBaseFilteringInteger();
    final PolarityType polarity = value.getPolarity();
    final MassSpectrumType spectrumType = value.getSpectrumType();
    final Integer msLevel = value.getMsLevel();
    final String scanDefinition = value.getScanDefinition();

    XMLUtils.appendRange(xmlElement, "scan_numbers", scanNumberRange);
    XMLUtils.appendRange(xmlElement, "retention_time", scanRetentionTimeRange);
    XMLUtils.appendRange(xmlElement, "mobility", scanMobilityRange);

    if (baseFilteringInteger != null) {
      Element newElement = parentDocument.createElement("baseFilteringInteger");
      newElement.setTextContent(baseFilteringInteger.toString());
      xmlElement.appendChild(newElement);
    }
    if (polarity != null) {
      Element newElement = parentDocument.createElement("polarity");
      newElement.setTextContent(polarity.toString());
      xmlElement.appendChild(newElement);
    }

    if (spectrumType != null) {
      Element newElement = parentDocument.createElement("spectrum_type");
      newElement.setTextContent(spectrumType.toString());
      xmlElement.appendChild(newElement);
    }

    if (msLevel != null) {
      Element newElement = parentDocument.createElement("ms_level");
      newElement.setTextContent(String.valueOf(msLevel));
      xmlElement.appendChild(newElement);
    }

    if (scanDefinition != null) {
      Element newElement = parentDocument.createElement("scan_definition");
      newElement.setTextContent(scanDefinition);
      xmlElement.appendChild(newElement);
    }

  }

  @Override
  public ScanSelectionComponent createEditingComponent() {
    return new ScanSelectionComponent();
  }

  @Override
  public void setValueFromComponent(ScanSelectionComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(ScanSelectionComponent component, ScanSelection newValue) {
    component.setValue(newValue);
  }

}
