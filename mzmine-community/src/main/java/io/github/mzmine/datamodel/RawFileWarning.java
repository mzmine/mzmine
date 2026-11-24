package io.github.mzmine.datamodel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A problem detected while a raw data file was imported.
 *
 * @param type    the kind of problem
 * @param details optional file specific details, e.g. counts, appended to the description
 */
public record RawFileWarning(@NotNull RawFileWarningType type, @Nullable String details) {

  public RawFileWarning(@NotNull RawFileWarningType type) {
    this(type, null);
  }

  /**
   * @return true if data may be missing or wrong
   */
  public boolean isCritical() {
    return type.isCritical();
  }

  /**
   * @return the type description followed by the details, if any
   */
  public @NotNull String message() {
    return details == null || details.isBlank() ? type.getDescription()
        : type.getDescription() + "\n" + details;
  }
}
