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
 * Selects two groups from a metadata column, e.g., for univariate comparison
 */
public class Metadata2GroupsSelectionParameter implements
    UserParameter<Metadata2GroupsSelection, Metadata2GroupsSelectionComponent> {

  private static final String XML_COLUMN_ATTR = "column";
  private static final String XML_GROUPA_ATTR = "groupA";
  private static final String XML_GROUPB_ATTR = "groupB";

  private final String name;
  private final String descr;
  @NotNull
  private Metadata2GroupsSelection value;

  public Metadata2GroupsSelectionParameter(String descr) {
    this("Metadata selection", descr);
  }

  public Metadata2GroupsSelectionParameter(String name, String descr) {
    this(name, descr, Metadata2GroupsSelection.NONE);
  }

  public Metadata2GroupsSelectionParameter(String name, String descr,
      @NotNull Metadata2GroupsSelection defaultValue) {
    this.name = name;
    this.descr = descr;
    value = defaultValue;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  @NotNull
  public Metadata2GroupsSelection getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable Metadata2GroupsSelection newValue) {
    value = requireNonNullElse(newValue, Metadata2GroupsSelection.NONE);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    // can only check if there is a value
    if (StringUtils.isBlank(value.columnName()) || StringUtils.isBlank(value.groupA())
        || StringUtils.isBlank(value.groupB())) {
      errorMessages.add(
          "No value set for parameter %s. Select a column and group.".formatted(getName()));
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    final String column = xmlElement.getAttribute(XML_COLUMN_ATTR);
    final String groupA = xmlElement.getAttribute(XML_GROUPA_ATTR);
    final String groupB = xmlElement.getAttribute(XML_GROUPB_ATTR);

    this.value = new Metadata2GroupsSelection(column.trim(), groupA.trim(), groupB.trim());
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    xmlElement.setAttribute(XML_COLUMN_ATTR, value.columnName());
    xmlElement.setAttribute(XML_GROUPA_ATTR, value.groupA());
    xmlElement.setAttribute(XML_GROUPB_ATTR, value.groupB());
  }

  @Override
  public String getDescription() {
    return descr;
  }

  @Override
  public Metadata2GroupsSelectionComponent createEditingComponent() {
    return new Metadata2GroupsSelectionComponent();
  }

  @Override
  public void setValueFromComponent(
      Metadata2GroupsSelectionComponent metadataGroupSelectionComponent) {
    this.value = metadataGroupSelectionComponent.getValue();
  }

  @Override
  public void setValueToComponent(Metadata2GroupsSelectionComponent metadataGroupSelectionComponent,
      @Nullable Metadata2GroupsSelection newValue) {
    metadataGroupSelectionComponent.setValue(newValue);
  }

  @Override
  public UserParameter<Metadata2GroupsSelection, Metadata2GroupsSelectionComponent> cloneParameter() {
    // value is immutable so can be reused
    return new Metadata2GroupsSelectionParameter(name, descr, value);
  }

}
