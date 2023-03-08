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

package io.github.mzmine.parameters.parametertypes.mztochargeparameter;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;
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
      @Nullable RangeMap<Double, Integer> newValue) {
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
