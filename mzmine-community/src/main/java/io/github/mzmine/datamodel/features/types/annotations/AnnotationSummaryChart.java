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
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary.Scores;
import io.github.mzmine.datamodel.features.annotationpriority.ExposomicsAnnotationLevel;
import io.github.mzmine.datamodel.features.annotationpriority.MsiAnnotationLevel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.color.ColorUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.maths.Precision;
import java.util.Arrays;
import java.util.OptionalDouble;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Canvas-backed multi-score chart for one {@link AnnotationSummary}. Renders a thin colored bar
/// across the top encoding the annotation type, followed by a row (or two rows when wide and tall
/// enough) of vertical score bars with rotated labels. Identical visual output to the chart
/// previously inlined in {@code AnnotationSummaryType.MicroChartCell}; extracted so it can be
/// reused outside of TreeTable cells.
///
/// Usage: instantiate, set a preferred size, install in a layout, call {@link #setAnnotation}. The
/// internal canvas is resized to fill this pane on every layout pass; {@link Tooltip} is installed
/// on this pane and refreshed on each {@code setAnnotation} call with the score breakdown.
public final class AnnotationSummaryChart extends Pane {

  private static final double BAR_SPACING = 2;
  private static final double TWO_ROW_HEIGHT_THRESHOLD = 75;

  private final Canvas canvas = new Canvas();
  private final Font arial = new Font(Font.getDefault().getName(), 9);
  private final PaintScale palette;
  private final Color bgColor;
  private final Color textColor;
  private final Color outlineColor;
  private final Tooltip tooltip = new Tooltip();
  private @Nullable AnnotationSummary annotation;

  public AnnotationSummaryChart() {
    getChildren().add(canvas);
    setMinSize(0, 0);

    final SimpleColorPalette defaultPalette = ConfigService.getDefaultColorPalette();
    palette = new SimpleColorPalette(defaultPalette.getNegativeColor(),
        defaultPalette.getNeutralColor(), defaultPalette.getPositiveColor()).toPaintScale(
        PaintScaleTransform.LINEAR, Range.closed(0d, 1d));

    final EStandardChartTheme theme = ConfigService.getConfiguration().getDefaultChartTheme();
    final boolean bgTransparent = ColorUtils.isTransparent(
        FxColorUtil.awtColorToFX(theme.getPlotBackgroundPaint()));
    textColor = FxColorUtil.awtColorToFX(theme.getAxisLabelPaint());
    outlineColor = bgTransparent ? textColor.deriveColor(0, 1, 1, 0.3)
        : FxColorUtil.awtColorToFX(theme.getPlotBackgroundPaint()).deriveColor(0, 1, 1, 0.3);
    bgColor = bgTransparent ? textColor.deriveColor(0, 1, 1, 0.1)
        : FxColorUtil.awtColorToFX(theme.getPlotBackgroundPaint());

    Tooltip.install(this, tooltip);
  }

  /// Bind a new annotation summary and refresh the chart + tooltip. {@code null} or a summary with
  /// a null inner annotation hides all chart content (canvas is cleared on the next layout).
  public void setAnnotation(@Nullable AnnotationSummary annotation) {
    this.annotation = annotation;
    if (annotation == null || annotation.annotation() == null) {
      tooltip.setGraphic(null);
    } else {
      final Scores[] scoreTypes = Arrays.stream(Scores.values()).filter(annotation::isActiveScore)
          .toArray(Scores[]::new);
      installTooltipContent(annotation, scoreTypes);
    }
    requestLayout();
  }

  public @Nullable AnnotationSummary getAnnotation() {
    return annotation;
  }

  /// Stable per-type accent color used as a thin colored bar across the top of the chart. Public so
  /// other UI bits that want a consistent "this is a spectral / lipid / DB match" stripe can reuse
  /// the same palette.
  public static @NotNull Color getAnnotationTypeColor(
      @NotNull Class<? extends DataType> typeClass) {
    final DataType type = DataTypes.get(typeClass);
    return switch (type) {
      case SpectralLibraryMatchesType _ -> new Color(0.749f, 0.1725f, 0.5176f, 1f);
      // analog spectral matches: lighter shade of the spectral-match color
      case AnalogSpectralLibraryMatchesType _ -> new Color(0.835f, 0.369f, 0.f, 1f);
      case CompoundDatabaseMatchesType _ -> new Color(0.f, 0.620f, 0.451f, 1f);
      case LipidMatchListType _ -> new Color(0.941f, 0.894f, 0.259f, 1f);
      default -> new Color(0.337f, 0.706f, 0.914f, 1f);
    };
  }

  @Override
  protected void layoutChildren() {
    super.layoutChildren();
    canvas.setWidth(getWidth());
    canvas.setHeight(getHeight());
    draw();
  }

  private @NotNull Color getScoreColor(double score) {
    return FxColorUtil.awtColorToFX(palette.getPaint(score));
  }

