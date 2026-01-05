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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import java.util.List;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class MicroBarChartTable extends Application {

  // 1. Data Model for a single bar
  public record BarItem(String label, double value, Color color) {

  }

  // 2. Data Model for the Table Row
  public static class ReportRow {

    private final String name;
    private final List<BarItem> stats;

    public ReportRow(String name, List<BarItem> stats) {
      this.name = name;
      this.stats = stats;
    }

    public String getName() {
      return name;
    }

    public List<BarItem> getStats() {
      return stats;
    }
  }

  @Override
  public void start(Stage stage) {
    TableView<ReportRow> table = new TableView<>();

    // Name Column
    TableColumn<ReportRow, String> nameCol = new TableColumn<>("Region");
    nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
    nameCol.setPrefWidth(100);

    // Chart Column
    TableColumn<ReportRow, List<BarItem>> chartCol = new TableColumn<>("Performance");
    chartCol.setCellValueFactory(new PropertyValueFactory<>("stats"));
    chartCol.setPrefWidth(300);

    // ASSIGN THE CUSTOM RENDERER
    chartCol.setCellFactory(column -> new MicroChartCell());

    table.getColumns().addAll(nameCol, chartCol);

    // Dummy Data
    ObservableList<ReportRow> data = FXCollections.observableArrayList(new ReportRow("North",
        List.of(new BarItem("Q1", 0.8, Color.TOMATO), new BarItem("Q2", 0.5, Color.CORNFLOWERBLUE),
            new BarItem("Q3", 0.9, Color.TEAL))), new ReportRow("South",
        List.of(new BarItem("Q1", 0.3, Color.TOMATO), new BarItem("Q2", 0.4, Color.CORNFLOWERBLUE),
            new BarItem("Q3", 0.5, Color.TEAL))), new ReportRow("East",
        List.of(new BarItem("Q1", 0.9, Color.TOMATO), new BarItem("Q2", 0.8, Color.CORNFLOWERBLUE),
            new BarItem("Q3", 0.2, Color.TEAL))));
    table.setItems(data);

    Scene scene = new Scene(new StackPane(table), 500, 300);
    stage.setTitle("Lightweight Table Cell Bars");
    stage.setScene(scene);
    stage.show();
  }

  // ---------------------------------------------------------
  // THE CUSTOM CELL IMPLEMENTATION
  // ---------------------------------------------------------
  private static class MicroChartCell extends TableCell<ReportRow, List<BarItem>> {

    private final Canvas canvas;
    private final double BAR_SPACING = 3;
    private final double MAX_VALUE = 1.0; // Normalize bars against this max

    public MicroChartCell() {
      // Create canvas once. We will resize it in layout.
      this.canvas = new Canvas();
      setGraphic(canvas);
      setMinHeight(50);
    }

    @Override
    protected void layoutChildren() {
      super.layoutChildren();
      // Important: Resize canvas to match the cell size so drawing works correctly
      canvas.setWidth(getWidth() - getGraphicTextGap() * 2);
      canvas.setHeight(getHeight() - getGraphicTextGap() * 2);
      draw(); // Redraw when resized
    }

    @Override
    protected void updateItem(List<BarItem> items, boolean empty) {
      super.updateItem(items, empty);
      draw(); // Redraw when data changes
    }

    private void draw() {
      GraphicsContext gc = canvas.getGraphicsContext2D();
      double w = canvas.getWidth();
      double h = canvas.getHeight();

      // 1. Clear previous drawing
      gc.clearRect(0, 0, w, h);

      List<BarItem> items = getItem();
      if (items == null || items.isEmpty()) {
        return;
      }

      // 2. Setup Dimensions
      int count = items.size();
      double availableWidth = w - (BAR_SPACING * (count - 1));
      double barWidth = availableWidth / count;

      // Reserve space for text at the bottom (approx 12px)
      double textHeight = 12;
      double chartHeight = h - textHeight - 2; // -2 for padding

      gc.setFont(new Font("Arial", 10));
      gc.setTextAlign(TextAlignment.CENTER);

      // 3. Draw Loop
      double xPos = 0;

      for (BarItem item : items) {
        // Calculate bar height based on value (0.0 to 1.0)
        double barH = (item.value() / MAX_VALUE) * chartHeight;
        double yPos = chartHeight - barH;

        // Draw Bar
        gc.setFill(item.color());
        // Use Math.round to snap to pixels for sharpness
        gc.fillRect(Math.round(xPos), Math.round(yPos), Math.round(barWidth), Math.round(barH));

        // Draw Text (Label below bar)
        gc.setFill(Color.BLACK);
        gc.fillText(item.label(), xPos + (barWidth / 2), h - 2);

        // Optional: Draw Value on top of bar if it fits
        if (barH > 15) {
          gc.setFill(Color.WHITE);
          gc.fillText(String.format("%.1f", item.value()), xPos + (barWidth / 2), yPos + 10);
        }

        xPos += barWidth + BAR_SPACING;
      }
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}