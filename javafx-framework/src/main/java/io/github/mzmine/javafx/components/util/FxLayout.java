/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.javafx.components.util;

import java.util.List;
import java.util.stream.IntStream;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.controlsfx.tools.Borders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FxLayout {

  public static final int DEFAULT_SPACE = 5;
  public static final int DEFAULT_ICON_SPACE = 0;
  public static final Insets DEFAULT_PADDING_INSETS = new Insets(5);

  /**
   * useful for debugging purposes
   */
  public static Border newRedBorder() {
    return new Border(
        new BorderStroke(javafx.scene.paint.Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
            BorderStroke.THIN));
  }

  public static FlowPane newIconPane(Orientation orientation, Node... children) {
    var alignment = orientation == Orientation.HORIZONTAL ? Pos.CENTER_LEFT : Pos.TOP_CENTER;
    var pane = newFlowPane(alignment, Insets.EMPTY, children);
    pane.setOrientation(orientation);
    pane.setHgap(DEFAULT_ICON_SPACE);
    pane.setVgap(DEFAULT_ICON_SPACE);
    return pane;
  }

  public static FlowPane newFlowPane() {
    return new FlowPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);
  }

  public static FlowPane newFlowPane(Node... children) {
    return newFlowPane(DEFAULT_PADDING_INSETS, children);
  }

  public static FlowPane newFlowPane(Pos alignment, Node... children) {
    return newFlowPane(alignment, DEFAULT_PADDING_INSETS, children);
  }

  public static FlowPane newFlowPane(Insets padding, Node... children) {
    return newFlowPane(Pos.CENTER_LEFT, padding, children);
  }

  public static FlowPane newFlowPane(Pos alignment, Insets padding, Node... children) {
    var pane = new FlowPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE, children);
    pane.setPadding(padding);
    pane.setAlignment(alignment);
    return pane;
  }

  public static VBox newVBox(Node... children) {
    return newVBox(DEFAULT_PADDING_INSETS, children);
  }

  public static VBox newVBox(Pos alignment, Node... children) {
    return newVBox(alignment, DEFAULT_PADDING_INSETS, children);
  }

  public static VBox newVBox(Insets padding, Node... children) {
    return newVBox(Pos.CENTER_LEFT, padding, children);
  }

  public static VBox newVBox(Pos alignment, Insets padding, Node... children) {
    return newVBox(alignment, padding, false, children);
  }

  public static VBox newVBox(Pos alignment, Insets padding, boolean fillWidth, Node... children) {
    var pane = new VBox(DEFAULT_SPACE, children);
    pane.setPadding(padding);
    pane.setAlignment(alignment);
    pane.setFillWidth(fillWidth);
    return pane;
  }

  public static HBox newHBox(Node... children) {
    return newHBox(DEFAULT_PADDING_INSETS, children);
  }

  public static HBox newHBox(Pos alignment, Node... children) {
    return newHBox(alignment, DEFAULT_PADDING_INSETS, children);
  }

  public static HBox newHBox(Insets padding, Node... children) {
    return newHBox(Pos.CENTER_LEFT, padding, children);
  }

  public static HBox newHBox(Pos alignment, Insets padding, Node... children) {
    return newHBox(alignment, padding, DEFAULT_SPACE, children);
  }

  public static HBox newHBox(Pos alignment, Insets padding, int spacing, Node... children) {
    var pane = new HBox(spacing, children);
    pane.setAlignment(alignment);
    pane.setPadding(padding);
    return pane;
  }

  public static StackPane newStackPane(Node... children) {
    return newStackPane(DEFAULT_PADDING_INSETS, children);
  }

  public static StackPane newStackPane(Insets padding, Node... children) {
    var pane = new StackPane(children);
    pane.setPadding(padding);
    return pane;
  }

  public static BorderPane newBorderPane(Node center) {
    return newBorderPane(DEFAULT_PADDING_INSETS, center);
  }

  public static BorderPane newBorderPane(Insets padding, Node center) {
    var pane = new BorderPane(center);
    pane.setPadding(padding);
    return pane;
  }

  public static void centerAllNodesHorizontally(GridPane pane) {
    pane.getChildren().forEach(node -> GridPane.setHalignment(node, HPos.CENTER));
  }

  public static void centerAllNodesVertically(GridPane pane) {
    pane.getChildren().forEach(node -> GridPane.setValignment(node, VPos.CENTER));
  }

  public static Node wrapInBorder(Node node) {
    return Borders.wrap(node).lineBorder().radius(FxLayout.DEFAULT_SPACE)
        .innerPadding(FxLayout.DEFAULT_SPACE).outerPadding(FxLayout.DEFAULT_SPACE).buildAll();
  }

  public static ScrollPane newScrollPane(final Node root) {
    return newScrollPane(root, null, null);
  }

  public static ScrollPane newScrollPane(final Node root, @Nullable ScrollBarPolicy hBarPolicy,
      @Nullable ScrollBarPolicy vBarPolicy) {
    ScrollPane scroll = new ScrollPane(root);
    scroll.setFitToWidth(true);
    scroll.setFitToHeight(true);
    scroll.setCenterShape(true);
    if (hBarPolicy != null) {
      scroll.setHbarPolicy(hBarPolicy);
    }
    if (vBarPolicy != null) {
      scroll.setVbarPolicy(vBarPolicy);
    }
    return scroll;
  }

  public static TitledPane newTitledPane(String title, Node node) {
    return new TitledPane(title, node);
  }

  public static Accordion newAccordion(TitledPane... panes) {
    return new Accordion(panes);
  }

  public static Accordion newAccordion(TitledPane expandedPane, @NotNull TitledPane... panes) {
    final Accordion accordion = newAccordion(panes);
    if (!accordion.getPanes().contains(expandedPane)) {
      accordion.getPanes().add(expandedPane);
    }
    accordion.setExpandedPane(expandedPane);
    return accordion;
  }

  public static Accordion newAccordion(boolean expandFirst, TitledPane... panes) {
    if (expandFirst && panes.length > 0) {
      var first = panes[0];
      return newAccordion(first, panes);
    }
    return newAccordion(panes);
  }

  /**
   * Adding an empty ColumnConstraints object for column2 has the effect of not setting any
   * constraints, leaving the GridPane to compute the column's layout based solely on its content's
   * size preferences and constraints.
   */
  public static GridPane newGrid2Col(final Node... children) {
    return newGrid2Col(DEFAULT_PADDING_INSETS, children);
  }

  /**
   * Adding an empty ColumnConstraints object for column2 has the effect of not setting any
   * constraints, leaving the GridPane to compute the column's layout based solely on its content's
   * size preferences and constraints.
   */
  public static GridPane newGrid2Col(Insets padding, final Node... children) {
    return newGrid2Col(GridColumnGrow.RIGHT, padding, children);
  }

  public static GridPane newGrid2Col(@NotNull GridColumnGrow grow, Insets padding,
      final Node... children) {
    return newGrid2Col(grow, padding, DEFAULT_SPACE, children);
  }

  public static GridPane newGrid2Col(@NotNull GridColumnGrow grow, Insets padding, int space,
      final Node... children) {
    var grid = new GridPane(space, space);
    grid.setPadding(padding);

    ColumnConstraints column1 = new ColumnConstraints();
    ColumnConstraints column2 = new ColumnConstraints();
    switch (grow) {
      case BOTH -> setGrowColumn(column1, column2);
      case LEFT -> setGrowColumn(column1);
      case RIGHT -> setGrowColumn(column2);
    }
    grid.getColumnConstraints().addAll(column1, column2);
    var row = new RowConstraints();
    row.setValignment(VPos.CENTER);
    grid.getRowConstraints().add(row);

    for (int i = 0; i < children.length; i += 2) {
      grid.add(children[i], 0, i / 2);
      if (i + 1 < children.length) {
        grid.add(children[i + 1], 1, i / 2);
      }
    }
    return grid;
  }

  public static void setGrowColumn(final ColumnConstraints... columns) {
    for (final ColumnConstraints column : columns) {
      column.setFillWidth(true);
      column.setHgrow(Priority.ALWAYS);
    }
  }

  public enum GridColumnGrow {
    LEFT, RIGHT, BOTH, NONE
  }

  public static ColumnConstraints newFillWidthColumn() {
    final ColumnConstraints cc = new ColumnConstraints();
    setGrowColumn(cc);
    return cc;
  }

  public static List<ColumnConstraints> newFillWidthColumns(int numColumns) {
    return IntStream.range(0, numColumns).mapToObj(i -> newFillWidthColumn()).toList();
  }

  public static RowConstraints newFillHeightRow() {
    final RowConstraints rc = new RowConstraints();
    setFillHeightRow(rc);
    return rc;
  }

  public static List<RowConstraints> newFillHeightRows(int numRows) {
    return IntStream.range(0, numRows).mapToObj(i -> newFillHeightRow()).toList();
  }

  private static void setFillHeightRow(RowConstraints rc) {
    rc.setFillHeight(true);
    rc.setVgrow(Priority.ALWAYS);
  }
}
