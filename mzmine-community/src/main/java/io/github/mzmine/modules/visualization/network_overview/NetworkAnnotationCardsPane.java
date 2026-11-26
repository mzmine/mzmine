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

package io.github.mzmine.modules.visualization.network_overview;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.spectraldb.entry.AnalogCompoundGroup.RowAnnotation;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;

/**
 * Shows all annotations of the selected rows as cards, one per grid row, paged by
 * {@link #PAGE_SIZE}. The preferred annotation of each row comes first, all other annotations are
 * sorted by the annotation sort config of the feature list. Analog matches are excluded, they are
 * shown in their own tab.
 */
public class NetworkAnnotationCardsPane extends BorderPane {

  private static final int PAGE_SIZE = 15;

  private final GridPane grid = FxLayout.newGrid1ColFillW(List.of());
  private final ScrollPane scroll = FxLayout.newScrollPane(grid);
  private final Label lblPage = FxLabels.newLabel("");
  private @NotNull List<AnnotationSummary> annotations = List.of();
  private int pageStart = 0;

  public NetworkAnnotationCardsPane() {
    setTop(FxLayout.newHBox(Pos.CENTER_LEFT,
        FxButtons.createButton("<<", () -> showPage(pageStart - PAGE_SIZE)),
        FxButtons.createButton(">>", () -> showPage(pageStart + PAGE_SIZE)), lblPage));
    // cards keep their pref height, fitting to the viewport squeezes them and they overlap
    scroll.setFitToHeight(false);
    setCenter(scroll);
  }

  /**
   * @param rows       the selected rows
   * @param additional annotations from other sources, e.g., selected analog compound nodes
   */
  public void setAnnotations(@NotNull List<FeatureListRow> rows,
      @NotNull List<RowAnnotation> additional) {
    annotations = sortAnnotations(rows, additional);
    showPage(0);
  }

  private void showPage(final int start) {
    pageStart = Math.clamp(start, 0, Math.max(0, annotations.size() - 1) / PAGE_SIZE * PAGE_SIZE);
    grid.getChildren().clear();

    final List<AnnotationSummary> page = annotations.subList(pageStart,
        Math.min(pageStart + PAGE_SIZE, annotations.size()));
    int gridRow = 0;
    for (final AnnotationSummary summary : page) {
      // annotation is never null, see sortAnnotations
      final Node pane = summary.annotation().createAnnotationPane(summary.row());
      if (pane != null) {
        grid.add(pane, 0, gridRow++);
      }
    }

    // new content starts at the top, otherwise the first cards of the next page are skipped
    scroll.setVvalue(scroll.getVmin());

    lblPage.setText(annotations.isEmpty() ? "No annotations"
        : "(%d-%d of %d)".formatted(pageStart + 1, pageStart + page.size(), annotations.size()));
  }

  /**
   * @return the preferred annotation of each row first, then all other annotations sorted by the
   * annotation sort config of the feature list. Analog matches and duplicates are removed.
   */
  static @NotNull List<AnnotationSummary> sortAnnotations(@NotNull List<FeatureListRow> rows,
      @NotNull List<RowAnnotation> additional) {
    // linked maps to dedupe annotations, e.g., the preferred annotation is also in the match lists
    final Map<FeatureAnnotation, AnnotationSummary> preferred = new LinkedHashMap<>();
    final Map<FeatureAnnotation, AnnotationSummary> others = new LinkedHashMap<>();

    for (final FeatureListRow row : rows) {
      final FeatureAnnotation pref = row.getPreferredAnnotation();
      // decision: analog matches have their own tab, also when set as preferred
      if (pref != null && !pref.isAnalogMatch()) {
        preferred.put(pref, AnnotationSummary.of(row, pref));
      }
      for (final FeatureAnnotation a : CompoundAnnotationUtils.getAllFeatureAnnotations(row)) {
        if (!a.isAnalogMatch()) {
          others.putIfAbsent(a, AnnotationSummary.of(row, a));
        }
      }
    }
    for (final RowAnnotation ra : additional) {
      if (!ra.annotation().isAnalogMatch()) {
        others.putIfAbsent(ra.annotation(), AnnotationSummary.of(ra.row(), ra.annotation()));
      }
    }
    others.keySet().removeAll(preferred.keySet());

    final AnnotationSummary any = Stream.concat(preferred.values().stream(),
        others.values().stream()).findFirst().orElse(null);
    if (any == null) {
      return List.of();
    }
    // assumption: all rows in one network belong to the same feature list
    final Comparator<AnnotationSummary> sorter = any.row().getFeatureList()
        .getAnnotationSortConfig().sortOrder().getComparatorHighFirst();
    return Stream.concat(preferred.values().stream().sorted(sorter),
        others.values().stream().sorted(sorter)).toList();
  }
}
