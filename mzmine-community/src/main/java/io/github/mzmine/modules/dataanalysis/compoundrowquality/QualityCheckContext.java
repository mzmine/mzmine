package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import org.jetbrains.annotations.NotNull;

/**
 * Bundle of thresholds passed into each {@link QualityCheck}. Immutable snapshot taken before
 * dispatching to a background thread.
 */
public record QualityCheckContext(@NotNull RTTolerance rtTolerance, @NotNull MZTolerance mzTolerance,
                                  @NotNull MZTolerance ms2Tolerance) {
}
