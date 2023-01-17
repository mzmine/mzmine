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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.UserParameter;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.scene.control.TextField;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

public class NumberListParameter implements UserParameter<List<Double>, TextField> {

  private static final Logger logger = Logger.getLogger(NumberListParameter.class.getName());

  protected String name, description;
  protected List<Double> value;
  protected final char separator;
  protected final int inputsize;
  protected final NumberFormat format;

  public NumberListParameter(String name, String description, int inputsize,
      List<Double> defaultValue, char separator, NumberFormat format) {
    this.name = name;
    this.description = description;
    this.inputsize = inputsize;
    value = defaultValue;
    this.separator = separator;
    this.format = format;
  }

  public NumberListParameter(String name, String description, List<Double> defaultValue,
      NumberFormat format) {
    this(name, description, 20, defaultValue, ',', format);
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
  public TextField createEditingComponent() {
    TextField stringComponent = new TextField();
    stringComponent.setPrefColumnCount(inputsize);
    return stringComponent;
  }

  @Override
  public List<Double> getValue() {
    return value;
  }

  @Override
  public void setValue(List<Double> value) {
    this.value = value;
  }

  @Override
  public NumberListParameter cloneParameter() {
    NumberListParameter copy = new NumberListParameter(name, description, inputsize, getValue(),
        separator, format);
    copy.setValue(this.getValue());

    return copy;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public void setValueFromComponent(TextField component) {
    final String text = component.getText().trim();
    if (text.isBlank()) {
      value = List.of();
      return;
    }

    value = getValueFromString(text);
    setValueToComponent(component, value); // update value after parsing
  }

  private List<Double> getValueFromString(String text) {
    final String[] split = StringUtils.split(text, separator);
    List<Double> list = new ArrayList<>();
    for (String s : split) {
      try {
        list.add(format.parse(s.trim()).doubleValue());
      } catch (ParseException | ClassCastException e) {
        logger.log(Level.WARNING, e, e::getMessage);
      }
    }
    return list;
  }

  @Override
  public void setValueToComponent(TextField component, List<Double> newValue) {
    String text = valueAsString(newValue);
    component.setText(text);
  }

  @NotNull
  private String valueAsString(List<Double> value) {
    return value.stream().map(format::format).collect(Collectors.joining(separator + " "));
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    value = getValueFromString(xmlElement.getTextContent());
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    xmlElement.setTextContent(valueAsString(value));
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if ((value == null) || (value.isEmpty())) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    return true;
  }

  @Override
  public boolean isSensitive() {
    return false;
  }
}
