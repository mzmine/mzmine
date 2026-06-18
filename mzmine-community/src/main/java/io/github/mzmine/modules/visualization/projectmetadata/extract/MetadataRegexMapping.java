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

package io.github.mzmine.modules.visualization.projectmetadata.extract;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * A single regex mapping that extracts one metadata column from the file name or path of selected
 * raw data files.
 *
 * @param inputSource   which string of the raw data file is used as regex input
 * @param columnName    the title of the target
 *                      {@link
 *                      io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn}
 * @param type          the target column type ({@link ExtractColumnType#AUTO} detects it from the
 *                      data)
 * @param regex         the regular expression (matched case-insensitively) applied to the input.
 *                      The first capture group is extracted if the regex defines one, otherwise the
 *                      whole match is used.
 * @param defaultValue  the value used when the regex does not match a file (blank = leave empty)
 * @param dropUnmapped  if true and value mappings are defined, extracted values that are not in the
 *                      mapping list are dropped (cell left empty)
 * @param valueMappings case-insensitive value mappings applied to the extracted value
 */
public record MetadataRegexMapping(@NotNull RegexInputSource inputSource,
                                   @NotNull String columnName, @NotNull ExtractColumnType type,
                                   @NotNull String regex, @NotNull String defaultValue,
                                   boolean dropUnmapped,
                                   @NotNull List<MetadataValueMapping> valueMappings) {

  public MetadataRegexMapping {
    inputSource = inputSource == null ? RegexInputSource.FILE_NAME : inputSource;
    columnName = columnName == null ? "" : columnName;
    type = type == null ? ExtractColumnType.AUTO : type;
    regex = regex == null ? "" : regex;
    defaultValue = defaultValue == null ? "" : defaultValue;
    valueMappings = valueMappings == null ? List.of() : List.copyOf(valueMappings);
  }

  /**
   * @return a new empty mapping with sensible defaults for a freshly added row.
   */
  public static MetadataRegexMapping createDefault() {
    return new MetadataRegexMapping(RegexInputSource.FILE_NAME, "", ExtractColumnType.AUTO, "", "",
        false, List.of());
  }

  /**
   * @return only the value mappings that define a non-blank {@link MetadataValueMapping#from()}
   * key.
   */
  public @NotNull List<MetadataValueMapping> activeValueMappings() {
    return valueMappings.stream().filter(MetadataValueMapping::isActive).toList();
  }
}
