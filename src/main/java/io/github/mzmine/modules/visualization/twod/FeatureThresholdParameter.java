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

package io.github.mzmine.modules.visualization.twod;

import java.util.Collection;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.github.mzmine.parameters.Parameter;

public class FeatureThresholdParameter implements Parameter<Object> {

  private FeatureThresholdMode mode = FeatureThresholdMode.ALL_FEATURES;
  private double intensityThreshold;
  private int topFeaturesThreshold;

  @Override
  public String getName() {
    return "Feature threshold settings";
  }

  public FeatureThresholdMode getMode() {
    return mode;
  }

  public void setMode(FeatureThresholdMode mode) {
    this.mode = mode;
  }

  public double getIntensityThreshold() {
    return intensityThreshold;
  }

  public void setIntensityThreshold(double intensityThreshold) {
    this.intensityThreshold = intensityThreshold;
  }

  public int getTopFeaturesThreshold() {
    return topFeaturesThreshold;
  }

  public void setTopFeaturesThreshold(int topFeaturesThreshold) {
    this.topFeaturesThreshold = topFeaturesThreshold;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList nodes = xmlElement.getElementsByTagName("mode");
    if (nodes.getLength() != 1)
      return;
    String content = nodes.item(0).getTextContent();
    mode = FeatureThresholdMode.valueOf(content);

    nodes = xmlElement.getElementsByTagName("intensityThreshold");
    if (nodes.getLength() != 1)
      return;
    content = nodes.item(0).getTextContent();
    intensityThreshold = Double.valueOf(content);

    nodes = xmlElement.getElementsByTagName("topFeaturesThreshold");
    if (nodes.getLength() != 1)
      return;
    content = nodes.item(0).getTextContent();
    topFeaturesThreshold = Integer.valueOf(content);

  }

  @Override
  public void saveValueToXML(Element xmlElement) {

    Document parentDocument = xmlElement.getOwnerDocument();

    Element newElement = parentDocument.createElement("mode");
    newElement.setTextContent(mode.name());
    xmlElement.appendChild(newElement);

    newElement = parentDocument.createElement("intensityThreshold");
    newElement.setTextContent(String.valueOf(intensityThreshold));
    xmlElement.appendChild(newElement);

    newElement = parentDocument.createElement("topFeaturesThreshold");
    newElement.setTextContent(String.valueOf(topFeaturesThreshold));
    xmlElement.appendChild(newElement);

  }

  @Override
  public FeatureThresholdParameter cloneParameter() {
    return this;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

  @Override
  public Object getValue() {
    return null;
  }

  @Override
  public void setValue(Object newValue) {}

}
