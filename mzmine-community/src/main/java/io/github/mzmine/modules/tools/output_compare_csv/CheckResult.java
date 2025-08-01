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
