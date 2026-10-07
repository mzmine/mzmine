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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.util.collections.IndexRangesList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Parameter holding an {@link IndexRangesList}. The user enters a comma separated list of single
 * indices and ranges, e.g. "1,5-6", which is parsed via {@link IndexRangesList#parse(String)}. The
 * value is {@code null} only while the entered text cannot be parsed, which is reported by
 * {@link #checkValue(Collection)}.
 */
public class IndexRangesParameter implements UserParameter<IndexRangesList, IndexRangesComponent> {

  private final @NotNull String name;
  private final @NotNull String description;
  private final boolean valueRequired;
  private @Nullable IndexRangesList value;

  public IndexRangesParameter(@NotNull final String name, @NotNull final String description) {
    this(name, description, true, new IndexRangesList(List.of()));
  }

  public IndexRangesParameter(@NotNull final String name, @NotNull final String description,
      final boolean valueRequired, @Nullable final IndexRangesList defaultValue) {
    this.name = name;
    this.description = description;
    this.valueRequired = valueRequired;
    this.value = defaultValue;
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
  public @Nullable IndexRangesList getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable final IndexRangesList newValue) {
    this.value = newValue;
  }

  @Override
  public @NotNull IndexRangesComponent createEditingComponent() {
    final IndexRangesComponent component = new IndexRangesComponent();
    component.setValue(value);
    return component;
  }

  @Override
  public void setValueFromComponent(@NotNull final IndexRangesComponent component) {
    // null indicates unparsable text and is kept so checkValue can report it
    this.value = component.getValue();
  }

  @Override
  public void setValueToComponent(@NotNull final IndexRangesComponent component,
      @Nullable final IndexRangesList newValue) {
    component.setValue(newValue);
  }

  @Override
  public @NotNull IndexRangesParameter cloneParameter() {
    return new IndexRangesParameter(name, description, valueRequired, value);
  }

  @Override
  public boolean checkValue(@NotNull final Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(name + ": cannot parse index ranges. Use a list like 1,5-6");
      return false;
    }
    if (valueRequired && value.isEmpty()) {
      errorMessages.add(name + " is not set. Provide index ranges like 1,5-6");
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(@NotNull final Element xmlElement) {
    final String text = xmlElement.getTextContent();
    try {
      value = IndexRangesList.parse(text);
    } catch (IllegalArgumentException e) {
      value = new IndexRangesList(List.of());
    }
  }

  @Override
  public void saveValueToXML(@NotNull final Element xmlElement) {
    if (value == null || value.isEmpty()) {
      return;
    }
    xmlElement.setTextContent(value.asString());
  }
}
