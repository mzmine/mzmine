/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import javafx.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Basic String value parameter with option to customize the node component
 *
 * @param <COMPONENT> the javafx node
 */
public abstract class StringValueParameter<COMPONENT extends Node> implements
    UserParameter<String, COMPONENT> {

  protected final boolean sensitive;
  protected String name, description;
  protected @NotNull String value = "";
  protected int inputsize = 20;
  protected boolean valueRequired = true;

  public StringValueParameter(String name, String description) {
    this(name, description, "");
  }

  public StringValueParameter(String name, String description, boolean isSensitive) {
    this(name, description, "", true, isSensitive);
  }

  public StringValueParameter(String name, String description, int inputsize) {
    this(name, description, "", true, false, inputsize);
  }

  public StringValueParameter(String name, String description, @Nullable String defaultValue) {
    this(name, description, defaultValue, true, false);
  }

  public StringValueParameter(String name, String description, @Nullable String defaultValue,
      boolean valueRequired) {
    this(name, description, defaultValue, valueRequired, false);
  }

  public StringValueParameter(String name, String description, @Nullable String defaultValue,
      boolean valueRequired, boolean isSensitive) {
    this(name, description, defaultValue, valueRequired, isSensitive, 20);
  }

  public StringValueParameter(String name, String description, @Nullable String defaultValue,
      boolean valueRequired, boolean isSensitive, int inputsize) {
    this.name = name;
    this.description = description;
    this.value = requireNonNullElse(defaultValue, "");
    this.valueRequired = valueRequired;
    this.sensitive = isSensitive;
    this.inputsize = inputsize;
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
  public @NotNull String getValue() {
    return value;
  }

  @Override
  public void setValue(String value) {
    this.value = requireNonNullElse(value, "");
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    value = requireNonNullElse(xmlElement.getTextContent(), "");
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    xmlElement.setTextContent(value);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (!valueRequired) {
      return true;
    }
    if (value.isBlank()) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    return true;
  }

  @Override
  public boolean isSensitive() {
    return sensitive;
  }
}
