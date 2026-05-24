package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Custom {@link QualityCheckResult} for the ion-types check.
/// <p>
/// Sub pane is a {@link FlowPane} of colored chips, one per distinct ion type observed across the
/// compound's non-isotope members. Each chip reads {@code "[ion] m/z value"}, uses the host
/// dashboard's per-row coloring, and is clickable (forwards the row to {@code onRowClick}).
public final class IonTypesQualityResult extends QualityCheckResult {

  private final @NotNull String summary;
  private final @NotNull List<@NotNull FeatureListRow> distinctIonRows;
  private final @NotNull ColorAssignment colorAssignment;
  private final @Nullable Consumer<@NotNull FeatureListRow> onRowClick;

  public IonTypesQualityResult(@NotNull QualityCheckStatus status, @NotNull String summary,
      @NotNull List<@NotNull FeatureListRow> distinctIonRows,
      @NotNull List<@NotNull FeatureListRow> involvedRows, @NotNull ColorAssignment colorAssignment,
      @Nullable Consumer<@NotNull FeatureListRow> onRowClick) {
    super(QualityCheckType.ION_TYPES, status, involvedRows);
    this.summary = summary;
    this.distinctIonRows = List.copyOf(distinctIonRows);
    this.colorAssignment = colorAssignment;
    this.onRowClick = onRowClick;
  }

  @Override
  public @NotNull Region buildMainPane() {
    final Label title = FragmentParentsRendering.configureWrap(
        FxLabels.newBoldLabel(type.getLabel()));
    final Label summaryLabel = FragmentParentsRendering.configureWrap(FxLabels.newLabel(summary));
    final VBox box = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, title, summaryLabel);
    box.setMinWidth(0);
    return box;
  }

  @Override
  public @Nullable Region buildSubPane() {
    if (distinctIonRows.isEmpty()) {
      return null;
    }
    final FlowPane chips = FxLayout.newFlowPane();
    chips.setPadding(Insets.EMPTY);
    chips.setMinWidth(0);
    for (final FeatureListRow row : distinctIonRows) {
      chips.getChildren()
          .add(FragmentParentsRendering.buildChip(row, chipText(row), colorAssignment, onRowClick));
    }
    return chips;
  }

  /// Chip text for one ion: {@code "[ion] m/z value"} — the ion-type string followed by the
  /// representative row's formatted m/z. Falls back to row id when the row has no m/z.
  private static @NotNull String chipText(@NotNull final FeatureListRow row) {
    final IonIdentity ion = row.getBestIonIdentity();
    // assumption: caller only passes rows whose getBestIonIdentity() is non-null; otherwise show
    // a defensive fallback rather than throwing.
    final String ionStr = ion == null ? "?" : ion.getIonType().toString();
    final Double mz = row.getAverageMZ();
    return mz == null ? (ionStr + " row " + row.getID())
        : (ionStr + " m/z " + ConfigService.getGuiFormats().mz(mz));
  }
}
