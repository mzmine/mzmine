package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bundle of thresholds, optional row coloring, and an optional row-click callback passed into each
 * {@link QualityCheck}. Immutable snapshot taken before dispatching to a background thread. The
 * coloring + click callback are nullable so the quality pane can run standalone (without a host
 * dashboard); checks that want to render colored / clickable member labels should fall back to
 * plain text when these are absent.
 */
public record QualityCheckContext(@NotNull RTTolerance rtTolerance,
                                  @NotNull MZTolerance mzTolerance,
                                  @NotNull MZTolerance ms2Tolerance,
                                  @Nullable ColorAssignment colorAssignment,
                                  @Nullable Consumer<@NotNull FeatureListRow> onRowClick) {

  public QualityCheckContext(@NotNull RTTolerance rtTolerance, @NotNull MZTolerance mzTolerance,
      @NotNull MZTolerance ms2Tolerance) {
    this(rtTolerance, mzTolerance, ms2Tolerance, null, null);
  }
}
