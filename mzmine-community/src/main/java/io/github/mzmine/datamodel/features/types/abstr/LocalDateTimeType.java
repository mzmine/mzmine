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

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.date.DateTimeUtils;
import java.time.LocalDateTime;
import java.util.function.Function;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A date and time. Formatted, exported, and saved in ISO format, e.g., 2025-03-05T15:43:52.
 */
public abstract class LocalDateTimeType extends DataType<LocalDateTime> {

  @Override
  public ObjectProperty<LocalDateTime> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<LocalDateTime> getValueClass() {
    return LocalDateTime.class;
  }

  @Override
  public @NotNull String getFormattedString(@Nullable final LocalDateTime value,
      final boolean export) {
    return value == null ? "" : value.toString();
  }

  @Override
  public @NotNull Function<@Nullable String, @Nullable LocalDateTime> getMapper() {
    return text -> text == null || text.isBlank() ? null : DateTimeUtils.parseOrElse(text, null);
  }

  @Override
  public void saveToXML(@NotNull final XMLStreamWriter writer, @Nullable final Object value)
      throws XMLStreamException {
    if (value == null) {
      writer.writeCharacters(CONST.XML_NULL_VALUE);
      return;
    }
    if (!(value instanceof LocalDateTime date)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: "
              + value.getClass());
    }
    writer.writeCharacters(date.toString());
  }

  @Override
  public @Nullable LocalDateTime loadFromXML(@NotNull final XMLStreamReader reader)
      throws XMLStreamException {
    final String text = reader.getElementText();
    if (CONST.XML_NULL_VALUE.equals(text) || text.isBlank()) {
      return null;
    }
    return LocalDateTime.parse(text.strip());
  }
}
