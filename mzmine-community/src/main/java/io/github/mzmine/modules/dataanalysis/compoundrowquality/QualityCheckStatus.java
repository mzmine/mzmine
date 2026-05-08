package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.javafx.util.FxIcons;
import org.jetbrains.annotations.NotNull;

/**
 * Status of a single {@link QualityCheckType} computed for a CompoundRow.
 */
public enum QualityCheckStatus {
  PASS, WARN, FAIL, UNAVAILABLE;

  public @NotNull FxIcons icon() {
    return switch (this) {
      case PASS -> FxIcons.CHECK_CIRCLE;
      case WARN -> FxIcons.EXCLAMATION_TRIANGLE;
      case FAIL -> FxIcons.X_CIRCLE;
      case UNAVAILABLE -> FxIcons.INFO_CIRCLE;
    };
  }
}
