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

package io.github.mzmine.datamodel.features.types.abstr;

import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.ParsingUtils;
import java.util.List;
import java.util.function.Function;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A list of strings. Formatted as a semicolon separated list. Exported and saved as a semicolon
 * separated list with CSV quoting, see {@link ParsingUtils#stringListToString}.
 */
public abstract class StringListType extends ListDataType<String> {

  public static final String SEPARATOR = "; ";

  /**
   * Accepts semicolon separated values with CSV quoting, see
   * {@link ParsingUtils#stringToStringList(String)}. Values are stripped and empty values are
   * removed.
   */
  public static @Nullable List<String> parse(@Nullable String text) {
    if (text == null) {
      return null;
    }
    if (text.isBlank()) {
      return List.of();
    }
    return ParsingUtils.stringToStringList(text.strip()).stream().map(String::strip)
        .filter(s -> !s.isEmpty()).toList();
  }

  @Override
  public @NotNull Function<@Nullable String, @Nullable List<String>> getMapper() {
    return StringListType::parse;
  }

  @Override
  public @NotNull String getFormattedString(@Nullable final List<String> value,
      final boolean export) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    if (export) {
      return ParsingUtils.stringListToString(value);
    }
    return String.join(SEPARATOR, value);
  }

  @Override
  public void saveToXML(@NotNull final XMLStreamWriter writer, @Nullable final Object value)
      throws XMLStreamException {
    if (!(value instanceof List<?> list)) {
      writer.writeCharacters(CONST.XML_NULL_VALUE);
      return;
    }
    writer.writeCharacters(ParsingUtils.stringListToString((List<String>) list));
  }

  @Override
  public @Nullable List<String> loadFromXML(@NotNull final XMLStreamReader reader)
      throws XMLStreamException {
    final String text = reader.getElementText();
    return CONST.XML_NULL_VALUE.equals(text) ? null : ParsingUtils.stringToStringList(text);
  }
}
