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

package io.github.mzmine.datamodel.features.types.annotations;

import com.opencsv.RFC4180ParserBuilder;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.io.JsonUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SynonymsType extends ListDataType<String> {

  @Override
  public @NotNull String getUniqueID() {
    return "synonyms";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Synonyms";
  }

  /**
   * Accepts JSON arrays and semicolon-separated names with CSV quoting. Commas belong to names.
   */
  public static @Nullable List<String> parse(@Nullable String text) {
    if (text == null) {
      return null;
    }
    if (text.isBlank()) {
      return List.of();
    }
    text = text.strip();
    if (text.startsWith("[")) {
      final Object json = JsonUtils.readValueOrNull(text);
      if (json instanceof List<?> list && list.stream().allMatch(String.class::isInstance)) {
        return list.stream().map(String.class::cast).toList();
      }
      // Chemical names may also start with brackets; keep parsing them as text.
    }
    try {
      final String[] names = new RFC4180ParserBuilder().withSeparator(';').build().parseLine(text);
      return Arrays.stream(names).map(String::strip).filter(name -> !name.isEmpty()).toList();
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot parse synonyms: " + text, e);
    }
  }

  @Override
  public @NotNull Function<@Nullable String, @Nullable List<String>> getMapper() {
    return SynonymsType::parse;
  }

  @Override
  public @NotNull String getFormattedString(@Nullable final List<String> value,
      final boolean export) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    if (export) {
      return JsonUtils.writeStringOrEmpty(value);
    }
    return String.join("; ", value);
  }

  @Override
  public void saveToXML(@NotNull final XMLStreamWriter writer, @Nullable final Object value,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {
    writer.writeCharacters(value == null ? CONST.XML_NULL_VALUE
        : JsonUtils.writeStringOrElse(value, CONST.XML_NULL_VALUE));
  }

  @Override
  public @Nullable List<String> loadFromXML(@NotNull final XMLStreamReader reader,
      @NotNull final MZmineProject project, @NotNull final ModularFeatureList flist,
      @NotNull final ModularFeatureListRow row, @Nullable final ModularFeature feature,
      @Nullable final RawDataFile file) throws XMLStreamException {
    final String text = reader.getElementText();
    return CONST.XML_NULL_VALUE.equals(text) ? null : parse(text);
  }
}
