/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package net.sf.mzmine.parameters.parametertypes.selectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;

public class FeaturesParameter
        implements UserParameter<List<Feature>, FeaturesComponent> {

    private String name = "Features";
    private List<Feature> value;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Feature> getValue() {
        return value;
    }

    @Override
    public void setValue(List<Feature> newValue) {
        this.value = newValue;
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
        if ((value == null) || (value.size() == 0)) {
            errorMessages.add("No features selected");
            return false;
        }
        return true;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
        PeakList[] allPeakLists = MZmineCore.getProjectManager()
                .getCurrentProject().getPeakLists();

        ArrayList<Object> newValues = new ArrayList<Object>();

        NodeList items = xmlElement.getElementsByTagName("feature");
        for (int i = 0; i < items.getLength(); i++) {
            NodeList children = items.item(i).getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                String str = children.item(j).getNodeValue();
            }
        }

    }

    @Override
    public void saveValueToXML(Element xmlElement) {
        if (value == null)
            return;
        Document parentDocument = xmlElement.getOwnerDocument();

        for (Feature item : value) {
            Element featureElement = parentDocument.createElement("feature");
            Document newDocument = featureElement.getOwnerDocument();
            Element peakListElement = newDocument
                    .createElement("peaklist_name");
            // item.?togetthepeaklistname
            Element peakListRowElement = newDocument
                    .createElement("peaklist_row_id");
            peakListRowElement.setNodeValue(item.toString());
            Element rawDataFileElement = newDocument
                    .createElement("rawdatafile_name");
            rawDataFileElement.setNodeValue(item.getDataFile().toString());
            featureElement.appendChild(peakListElement);
            featureElement.appendChild(peakListRowElement);
            featureElement.appendChild(rawDataFileElement);
            xmlElement.appendChild(featureElement);
        }

    }

    @Override
    public String getDescription() {
        return "Features that this module will take as its input.";
    }

    @Override
    public FeaturesComponent createEditingComponent() {
        FeaturesComponent featuresComponent = new FeaturesComponent();
        return featuresComponent;
    }

    @Override
    public void setValueFromComponent(FeaturesComponent component) {
        value = component.getValue();
    }

    @Override
    public void setValueToComponent(FeaturesComponent component,
            List<Feature> newValue) {
        component.setValue(newValue);
    }

    @Override
    public UserParameter<List<Feature>, FeaturesComponent> cloneParameter() {
        return null;
    }

}
