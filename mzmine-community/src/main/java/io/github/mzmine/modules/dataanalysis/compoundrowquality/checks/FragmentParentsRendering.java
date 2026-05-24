package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Shared rendering helpers for "fragment ← parents" quality check results
/// ({@link ImsFragmentationQualityResult}, {@link InSourceFragmentationQualityResult}).
/// <p>
/// Centralizes the chip label format, per-row coloring lookup, click wiring, and the parent display
/// ordering so every fragment-style check renders the same way.
public final class FragmentParentsRendering {

  /// Parent display order: ion-identified rows first, then by ascending m/z. Boolean {@code false}
  /// sorts before {@code true}, so {@code hasIonIdentity == false} for rows without an ion keeps
  /// ion rows ahead of unknowns.
  public static final Comparator<FeatureListRow> PARENT_ORDER = Comparator //
      .comparing((FeatureListRow r) -> r.getBestIonIdentity() == null) //
      .thenComparingDouble(FeatureListRow::getAverageMZ);

  private FragmentParentsRendering() {
  }

  /// Build one "{@code [fragment] ← [parent] [parent] …}" line. The arrow + parent chips are only
  /// added when at least one parent is present, so rows without an identifiable parent (e.g. an
  /// in-source fragment that was tagged upstream but whose precursor MS2 hit can't be located in
  /// this compound) render as a standalone fragment chip.
  public static @NotNull FlowPane buildFragmentLine(@NotNull final FragmentParents entry,
      @NotNull final ColorAssignment colorAssignment,
      @Nullable final Consumer<@NotNull FeatureListRow> onRowClick) {
    final FlowPane line = FxLayout.newFlowPane();
    line.setPadding(Insets.EMPTY);
    line.setMinWidth(0);
    line.getChildren().add(buildChip(entry.fragment(), colorAssignment, onRowClick));
    if (!entry.parents().isEmpty()) {
      line.getChildren().add(new Label("←"));
      for (final FeatureListRow parent : entry.parents()) {
        line.getChildren().add(buildChip(parent, colorAssignment, onRowClick));
      }
    }
    return line;
  }

  /// Build a single colored, optionally-clickable chip Label for a row using
  /// {@link #chipText(FeatureListRow)} for the label text. Colors are looked up via
  /// {@link ColorAssignment#colorFor(FeatureListRow)} so chips match the host dashboard's per-row
  /// coloring.
  public static @NotNull Label buildChip(@NotNull final FeatureListRow row,
      @NotNull final ColorAssignment colorAssignment,
      @Nullable final Consumer<@NotNull FeatureListRow> onRowClick) {
    return buildChip(row, chipText(row), colorAssignment, onRowClick);
  }

  /// Build a single colored, optionally-clickable chip Label with caller-supplied text. Same
  /// coloring + click behaviour as {@link #buildChip(FeatureListRow, ColorAssignment, Consumer)};
  /// use this overload when the chip text needs a different format than the default ion-or-m/z.
  public static @NotNull Label buildChip(@NotNull final FeatureListRow row,
      @NotNull final String text, @NotNull final ColorAssignment colorAssignment,
      @Nullable final Consumer<@NotNull FeatureListRow> onRowClick) {
    final Color color = colorAssignment.colorFor(row);
    final Label label = FxLabels.colored(new Label(text), color);
    if (onRowClick != null) {
      label.setCursor(Cursor.HAND);
      label.setOnMouseClicked(_ -> onRowClick.accept(row));
    }
    return label;
  }

  /// Chip text: the {@link IonIdentity} ion-type string (no m/z) when present, otherwise the
  /// formatted m/z alone. Row id is the last-resort fallback for rows without either.
  public static @NotNull String chipText(@NotNull final FeatureListRow row) {
    final IonIdentity ion = row.getBestIonIdentity();
    if (ion != null) {
      return ion.getIonType().toString();
    }
    final Double mz = row.getAverageMZ();
    return mz == null ? ("row " + row.getID()) : ConfigService.getGuiFormats().mz(mz);
  }

  /// Configure a Label so it wraps inside a width-constrained parent: zero min width lets the
  /// parent shrink the label below its computed text width and the label wraps text instead.
  public static @NotNull Label configureWrap(@NotNull final Label label) {
    label.setWrapText(true);
    label.setMinWidth(0);
    label.setMaxWidth(Double.MAX_VALUE);
    return label;
  }

  /// Flatten fragments + their parents into a single ordered list, preserving order (fragments
  /// first, parents next in display order, duplicates removed). Used by result classes to expose a
  /// flat {@code involvedRows} list to the base {@code QualityCheckResult}.
  public static @NotNull List<@NotNull FeatureListRow> flattenInvolved(
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
