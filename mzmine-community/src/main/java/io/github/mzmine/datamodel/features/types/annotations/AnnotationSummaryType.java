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

package io.github.mzmine.datamodel.features.types.annotations;

import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldLabel;
import static io.github.mzmine.javafx.components.factories.FxLabels.newLabel;

import com.google.common.collect.Range;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary.Scores;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummaryOrder;
import io.github.mzmine.datamodel.features.annotationpriority.MsiAnnotationLevel;
import io.github.mzmine.datamodel.features.annotationpriority.SchymanskiAnnotationLevel;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.modifiers.NoTextColumn;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.color.ColorUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnnotationSummaryType extends DataType<AnnotationSummary> implements
    GraphicalColumType<AnnotationSummary>, NoTextColumn {

  @Override
  public @NotNull String getUniqueID() {
    return "annotation_quality_summary";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "AQS";
  }

  @Override
  public @Nullable TreeTableColumn<ModularFeatureListRow, Object> createColumn(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType, int subColumnIndex) {
    final TreeTableColumn<ModularFeatureListRow, AnnotationSummary> column = new TreeTableColumn<>(
        getHeaderString());
    column.setUserData(this);
    if (parentType != null) {
      // parent type set -> is a sub type of an annotation/list type. get annotation from there
      column.setCellValueFactory(
          new Callback<CellDataFeatures<ModularFeatureListRow, AnnotationSummary>, ObservableValue<AnnotationSummary>>() {
            // cache the AnnotationSummary because sorting and isotope pattern score is expensive
            // first time sorting is slow and from then on it is as fast as all the other columns
            private record RowAnnotationSummary(FeatureListRow row, FeatureAnnotation annotation) {

            }

            private final Map<RowAnnotationSummary, AnnotationSummary> cache = new HashMap<>();

            @Override
            public ObservableValue<AnnotationSummary> call(
                CellDataFeatures<ModularFeatureListRow, AnnotationSummary> cdf) {
              final ModularFeatureListRow row = cdf.getValue().getValue();
              final Object value = row.get((DataType<?>) parentType);
              final FeatureAnnotation annotation;
              if (value instanceof List list) {
                annotation =
                    list != null && !list.isEmpty() ? (FeatureAnnotation) list.getFirst() : null;
              } else if (value instanceof FeatureAnnotation a) {
                annotation = a;
              } else {
                annotation = null;
              }

              if (annotation == null) {
                return new ReadOnlyObjectWrapper<>();
              }
              final RowAnnotationSummary cacheKey = new RowAnnotationSummary(row, annotation);
              final AnnotationSummary summary = cache.computeIfAbsent(cacheKey,
                  _ -> AnnotationSummary.of(row, annotation));
              return new ReadOnlyObjectWrapper<>(summary);
            }
          });
    } else {
      // currently not used but in case this type was added directly to the row, then use the preferred annotation
      column.setCellValueFactory(cdf -> new ReadOnlyObjectWrapper<>(
          CompoundAnnotationUtils.getBestAnnotationSummary(cdf.getValue().getValue())));
    }

    column.setCellFactory(col -> new MicroChartCell());
    column.setMinWidth(45);
    column.setPrefWidth(45);
    column.setSortable(true);
    // flip sorting so that first click gives correct sorting
    column.setComparator(AnnotationSummaryOrder.MZMINE.getComparatorHighFirst());
//    column.setMaxWidth(60);

    return (TreeTableColumn) column;
  }

  @Override
  public Property<AnnotationSummary> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<AnnotationSummary> getValueClass() {
    return AnnotationSummary.class;
  }

  @Override
  public @Nullable Node createCellContent(@NotNull ModularFeatureListRow row,
      AnnotationSummary cellData, @Nullable RawDataFile raw, AtomicDouble progress) {
    throw new IllegalStateException("Statement should be unreachable due to custom cell factory.");
  }

  private static class MicroChartCell extends
      TreeTableCell<ModularFeatureListRow, AnnotationSummary> {

    private static final double BAR_SPACING = 2;
    private static final double MIN_BAR_WIDTH = 15;
    private static final double TWO_ROW_HEIGHT_THRESHOLD = 75;

    private final Canvas canvas = new Canvas();
    private final Font arial = new Font(Font.getDefault().getName(), 9);
    private final PaintScale palette;
    private final Color bgColor;
    private final Color textColor;
    private final Color outlineColor;
    private final Tooltip tooltip = new Tooltip();

    public MicroChartCell() {
      setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
      setGraphic(canvas);
      setMinHeight(50);

      setTooltip(tooltip);
//      setMinWidth(getMinChartWidth());

      final SimpleColorPalette defaultPalette = ConfigService.getDefaultColorPalette();
      palette = new SimpleColorPalette(defaultPalette.getNegativeColor(),
          defaultPalette.getNeutralColor(), defaultPalette.getPositiveColor()).toPaintScale(
          PaintScaleTransform.LINEAR, Range.closed(0d, 1d));

      final EStandardChartTheme defaultChartTheme = ConfigService.getConfiguration()
          .getDefaultChartTheme();
      final boolean defaultBackgroundTransparent = ColorUtils.isTransparent(
          FxColorUtil.awtColorToFX(defaultChartTheme.getPlotBackgroundPaint()));
      textColor = FxColorUtil.awtColorToFX(defaultChartTheme.getAxisLabelPaint());
      outlineColor = defaultBackgroundTransparent ? textColor.deriveColor(0, 1, 1, 0.3)
          : FxColorUtil.awtColorToFX(defaultChartTheme.getPlotBackgroundPaint())
              .deriveColor(0, 1, 1, 0.3);
      bgColor = defaultBackgroundTransparent ? textColor.deriveColor(0, 1, 1, 0.1)
          : FxColorUtil.awtColorToFX(defaultChartTheme.getPlotBackgroundPaint());
    }

    @Override
    protected void layoutChildren() {
      super.layoutChildren();
      canvas.setWidth(getWidth() - getGraphicTextGap() * 2);
      canvas.setHeight(getHeight() - getGraphicTextGap() * 2);
      draw();
    }

    @Override
    protected void updateItem(AnnotationSummary item, boolean empty) {
      super.updateItem(item, empty);
      draw();
    }

    private void draw() {
      final GraphicsContext gc = canvas.getGraphicsContext2D();

      final double totalWidth = canvas.getWidth();
//      final double annotationLevelsSpace = 2 * (arial.getSize() + BAR_SPACING); // option to put levels on the side (ansgar did not like it)
      final double annotationLevelsSpace = 0;

      final double chartMaxWidth = totalWidth - annotationLevelsSpace;
      final double height = canvas.getHeight();

      gc.clearRect(0, 0, totalWidth, height);

      AnnotationSummary annotationSummary = getItem();
      if (annotationSummary == null || isEmpty() || !isVisible()) {
        tooltip.setGraphic(null);
        setTooltip(null);
        return;
      }

      // only plot actually active types like CCS only if ion mobility
      final Scores[] scoreTypes = Arrays.stream(Scores.values())
          .filter(annotationSummary::isActiveScore).toArray(Scores[]::new);

      setTooltip(annotationSummary, scoreTypes);

      // 1. Layout Calculations
      final boolean useTwoRows = height > TWO_ROW_HEIGHT_THRESHOLD && totalWidth < 100;
      final int totalItems = scoreTypes.length;
      final int numCols = useTwoRows ? (int) Math.ceil(totalItems / 2.0) : totalItems;
      final int numRows = useTwoRows ? 2 : 1;

      final double rowHeight = height / numRows;
      final double chartHeight = rowHeight - BAR_SPACING;

      final double availableWidth = chartMaxWidth - (BAR_SPACING * (numCols - 1));
      final double barWidth = availableWidth / numCols;

      gc.setFill(bgColor);
      gc.fillRect(0, 0, chartMaxWidth, height);

      gc.setFont(arial);
      // Align LEFT means "Bottom" when rotated -90 degrees
      gc.setTextAlign(TextAlignment.LEFT);
      // Center vertically so the text runs exactly up the middle of the bar
      gc.setTextBaseline(javafx.geometry.VPos.CENTER);

      for (int i = 0; i < totalItems; i++) {
        final Scores scoreType = scoreTypes[i];

        // Grid Position
        final int rowIndex = useTwoRows ? (i / numCols) : 0;
        final int colIndex = useTwoRows ? (i % numCols) : i;

        final double xOffset = colIndex * (barWidth + BAR_SPACING);
        final double yOffset = rowIndex * rowHeight + (rowIndex * BAR_SPACING);

        final double score = annotationSummary.score(scoreType).orElse(0d);
        final double barH = score * chartHeight;
        final double topEdge = yOffset + chartHeight - barH;

        // Draw Bar
        gc.setFill(getScoreColor(score));
        // ceil top and height to not leave a pixel free at the bottom of the chart
        gc.fillRect(Math.round(xOffset), Math.ceil(topEdge), Math.round(barWidth), Math.ceil(barH));

//        gc.setStroke(outlineColor);
//        gc.strokeRect(Math.round(xOffset), Math.ceil(yOffset), Math.round(barWidth), Math.ceil(chartHeight));

        // Draw Label (Rotated on top of bar)
        gc.save();

        // Pivot point: Center of the bar width, Bottom of the row
        final double pivotX = xOffset + (barWidth / 2);
        final double pivotY = yOffset + rowHeight - 4; // -4 padding from absolute bottom
        gc.translate(pivotX, pivotY);
        gc.rotate(-90);
        // Determine contrast color.
        // If bar is very short (score < ~0.2), text might float above it on the background.
        // If bar is tall, text is on the bar.
        if (score > 0.2) {
          gc.setFill(Color.WHITE); // Assuming bars are colored/dark
        } else {
          gc.setFill(textColor);
        }
        // Draw at (0,0) relative to the pivot.
        // Due to rotation: X moves UP, Y moves RIGHT (which is centered via VPos)
        gc.fillText(scoreType.label(), 0, 0);

        gc.restore();
      }

      gc.setStroke(outlineColor);
      gc.strokeRect(0, 0, chartMaxWidth, height);

      /*final double pivotX = chartMaxWidth;
      final double pivotY = height;
      gc.save();
      gc.translate(pivotX, pivotY);
      gc.rotate(-90);
      gc.setFill(textColor);
      gc.fillText(annotationSummary.deriveSchymanskiLevel(), BAR_SPACING,
          0 + arial.getSize() / 2 + BAR_SPACING);
      gc.fillText(annotationSummary.deriveSumnerLevel(), BAR_SPACING,
          arial.getSize() + arial.getSize() / 2 + BAR_SPACING);
      gc.restore();*/
    }

    private void setTooltip(AnnotationSummary annotationSummary, Scores[] scoreTypes) {
      final MsiAnnotationLevel msiLevel = annotationSummary.deriveMsiLevel();
      final SchymanskiAnnotationLevel schymanskiLevel = annotationSummary.deriveSchymanskiLevel();

      final VBox left = FxLayout.newVBox(Pos.CENTER_RIGHT, Insets.EMPTY, //
          newBoldLabel("Annotation levels:") //
          , newLabel(msiLevel.getLabel() + " = ") //
          , newLabel(schymanskiLevel.getLabel() + " = ") //
          , newBoldLabel("") // space holder
          , newBoldLabel("Scores:") //
      );
      final VBox right = FxLayout.newVBox(Pos.CENTER_LEFT, Insets.EMPTY, //
          newBoldLabel("") // space holder
          , newBoldLabel(msiLevel.numberLevel() + "") //
          , newBoldLabel(schymanskiLevel.fullLevel()) //
          , newBoldLabel("") // space holder
          , newBoldLabel("") // space holder
      );

      for (Scores type : scoreTypes) {
        left.getChildren().add(newLabel(type.fullName() + " = "));
        right.getChildren().add(newBoldLabel(annotationSummary.scoreLabel(type)));
      }

      // grid pane did not work, scaled too large and with background
      tooltip.setGraphic(FxLayout.newHBox(left, right));
      setTooltip(tooltip);
    }

    private Color getScoreColor(double score) {
      return FxColorUtil.awtColorToFX(palette.getPaint(score));
    }
  }

}
