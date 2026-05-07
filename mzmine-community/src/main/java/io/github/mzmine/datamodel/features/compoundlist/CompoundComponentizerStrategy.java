package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CompoundComponentizerStrategy {

  @NotNull List<ModularCompoundRow> componentize(
      @NotNull ModularFeatureList featureList,
      @NotNull CompoundList targetList);

  /**
   * Per-strategy precondition check. Implementations return a user-facing error message when the
   * strategy cannot run on the given feature list (e.g. missing IIN or correlation map), or
   * {@code null} when inputs are sufficient.
   */
  default @Nullable String validateInputs(@NotNull ModularFeatureList featureList) {
    return null;
  }
}
