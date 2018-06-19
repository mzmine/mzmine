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

package net.sf.mzmine.parameters.parametertypes.selectors;

import java.util.Collection;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.util.XMLUtils;

public class ScanSelectionParameter implements UserParameter<ScanSelection, ScanSelectionComponent> {
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
        Range<Double> scanRTRange = null;
        PolarityType polarity = null;
        MassSpectrumType spectrumType = null;
        Integer msLevel = null;
        String scanDefinition = null;

        scanNumberRange = XMLUtils.parseIntegerRange(xmlElement, "scan_numbers");
        scanRTRange = XMLUtils.parseDoubleRange(xmlElement, "retention_time");

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

        this.value = new ScanSelection(scanNumberRange, baseFilteringInteger, scanRTRange, polarity, spectrumType, msLevel,
                scanDefinition);
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
        if (value == null)
            return;
        Document parentDocument = xmlElement.getOwnerDocument();

        final Range<Integer> scanNumberRange = value.getScanNumberRange();
        final Range<Double> scanRetentionTimeRange = value.getScanRTRange();
        final Integer baseFilteringInteger = value.getBaseFilteringInteger();
        final PolarityType polarity = value.getPolarity();
        final MassSpectrumType spectrumType = value.getSpectrumType();
        final Integer msLevel = value.getMsLevel();
        final String scanDefinition = value.getScanDefinition();

        XMLUtils.appendRange(xmlElement, "scan_numbers", scanNumberRange);
        XMLUtils.appendRange(xmlElement, "retention_time", scanRetentionTimeRange);

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
