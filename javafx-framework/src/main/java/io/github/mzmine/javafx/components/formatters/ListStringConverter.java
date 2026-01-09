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

package io.github.mzmine.javafx.components.formatters;

import java.util.List;
import java.util.function.Function;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;

public class ListStringConverter<T> extends StringConverter<T> {

  private final List<T> items;
  private final Function<T, String> toStringFunction;

  public ListStringConverter(@NotNull List<T> items) {
    this(items, String::valueOf);
  }

  public ListStringConverter(@NotNull List<T> items,
      @NotNull Function<T, String> toStringFunction) {
    this.items = items;
    this.toStringFunction = toStringFunction;
  }

  public static <T> TextFormatter<T> createFormatter(@NotNull List<T> items) {
    return createFormatter(items, String::valueOf);
  }

  public static <T> TextFormatter<T> createFormatter(@NotNull List<T> items,
      @NotNull Function<T, String> toStringFunction) {
    return new TextFormatter<T>(new ListStringConverter<>(items, toStringFunction));
  }

  @Override
  public String toString(T item) {
    return item != null ? toStringFunction.apply(item) : "";
  }

  @Override
  public T fromString(String string) {
    if (string == null || string.isEmpty()) {
      return null;
    }

    for (T item : items) {
      if (toStringFunction.apply(item).equals(string)) {
        return item;
      }
    }
    return null;
  }
}
