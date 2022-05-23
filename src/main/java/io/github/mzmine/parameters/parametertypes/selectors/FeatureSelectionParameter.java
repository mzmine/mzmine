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

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.XMLUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class FeatureSelectionParameter
    implements UserParameter<List<FeatureSelection>, FeatureSelectionComponent> {

  private final String name, description;
  private List<FeatureSelection> value;

  public FeatureSelectionParameter() {
    this("Peaks", "Select peaks that should be included.", null);
  }

  public FeatureSelectionParameter(String name, String description, List<FeatureSelection> defaultValue) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;
  }

  @Override
  public List<FeatureSelection> getValue() {
    return value;
  }

  @Override
  public void setValue(List<FeatureSelection> newValue) {
    this.value = Lists.newArrayList(newValue);
  }

  @Override
  public FeatureSelectionParameter cloneParameter() {
    FeatureSelectionParameter copy =
        new FeatureSelectionParameter(name, description, Lists.newArrayList(value));
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
    if ((value == null) || (value.size() == 0)) {
      errorMessages.add("No peaks selected");
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

    List<FeatureSelection> newValue = Lists.newArrayList();
    NodeList selItems = xmlElement.getElementsByTagName("selection");
    for (int i = 0; i < selItems.getLength(); i++) {
      Element selElement = (Element) selItems.item(i);
      Range<Integer> idRange = XMLUtils.parseIntegerRange(selElement, "id");
      Range<Double> mzRange = XMLUtils.parseDoubleRange(selElement, "mz");
      final Range<Double> rtDoubleRange = XMLUtils.parseDoubleRange(selElement, "rt");
      Range<Float> rtRange = rtDoubleRange != null ? RangeUtils.toFloatRange(rtDoubleRange) : null;
      String name = XMLUtils.parseString(selElement, "name");
      FeatureSelection ps = new FeatureSelection(idRange, mzRange, rtRange, name);
      newValue.add(ps);
    }
    this.value = newValue;
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    Document parentDocument = xmlElement.getOwnerDocument();

    for (FeatureSelection ps : value) {
      Element selElement = parentDocument.createElement("selection");
      xmlElement.appendChild(selElement);
      XMLUtils.appendRange(selElement, "id", ps.getIDRange());
      XMLUtils.appendRange(selElement, "mz", ps.getMZRange());
      XMLUtils.appendRange(selElement, "rt", ps.getRTRange());
      XMLUtils.appendString(selElement, "name", ps.getName());
    }

  }

  @Override
  public FeatureSelectionComponent createEditingComponent() {
    return new FeatureSelectionComponent();
  }

  @Override
  public void setValueFromComponent(FeatureSelectionComponent component) {
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(FeatureSelectionComponent component, List<FeatureSelection> newValue) {
    component.setValue(newValue);
  }

  /**
   * Shortcut to set value based on feature list rows
   */
  public void setValue(FeatureListRow rows[]) {
    List<FeatureSelection> newValue = Lists.newArrayList();
    for (FeatureListRow row : rows) {
      Range<Integer> idRange = Range.singleton(row.getID());
      Range<Double> mzRange = Range.singleton(row.getAverageMZ());
      Range<Float> rtRange = Range.singleton(row.getAverageRT());
      FeatureSelection ps = new FeatureSelection(idRange, mzRange, rtRange, null);
      newValue.add(ps);
    }
    setValue(newValue);
  }

  public FeatureListRow[] getMatchingRows(FeatureList featureList) {

    final List<FeatureListRow> matchingRows = new ArrayList<>();
    rows: for (FeatureListRow row : featureList.getRows()) {
      for (FeatureSelection ps : value) {
        if (ps.checkPeakListRow(row)) {
          matchingRows.add(row);
          continue rows;
        }
      }
    }
    return matchingRows.toArray(new FeatureListRow[matchingRows.size()]);

  }

}
