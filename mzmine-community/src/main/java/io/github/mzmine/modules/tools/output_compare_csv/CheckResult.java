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

package io.github.mzmine.modules.tools.output_compare_csv;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.github.mzmine.datamodel.features.types.DataType;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @param identifier any string identifying this result
 * @param severity   the level
 * @param type       a data type if this message is related to a data type
 * @param value      first value if this was value comparison
 * @param otherValue second value if this was a value comparison
 * @param message    the message describing the result
 */
public record CheckResult(@NotNull String identifier, @NotNull Severity severity,
                          @Nullable String type,
                          @JsonSerialize(using = ToStringSerializer.class) @Nullable Object value,
                          @JsonSerialize(using = ToStringSerializer.class) @Nullable Object otherValue,
                          @NotNull String message) {

  public static CheckResult create(@NotNull String identifier, @NotNull Severity severity,
      @Nullable DataType type, @Nullable Object value, @Nullable Object otherValue,
      final String message) {
    return new CheckResult(identifier, severity, type == null ? null : type.getUniqueID(), value,
        otherValue, message);
  }

  public static CheckResult create(@NotNull String identifier, @NotNull Severity severity,
      @Nullable Object value, @Nullable Object otherValue, final String message) {
    return new CheckResult(identifier, severity, null, value, otherValue, message);
  }

  public static CheckResult create(@NotNull String identifier, @NotNull Severity severity,
      final String message) {
    return new CheckResult(identifier, severity, null, null, null, message);
  }

  public enum Severity {
    INFO, WARN, ERROR;

    public static Severity parse(final String str) {
      return switch (str.toLowerCase()) {
        case "warn", "warning" -> WARN;
        case "error", "severe" -> ERROR;
        default -> INFO;
      };
    }

    public void applyInPlace(final List<CheckResult> checks) {
      checks.removeIf(check -> check.severity.ordinal() < this.ordinal());
    }
  }
}
