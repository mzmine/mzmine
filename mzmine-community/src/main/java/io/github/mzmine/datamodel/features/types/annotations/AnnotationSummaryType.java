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

package io.github.mzmine.datamodel.features.types.annotations;

import com.google.common.collect.Range;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.AnnotationSummary;
import io.github.mzmine.datamodel.features.compoundannotations.AnnotationSummary.Scores;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.LinkedGraphicalType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnnotationSummaryType extends LinkedGraphicalType {

  @Override
  public @NotNull String getUniqueID() {
    return "annotation_summary";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "AS";
  }

  @Override
  public @Nullable Node createCellContent(@NotNull ModularFeatureListRow row, Boolean cellData,
      @Nullable RawDataFile raw, AtomicDouble progress) {
    throw new IllegalStateException("Statement should be unreachable due to custom cell factory.");
  }

  @Override
  public @Nullable TreeTableColumn<ModularFeatureListRow, Object> createColumn(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType, int subColumnIndex) {

    TreeTableColumn<ModularFeatureListRow, FeatureAnnotation> column = new TreeTableColumn<>(
        getHeaderString());
    column.setUserData(this);
    if (parentType != null) {
      column.setCellValueFactory(cdf -> {
        var value = (List<? extends FeatureAnnotation>) cdf.getValue().getValue()
            .get((DataType<?>) parentType);
        return new ReadOnlyObjectWrapper<>(value != null ? value.getFirst() : null);
      });
    } else {
      column.setCellValueFactory(cdf -> new ReadOnlyObjectWrapper<>(
          CompoundAnnotationUtils.getBestFeatureAnnotation(cdf.getValue().getValue())
              .orElse(null)));
    }

    column.setCellFactory(col -> new MicroChartCell());
    column.setMinWidth(60);

    return (TreeTableColumn) column;
  }

  private static class MicroChartCell extends
      TreeTableCell<ModularFeatureListRow, FeatureAnnotation> {

    private static final double BAR_SPACING = 3;
    private static final double MIN_BAR_WIDTH = 15;
    // Threshold to switch to two rows
    private static final double TWO_ROW_THRESHOLD = 75;

    private final Canvas canvas = new Canvas();
    private final Font arial = new Font("Arial", 8);
    private final PaintScale palette;
    // We access values frequently, cache them if possible, or call values() in draw
    private final Scores[] scoreTypes = Scores.values();

    public MicroChartCell() {
      setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
      setGraphic(canvas);
      setMinHeight(50);
      setMinWidth(getMinChartWidth());

      final SimpleColorPalette defaultPalette = ConfigService.getDefaultColorPalette();
      palette = new SimpleColorPalette(defaultPalette.getNegativeColor(),
          /*defaultPalette.getNeutralColor(),*/ defaultPalette.getPositiveColor()).toPaintScale(
          PaintScaleTransform.LINEAR, Range.closed(0d, 1d));
    }

    static double getMinChartWidth() {
      return 50;
//      final int numScores = Scores.values().length / 2;
//      return numScores * BAR_SPACING + numScores * MIN_BAR_WIDTH + 2 * 3;
    }

    @Override
    protected void layoutChildren() {
      super.layoutChildren();
      // Ensure canvas matches cell size
      canvas.setWidth(getWidth() - getGraphicTextGap() * 2);
      canvas.setHeight(getHeight() - getGraphicTextGap() * 2);
      draw();
    }

    @Override
    protected void updateItem(FeatureAnnotation item, boolean empty) {
      super.updateItem(item, empty);
      draw();
    }

    private void draw() {
      final GraphicsContext gc = canvas.getGraphicsContext2D();
      final double width = canvas.getWidth();
      final double height = canvas.getHeight();

      // 1. Clear Canvas
      gc.clearRect(0, 0, width, height);

      if (isEmpty() || getItem() == null || !isVisible()) {
        return;
      }

      FeatureAnnotation annotation = getItem();
      if (annotation == null) {
        return;
      }

      final var annotationSummary = AnnotationSummary.of(getTableRow().getItem(), annotation);

      // 2. Determine Layout Mode (1 Row or 2 Rows)
      boolean useTwoRows = height > TWO_ROW_THRESHOLD;

      int totalItems = scoreTypes.length;
      // If 2 rows, columns is half the items (rounded up), otherwise all items
      int numCols = useTwoRows ? (int) Math.ceil(totalItems / 2.0) : totalItems;
      int numRows = useTwoRows ? 2 : 1;

      // 3. Calculate Dimensions per Cell
      // How much height does one row get?
      double rowHeight = height / numRows;

      // Reserve space for text at the bottom of *each* row (approx 10px)
      final double textHeight = 10;
      final double chartHeight = rowHeight - textHeight - 2; // -2 for padding

      // Calculate width of a single bar based on available columns
      final double availableWidth = width - (BAR_SPACING * (numCols - 1));
      final double barWidth = availableWidth / numCols;

      gc.setFont(arial);
      gc.setTextAlign(TextAlignment.CENTER);

      final Color textColor =
          ConfigService.getConfiguration().getTheme().isDark() ? Color.WHITE : Color.BLACK;

      // 4. Draw Loop
      for (int i = 0; i < totalItems; i++) {
        Scores scoreType = scoreTypes[i];

        // Calculate Grid Position
        int rowIndex = useTwoRows ? (i / numCols) : 0;
        int colIndex = useTwoRows ? (i % numCols) : i;

        // Calculate Pixel Offsets
        double xOffset = colIndex * (barWidth + BAR_SPACING);
        double yOffset = rowIndex * rowHeight;

        // Get Data
        final double score = annotationSummary.score(scoreType);

        // Calculate Bar Geometry relative to the specific Row
        final double barH = score * chartHeight;
        // The top of the bar is: (Row Start) + (Max Chart Height) - (Actual Bar Height)
        final double topEdge = yOffset + chartHeight - barH;

        // Draw Bar
        gc.setFill(getScoreColor(score));
        gc.fillRect(Math.round(xOffset), Math.round(topEdge), Math.round(barWidth),
            Math.ceil(barH));

        // Draw Label (bottom of the current row)
        gc.setFill(textColor);
        // y position is: Row Start + Row Height - Padding
        gc.fillText(scoreType.label(), xOffset + (barWidth / 2), yOffset + rowHeight - 2);

        // Optional: Draw Value on top of bar if it fits
        // (Check if bar is tall enough AND if we aren't smashing into the row above)
//        if (barH > 12) {
//          gc.setFill(Color.WHITE);
//          gc.fillText(String.format("%.2f", score), xOffset + (barWidth / 2), topEdge + 9, barWidth);
//        }
      }
    }

    private Color getScoreColor(double score) {
      return FxColorUtil.awtColorToFX(palette.getPaint(score));
    }
  }

}
