/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
