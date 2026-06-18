package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import org.jetbrains.annotations.NotNull;

/**
 * One quality check for a compound row. Stateless and safe to invoke off the FX thread.
 */
public interface QualityCheck {

  @NotNull QualityCheckType type();

  @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context);
}
