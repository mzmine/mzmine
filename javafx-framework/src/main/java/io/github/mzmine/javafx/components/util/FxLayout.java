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

import io.github.mzmine.javafx.components.GridRow;
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
import javafx.scene.control.Separator;
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

  public static HBox newHBox(int spacing, Node... children) {
    return newHBox(Pos.CENTER_LEFT, DEFAULT_PADDING_INSETS, spacing, children);
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

  public static void applyDefaults(VBox pane, Insets padding) {
    apply(pane, DEFAULT_SPACE, padding, Pos.CENTER_LEFT);
  }

  public static void applyDefaults(HBox pane, Insets padding) {
    apply(pane, DEFAULT_SPACE, padding, Pos.CENTER_LEFT);
  }

  public static void applyDefaults(FlowPane pane, Insets padding) {
    apply(pane, DEFAULT_SPACE, DEFAULT_SPACE, padding, Pos.CENTER_LEFT);
  }

  public static void apply(VBox pane, int space, Insets padding, Pos pos) {
    pane.setPadding(padding);
    pane.setSpacing(space);
    pane.setAlignment(pos);
  }

  public static void apply(HBox pane, int space, Insets padding, Pos pos) {
    pane.setPadding(padding);
    pane.setSpacing(space);
    pane.setAlignment(pos);
  }

  public static void apply(FlowPane pane, int vGap, int hGap, Insets padding, Pos pos) {
    pane.setPadding(padding);
    pane.setVgap(vGap);
    pane.setHgap(hGap);
    pane.setAlignment(pos);
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

  public static BorderPaneBuilder newBorderPane() {
    return new BorderPaneBuilder();
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

  /**
   * A non animated pane
   */
  public static TitledPane newTitledPane(String title, Node node) {
    return newTitledPane(title, node, false);
  }

  public static TitledPane newTitledPane(String title, Node node, boolean animated) {
    final TitledPane pane = new TitledPane(title, node);
    // default disable animation - slows down when plots are shown with many data points
    pane.setAnimated(animated);
    return pane;
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
    return applyGrid2Col(grid, grow, padding, space, children);
  }

  public static GridPane applyGrid2Col(@NotNull GridPane grid, final Node... children) {
    // added more spacing, because validation overlaps with other components and takes away fokus
    // like a text box is wider then and a spinner on top is hard to control with default spacing
    return applyGrid2Col(grid, GridColumnGrow.RIGHT, DEFAULT_PADDING_INSETS, DEFAULT_SPACE * 1.85,
        children);
  }

  public static GridPane applyGrid2Col(@NotNull GridPane grid, @NotNull GridColumnGrow grow,
      Insets padding, double space, final Node... children) {
    grid.setPadding(padding);
    grid.setVgap(space);
    grid.setHgap(space);

    ColumnConstraints column1 = new ColumnConstraints();
    column1.setHgrow(Priority.NEVER);
    column1.setHalignment(HPos.RIGHT);
    ColumnConstraints column2 = new ColumnConstraints();
    switch (grow) {
      case BOTH -> setGrowColumn(column1, column2);
      case LEFT -> setGrowColumn(column1);
      case RIGHT -> setGrowColumn(column2);
    }
    grid.getColumnConstraints().addAll(column1, column2);
    var rowConstraint = new RowConstraints();
    rowConstraint.setValignment(VPos.CENTER);
    grid.getRowConstraints().add(rowConstraint);

    return addToGrid(grid, children);
  }

  /**
   * Add all children to grid. Special handling of {@link GridRow} which fills a full row
   */
  public static GridPane addToGrid(final GridPane grid, final @Nullable Node... children) {
    int cols = grid.getColumnCount();
    int row = 0;
    int col = 0;
    for (final @Nullable Node child : children) {
      // always fills a full row. If row is started this will flow into the new row
      if (child instanceof GridRow || child instanceof Separator) {
        if (col > 0) {
          row++;
        }
        grid.add(child, 0, row, 2, 1);
        col = 0;
        row++;
      } else {
        if (child != null) {
          grid.add(child, col, row);
        }
        col++;
      }
      if (col == cols) {
        col = 0;
        row++;
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

  /**
   * Bind managed to visible. Managed usually needs to be false if visible is false so that it does
   * not take the space in the layout. But default is not bound. Sometimes this is not wanted.
   *
   * @return the input node
   */
  public static <T extends Node> T bindManagedToVisible(T node) {
    node.managedProperty().bind(node.visibleProperty());
    return node;
  }

  /**
   * Default row constraints
   */
  public static RowConstraints newGridRowConstraints() {
    return newGridRowConstraints(VPos.CENTER, false);
  }

  public static RowConstraints newGridRowConstraints(VPos vAlignment, boolean fillHeight) {
    final RowConstraints constraints = new RowConstraints();
    constraints.setValignment(vAlignment);
    if (fillHeight) {
      setFillHeightRow(constraints);
    }
    return constraints;
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

  /**
   * Fill a full
   * {@link GridPane) row if combined with factory methods like {@link #newGrid2Col(Node...)}
   */
  public static GridRow gridRow(Node... children) {
    return new GridRow(children);
  }


  public static BorderPane addNode(BorderPane borderPane, Node node, Position position) {
    switch (position) {
      case TOP -> borderPane.setTop(node);
      case BOTTOM -> borderPane.setBottom(node);
      case LEFT -> borderPane.setLeft(node);
      case RIGHT -> borderPane.setRight(node);
      case CENTER -> borderPane.setCenter(node);
    }
    return borderPane;
  }

  public enum Position {
    CENTER, TOP, LEFT, BOTTOM, RIGHT
  }
}
