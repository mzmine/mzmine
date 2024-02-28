/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.projectmetadata.table.columns;

import io.github.mzmine.modules.visualization.projectmetadata.ProjectMetadataColumnParameters.AvailableTypes;
import io.github.mzmine.util.DateTimeUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Specific Date-type implementation of the project parameter.
 */
public final class DateMetadataColumn extends MetadataColumn<LocalDateTime> {

  public DateMetadataColumn(String title) {
    super(title);
  }

  public DateMetadataColumn(String title, String description) {
    super(title, description);
  }

  @Override
  public boolean checkInput(Object value) {
    return value instanceof LocalDateTime;
  }

  @Override
  public AvailableTypes getType() {
    return AvailableTypes.DATETIME;
  }

  @Override
  public @Nullable LocalDateTime convertOrElse(@Nullable String input,
      @Nullable LocalDateTime defaultValue) {
    try {
      return input == null ? defaultValue : convertOrThrow(input);
    } catch (DateTimeParseException ignored) {
      return defaultValue;
    }
  }

  @Override
  public LocalDateTime convertOrThrow(@NotNull final String input) {
    //ISO-8601 format
    return DateTimeUtils.parse(input.trim());
  }

  @Override
  public LocalDateTime defaultValue() {
    return null;
  }

  @Override
  public LocalDateTime exampleValue() {
    return LocalDateTime.of(2021, 6, 10, 22, 23);
  }
}
