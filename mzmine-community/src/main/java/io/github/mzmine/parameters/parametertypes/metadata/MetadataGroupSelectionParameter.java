/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Selects a single group from a metadata column
 */
public class MetadataGroupSelectionParameter implements
    UserParameter<MetadataGroupSelection, MetadataGroupSelectionComponent> {

  private final String name;
  private final String descr;
  private final String XML_COLUMN_ATTR = "column";
  private final String XML_GROUP_ATTR = "group";
  private MetadataGroupSelection value = new MetadataGroupSelection("", "");

  public MetadataGroupSelectionParameter(String name, String descr) {
    this.name = name;
    this.descr = descr;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public MetadataGroupSelection getValue() {
    return value;
  }

  @Override
  public void setValue(MetadataGroupSelection newValue) {
    value = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add("No value set for " + getName() + " (null).");
      return false;
    }
    if (!value.isValid()) {
      errorMessages.add("Selected metadata column or group does not exist (case sensitive)");
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    final String column = xmlElement.getAttribute(XML_COLUMN_ATTR);
    final String group = xmlElement.getAttribute(XML_GROUP_ATTR);

    this.value = new MetadataGroupSelection(column.trim(), group.trim());
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
  public MetadataGroupSelectionComponent createEditingComponent() {
    return new MetadataGroupSelectionComponent();
  }

  @Override
  public void setValueFromComponent(
      MetadataGroupSelectionComponent metadataGroupSelectionComponent) {
    this.value = metadataGroupSelectionComponent.getValue();
  }

  @Override
  public void setValueToComponent(MetadataGroupSelectionComponent metadataGroupSelectionComponent,
      @Nullable MetadataGroupSelection newValue) {
    metadataGroupSelectionComponent.setValue(newValue);
  }

  @Override
  public UserParameter<MetadataGroupSelection, MetadataGroupSelectionComponent> cloneParameter() {
    final MetadataGroupSelectionParameter param = new MetadataGroupSelectionParameter(name, descr);
    if (value == null) {
      param.setValue(null);
      return param;
    }
    param.setValue(new MetadataGroupSelection(value.columnName(), value.groupStr()));
    return param;
  }

}