  private void draw() {
    final GraphicsContext gc = canvas.getGraphicsContext2D();
    final double totalWidth = canvas.getWidth();
    final double annotationTypeBarHeight = 3;
    final double chartMaxWidth = totalWidth;
    final double chartMaxHeight = canvas.getHeight();
    final double availableHeight = chartMaxHeight - (annotationTypeBarHeight + BAR_SPACING);

    gc.clearRect(0, 0, totalWidth, chartMaxHeight);

    final AnnotationSummary annotationSummary = this.annotation;
    if (annotationSummary == null || annotationSummary.annotation() == null || chartMaxWidth <= 0
        || chartMaxHeight <= 0) {
      return;
    }

    final Scores[] scoreTypes = Arrays.stream(Scores.values())
        .filter(annotationSummary::isActiveScore).toArray(Scores[]::new);
    if (scoreTypes.length == 0) {
      return;
    }

    final boolean useTwoRows = availableHeight > TWO_ROW_HEIGHT_THRESHOLD && totalWidth < 100;
    final int totalItems = scoreTypes.length;
    final int numCols = useTwoRows ? (int) Math.ceil(totalItems / 2.0) : totalItems;
    final int numRows = useTwoRows ? 2 : 1;

    final double rowHeight = availableHeight / numRows;
    final double chartHeight = rowHeight - BAR_SPACING;

    final double availableWidth = chartMaxWidth - (BAR_SPACING * (numCols - 1));
    final double barWidth = availableWidth / Math.max(numCols, 1);

    gc.setFill(bgColor);
    gc.fillRect(0, 0, chartMaxWidth, chartMaxHeight);

    gc.setFill(getAnnotationTypeColor(annotationSummary.annotation().getDataType()));
    gc.fillRect(0, 0, chartMaxWidth, annotationTypeBarHeight);

    gc.setFont(arial);
    gc.setTextAlign(TextAlignment.LEFT);
    gc.setTextBaseline(VPos.CENTER);

    for (int i = 0; i < totalItems; i++) {
      final Scores scoreType = scoreTypes[i];

      final int rowIndex = useTwoRows ? (i / numCols) : 0;
      final int colIndex = useTwoRows ? (i % numCols) : i;
      final double xOffset = colIndex * (barWidth + BAR_SPACING);
      final double yOffset =
          (chartMaxHeight - availableHeight) + rowIndex * rowHeight + (rowIndex * BAR_SPACING);

      final OptionalDouble optScore = annotationSummary.score(scoreType);
      final double score = optScore.orElse(0d);
      final double barH = score * chartHeight;
      final double topEdge = yOffset + chartHeight - barH;

      gc.setFill(getScoreColor(score));
      gc.fillRect(Math.round(xOffset), Math.ceil(topEdge), Math.round(barWidth), Math.ceil(barH));

      gc.save();
      final double pivotX = xOffset + (barWidth / 2);
      final double pivotY = yOffset + rowHeight - 4;
      gc.translate(pivotX, pivotY);
      gc.rotate(-90);

      if (score > 0.2) {
        gc.setFill(Color.WHITE);
      } else if (optScore.isPresent() && Precision.equalFloatSignificance(score, 0d)) {
        // value present in the source but outside paint-scale bounds — recolor for visibility
        gc.setFill(
            ConfigService.getConfiguration().getTheme().isDark() ? getScoreColor(0).brighter()
                                                                   .brighter() : getScoreColor(0));
      } else {
        gc.setFill(textColor);
      }
      gc.fillText(scoreType.label(), 0, 0);
      gc.restore();
    }

    gc.setStroke(outlineColor);
    gc.strokeRect(0, 0, chartMaxWidth, chartMaxHeight);
  }

  private void installTooltipContent(@NotNull AnnotationSummary annotationSummary,
      @NotNull Scores[] scoreTypes) {
    final MsiAnnotationLevel msiLevel = annotationSummary.deriveMsiLevel();
    final ExposomicsAnnotationLevel schymanskiLevel = annotationSummary.deriveExposomicsLevel();

    final VBox left = FxLayout.newVBox(Pos.CENTER_RIGHT, Insets.EMPTY,
        newBoldLabel("Annotation levels:"), newLabel(msiLevel.getLabel() + " = "),
        newLabel(schymanskiLevel.getLabel() + " = "), newBoldLabel(""), newBoldLabel("Scores:"));
    final VBox right = FxLayout.newVBox(Pos.CENTER_LEFT, Insets.EMPTY, newBoldLabel(""),
        newBoldLabel(msiLevel.numberLevel() + ""), newBoldLabel(schymanskiLevel.fullLevel()),
        newBoldLabel(""), newBoldLabel(""));
    left.setSpacing(0);
    right.setSpacing(0);

    for (final Scores type : scoreTypes) {
      left.getChildren().add(newLabel(type.fullName() + " = "));
      right.getChildren().add(newBoldLabel(annotationSummary.scoreLabel(type)));
    }
    tooltip.setGraphic(FxLayout.newHBox(left, right));
  }
}
