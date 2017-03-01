/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.msms;

import java.util.Collection;

import net.sf.mzmine.parameters.Parameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PeakThresholdParameter implements Parameter<Object> {

    private PeakThresholdMode mode = PeakThresholdMode.ALL_PEAKS;
    private double intensityThreshold;
    private int topPeaksThreshold;

    @Override
    public String getName() {
	return "Peak threshold settings";
    }

    public PeakThresholdMode getMode() {
	return mode;
    }

    public void setMode(PeakThresholdMode mode) {
	this.mode = mode;
    }

    public double getIntensityThreshold() {
	return intensityThreshold;
    }

    public void setIntensityThreshold(double intensityThreshold) {
	this.intensityThreshold = intensityThreshold;
    }

    public int getTopPeaksThreshold() {
	return topPeaksThreshold;
    }

    public void setTopPeaksThreshold(int topPeaksThreshold) {
	this.topPeaksThreshold = topPeaksThreshold;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
	NodeList nodes = xmlElement.getElementsByTagName("mode");
	if (nodes.getLength() != 1)
	    return;
	String content = nodes.item(0).getTextContent();
	mode = PeakThresholdMode.valueOf(content);

	nodes = xmlElement.getElementsByTagName("intensityThreshold");
	if (nodes.getLength() != 1)
	    return;
	content = nodes.item(0).getTextContent();
	intensityThreshold = Double.valueOf(content);

	nodes = xmlElement.getElementsByTagName("topPeaksThreshold");
	if (nodes.getLength() != 1)
	    return;
	content = nodes.item(0).getTextContent();
	topPeaksThreshold = Integer.valueOf(content);

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

	newElement = parentDocument.createElement("topPeaksThreshold");
	newElement.setTextContent(String.valueOf(topPeaksThreshold));
	xmlElement.appendChild(newElement);

    }

    @Override
    public PeakThresholdParameter cloneParameter() {
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
    public void setValue(Object newValue) {
    }

}
