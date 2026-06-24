package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Custom {@link QualityCheckResult} for the in-source fragmentation check.
/// <p>
/// Renders each fragment as a colored chip followed by an arrow and the higher-m/z parent rows
/// whose MS2 spectrum contains the fragment's precursor peak (also as colored chips), one line per
/// fragment. Rows that were upstream-tagged as {@code IN_SOURCE_FRAGMENT} but have no detectable
/// MS2 parent render as a standalone fragment chip with no arrow. Chip colors match the host
/// dashboard's per-row coloring; chips are clickable and write to the shared
/// {@code selectedMemberRow} property. Shared rendering helpers live in
/// {@link FragmentParentsRendering}.
public final class InSourceFragmentationQualityResult extends QualityCheckResult {

  private final @NotNull String summary;
  private final @NotNull List<@NotNull FragmentParents> fragments;
  private final @NotNull ColorAssignment colorAssignment;
  private final @Nullable ObjectProperty<@Nullable FeatureListRow> selectedMemberRow;

  public InSourceFragmentationQualityResult(@NotNull QualityCheckStatus status,
      @NotNull String summary, @NotNull List<@NotNull FragmentParents> fragments,
      @NotNull ColorAssignment colorAssignment,
      @Nullable ObjectProperty<@Nullable FeatureListRow> selectedMemberRow) {
    super(QualityCheckType.IN_SOURCE_FRAGMENTATION, status,
        FragmentParentsRendering.flattenInvolved(fragments));
    this.summary = summary;
    this.fragments = List.copyOf(fragments);
    this.colorAssignment = colorAssignment;
    this.selectedMemberRow = selectedMemberRow;
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
    if (fragments.isEmpty()) {
      return null;
    }
    final VBox body = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true);
    body.setMinWidth(0);
    for (final FragmentParents entry : fragments) {
      body.getChildren().add(
          FragmentParentsRendering.buildFragmentLine(entry, colorAssignment, selectedMemberRow));
    }
    return body;
  }
}
