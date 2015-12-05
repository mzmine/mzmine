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

import java.util.ArrayList;
import java.util.Collection;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.base.Strings;

public class RawDataFilesParameter
        implements UserParameter<RawDataFilesSelection, RawDataFilesComponent> {

    private int minCount, maxCount;

    private RawDataFilesSelection value;

    public RawDataFilesParameter() {
        this(1, Integer.MAX_VALUE);
    }

    public RawDataFilesParameter(RawDataFilesSelection value) {
        this(1, Integer.MAX_VALUE);
        this.value = value;
    }

    public RawDataFilesParameter(int minCount) {
        this(minCount, Integer.MAX_VALUE);
    }

    public RawDataFilesParameter(int minCount, int maxCount) {
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    @Override
    public RawDataFilesSelection getValue() {
        return value;
    }

    @Override
    public void setValue(RawDataFilesSelection newValue) {
        this.value = newValue;
    }

    public void setValue(RawDataFilesSelectionType selectionType) {
        if (value == null)
            value = new RawDataFilesSelection();
        value.setSelectionType(selectionType);
    }

    public void setValue(RawDataFilesSelectionType selectionType,
            RawDataFile dataFiles[]) {
        if (value == null)
            value = new RawDataFilesSelection();
        value.setSelectionType(selectionType);
        value.setSpecificFiles(dataFiles);
    }

    @Override
    public RawDataFilesParameter cloneParameter() {
        RawDataFilesParameter copy = new RawDataFilesParameter(minCount,
                maxCount);
        if (value != null)
            copy.value = value.clone();
        return copy;
    }

    @Override
    public String getName() {
        return "Raw data files";
    }

    @Override
    public String getDescription() {
        return "Raw data files that this module will take as its input.";
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
        RawDataFile matchingFiles[];
        if (value == null)
            matchingFiles = new RawDataFile[0];
        else
            matchingFiles = value.getMatchingRawDataFiles();

        if (matchingFiles.length < minCount) {
            errorMessages.add("At least " + minCount
                    + " raw data files must be selected");
            return false;
        }
        if (matchingFiles.length > maxCount) {
            errorMessages.add(
                    "Maximum " + maxCount + " raw data files may be selected");
            return false;
        }
        return true;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {

        RawDataFile[] currentDataFiles = MZmineCore.getProjectManager()
                .getCurrentProject().getDataFiles();

        RawDataFilesSelectionType selectionType;
        final String attrValue = xmlElement.getAttribute("type");

        if (Strings.isNullOrEmpty(attrValue))
            selectionType = RawDataFilesSelectionType.GUI_SELECTED_FILES;
        else
            selectionType = RawDataFilesSelectionType
                    .valueOf(xmlElement.getAttribute("type"));

        ArrayList<Object> newValues = new ArrayList<Object>();

        NodeList items = xmlElement.getElementsByTagName("specific_file");
        for (int i = 0; i < items.getLength(); i++) {
            String itemString = items.item(i).getTextContent();
            for (RawDataFile df : currentDataFiles) {
                if (df.getName().equals(itemString))
                    newValues.add(df);
            }
        }
        RawDataFile specificFiles[] = newValues.toArray(new RawDataFile[0]);

        String namePattern = null;
        items = xmlElement.getElementsByTagName("name_pattern");
        for (int i = 0; i < items.getLength(); i++) {
            namePattern = items.item(i).getTextContent();
        }

        this.value = new RawDataFilesSelection();
        this.value.setSelectionType(selectionType);
        this.value.setSpecificFiles(specificFiles);
        this.value.setNamePattern(namePattern);
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
        if (value == null)
            return;
        Document parentDocument = xmlElement.getOwnerDocument();
        xmlElement.setAttribute("type", value.getSelectionType().name());

        if (value.getSpecificFiles() != null) {
            for (RawDataFile item : value.getSpecificFiles()) {
                Element newElement = parentDocument
                        .createElement("specific_file");
                newElement.setTextContent(item.getName());
                xmlElement.appendChild(newElement);
            }
        }

        if (value.getNamePattern() != null) {
            Element newElement = parentDocument.createElement("name_pattern");
            newElement.setTextContent(value.getNamePattern());
            xmlElement.appendChild(newElement);
        }

    }

    @Override
    public RawDataFilesComponent createEditingComponent() {
        return new RawDataFilesComponent();
    }

    @Override
    public void setValueFromComponent(RawDataFilesComponent component) {
        value = component.getValue();
    }

    @Override
    public void setValueToComponent(RawDataFilesComponent component,
            RawDataFilesSelection newValue) {
        component.setValue(newValue);
    }

}
