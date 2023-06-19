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

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class RawDataFilesParameter implements
    UserParameter<RawDataFilesSelection, RawDataFilesComponent> {

  private static final String PATH_ATTRIBUTE = "path";
  private String name = "Raw data files";
  private final int minCount;
  private final int maxCount;

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

  public void setValue(RawDataFilesSelectionType selectionType, RawDataFile[] dataFiles) {
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
    RawDataFile[] matchingFiles;
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
    RawDataFilePlaceholder[] specificFiles = newValues.toArray(new RawDataFilePlaceholder[0]);

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
  public void setValueToComponent(RawDataFilesComponent component, @Nullable RawDataFilesSelection newValue) {
    component.setValue(newValue);
  }

}
