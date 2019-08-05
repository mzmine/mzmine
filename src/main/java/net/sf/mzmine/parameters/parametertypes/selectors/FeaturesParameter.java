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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;

public class FeaturesParameter
        implements UserParameter<List<FeatureSelection>, FeaturesComponent> {

    private String name = "Features";
    private List<FeatureSelection> value;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<FeatureSelection> getValue() {
        return value;
    }

    @Override
    public void setValue(List<FeatureSelection> newValue) {
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

        List<FeatureSelection> newValues = new ArrayList<FeatureSelection>();

        NodeList items = xmlElement.getElementsByTagName("feature");
        for (int i = 0; i < items.getLength(); i++) {
            Node doc = items.item(i);
            if (doc instanceof Element) {
                Element docElement = (Element) doc;
                for (PeakList peakList : allPeakLists) {
                    PeakListRow[] rows = peakList.getRows();
                    RawDataFile[] dataFiles = peakList.getRawDataFiles();
                    if (peakList.getName()
                            .equals(docElement
                                    .getElementsByTagName("peaklist_name")
                                    .item(0).getNodeValue())) {
                        for (PeakListRow row : rows) {
                            int rownum = 0;
                            if (row.toString().equals(docElement
                                    .getElementsByTagName("peaklist_row_id")
                                    .item(0).getNodeValue())) {
                                for (RawDataFile dataFile : dataFiles) {
                                    if (dataFile.getName()
                                            .equals(docElement
                                                    .getElementsByTagName(
                                                            "rawdatafile_name")
                                                    .item(0).getNodeValue())) {
                                        Feature feature = peakList
                                                .getPeak(rownum, dataFile);
                                        newValues.add(new FeatureSelection(
                                                peakList, feature, row,
                                                dataFile));
                                    }
                                }
                            }
                            rownum++;
                        }
                    }
                }
            }
        }
        this.value = new ArrayList<FeatureSelection>();
        this.value = newValues;
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
        if (value == null)
            return;
        Document parentDocument = xmlElement.getOwnerDocument();

        for (FeatureSelection item : value) {
            Element featureElement = parentDocument.createElement("feature");
            Document newDocument = featureElement.getOwnerDocument();
            Element peakListElement = newDocument
                    .createElement("peaklist_name");
            peakListElement.setNodeValue(item.getPeakList().getName());
            Element peakListRowElement = newDocument
                    .createElement("peaklist_row_id");
            peakListRowElement.setNodeValue(item.getPeakListRow().toString());
            Element rawDataFileElement = newDocument
                    .createElement("rawdatafile_name");
            rawDataFileElement.setNodeValue(item.getRawDataFile().getName());
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
            List<FeatureSelection> newValue) {
        component.setValue(newValue);
    }

    @Override
    public FeaturesParameter cloneParameter() {
        FeaturesParameter copy = new FeaturesParameter();
        for (FeatureSelection featureSelection : value) {
            FeatureSelection selection = featureSelection.clone();
            if (copy.value != null) {
                copy.value.add(selection);
            }
        }
        return copy;
    }

}
