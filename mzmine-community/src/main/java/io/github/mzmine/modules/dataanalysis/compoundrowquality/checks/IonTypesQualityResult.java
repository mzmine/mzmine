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
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
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
/// dashboard's per-row coloring, is clickable (writes to {@code selectedMemberRow}), and toggles
/// its bold style when the selection matches its row.
public final class IonTypesQualityResult extends QualityCheckResult {

  /// Collapsed summary lists this many ion types, e.g. GC-EI compounds have hundreds of fragments
  private static final int COLLAPSED_SUMMARY_IONS = 6;
  /// Collapsed sub pane shows this many ion chips
  private static final int COLLAPSED_CHIPS = 12;

  private final @NotNull List<@NotNull String> ionTypeNames;
  // shared by summary and chips, so one toggle unfolds both
  private final BooleanProperty expanded = new SimpleBooleanProperty(false);
  private final @NotNull List<@NotNull FeatureListRow> distinctIonRows;
  private final @NotNull ColorAssignment colorAssignment;
  private final @Nullable ObjectProperty<@Nullable FeatureListRow> selectedMemberRow;

  /**
   * @param ionTypeNames    distinct ion type names in the order of distinctIonRows
   * @param distinctIonRows one row per distinct ion type
   */
  public IonTypesQualityResult(@NotNull QualityCheckStatus status,
      @NotNull List<@NotNull String> ionTypeNames,
      @NotNull List<@NotNull FeatureListRow> distinctIonRows,
      @NotNull List<@NotNull FeatureListRow> involvedRows, @NotNull ColorAssignment colorAssignment,
      @Nullable ObjectProperty<@Nullable FeatureListRow> selectedMemberRow) {
    super(QualityCheckType.ION_TYPES, status, involvedRows);
    this.ionTypeNames = List.copyOf(ionTypeNames);
    this.distinctIonRows = List.copyOf(distinctIonRows);
    this.colorAssignment = colorAssignment;
    this.selectedMemberRow = selectedMemberRow;
  }

  @Override
  public @NotNull Region buildMainPane() {
    final Label title = FragmentParentsRendering.configureWrap(
        FxLabels.newBoldLabel(type.getLabel()));
    final Label summaryLabel = FragmentParentsRendering.configureWrap(FxLabels.newLabel(""));
    summaryLabel.textProperty()
        .bind(Bindings.createStringBinding(() -> summaryText(expanded.get()), expanded));
    final VBox box = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, title, summaryLabel);
    if (ionTypeNames.size() > COLLAPSED_SUMMARY_IONS) {
      box.getChildren().add(
          createToggleLink(ionTypeNames.size() - COLLAPSED_SUMMARY_IONS, "ion types"));
    }
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
    expanded.subscribe(isExpanded -> fillChips(chips, isExpanded));
    return chips;
  }

  private void fillChips(@NotNull final FlowPane chips, final boolean isExpanded) {
    chips.getChildren().clear();
    final boolean collapsible = distinctIonRows.size() > COLLAPSED_CHIPS;
    final int shown = collapsible && !isExpanded ? COLLAPSED_CHIPS : distinctIonRows.size();
    for (final FeatureListRow row : distinctIonRows.subList(0, shown)) {
      chips.getChildren().add(
          FragmentParentsRendering.buildChip(row, chipText(row), colorAssignment,
              selectedMemberRow));
    }
    if (collapsible) {
      chips.getChildren().add(createToggleLink(distinctIonRows.size() - COLLAPSED_CHIPS, "ions"));
    }
  }

  private @NotNull String summaryText(final boolean isExpanded) {
    final int n = ionTypeNames.size();
    final List<String> shown =
        isExpanded ? ionTypeNames : ionTypeNames.subList(0, Math.min(n, COLLAPSED_SUMMARY_IONS));
    return n + " ion type" + (n == 1 ? "" : "s") + ": " + String.join(", ", shown);
  }

  /**
   * @param hidden number of hidden items while collapsed
   * @param items  name of the items in the link text
   * @return link that toggles between collapsed and expanded
   */
  private @NotNull Hyperlink createToggleLink(final int hidden, @NotNull final String items) {
    final Hyperlink link = FxLabels.newHyperlink(() -> expanded.set(!expanded.get()), "");
    link.textProperty().bind(Bindings.when(expanded).then("Show less")
        .otherwise("+ %d more %s".formatted(hidden, items)));
    return link;
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
