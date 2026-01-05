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
    column.setMinWidth(MicroChartCell.getMinChartWidth());

    return (TreeTableColumn) column;
  }

  private static class MicroChartCell extends
      TreeTableCell<ModularFeatureListRow, FeatureAnnotation> {

    private static final double BAR_SPACING = 3;
    private static final double MIN_BAR_WIDTH = 15;
    private final Canvas canvas = new Canvas();
    private final Font arial = new Font("Arial", 8);
    private final PaintScale palette;

    public MicroChartCell() {
      setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
      setGraphic(canvas);
      setMinHeight(50);
      setMinWidth(getMinChartWidth());
      SimpleColorPalette defaultPalette = ConfigService.getDefaultColorPalette();
      palette = new SimpleColorPalette(defaultPalette.getNegativeColor(),
          defaultPalette.getNeutralColor(), defaultPalette.getPositiveColor()).toPaintScale(
          PaintScaleTransform.LINEAR, Range.closed(0d, 1d));
//      palette = ConfigService.getConfiguration().getDefaultPaintScalePalette().toPaintScale(PaintScaleTransform.LINEAR, Range.closed(0d, 1d));
    }

    static double getMinChartWidth() {
      final int numScores = Scores.values().length;
      return numScores * BAR_SPACING + numScores * MIN_BAR_WIDTH + 2 * 3;
    }

    @Override
    protected void layoutChildren() {
      super.layoutChildren();
      canvas.setWidth(getWidth() - getGraphicTextGap() * 2);
      canvas.setHeight(getHeight() - getGraphicTextGap() * 2);
      draw(); // Redraw when resized
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

      gc.clearRect(0, 0, width, height);

      FeatureAnnotation annotation = getItem();
      if (annotation == null) {
        return;
      }

      final var annotationSummary = AnnotationSummary.of(getTableRow().getItem(), annotation);

      final int count = 6;
      final double availableWidth = width - (BAR_SPACING * (count - 1));
      final double barWidth = availableWidth / count;

      // Reserve space for text at the bottom (approx 12px)
      final double textHeight = 10;
      final double chartHeight = height - textHeight - 2; // -2 for padding

      gc.setFont(arial);
      gc.setTextAlign(TextAlignment.CENTER);

      // 3. Draw Loop
      double leftEdge = 0;

      for (Scores scoreType : Scores.values()) {
        // Calculate bar height based on value (0.0 to 1.0)
        final double score = annotationSummary.score(scoreType);
        final double barH = score * chartHeight;
        final double topEdge = chartHeight - barH;

        gc.setFill(getScoreColor(score));
        gc.fillRect(Math.round(leftEdge), Math.round(topEdge), Math.round(barWidth),
            Math.ceil(barH));
//        gc.strokeRect(Math.round(leftEdge), 0, Math.round(barWidth), Math.ceil(chartHeight));

//        gc.setFill(Color.RED);
//        gc.fillOval(leftEdge, topEdge, 1, 1);

        // Draw Text (Label below bar)
        gc.setFill(
            ConfigService.getConfiguration().getTheme().isDark() ? Color.WHITE : Color.BLACK);
        gc.fillText(scoreType.label(), leftEdge + (barWidth / 2), height - 2);

        // Optional: Draw Value on top of bar if it fits
        if (barH > 15) {
          gc.setFill(Color.WHITE);
          gc.fillText(String.format("%.2f", score), leftEdge + (barWidth / 2), topEdge + 10);
        }

        leftEdge += barWidth + BAR_SPACING;
      }
    }

    private Color getScoreColor(double score) {
      return FxColorUtil.awtColorToFX(palette.getPaint(score));
      /*if (score < 0.333) {
        return palette.getNegativeColor();
      }
      if (score < 0.666) {
        return palette.getNeutralColor();
      }
      return palette.getPositiveColor();*/
    }
  }

}
