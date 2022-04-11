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

package io.github.mzmine.parameters.parametertypes.mztochargeparameter;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import java.util.Map.Entry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Allows selection of default charges for a given mz range in case no charge could be determined.
 */
public class MzToChargeParameter implements
    UserParameter<RangeMap<Double, Integer>, MzToChargeComponent> {

  public static final String RANGE_ELEMENT = "range";
  public static final String RANGE_LOWER = "lower";
  public static final String RANGE_UPPER = "upper";
  public static final String CHARGE = "charge";

  private final String name;
  private final String description;
  private RangeMap<Double, Integer> value;

  public MzToChargeParameter(String name, String description) {
    this.name = name;
    this.description = description;
    this.value = TreeRangeMap.create();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public RangeMap<Double, Integer> getValue() {
    return value;
  }

  @Override
  public void setValue(RangeMap<Double, Integer> newValue) {
    this.value = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    for (Range<Double> range1 : value.asMapOfRanges().keySet()) {
      for (Range<Double> range2 : value.asMapOfRanges().keySet()) {
        if (range1 == range2) {
          continue;
        }
        if (range1.isConnected(range2)) {
          errorMessages.add(
              "Overlaps are not allowed.\nRanges " + range1.toString() + " and " + range2.toString()
                  + " overlap.");
        }
      }
    }
    return errorMessages.isEmpty();
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    NodeList ranges = xmlElement.getElementsByTagName(RANGE_ELEMENT);

    RangeMap<Double, Integer> newValue = TreeRangeMap.create();

    for (int i = 0; i < ranges.getLength(); i++) {
      Element rangeElement = (Element) ranges.item(i);
      double lower = Double.parseDouble(rangeElement.getAttribute(RANGE_LOWER));
      double upper = Double.parseDouble(rangeElement.getAttribute(RANGE_UPPER));
      int charge = Integer.parseInt(rangeElement.getAttribute(CHARGE));

      newValue.put(Range.closed(lower, upper), charge);
    }
    this.value = newValue;
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    Document doc = xmlElement.getOwnerDocument();

    for (Entry<Range<Double>, Integer> rangeCharge : value.asMapOfRanges().entrySet()) {
      Element rangeElement = doc.createElement(RANGE_ELEMENT);
      rangeElement.setAttribute(RANGE_LOWER, rangeCharge.getKey().lowerEndpoint().toString());
      rangeElement.setAttribute(RANGE_UPPER, rangeCharge.getKey().upperEndpoint().toString());
      rangeElement.setAttribute(CHARGE, rangeCharge.getValue().toString());
      xmlElement.appendChild(rangeElement);
    }
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public MzToChargeComponent createEditingComponent() {
    return new MzToChargeComponent();
  }

  @Override
  public void setValueFromComponent(MzToChargeComponent mzToChargeComponent) {
    this.value = mzToChargeComponent.getValue();
  }

  @Override
  public void setValueToComponent(MzToChargeComponent mzToChargeComponent,
      RangeMap<Double, Integer> newValue) {
    mzToChargeComponent.setValue(newValue);
  }

  @Override
  public UserParameter<RangeMap<Double, Integer>, MzToChargeComponent> cloneParameter() {

    MzToChargeParameter clone = new MzToChargeParameter(name, description);
    TreeRangeMap<Double, Integer> newValue = TreeRangeMap.create();

    value.asMapOfRanges().entrySet().forEach(e -> newValue
        .put(Range.closed(e.getKey().lowerEndpoint(), e.getKey().upperEndpoint()), e.getValue()));
    clone.setValue(newValue);

    return clone;
  }
}
