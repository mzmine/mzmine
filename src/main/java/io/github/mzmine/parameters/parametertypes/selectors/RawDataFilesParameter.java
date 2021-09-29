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

package io.github.mzmine.parameters.parametertypes.selectors;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class RawDataFilesParameter implements
    UserParameter<RawDataFilesSelection, RawDataFilesComponent> {

  private static final String PATH_ATTRIBUTE = "path";
  private String name = "Raw data files";
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

  public RawDataFilesParameter(String name, int minCount, int maxCount) {
    this.name = name;
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
    if (value == null) {
      value = new RawDataFilesSelection();
    }
    value.setSelectionType(selectionType);
  }

  public void setValue(RawDataFilesSelectionType selectionType, RawDataFile dataFiles[]) {
    if (value == null) {
      value = new RawDataFilesSelection();
    }
    value.setSelectionType(selectionType);
    value.setSpecificFiles(dataFiles);
  }

  @Override
  public RawDataFilesParameter cloneParameter() {
    RawDataFilesParameter copy = new RawDataFilesParameter(name, minCount, maxCount);
    if (value != null) {
      copy.value = value.clone();
    }
    return copy;
  }

  public RawDataFilesParameter cloneParameter(boolean keepSelection) {
    RawDataFilesParameter copy = new RawDataFilesParameter(name, minCount, maxCount);
    if (value != null) {
      copy.value = keepSelection ? value.cloneAndKeepSelection() : value.clone();
    }
    return copy;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return "Raw data files that this module will take as its input.";
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    RawDataFile matchingFiles[];
    if (value == null) {
      matchingFiles = new RawDataFile[0];
    } else {
      matchingFiles = value.getMatchingRawDataFiles();
    }
    if (value != null) {
      value.resetSelection(); // has to be reset after evaluation
    }

    if (matchingFiles.length < minCount) {
      errorMessages.add("At least " + minCount + " raw data files must be selected");
      return false;
    }
    if (matchingFiles.length > maxCount) {
      errorMessages.add("Maximum " + maxCount + " raw data files may be selected");
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

    RawDataFile[] currentDataFiles = MZmineCore.getProjectManager().getCurrentProject()
        .getDataFiles();

    RawDataFilesSelectionType selectionType;
    final String attrValue = xmlElement.getAttribute("type");

    if (Strings.isNullOrEmpty(attrValue)) {
      selectionType = RawDataFilesSelectionType.GUI_SELECTED_FILES;
    } else {
      selectionType = RawDataFilesSelectionType.valueOf(xmlElement.getAttribute("type"));
    }

    List<Object> newValues = new ArrayList<>();

    NodeList items = xmlElement.getElementsByTagName("specific_file");
    for (int i = 0; i < items.getLength(); i++) {
      String itemString = items.item(i).getTextContent();
      String path = ((Element) items.item(i)).getAttribute(PATH_ATTRIBUTE);
      path = path.isEmpty() ? null : path;
      newValues.add(new RawDataFilePlaceholder(itemString, path));
    }
    RawDataFilePlaceholder specificFiles[] = newValues.toArray(new RawDataFilePlaceholder[0]);

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
    if (value == null) {
      return;
    }
    Document parentDocument = xmlElement.getOwnerDocument();
    xmlElement.setAttribute("type", value.getSelectionType().name());

    if (value.getSelectionType() == RawDataFilesSelectionType.SPECIFIC_FILES
        && value.getSpecificFiles() != null) {
      for (RawDataFile item : value.getSpecificFilesPlaceholders()) {
        assert item != null;
        Element newElement = parentDocument.createElement("specific_file");
        String fileName = item.getName();
        newElement.setTextContent(fileName);
        newElement.setAttribute(PATH_ATTRIBUTE, item.getAbsolutePath());
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
  public void setValueToComponent(RawDataFilesComponent component, RawDataFilesSelection newValue) {
    component.setValue(newValue);
  }

}
