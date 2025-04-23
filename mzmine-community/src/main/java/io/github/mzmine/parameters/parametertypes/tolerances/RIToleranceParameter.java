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

package io.github.mzmine.parameters.parametertypes.tolerances;

import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.RIColumn;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class RIToleranceParameter implements UserParameter<RITolerance, RIToleranceComponent> {

  private static final String DEFAULT_NAME = "Retention index tolerance";
  private static final String DEFAULT_DESC = "Maximum allowed difference between two retention index values";
  private final ObservableList<RIColumn> columnTypes;
  private final String name;
  private final String description;
  private RITolerance value;

  public RIToleranceParameter() {
    this(DEFAULT_NAME, DEFAULT_DESC);
  }

  public RIToleranceParameter(String name, String description) {
    ArrayList<RIColumn> columnTypes = new ArrayList<>(Arrays.asList(RIColumn.values()));
    columnTypes.remove(RIColumn.DEFAULT);

    this(name, description, FXCollections.observableArrayList(columnTypes));
  }

  public RIToleranceParameter(String name, String description,
      ObservableList<RIColumn> columnTypes) {
    this.name = name;
    this.description = description;
    this.columnTypes = columnTypes;
  }

  public RIToleranceParameter(String name, String description, RITolerance defaultValue) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;

    ArrayList<RIColumn> columnTypes = new ArrayList<>(Arrays.asList(RIColumn.values()));
    columnTypes.remove(RIColumn.DEFAULT);
    this.columnTypes = FXCollections.observableArrayList(columnTypes);
  }

  public RIToleranceParameter(RITolerance defaultValue) {
    this.name = DEFAULT_NAME;
    this.description = DEFAULT_DESC;
    this.value = defaultValue;

    ArrayList<RIColumn> columnTypes = new ArrayList<>(Arrays.asList(RIColumn.values()));
    columnTypes.remove(RIColumn.DEFAULT);
    this.columnTypes = FXCollections.observableArrayList(columnTypes);
  }

  public RIToleranceParameter(String name, String description, RITolerance defaultValue,
      ObservableList<RIColumn> columnTypes) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;
    this.columnTypes = columnTypes;
  }

  /**
   * @see io.github.mzmine.parameters.UserParameter#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * @see io.github.mzmine.parameters.UserParameter#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public RIToleranceComponent createEditingComponent() {
    return new RIToleranceComponent(columnTypes);
  }

  @Override
  public RIToleranceParameter cloneParameter() {
    RIToleranceParameter copy = new RIToleranceParameter(name, description, columnTypes);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public void setValueFromComponent(RIToleranceComponent component) {
    this.value = component.getValue();
  }

  @Override
  public void setValueToComponent(RIToleranceComponent component, @Nullable RITolerance newValue) {
    component.setValue(newValue);
  }

  @Override
  public RITolerance getValue() {
    return value;
  }

  @Override
  public void setValue(RITolerance newValue) {
    this.value = newValue;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String columnAttrType = xmlElement.getAttribute("columnType");
    if (columnAttrType == null || columnAttrType.isEmpty()) {
      return;
    }

    RIColumn columnType = RIColumn.valueOf(columnAttrType);

    String toleranceNum = xmlElement.getTextContent();
    if (toleranceNum.length() == 0) {
      return;
    }
    float tolerance = Float.parseFloat(toleranceNum);
    this.value = new RITolerance(tolerance, columnType);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    xmlElement.setAttribute("columnType", value.getColumn().name());
    float tolerance = value.getTolerance();
    String toleranceNum = String.valueOf(tolerance);
    xmlElement.setTextContent(toleranceNum);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    float tolerance = value.getTolerance();
    if (tolerance <= 0) {
      errorMessages.add("Invalid retention index tolerance value.");
      return false;

    }
    return true;
  }
}
