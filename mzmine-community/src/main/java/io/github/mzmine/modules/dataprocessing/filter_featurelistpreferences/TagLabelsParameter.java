/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_featurelistpreferences;

import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.StringUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Parameter for the ordered labels of the feature-list tag checkboxes.
 */
public class TagLabelsParameter implements UserParameter<List<String>, TagLabelsComponent> {

  private static final String XML_LABEL_TAG = "label";

  private final @NotNull String name;
  private final @NotNull String description;
  private @NotNull List<String> value;

  public TagLabelsParameter(@NotNull final String name, @NotNull final String description,
      @NotNull final List<String> defaultValue) {
    this.name = name;
    this.description = description;
    setValue(defaultValue);
  }

  private static @NotNull List<String> normalize(@Nullable final List<String> labels) {
    if (labels == null) {
      return List.of();
    }
    return labels.stream().map(String::trim).filter(StringUtils::hasValue).toList();
  }

  @Override
  public @NotNull String getName() {
    return name;
  }

  @Override
  public @NotNull String getDescription() {
    return description;
  }

  @Override
  public @NotNull List<String> getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable final List<String> newValue) {
    value = normalize(newValue);
  }

  @Override
  public @NotNull TagLabelsComponent createEditingComponent() {
    return new TagLabelsComponent(value);
  }

  @Override
  public void setValueFromComponent(@NotNull final TagLabelsComponent component) {
    setValue(component.getValue());
  }

  @Override
  public void setValueToComponent(@NotNull final TagLabelsComponent component,
      @Nullable final List<String> newValue) {
    component.setValue(newValue);
  }

  @Override
  public @NotNull TagLabelsParameter cloneParameter() {
    return new TagLabelsParameter(name, description, value);
  }

  @Override
  public void loadValueFromXML(@NotNull final Element xmlElement) {
    final NodeList nodes = xmlElement.getElementsByTagName(XML_LABEL_TAG);
    if (nodes.getLength() == 0) {
      return;
    }

    final List<String> labels = new ArrayList<>(nodes.getLength());
    for (int i = 0; i < nodes.getLength(); i++) {
      final String label = nodes.item(i).getTextContent();
      if (StringUtils.hasValue(label)) {
        labels.add(label);
      }
    }
    setValue(labels);
  }

  @Override
  public void saveValueToXML(@NotNull final Element xmlElement) {
    final Document document = xmlElement.getOwnerDocument();
    for (final String label : value) {
      final Element labelElement = document.createElement(XML_LABEL_TAG);
      labelElement.setTextContent(label);
      xmlElement.appendChild(labelElement);
    }
  }

  @Override
  public boolean checkValue(@NotNull final Collection<String> errorMessages) {
    if (value.isEmpty()) {
      errorMessages.add(name + ": define at least one tag label");
      return false;
    }
    return true;
  }
}
