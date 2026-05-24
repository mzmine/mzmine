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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Custom {@link QualityCheckResult} for the IMS fragmentation check.
/// <p>
/// Renders each fragment as a colored chip followed by an arrow and its mobility-correlated parent
/// rows (also as colored chips), one line per fragment. Chip colors match the host dashboard's
/// per-row coloring; chips are clickable and forward the row to the {@code onRowClick} callback
/// (typically wired to focus the row in the dashboard plots).
public final class ImsFragmentationQualityResult extends QualityCheckResult {

  /// One fragment row paired with the parent rows it shares mobility correlation with. Parents are
  /// stored pre-sorted in display order (ion-identified first, then by m/z).
  public record FragmentParents(@NotNull FeatureListRow fragment,
                                @NotNull List<@NotNull FeatureListRow> parents) {

  }

  private final @NotNull String summary;
  private final @NotNull List<@NotNull FragmentParents> fragments;
  private final @NotNull ColorAssignment colorAssignment;
  private final @Nullable Consumer<@NotNull FeatureListRow> onRowClick;

  public ImsFragmentationQualityResult(@NotNull QualityCheckStatus status, @NotNull String summary,
      @NotNull List<@NotNull FragmentParents> fragments, @NotNull ColorAssignment colorAssignment,
      @Nullable Consumer<@NotNull FeatureListRow> onRowClick) {
    super(QualityCheckType.IMS_FRAGMENTATION, status, flattenInvolved(fragments));
    this.summary = summary;
    this.fragments = List.copyOf(fragments);
    this.colorAssignment = colorAssignment;
    this.onRowClick = onRowClick;
  }

  @Override
  public @NotNull Region buildMainPane() {
    final Label title = configureWrap(FxLabels.newBoldLabel(type.getLabel()));
    final Label summaryLabel = configureWrap(FxLabels.newLabel(summary));
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
      body.getChildren().add(buildFragmentLine(entry));
    }
    return body;
  }

  private @NotNull FlowPane buildFragmentLine(@NotNull final FragmentParents entry) {
    final FlowPane line = FxLayout.newFlowPane();
    line.setPadding(Insets.EMPTY);
    line.setMinWidth(0);
    line.getChildren().add(buildChip(entry.fragment()));
    line.getChildren().add(new Label("←"));
    for (final FeatureListRow parent : entry.parents()) {
      line.getChildren().add(buildChip(parent));
    }
    return line;
  }

  private @NotNull Label buildChip(@NotNull final FeatureListRow row) {
    final Color color = colorAssignment.colorFor(row);
    final Label label = FxLabels.colored(new Label(chipText(row)), color);
    if (onRowClick != null) {
      label.setCursor(Cursor.HAND);
      label.setOnMouseClicked(_ -> onRowClick.accept(row));
    }
    return label;
  }

  /// Chip text: the {@link IonIdentity} ion-type string (no m/z) when present, otherwise the
  /// formatted m/z alone. Row id is the last-resort fallback for rows without either.
  private static @NotNull String chipText(@NotNull final FeatureListRow row) {
    final IonIdentity ion = row.getBestIonIdentity();
    if (ion != null) {
      return ion.getIonType().toString();
    }
    final Double mz = row.getAverageMZ();
    return mz == null ? ("row " + row.getID()) : ConfigService.getGuiFormats().mz(mz);
  }

  /// Configure a Label so it wraps inside a width-constrained parent: zero min width lets the
  /// parent shrink the label below its computed text width and the label wraps text instead.
  private static @NotNull Label configureWrap(@NotNull Label label) {
    label.setWrapText(true);
    label.setMinWidth(0);
    label.setMaxWidth(Double.MAX_VALUE);
    return label;
  }

  /// Flatten fragments + their parents into the {@code involvedRows} of the base class, preserving
  /// order (fragments come first, parents in display order, duplicates removed).
  private static @NotNull List<@NotNull FeatureListRow> flattenInvolved(
      @NotNull final List<@NotNull FragmentParents> fragments) {
    final Set<FeatureListRow> ordered = new LinkedHashSet<>();
    for (final FragmentParents entry : fragments) {
      ordered.add(entry.fragment());
    }
    for (final FragmentParents entry : fragments) {
      ordered.addAll(entry.parents());
    }
    return new ArrayList<>(ordered);
  }
}
