/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.parametertypes.rangeortolerancetable;

import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.util.ParsingUtils;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @param <T>
 * @param <ToleranceParam>
 * @param <ToleranceComponent>
 */
public abstract class RangesOrToleranceParameter<T extends Number & Comparable<T>, //
    TolType extends Tolerance<T>, //
    ToleranceParam extends UserParameter<TolType, ToleranceComponent>, //
    ToleranceComponent extends Node> implements //
    UserParameter<RangeOrValueResult<T>, RangesOrToleranceComponent<T, ToleranceComponent>> {

  protected final String name;
  protected final String description;
  protected final ToleranceParam toleranceParameter;
  protected final NumberFormat numberFormat;
  protected final String unit;
  protected @NotNull RangeOrValueResult<T> value;

  public RangesOrToleranceParameter(String name, String description, String unit,
      NumberFormat format, @NotNull ToleranceParam toleranceParameter) {
    this.name = name;
    this.description = description;
    this.toleranceParameter = toleranceParameter;
    this.unit = unit;
    this.numberFormat = format;
    value = new RangeOrValueResult<>(List.of(), toleranceParameter.getValue());
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setValueFromComponent(
      RangesOrToleranceComponent<T, ToleranceComponent> rangesOrToleranceComponent) {
    toleranceParameter.setValueFromComponent(rangesOrToleranceComponent.toleranceComponent);
    value = rangesOrToleranceComponent.getValue();
  }

  @Override
  public void setValueToComponent(
      RangesOrToleranceComponent<T, ToleranceComponent> rangesOrToleranceComponent,
      @Nullable RangeOrValueResult<T> newValue) {
    rangesOrToleranceComponent.setValue(newValue);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public @NotNull RangeOrValueResult<T> getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable RangeOrValueResult<T> newValue) {
    if (newValue != null) {
      this.value = newValue;
      toleranceParameter.setValue((TolType) this.value.tolerance());
    } else {
      // keep the tolerance if new value is null
      this.value = new RangeOrValueResult<>(List.of(), this.value.tolerance());
      toleranceParameter.setValue((TolType) value.tolerance());
    }
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    boolean failed = !toleranceParameter.checkValue(errorMessages);
    String invalid = value.ranges().stream().filter(v -> !v.isValid()).map(RangeOrValue::toString)
        .collect(Collectors.joining(", "));
    if (!invalid.isBlank()) {
      errorMessages.add("Ranges " + invalid + " are invalid.");
      failed = true;
    }
    return !failed;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    final Element toleranceParam = (Element) xmlElement.getElementsByTagName(
        SimpleParameterSet.parameterElement).item(0);
    assert toleranceParam.getAttribute(SimpleParameterSet.nameAttribute)
        .equals(toleranceParameter.getName());
    toleranceParameter.loadValueFromXML(toleranceParam);

    final Element rovListElement = (Element) xmlElement.getElementsByTagName("ranges_or_values")
        .item(0);
    final NodeList rovList = rovListElement.getElementsByTagName("range_or_value");

    List<RangeOrValue<T>> ranges = new ArrayList<>();
    for (int i = 0; i < rovList.getLength(); i++) {
      final Element rov = (Element) rovList.item(i);
      String strLower = rov.getAttribute("lower");
      String strUpper = rov.getAttribute("upper");

      if (CONST.XML_NULL_VALUE.equals(strLower)) {
        strLower = null;
      }
      if (CONST.XML_NULL_VALUE.equals(strUpper)) {
        strUpper = null;
      }

      ranges.add(new RangeOrValue<>(parseFromString(strLower), parseFromString(strUpper)));
    }

    value = new RangeOrValueResult<>(ranges, toleranceParameter.getValue());
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    final Document doc = xmlElement.getOwnerDocument();

    final Element toleranceElement = doc.createElement(SimpleParameterSet.parameterElement);
    toleranceElement.setAttribute(SimpleParameterSet.nameAttribute, toleranceParameter.getName());
    toleranceParameter.saveValueToXML(toleranceElement);

    final Element rovList = doc.createElement("ranges_or_values");

    xmlElement.appendChild(toleranceElement);
    xmlElement.appendChild(rovList);

    for (RangeOrValue<T> range : value.ranges()) {
      final Element element = doc.createElement("range_or_value");
      element.setAttribute("lower", ParsingUtils.parseNullableString(
          range.getLower() != null ? numberFormat.format(range.getLower()) : null));
      element.setAttribute("upper", ParsingUtils.parseNullableString(
          range.getUpper() != null ? numberFormat.format(range.getUpper()) : null));
      rovList.appendChild(element);
    }
  }

  protected abstract T parseFromString(@Nullable String string);
}
