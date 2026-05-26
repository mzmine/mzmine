package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckEvent;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckEvent.FragmentEnergyMethodSelectedEvent;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Custom {@link QualityCheckResult} for the MS2-availability check. Sub pane lists one line per
/// member row that has at least one MS2 scan; each line shows the colored, clickable row chip
/// followed by the row's fragment scans grouped by activation energy + method (e.g. "30 HCD; 50
/// HCD; 25 CID"). Each group chip is itself clickable and emits a
/// {@link FragmentEnergyMethodSelectedEvent} so the host (e.g. the compound dashboard) can switch
/// the MS2 chart to a matching scan.
public final class Ms2AvailableQualityResult extends QualityCheckResult {

  /// One row's fragment scans grouped by activation energy + method. {@code energy} is nullable for
  /// scans that don't report one (rare but possible); listeners should match on missing energy when
  /// {@code energy} is {@code null}.
  public record ScanGroup(@Nullable Float energy, @NotNull ActivationMethod method, int count) {

  }

  /// One member-row's MS2 evidence: the row itself plus its scan groups, pre-sorted in display
  /// order. The {@code count} field on each group drives the chip tooltip / future labelling.
  public record RowScans(@NotNull FeatureListRow row, @NotNull List<@NotNull ScanGroup> groups) {

  }

  private final @NotNull String summary;
  private final @NotNull List<@NotNull RowScans> rowScans;
  private final @NotNull ColorAssignment colorAssignment;
  private final @Nullable ObjectProperty<@Nullable FeatureListRow> selectedMemberRow;
  private final @Nullable Consumer<@NotNull QualityCheckEvent> onEvent;

  public Ms2AvailableQualityResult(@NotNull QualityCheckStatus status, @NotNull String summary,
      @NotNull List<@NotNull RowScans> rowScans,
      @NotNull List<@NotNull FeatureListRow> involvedRows, @NotNull ColorAssignment colorAssignment,
      @Nullable ObjectProperty<@Nullable FeatureListRow> selectedMemberRow,
      @Nullable Consumer<@NotNull QualityCheckEvent> onEvent) {
    super(QualityCheckType.MS2_AVAILABLE, status, involvedRows);
    this.summary = summary;
    this.rowScans = List.copyOf(rowScans);
    this.colorAssignment = colorAssignment;
    this.selectedMemberRow = selectedMemberRow;
    this.onEvent = onEvent;
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
    if (rowScans.isEmpty()) {
      return null;
    }
    final VBox body = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true);
    body.setMinWidth(0);
    for (final RowScans entry : rowScans) {
      body.getChildren().add(buildRowLine(entry));
    }
    return body;
  }

  private @NotNull FlowPane buildRowLine(@NotNull final RowScans entry) {
    final FlowPane line = FxLayout.newFlowPane();
    line.setPadding(Insets.EMPTY);
    line.setMinWidth(0);
    // Member-row chip: colored, clickable, writes to selectedMemberRow and bolds on selection.
    line.getChildren()
        .add(FragmentParentsRendering.buildChip(entry.row(), colorAssignment, selectedMemberRow));
    line.getChildren().add(new Label(":"));
    // Scan group chips: same color as the row, clickable, write to selectedMemberRow AND fire the
    // FragmentEnergyMethodSelectedEvent so the host can pick a matching scan.
    for (int i = 0; i < entry.groups().size(); i++) {
      final ScanGroup group = entry.groups().get(i);
      line.getChildren().add(buildScanGroupChip(entry.row(), group));
      if (i < entry.groups().size() - 1) {
        line.getChildren().add(new Label(";"));
      }
    }
    return line;
  }

  private @NotNull Label buildScanGroupChip(@NotNull final FeatureListRow row,
      @NotNull final ScanGroup group) {
    // buildChip with selectedMemberRow installs both the row-selection write on click and the
    // bold-style binding. We then layer the FragmentEnergyMethodSelectedEvent dispatch on top.
    final Label label = FragmentParentsRendering.buildChip(row, formatScanGroup(group),
        colorAssignment, selectedMemberRow);
    label.setCursor(Cursor.HAND);
    label.setOnMouseClicked(_ -> {
      if (selectedMemberRow != null) {
        selectedMemberRow.setValue(row);
      }
      if (onEvent != null) {
        onEvent.accept(new FragmentEnergyMethodSelectedEvent(row, group.energy(), group.method()));
      }
    });
    return label;
  }

  private static @NotNull String formatScanGroup(@NotNull final ScanGroup group) {
    final String energyStr = group.energy() == null ? "?" : formatEnergy(group.energy());
    return energyStr + " " + group.method().getAbbreviation();
  }

  /// Energy format: integer when {@code |energy| >= 10}, one decimal otherwise. Keeps chips short
  /// for typical 20–50 eV / a.u. settings while preserving precision for low-energy values.
  private static @NotNull String formatEnergy(@NotNull final Float energy) {
    final float v = energy;
    if (Math.abs(v) >= 10f) {
      return Integer.toString(Math.round(v));
    }
    return "%.1f".formatted(v);
  }
}
