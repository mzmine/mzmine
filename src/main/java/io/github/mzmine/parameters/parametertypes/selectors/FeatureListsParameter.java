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
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import java.util.ArrayList;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class FeatureListsParameter implements UserParameter<FeatureListsSelection, FeatureListsComponent> {

  private static final String DEFAULT_DESC = "Feature lists that this module will take as its input.";

  private String name = "Feature lists";
  private int minCount, maxCount;

  private @NotNull
  FeatureListsSelection value = new FeatureListsSelection();
  private final String description;

  public FeatureListsParameter() {
    this(1, Integer.MAX_VALUE);
  }

  public FeatureListsParameter(int minCount) {
    this(minCount, Integer.MAX_VALUE);
  }

  public FeatureListsParameter(int minCount, int maxCount) {
    this.minCount = minCount;
    this.maxCount = maxCount;
    description = DEFAULT_DESC;
  }

  public FeatureListsParameter(String name, int minCount, int maxCount) {
    this.name = name;
    this.minCount = minCount;
    this.maxCount = maxCount;
    description = DEFAULT_DESC;
  }

  public FeatureListsParameter(String name, String description, int minCount, int maxCount) {
    this.name = name;
    this.minCount = minCount;
    this.maxCount = maxCount;
    this.description = description;
  }

  @Override
  public FeatureListsSelection getValue() {
    return value;
  }

  @Override
  public void setValue(@NotNull FeatureListsSelection newValue) {
    this.value = newValue;
  }

  public void setValue(FeatureListsSelectionType selectionType, FeatureList peakLists[]) {
    if (value == null)
      value = new FeatureListsSelection();
    value.setSelectionType(selectionType);
    value.setSpecificFeatureLists(peakLists);
  }

  public void setValue(FeatureListsSelectionType selectionType) {
    if (value == null)
      value = new FeatureListsSelection();
    value.setSelectionType(selectionType);
  }

  @Override
  public FeatureListsParameter cloneParameter() {
    FeatureListsParameter copy = new FeatureListsParameter(name, minCount, maxCount);
    if (value != null)
      copy.value = value.clone();
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
    FeatureList matchingPeakLists[];
    if (value == null)
      matchingPeakLists = new FeatureList[0];
    else      matchingPeakLists = value.getMatchingFeatureLists();

    if (matchingPeakLists.length < minCount) {
      errorMessages.add("At least " + minCount + " feature lists  must be selected");
      return false;
    }
    if (matchingPeakLists.length > maxCount) {
      errorMessages.add("Maximum " + maxCount + " feature lists may be selected");
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

    FeatureList[] currentDataPeakLists = MZmineCore.getProjectManager().getCurrentProject()
        .getCurrentFeatureLists().toArray(FeatureList[]::new);

    FeatureListsSelectionType selectionType;
    final String attrValue = xmlElement.getAttribute("type");

    if (Strings.isNullOrEmpty(attrValue)) {
      selectionType = FeatureListsSelectionType.GUI_SELECTED_FEATURELISTS;
    } else {
      selectionType = FeatureListsSelectionType.valueOf(xmlElement.getAttribute("type"));
    }

    ArrayList<Object> newValues = new ArrayList<Object>();

    NodeList items = xmlElement.getElementsByTagName("specific_peak_list");
    for (int i = 0; i < items.getLength(); i++) {
      String itemString = items.item(i).getTextContent();
      for (FeatureList df : currentDataPeakLists) {
        if (df.getName().equals(itemString))
          newValues.add(df);
      }
    }
    FeatureList specificPeakLists[] = newValues.toArray(new FeatureList[0]);

    String namePattern = null;
    items = xmlElement.getElementsByTagName("name_pattern");
    for (int i = 0; i < items.getLength(); i++) {
      namePattern = items.item(i).getTextContent();
    }

    this.value = new FeatureListsSelection();
    this.value.setSelectionType(selectionType);
    this.value.setSpecificFeatureLists(specificPeakLists);
    this.value.setNamePattern(namePattern);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    Document parentDocument = xmlElement.getOwnerDocument();
    xmlElement.setAttribute("type", value.getSelectionType().name());

    if (value.getSpecificFeatureLists() != null) {
      for (FeatureList item : value.getSpecificFeatureLists()) {
        Element newElement = parentDocument.createElement("specific_peak_list");
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
  public FeatureListsComponent createEditingComponent() {
    return new FeatureListsComponent();
  }

  @Override
  public void setValueFromComponent(FeatureListsComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(FeatureListsComponent component, @Nullable FeatureListsSelection newValue) {
    component.setValue(newValue);
  }

}
