/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class FeatureSelectionParameter implements
    UserParameter<List<FeatureSelection>, FeatureSelectionComponent> {

  private final String name, description;
  private List<FeatureSelection> value;

  public FeatureSelectionParameter() {
    this("Peaks", "Select peaks that should be included.", null);
  }

  public FeatureSelectionParameter(String name, String description,
      List<FeatureSelection> defaultValue) {
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
    FeatureSelectionParameter copy = new FeatureSelectionParameter(name, description,
        value != null ? Lists.newArrayList(value) : null);
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
    if (value == null) {
      return;
    }
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
  public void setValueToComponent(FeatureSelectionComponent component,
      @Nullable List<FeatureSelection> newValue) {
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
    rows:
    for (FeatureListRow row : featureList.getRows()) {
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
