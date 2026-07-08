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
import java.util.Objects;
import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
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
/// Centralizes the chip label format, per-row coloring lookup, click + selection-style wiring,
/// and the parent display ordering so every fragment-style check renders the same way.
public final class FragmentParentsRendering {

  /// CSS class used to style chips whose row is currently selected.
  private static final String SELECTED_STYLE_CLASS = "bold-label";

  /// Key under which the strong reference to a chip's selection-{@link ChangeListener} is stored on
  /// the {@link Label}'s {@code getProperties()} map. The listener is then registered on the
  /// selection property via a {@link WeakChangeListener} — the strong reference lives only as long
  /// as the chip itself, so the chip and its listener are eligible for GC together once the chip
  /// leaves the scene.
  private static final String LISTENER_PROPERTY_KEY = "qc.selectedMemberRowListener";

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
      @Nullable final ObjectProperty<@Nullable FeatureListRow> selectedMemberRow) {
    final FlowPane line = FxLayout.newFlowPane();
    line.setPadding(Insets.EMPTY);
    line.setMinWidth(0);
    line.getChildren().add(buildChip(entry.fragment(), colorAssignment, selectedMemberRow));
    if (!entry.parents().isEmpty()) {
      line.getChildren().add(new Label("←"));
      for (final FeatureListRow parent : entry.parents()) {
        line.getChildren().add(buildChip(parent, colorAssignment, selectedMemberRow));
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
      @Nullable final ObjectProperty<@Nullable FeatureListRow> selectedMemberRow) {
    return buildChip(row, chipText(row), colorAssignment, selectedMemberRow);
  }

  /// Build a single colored, optionally-clickable chip Label with caller-supplied text. The chip
  /// writes {@code row} to {@code selectedMemberRow} on click and listens to that property so it
  /// can switch the bold style class on and off when the selection matches its row.
  public static @NotNull Label buildChip(@NotNull final FeatureListRow row,
      @NotNull final String text, @NotNull final ColorAssignment colorAssignment,
      @Nullable final ObjectProperty<@Nullable FeatureListRow> selectedMemberRow) {
    final Color color = colorAssignment.colorFor(row);
    final Label label = FxLabels.colored(new Label(text), color);
    if (selectedMemberRow != null) {
      label.setCursor(Cursor.HAND);
      label.setOnMouseClicked(_ -> selectedMemberRow.setValue(row));
      bindSelectionBold(label, row, selectedMemberRow);
    }
    return label;
  }

  /// Wire {@code label}'s bold-style class to whether {@code selectedMemberRow}'s value equals
  /// {@code row}. Uses a {@link WeakChangeListener} so the property doesn't keep the label (and
  /// thus the surrounding check result) alive after it has left the scene. The strong reference to
  /// the underlying listener lives only on the label's properties map; once the label is
  /// unreachable both the strong listener and the WeakChangeListener are collected and the listener
  /// is dropped from the property on its next invocation.
  public static void bindSelectionBold(@NotNull final Label label,
      @NotNull final FeatureListRow row,
      @NotNull final ObservableValue<@Nullable FeatureListRow> selectedMemberRow) {
    applyBoldStyle(label, Objects.equals(selectedMemberRow.getValue(), row));
    final ChangeListener<FeatureListRow> listener = (obs, was, is) -> applyBoldStyle(label,
        Objects.equals(is, row));
    // Strong ref via the label's properties map — GC'd together with the label.
    label.getProperties().put(LISTENER_PROPERTY_KEY, listener);
    selectedMemberRow.addListener(new WeakChangeListener<>(listener));
  }

  private static void applyBoldStyle(@NotNull final Label label, final boolean selected) {
    final ObservableList<String> classes = label.getStyleClass();
    if (selected) {
      if (!classes.contains(SELECTED_STYLE_CLASS)) {
        classes.add(SELECTED_STYLE_CLASS);
      }
    } else {
      classes.remove(SELECTED_STYLE_CLASS);
    }
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
