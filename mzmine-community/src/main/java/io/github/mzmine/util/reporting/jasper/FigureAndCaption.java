package io.github.mzmine.util.reporting.jasper;

import io.github.mzmine.gui.chartbasics.graphicsexport.ChartExportUtil;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.mzmine.reports.SingleFigureRow;
import io.mzmine.reports.TwoFigureRow;
import java.awt.image.BufferedImage;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record FigureAndCaption(@Nullable Object chartSvg, @Nullable String caption) {

  @NotNull
  static FigureAndCaption of(@NotNull final EChartViewer chart, @NotNull final Format format,
      final int width, final int height, final @Nullable String caption) throws IOException {
    final Object figure = switch (format) {
      case SVG -> ChartExportUtil.writeChartToSvgString(chart.getChart(), width, height).getBytes();
      case PNG -> ChartExportUtil.paintScaledChartToBufferedImage(chart.getChart(),
          chart.getRenderingInfo(), width, height, 150, BufferedImage.TYPE_INT_ARGB);
    };

    return new FigureAndCaption(figure, caption);
  }

  @NotNull
  static FigureAndCaption asSingleColumn(@NotNull final EChartViewer chart,
      @NotNull final Format format, final @Nullable String caption) throws IOException {
    return of(chart, format, SingleFigureRow.WIDTH, SingleFigureRow.HEIGHT, caption);
  }

  @NotNull
  static FigureAndCaption asSingleColumnPng(@NotNull final EChartViewer chart,
      @NotNull final Format format, final @Nullable String caption) throws IOException {
    return asSingleColumn(chart, Format.PNG, caption);
  }

  @NotNull
  static FigureAndCaption asSingleColumnSvg(@NotNull final EChartViewer chart,
      final @Nullable String caption) throws IOException {
    return asSingleColumn(chart, Format.SVG, caption);
  }

  @NotNull
  static FigureAndCaption asTwoColumn(@NotNull final EChartViewer chart,
      @NotNull final Format format, final @Nullable String caption) throws IOException {
    return of(chart, format, TwoFigureRow.WIDTH, TwoFigureRow.HEIGHT, caption);
  }

  @NotNull
  static FigureAndCaption asTwoColumnSvg(@NotNull final EChartViewer chart,
      final @Nullable String caption) throws IOException {
    return asTwoColumn(chart, Format.SVG, caption);
  }

  @NotNull
  static FigureAndCaption asTwoColumnPng(@NotNull final EChartViewer chart,
      final @Nullable String caption) throws IOException {
    return asTwoColumn(chart, Format.SVG, caption);
  }

  enum Format {
    PNG, SVG;
  }
}
