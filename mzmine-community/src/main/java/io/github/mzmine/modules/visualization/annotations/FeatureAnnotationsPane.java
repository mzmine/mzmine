/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.annotations;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.gui.framework.fx.SelectedRowsBinding;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shows the annotations of a row as panes in a list view. The preferred annotation comes first, all
 * other annotations are sorted by the annotation sort config of the feature list. Shows either
 * only analog matches or only the other annotations, so both can go into separate tabs.
 * <p>
 * On top, a horizontal strip of {@link AnnotationGroupCard}s groups the annotations by compound.
 * Selecting a group card only shows the annotations of that group.
 */
public class FeatureAnnotationsPane extends BorderPane implements SelectedRowsBinding {

  private static final double PANE_HEIGHT = 300d;

  private final ObjectProperty<List<FeatureListRow>> selectedRows = new SimpleObjectProperty<>(
      List.of());
  private final ObservableList<AnnotationSummary> shown = FXCollections.observableArrayList();
  private final ListView<AnnotationSummary> listView = new ListView<>(shown);
  // panes are expensive (charts), so keep them while the row is shown. Cleared on setRow
  private final Map<AnnotationSummary, Node> panes = new HashMap<>();
  private final HBox groupCards = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY);
  private final ToggleGroup groupToggle = new ToggleGroup();
  private final Label placeholder = FxLabels.newLabel("No annotations");
  private final boolean analogMatches;
  private @NotNull List<AnnotationSummary> annotations = List.of();

  /**
   * @param analogMatches true to show only analog matches, false to show all other annotations
   */
  public FeatureAnnotationsPane(boolean analogMatches) {
    this.analogMatches = analogMatches;
    final ScrollPane groupScroll = FxLayout.newScrollPane(groupCards, ScrollBarPolicy.AS_NEEDED,
        ScrollBarPolicy.NEVER);
    // fixed viewport height so the pane does not grow. Cards fill the viewport height, so they
    // shrink when the horizontal scroll bar takes space instead of being covered by it
    groupScroll.setFitToWidth(false);
    groupScroll.setFitToHeight(true);
    groupScroll.setPrefViewportHeight(AnnotationGroupCard.FULL_HEIGHT);
    // no empty space if there are no cards
    groupScroll.visibleProperty().bind(Bindings.isNotEmpty(groupCards.getChildren()));
    groupScroll.managedProperty().bind(groupScroll.visibleProperty());
    setTop(groupScroll);

    listView.setPlaceholder(placeholder);
    listView.setCellFactory(_ -> {
      final ListCell<AnnotationSummary> cell = new ListCell<>();
      // item is null for empty cells, map then returns null
      cell.graphicProperty().bind(cell.itemProperty().map(this::getPane));
      return cell;
    });
    setCenter(listView);

    selectedRows.subscribe(
        rows -> setRow(rows == null || rows.isEmpty() ? null : rows.getFirst()));
  }

  /**
   * @return the cached pane for this annotation, null if the annotation type has no pane
   */
  private @Nullable Node getPane(@NotNull AnnotationSummary summary) {
    return panes.computeIfAbsent(summary, s -> {
      // annotation is never null, see sortAnnotations
      final Node pane = s.annotation().createAnnotationPane(s.row());
      if (pane instanceof Region region) {
        // list cells only give the graphic its pref width. Fill the list, minus the scroll bar
        region.prefWidthProperty().bind(listView.widthProperty().subtract(30));
        // decision: same fixed height for all panes to show more of them
        region.setMinHeight(PANE_HEIGHT);
        region.setPrefHeight(PANE_HEIGHT);
        region.setMaxHeight(PANE_HEIGHT);
        // children with a larger fixed height (e.g. metadata boxes) would draw into the next cell
        final Rectangle clip = new Rectangle();
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
      }
      return pane;
    });
  }

  /**
   * Only the first selected row is shown, see {@link #setRow(FeatureListRow)}.
   *
   * @return the selected rows property
   */
  @Override
  public @NotNull ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return selectedRows;
  }

  /**
   * @param row the row to show annotations for, null to clear. For a {@link CompoundRow}, analog
   *            matches are taken from its preferred row
   */
  private void setRow(@Nullable FeatureListRow row) {
    // a compound row's own (possibly empty) analog matches would hide those of the preferred row
    final FeatureListRow source =
        analogMatches && row instanceof CompoundRow compoundRow ? compoundRow.getPreferredRow()
            : row;
    annotations = sortAnnotations(source, analogMatches);
    panes.clear();

    // tell the user if the row only has the annotations shown in the other pane
    final boolean otherKindOnly = annotations.isEmpty() && source != null
        && !CompoundAnnotationUtils.getAllFeatureAnnotations(source).isEmpty();
    placeholder.setText(!otherKindOnly ? "No annotations"
        : analogMatches ? "Only direct matches" : "Only analog matches");

    groupToggle.selectToggle(null);
    groupToggle.getToggles().clear();
    groupCards.getChildren().clear();
    if (!annotations.isEmpty()) {
      final Comparator<AnnotationSummary> sorter = getSorter(annotations.getFirst().row());
      // linked map keeps the group of the preferred annotation first
      final Map<String, List<AnnotationSummary>> groups = new LinkedHashMap<>();
      for (final AnnotationSummary summary : annotations) {
        groups.computeIfAbsent(CompoundAnnotationUtils.uniqueAnnotationKey(summary.annotation()),
            _ -> new ArrayList<>()).add(summary);
      }
      for (final List<AnnotationSummary> group : groups.values()) {
        final AnnotationGroupCard card = new AnnotationGroupCard(group, sorter);
        card.setToggleGroup(groupToggle);
        // on user action only, so that setRow does not build the cards twice
        card.setOnAction(_ -> showSelectedGroup());
        groupCards.getChildren().add(card);
      }
      // the preferred annotation is first, see sortAnnotations. Its group is the first card.
      // source is not null, otherwise annotations would be empty
      if (annotations.getFirst().annotation().equals(source.getPreferredAnnotation())) {
        groupToggle.selectToggle((AnnotationGroupCard) groupCards.getChildren().getFirst());
      }
    }
    showSelectedGroup();
  }

  /**
   * Shows the annotations of the selected group card, or all annotations if no card is selected.
   */
  private void showSelectedGroup() {
    shown.setAll(
        groupToggle.getSelectedToggle() instanceof AnnotationGroupCard card ? card.getAnnotations()
            : annotations);
    // new content starts at the top
    listView.scrollTo(0);
  }

  /**
   * @param row           the row, null to clear
   * @param analogMatches true to keep only analog matches, false to keep only regular annotations
   * @return the preferred annotation first, then all other annotations sorted by the annotation
   * sort config of the feature list.
   */
  static @NotNull List<AnnotationSummary> sortAnnotations(@Nullable FeatureListRow row,
      boolean analogMatches) {
    if (row == null) {
      return List.of();
    }
    final FeatureAnnotation preferred = row.getPreferredAnnotation();
    final List<AnnotationSummary> sorted = CompoundAnnotationUtils.getAllFeatureAnnotations(row)
        .stream().filter(a -> a.isAnalogMatch() == analogMatches && !a.equals(preferred))
        .map(a -> AnnotationSummary.of(row, a)).sorted(getSorter(row)).toList();

    // decision: the preferred annotation is only shown in the pane of its kind (analog or not)
    return preferred != null && preferred.isAnalogMatch() == analogMatches ? Stream.concat(
        Stream.of(AnnotationSummary.of(row, preferred)), sorted.stream()).toList() : sorted;
  }

  private static @NotNull Comparator<AnnotationSummary> getSorter(@NotNull FeatureListRow row) {
    return row.getFeatureList().getAnnotationSortConfig().sortOrder().getComparatorHighFirst();
  }
}
