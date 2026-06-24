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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Selects a single group from a metadata column
 */
public class MetadataListGroupsSelectionParameter implements
    UserParameter<MetadataListGroupsSelection, MetadataListGroupsSelectionComponent> {

  private final String name;
  private final String descr;
  private final String XML_COLUMN_ATTR = "column";
  private final String XML_GROUP_ATTR = "group";
  @NotNull
  private MetadataListGroupsSelection value;
  private boolean requireValue = true;

  public MetadataListGroupsSelectionParameter() {
    this("Sample grouping",
        "Select a sample metadata column and one or multiple groups from this column.");
  }

  public MetadataListGroupsSelectionParameter(String name, String descr) {
    this(name, descr, MetadataListGroupsSelection.NONE);
  }

  public MetadataListGroupsSelectionParameter(String name, String descr,
      @Nullable MetadataListGroupsSelection defaultValue) {
    this(name, descr, defaultValue, true);
  }

  public MetadataListGroupsSelectionParameter(String name, String descr,
      @Nullable MetadataListGroupsSelection defaultValue, boolean requireValue) {
    this.name = name;
    this.descr = descr;
    this.requireValue = requireValue;
    value = requireNonNullElse(defaultValue, MetadataListGroupsSelection.NONE);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public @NotNull MetadataListGroupsSelection getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable MetadataListGroupsSelection newValue) {
    value = requireNonNullElse(newValue, MetadataListGroupsSelection.NONE);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (!requireValue) {
      return true;
    }
    // can only check if there is a value
    if (StringUtils.isBlank(value.columnName()) || value.groups().isEmpty()) {
      errorMessages.add(
          "No value set for parameter %s. Select a column and group.".formatted(getName()));
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    final String column = xmlElement.getAttribute(XML_COLUMN_ATTR);

    List<String> groups = new ArrayList<>();

    int i = 0;
    while (true) {
      final String group = xmlElement.getAttribute(XML_GROUP_ATTR + i);
      if (StringUtils.isBlank(group)) {
        break;
      }
      groups.add(group);
      i++;
    }

    this.value = new MetadataListGroupsSelection(column.trim(), groups);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    xmlElement.setAttribute(XML_COLUMN_ATTR, value != null ? value.columnName() : "");

    final List<@NotNull String> grps = List.copyOf(value.groups());
    for (int i = 0; i < grps.size(); i++) {
      xmlElement.setAttribute(XML_GROUP_ATTR + i, grps.get(i));
    }
  }

  @Override
  public String getDescription() {
    return descr;
  }

  @Override
  public MetadataListGroupsSelectionComponent createEditingComponent() {
    return new MetadataListGroupsSelectionComponent(value);
  }

  @Override
  public void setValueFromComponent(
      MetadataListGroupsSelectionComponent metadataGroupSelectionComponent) {
    this.value = metadataGroupSelectionComponent.getValue();
  }

  @Override
  public void setValueToComponent(
      MetadataListGroupsSelectionComponent metadataGroupSelectionComponent,
      @Nullable MetadataListGroupsSelection newValue) {
    metadataGroupSelectionComponent.setValue(newValue);
  }

  @Override
  public MetadataListGroupsSelectionParameter cloneParameter() {
    return new MetadataListGroupsSelectionParameter(name, descr, value, requireValue);
  }

}
