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

package io.github.mzmine.modules.visualization.dash_lipidqc;

import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import javafx.scene.layout.Priority;

/**
 * Factory for building the standard grid layout used in the lipid annotation QC dashboard,
 * wrapping panes in titled subsections and arranging them in a six-cell grid.
 */
final class DashboardLayoutFactory {

  private DashboardLayoutFactory() {
  }

  static @NotNull BorderPane wrapInSubsection(final @NotNull String title,
      final @NotNull Region content) {
    final BorderPane pane = new BorderPane(content);
    final Label titleLabel = new Label(title);
    titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 4 6 4 6;");
    pane.setTop(titleLabel);
    pane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    VBox.setVgrow(pane, Priority.ALWAYS);
    return pane;
  }

  static @NotNull Region createSixPaneLayout(final @NotNull Region summaryPane,
      final @NotNull Region kendrickPane, final @NotNull Region qualityPane,
      final @NotNull Region retentionPane, final @NotNull Region matchedSignalsPane,
      final @NotNull Region isotopePane) {
    final GridPane grid = new GridPane();
    grid.setHgap(8);
    grid.setVgap(8);

    for (int col = 0; col < 3; col++) {
      final ColumnConstraints constraints = new ColumnConstraints();
      constraints.setPercentWidth(100d / 3d);
      constraints.setHgrow(Priority.ALWAYS);
      grid.getColumnConstraints().add(constraints);
    }
    for (int row = 0; row < 2; row++) {
      final RowConstraints constraints = new RowConstraints();
      constraints.setPercentHeight(50d);
      constraints.setVgrow(Priority.ALWAYS);
      grid.getRowConstraints().add(constraints);
    }

    final List<Region> panes = List.of(summaryPane, kendrickPane, qualityPane, retentionPane,
        matchedSignalsPane, isotopePane);
    for (final Region pane : panes) {
      pane.setMinSize(260, 180);
      pane.setPrefSize(360, 250);
      pane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }
    retentionPane.setMinHeight(120);

    grid.add(summaryPane, 0, 0);
    grid.add(kendrickPane, 0, 1);
    grid.add(qualityPane, 1, 0);
    grid.add(retentionPane, 1, 1);
    grid.add(matchedSignalsPane, 2, 0);
    grid.add(isotopePane, 2, 1);

    GridPane.setHgrow(summaryPane, Priority.ALWAYS);
    GridPane.setVgrow(summaryPane, Priority.ALWAYS);
    GridPane.setHgrow(kendrickPane, Priority.ALWAYS);
    GridPane.setVgrow(kendrickPane, Priority.ALWAYS);
    GridPane.setHgrow(qualityPane, Priority.ALWAYS);
    GridPane.setVgrow(qualityPane, Priority.ALWAYS);
    GridPane.setHgrow(retentionPane, Priority.ALWAYS);
    GridPane.setVgrow(retentionPane, Priority.ALWAYS);
    GridPane.setHgrow(matchedSignalsPane, Priority.ALWAYS);
    GridPane.setVgrow(matchedSignalsPane, Priority.ALWAYS);
    GridPane.setHgrow(isotopePane, Priority.ALWAYS);
    GridPane.setVgrow(isotopePane, Priority.ALWAYS);
    return grid;
  }
}
