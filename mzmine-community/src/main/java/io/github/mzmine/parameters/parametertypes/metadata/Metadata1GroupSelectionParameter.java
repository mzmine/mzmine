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

package io.github.mzmine.parameters.parametertypes.metadata;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.StringUtils;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Selects a single group from a metadata column
 */
public class Metadata1GroupSelectionParameter implements
    UserParameter<Metadata1GroupSelection, Metadata1GroupSelectionComponent> {

  private final String name;
  private final String descr;
  private final String XML_COLUMN_ATTR = "column";
  private final String XML_GROUP_ATTR = "group";
  @NotNull
  private Metadata1GroupSelection value;

  public Metadata1GroupSelectionParameter() {
    this("Sample grouping", "Select a sample metadata column and group from this column.");
  }

  public Metadata1GroupSelectionParameter(String name, String descr) {
    this(name, descr, new Metadata1GroupSelection("", ""));
  }

  public Metadata1GroupSelectionParameter(String name, String descr,
      @Nullable Metadata1GroupSelection defaultValue) {
    this.name = name;
    this.descr = descr;
    value = requireNonNullElse(defaultValue, Metadata1GroupSelection.NONE);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public @NotNull Metadata1GroupSelection getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable Metadata1GroupSelection newValue) {
    value = requireNonNullElse(newValue, Metadata1GroupSelection.NONE);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    // can only check if there is a value
    if (StringUtils.isBlank(value.columnName()) || StringUtils.isBlank(value.groupStr())) {
      errorMessages.add(
          "No value set for parameter %s. Select a column and group.".formatted(getName()));
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    final String column = xmlElement.getAttribute(XML_COLUMN_ATTR);
    final String group = xmlElement.getAttribute(XML_GROUP_ATTR);

    this.value = new Metadata1GroupSelection(column.trim(), group.trim());
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    xmlElement.setAttribute(XML_COLUMN_ATTR, value != null ? value.columnName() : "");
    xmlElement.setAttribute(XML_GROUP_ATTR, value != null ? value.groupStr() : "");
  }

  @Override
  public String getDescription() {
    return descr;
  }

  @Override
  public Metadata1GroupSelectionComponent createEditingComponent() {
    return new Metadata1GroupSelectionComponent();
  }

  @Override
  public void setValueFromComponent(
      Metadata1GroupSelectionComponent metadataGroupSelectionComponent) {
    this.value = metadataGroupSelectionComponent.getValue();
  }

  @Override
  public void setValueToComponent(Metadata1GroupSelectionComponent metadataGroupSelectionComponent,
      @Nullable Metadata1GroupSelection newValue) {
    metadataGroupSelectionComponent.setValue(newValue);
  }

  @Override
  public UserParameter<Metadata1GroupSelection, Metadata1GroupSelectionComponent> cloneParameter() {
    final Metadata1GroupSelectionParameter param = new Metadata1GroupSelectionParameter(name,
        descr);
    if (value == null) {
      param.setValue(null);
      return param;
    }
    param.setValue(new Metadata1GroupSelection(value.columnName(), value.groupStr()));
    return param;
  }

}
