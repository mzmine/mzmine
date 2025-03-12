package io.github.mzmine.modules.tools.output_compare;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.github.mzmine.datamodel.features.types.DataType;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record DataCheckResult(
    @JsonSerialize(using = ToStringSerializer.class) @Nullable DataType type,
    @NotNull String identifier, @NotNull Severity severity,
    @JsonSerialize(using = ToStringSerializer.class) @Nullable Object value,
    @JsonSerialize(using = ToStringSerializer.class) @Nullable Object otherValue,
    @NotNull String message) {

  public static DataCheckResult create(@Nullable DataType type, @NotNull Severity severity,
      @NotNull String identifier, @Nullable Object value, @Nullable Object otherValue,
      final String message) {
    return new DataCheckResult(type, identifier, severity, value, otherValue, message);
  }

  public static DataCheckResult create(@NotNull Severity severity, @NotNull String identifier,
      @Nullable Object value, @Nullable Object otherValue, final String message) {
    return new DataCheckResult(null, identifier, severity, value, otherValue, message);
  }

  public static DataCheckResult create(@NotNull Severity severity, @NotNull String identifier,
      final String message) {
    return new DataCheckResult(null, identifier, severity, null, null, message);
  }

  public enum Severity {
    INFO, WARN, ERROR;

    public void applyInPlace(final List<DataCheckResult> checks) {
      checks.removeIf(check -> check.severity.ordinal() < this.ordinal());
    }
  }
}
