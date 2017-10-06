/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.base.Strings;

public class PeakListsParameter implements UserParameter<PeakListsSelection, PeakListsComponent> {

  private String name = "Peak lists";
  private int minCount, maxCount;

  private PeakListsSelection value;

  public PeakListsParameter() {
    this(1, Integer.MAX_VALUE);
  }

  public PeakListsParameter(int minCount) {
    this(minCount, Integer.MAX_VALUE);
  }

  public PeakListsParameter(int minCount, int maxCount) {
    this.minCount = minCount;
    this.maxCount = maxCount;
  }
  
  public PeakListsParameter(String name, int minCount, int maxCount) {
    this.name = name;
    this.minCount = minCount;
    this.maxCount = maxCount;
  }

  @Override
  public PeakListsSelection getValue() {
    return value;
  }

  @Override
  public void setValue(PeakListsSelection newValue) {
    this.value = newValue;
  }

  public void setValue(PeakListsSelectionType selectionType, PeakList peakLists[]) {
    if (value == null)
      value = new PeakListsSelection();
    value.setSelectionType(selectionType);
    value.setSpecificPeakLists(peakLists);
  }

  public void setValue(PeakListsSelectionType selectionType) {
    if (value == null)
      value = new PeakListsSelection();
    value.setSelectionType(selectionType);
  }

  @Override
  public PeakListsParameter cloneParameter() {
    PeakListsParameter copy = new PeakListsParameter(name, minCount, maxCount);
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
    return "Peak lists that this module will take as its input.";
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    PeakList matchingPeakLists[];
    if (value == null)
      matchingPeakLists = new PeakList[0];
    else
      matchingPeakLists = value.getMatchingPeakLists();

    if (matchingPeakLists.length < minCount) {
      errorMessages.add("At least " + minCount + " peak lists  must be selected");
      return false;
    }
    if (matchingPeakLists.length > maxCount) {
      errorMessages.add("Maximum " + maxCount + " peak lists may be selected");
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

    PeakList[] currentDataPeakLists =
        MZmineCore.getProjectManager().getCurrentProject().getPeakLists();

    PeakListsSelectionType selectionType;
    final String attrValue = xmlElement.getAttribute("type");

    if (Strings.isNullOrEmpty(attrValue))
      selectionType = PeakListsSelectionType.GUI_SELECTED_PEAKLISTS;
    else
      selectionType = PeakListsSelectionType.valueOf(xmlElement.getAttribute("type"));

    ArrayList<Object> newValues = new ArrayList<Object>();

    NodeList items = xmlElement.getElementsByTagName("specific_peak_list");
    for (int i = 0; i < items.getLength(); i++) {
      String itemString = items.item(i).getTextContent();
      for (PeakList df : currentDataPeakLists) {
        if (df.getName().equals(itemString))
          newValues.add(df);
      }
    }
    PeakList specificPeakLists[] = newValues.toArray(new PeakList[0]);

    String namePattern = null;
    items = xmlElement.getElementsByTagName("name_pattern");
    for (int i = 0; i < items.getLength(); i++) {
      namePattern = items.item(i).getTextContent();
    }

    this.value = new PeakListsSelection();
    this.value.setSelectionType(selectionType);
    this.value.setSpecificPeakLists(specificPeakLists);
    this.value.setNamePattern(namePattern);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    Document parentDocument = xmlElement.getOwnerDocument();
    xmlElement.setAttribute("type", value.getSelectionType().name());

    if (value.getSpecificPeakLists() != null) {
      for (PeakList item : value.getSpecificPeakLists()) {
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
  public PeakListsComponent createEditingComponent() {
    return new PeakListsComponent();
  }

  @Override
  public void setValueFromComponent(PeakListsComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(PeakListsComponent component, PeakListsSelection newValue) {
    component.setValue(newValue);
  }

}
