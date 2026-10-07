package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.javafx.util.FxIcons;
import org.jetbrains.annotations.NotNull;

/**
 * Status of a single {@link QualityCheckType} computed for a CompoundRow.
 * <p>
 * {@link #DOES_NOT_APPLY} signals that the check cannot meaningfully run for this dataset (e.g. an
 * IMS check on a non-IMS feature list). The interactor filters {@link #DOES_NOT_APPLY} results out
 * before publishing to the model so the pane never shows them; the value still exists so checks
 * can return a well-typed result without needing a side channel.
 */
public enum QualityCheckStatus {
  PASS, WARN, FAIL, UNAVAILABLE, DOES_NOT_APPLY;

  public @NotNull FxIcons icon() {
    return switch (this) {
      case PASS -> FxIcons.CHECK_CIRCLE;
      case WARN -> FxIcons.EXCLAMATION_TRIANGLE;
      case FAIL -> FxIcons.X_CIRCLE;
      case UNAVAILABLE, DOES_NOT_APPLY -> FxIcons.INFO_CIRCLE;
    };
  }
}
