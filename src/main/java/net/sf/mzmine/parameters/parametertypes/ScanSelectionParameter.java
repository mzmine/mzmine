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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.parameters.parametertypes;

import java.util.Collection;

import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Range;

public class ScanSelectionParameter implements
        UserParameter<ScanSelection, ScanSelectionComponent> {

    private final String name, description;
    private ScanSelection value;

    public ScanSelectionParameter() {
        this("Scans", "Select scans that should be included.", null);
    }

    public ScanSelectionParameter(ScanSelection defaultValue) {
        this("Scans", "Select scans that should be included.", defaultValue);
    }

    public ScanSelectionParameter(String name, String description,
            ScanSelection defaultValue) {
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
        ScanSelectionParameter copy = new ScanSelectionParameter(name,
                description, value);
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
        Range<Double> scanRetentionTimeRange = null;
        PolarityType polarity = null;
        Integer msLevel = null;

        NodeList items = xmlElement.getElementsByTagName("scan_numbers");
        for (int i = 0; i < items.getLength(); i++) {
            if (items.item(i).getChildNodes().getLength() != 2)
                continue;
            String minText = items.item(i).getChildNodes().item(0)
                    .getTextContent();
            String maxText = items.item(i).getChildNodes().item(1)
                    .getTextContent();
            scanNumberRange = Range.closed(Integer.valueOf(minText),
                    Integer.valueOf(maxText));
        }

        items = xmlElement.getElementsByTagName("retention_time");
        for (int i = 0; i < items.getLength(); i++) {
            if (items.item(i).getChildNodes().getLength() != 2)
                continue;
            String minText = items.item(i).getChildNodes().item(0)
                    .getTextContent();
            String maxText = items.item(i).getChildNodes().item(1)
                    .getTextContent();
            scanRetentionTimeRange = Range.closed(Double.valueOf(minText),
                    Double.valueOf(maxText));
        }

        items = xmlElement.getElementsByTagName("ms_level");
        for (int i = 0; i < items.getLength(); i++) {
            msLevel = Integer.valueOf(items.item(i).getTextContent());
        }

        items = xmlElement.getElementsByTagName("polarity");
        for (int i = 0; i < items.getLength(); i++) {
            polarity = PolarityType.fromString(items.item(i).getTextContent());
        }

        this.value = new ScanSelection(scanNumberRange, scanRetentionTimeRange,
                polarity, msLevel);
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
        if (value == null)
            return;
        Document parentDocument = xmlElement.getOwnerDocument();

        final Range<Integer> scanNumberRange = value.getScanNumberRange();
        final Range<Double> scanRetentionTimeRange = value
                .getScanRetentionTimeRange();
        final PolarityType polarity = value.getPolarity();
        final Integer msLevel = value.getMsLevel();

        if (scanNumberRange != null) {
            Element scanNumElement = parentDocument
                    .createElement("scan_numbers");
            xmlElement.appendChild(scanNumElement);
            Element newElement = parentDocument.createElement("min");
            newElement.setTextContent(String.valueOf(scanNumberRange
                    .lowerEndpoint()));
            scanNumElement.appendChild(newElement);
            newElement = parentDocument.createElement("max");
            newElement.setTextContent(String.valueOf(scanNumberRange
                    .upperEndpoint()));
            scanNumElement.appendChild(newElement);
        }

        if (scanRetentionTimeRange != null) {
            Element scanRtElement = parentDocument
                    .createElement("retention_time");
            xmlElement.appendChild(scanRtElement);
            Element newElement = parentDocument.createElement("min");
            newElement.setTextContent(String.valueOf(scanRetentionTimeRange
                    .lowerEndpoint()));
            scanRtElement.appendChild(newElement);
            newElement = parentDocument.createElement("max");
            newElement.setTextContent(String.valueOf(scanRetentionTimeRange
                    .upperEndpoint()));
            scanRtElement.appendChild(newElement);
        }

        if (polarity != null) {
            Element newElement = parentDocument.createElement("polarity");
            newElement.setTextContent(polarity.toString());
            xmlElement.appendChild(newElement);
        }

        if (msLevel != null) {
            Element newElement = parentDocument.createElement("ms_level");
            newElement.setTextContent(String.valueOf(msLevel));
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
    public void setValueToComponent(ScanSelectionComponent component,
            ScanSelection newValue) {
        component.setValue(newValue);
    }

}
