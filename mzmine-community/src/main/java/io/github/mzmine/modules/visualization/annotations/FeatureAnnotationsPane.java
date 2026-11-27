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

import io.github.mzmine.datamodel.features.DataTypeValueChangeListener;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.PreferredAnnotationType;
import io.github.mzmine.gui.framework.fx.SelectedRowsBinding;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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
 * Shows annotations as panes in a list view, in the given order. On top, a horizontal strip of
 * {@link AnnotationGroupCard}s groups the annotations by compound. Selecting a group card only
 * shows the annotations of that group.
 * <p>
 * Create it with {@link #forSelectedRows(SelectedRowsBinding, boolean)} to show the annotations of
 * the first selected row, or with {@link #forAnnotations(ObservableList)} for any list of
 * annotations.
 */
public class FeatureAnnotationsPane extends BorderPane {

  private static final double PANE_HEIGHT = 350d;
  // changes of these types of the shown row reload the annotations
  private static final List<DataType> WATCHED_TYPES = Stream.concat(
      CompoundAnnotationUtils.annotationTypePriority.stream(),
      Stream.of(DataTypes.get(PreferredAnnotationType.class))).toList();

  // the shown annotations in their order. A group card selects a subset
  private final ObservableList<AnnotationSummary> annotations;
  private final ObservableList<AnnotationSummary> shown = FXCollections.observableArrayList();
  private final ListView<AnnotationSummary> listView = new ListView<>(shown);
  // panes are expensive (charts), so keep them while the annotations are shown
  private final Map<AnnotationSummary, Node> panes = new HashMap<>();
  private final HBox groupCards = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY);
  private final ToggleGroup groupToggle = new ToggleGroup();
  // true while showAnnotations rebuilds the cards, it shows the selected group once at the end
  private boolean rebuildingCards = false;
  private final Label placeholder = FxLabels.newLabel("No annotations");

  /**
   * Shows the annotations of the first selected row. The preferred annotation comes first, all
   * other annotations are sorted by the annotation sort config of the feature list.
   *
   * @param selectedRows  provides the selected rows
   * @param analogMatches true to show only analog matches, false to show all other annotations. For
   *                      a {@link CompoundRow}, analog matches are taken from its preferred row
   * @return the pane
   */
  public static @NotNull FeatureAnnotationsPane forSelectedRows(
      @NotNull SelectedRowsBinding selectedRows, boolean analogMatches) {
    final FeatureAnnotationsPane pane = new FeatureAnnotationsPane(
        FXCollections.observableArrayList());
    final ObservableValue<FeatureListRow> row = selectedRows.selectedRowsProperty()
        .map(rows -> rows.isEmpty() ? null : rows.getFirst());
    row.subscribe(r -> pane.setRow(r, analogMatches));

    // reload when the annotations of the row change, e.g. when they are removed in the feature
    // table. Only while the pane is in a scene, so the feature list does not keep a closed pane
    final AtomicReference<Runnable> removeListener = new AtomicReference<>(() -> {
    });
    pane.sceneProperty().flatMap(_ -> row).subscribe(watched -> {
      removeListener.getAndSet(() -> {
      }).run();
      final FeatureListRow source = getSourceRow(watched, analogMatches);
      if (source != null) {
        removeListener.set(
            addAnnotationListener(source, () -> pane.setRow(watched, analogMatches)));
      }
    });
    return pane;
  }

  /**
   * Calls onChange on the FX thread when an annotation type of the row changes. Multiple changes in
   * a row, e.g. clearing all annotations, only call it once.
   *
   * @param row      the row to watch
   * @param onChange called on the FX thread
   * @return removes the listener
   */
  private static @NotNull Runnable addAnnotationListener(@NotNull FeatureListRow row,
      @NotNull Runnable onChange) {
    final AtomicBoolean scheduled = new AtomicBoolean(false);
    final DataTypeValueChangeListener<Object> listener = (model, _, _, _) -> {
      // the listener is registered on the schema, so it is called for all rows
      if (model == row && scheduled.compareAndSet(false, true)) {
        FxThread.runLater(() -> {
          scheduled.set(false);
          onChange.run();
        });
      }
    };
    for (final DataType type : WATCHED_TYPES) {
      // compound rows store their values in the compound row schema, not in the feature list
      if (row instanceof CompoundRow compoundRow) {
        compoundRow.getCompoundList().getCompoundRowSchema()
            .addDataTypeValueChangeListener(type, listener);
      } else {
        row.getFeatureList().addRowTypeValueListener(type, listener);
      }
    }
    return () -> {
      for (final DataType type : WATCHED_TYPES) {
        if (row instanceof CompoundRow compoundRow) {
          compoundRow.getCompoundList().getCompoundRowSchema()
              .removeDataTypeValueChangeListener(type, listener);
        } else {
          row.getFeatureList().removeRowTypeValueListener(type, listener);
        }
      }
    };
  }

  /**
   * @param annotations the annotations to show in this order. Update it with
   *                    {@link ObservableList#setAll}, every change rebuilds all group cards
   * @return the pane
   */
  public static @NotNull FeatureAnnotationsPane forAnnotations(
      @NotNull ObservableList<AnnotationSummary> annotations) {
    return new FeatureAnnotationsPane(annotations);
  }

  private FeatureAnnotationsPane(@NotNull ObservableList<AnnotationSummary> annotations) {
    this.annotations = annotations;
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

    // not onAction: arrow keys change the selected card without firing it
    groupToggle.selectedToggleProperty().subscribe(_ -> {
      if (!rebuildingCards) {
        showSelectedGroup();
      }
    });
    annotations.addListener((ListChangeListener<AnnotationSummary>) _ -> showAnnotations());
    showAnnotations();
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
        // a selected cell sets -fx-background to the selection color, labels derive their text
        // color from it and turn dark in dark themes. Keep the unselected background for the pane
        region.setStyle("-fx-background: -fx-control-inner-background;");
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
   * @param row           the row to show annotations for, null to clear
   * @param analogMatches true to show only analog matches, see
   *                      {@link #forSelectedRows(SelectedRowsBinding, boolean)}
   */
  private void setRow(@Nullable FeatureListRow row, boolean analogMatches) {
    setPlaceholderText(getEmptyText(row, analogMatches));
    annotations.setAll(getRowAnnotations(row, analogMatches));
  }

  /**
   * @param text the text shown when there are no annotations
   */
  public void setPlaceholderText(@NotNull String text) {
    placeholder.setText(text);
  }

  /**
   * The text for an empty pane. Only shown if the row has no annotations of the shown kind, so it
   * tells the user if the row only has the annotations of the other kind.
   *
   * @param row           the shown row, null if none
   * @param analogMatches true if the pane shows analog matches
   * @return the placeholder text
   */
  public static @NotNull String getEmptyText(@Nullable FeatureListRow row, boolean analogMatches) {
    if (row == null || CompoundAnnotationUtils.getAllFeatureAnnotations(row).isEmpty()) {
      return "No annotations";
    }
    return analogMatches ? "Only direct matches" : "Only analog matches";
  }

  /**
   * Rebuilds the group cards and preselects the card of the preferred annotation.
   */
  private void showAnnotations() {
    rebuildingCards = true;
    panes.clear();
    groupToggle.selectToggle(null);
    groupToggle.getToggles().clear();
    groupCards.getChildren().clear();
    if (!annotations.isEmpty()) {
      final Comparator<AnnotationSummary> sorter = getSorter(annotations.getFirst().row());
      // linked map keeps the group of the first (preferred) annotation first
      final Map<String, List<AnnotationSummary>> groups = new LinkedHashMap<>();
      for (final AnnotationSummary summary : annotations) {
        groups.computeIfAbsent(CompoundAnnotationUtils.uniqueAnnotationKey(summary.annotation()),
            _ -> new ArrayList<>()).add(summary);
      }
      for (final List<AnnotationSummary> group : groups.values()) {
        final AnnotationGroupCard card = new AnnotationGroupCard(group, sorter);
        card.setToggleGroup(groupToggle);
        groupCards.getChildren().add(card);
      }
      // the first group holds the first annotation. Preselect it if it is the preferred one
      final AnnotationSummary first = annotations.getFirst();
      if (first.annotation().equals(first.row().getPreferredAnnotation())) {
        groupToggle.selectToggle((AnnotationGroupCard) groupCards.getChildren().getFirst());
      }
    }
    rebuildingCards = false;
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
   * @param analogMatches true to keep only analog matches, false to keep only regular annotations.
   *                      For a {@link CompoundRow}, analog matches are taken from its preferred
   *                      row
   * @return the preferred annotation first, then all other annotations sorted by the annotation
   * sort config of the feature list.
   */
  public static @NotNull List<AnnotationSummary> getRowAnnotations(@Nullable FeatureListRow row,
      boolean analogMatches) {
    final FeatureListRow source = getSourceRow(row, analogMatches);
    if (source == null) {
      return List.of();
    }
    final FeatureAnnotation preferred = source.getPreferredAnnotation();
    final Stream<AnnotationSummary> sorted = CompoundAnnotationUtils.getAllFeatureAnnotations(
            source)
        .stream().filter(a -> a.isAnalogMatch() == analogMatches && !a.equals(preferred))
        .map(a -> AnnotationSummary.of(source, a)).sorted(getSorter(source));

    // decision: the preferred annotation is only shown in the pane of its kind (analog or not)
    return preferred != null && preferred.isAnalogMatch() == analogMatches ? Stream.concat(
        Stream.of(AnnotationSummary.of(source, preferred)), sorted).toList() : sorted.toList();
  }

  /**
   * @param row           the selected row
   * @param analogMatches true if analog matches are shown
   * @return the row that holds the shown annotations
   */
  private static @Nullable FeatureListRow getSourceRow(@Nullable FeatureListRow row,
      boolean analogMatches) {
    // a compound row's own (possibly empty) analog matches would hide those of the preferred row
    return analogMatches && row instanceof CompoundRow compoundRow ? compoundRow.getPreferredRow()
        : row;
  }

  /**
   * @param row a row of the feature list
   * @return the annotation sort order of the feature list, best first
   */
  public static @NotNull Comparator<AnnotationSummary> getSorter(@NotNull FeatureListRow row) {
    return row.getFeatureList().getAnnotationSortConfig().sortOrder().getComparatorHighFirst();
  }
}
