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

package io.github.mzmine.javafx.components.converters;

import java.util.Collection;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

/**
 * Uses the options of a combobox or other list of options to convert to actually selected element.
 * <p>
 * The issue is that if a ComboBox is editable then the text entered is used as value and not the
 * actual value object that may be selected. Need to set the string converter in this case to all
 * ComboBoxes that have values different from type String.
 * <p>
 * IMPORTANT: ComboBox of String do not need this converter. But can help to reduce values to
 * actually available values. For ComboBoxes that can also take free text values like the metadata
 * group, where values are not known ahead of time, use a different component or use the regular
 * {@link DefaultStringConverter}.
 */
public class OptionsStringConverter<T> extends StringConverter<T> {

  private final Collection<T> options;

  public OptionsStringConverter(ComboBox<T> comboBox) {
    this(comboBox.getItems());
  }

  public OptionsStringConverter(Collection<T> options) {
    this.options = options;
  }

  @Override
  public String toString(T t) {
    // uses to string not unique ID
    return t == null ? "" : t.toString();
  }

  @Override
  public T fromString(String s) {
    for (T option : options) {
      if (option == null) {
        continue;
      }
      if (option.toString().equals(s)) {
        return option;
      }
    }

    // need to return null here otherwise there will be ClassCastExceptions
    // in ComboBox.getValue - which will be String instead of T if we return the input s
    return null;
  }
}
