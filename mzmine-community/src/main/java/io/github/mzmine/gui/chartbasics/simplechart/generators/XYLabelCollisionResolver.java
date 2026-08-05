package io.github.mzmine.gui.chartbasics.simplechart.generators;

import io.github.mzmine.gui.chartbasics.gui.javafx.model.FxXYPlot;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * Shared screen-space collision detection for
 * {@link org.jfree.chart.labels.XYItemLabelGenerator} implementations. For each candidate label it
 * estimates the {@link Rectangle2D} the rendered label would occupy on the plot canvas and decides
 * whether it fits inside the data area without intersecting any of the previously accepted label
 * bounds in the shared cache.
 *
 * <p>Wiring expected from a consumer:
 * <ul>
 *   <li>The owning model holds a {@code List<Rectangle2D>} of drawn-label bounds.</li>
 *   <li>The view builder clears that list on every {@code ChartProgressEvent.DRAWING_STARTED}
 *   (a progress listener catches resizes that a {@code PlotChangeListener} would miss).</li>
 *   <li>Each label generator calls {@link #tryAcceptLabel} and returns the label only when the
 *   call returns {@code true}.</li>
 * </ul>
 */
public class XYLabelCollisionResolver {

  // Padding around the text bounds and vertical gap between data point and label.
  // Matches ColoredBubbleDatasetRenderer's constants so spectra and bubble plots feel consistent.
  private static final double PADDING = 2.0;
  private static final double LABEL_GAP = 2.0;

  // assumption: a shared, immutable FontRenderContext is good enough for collision detection.
  // It does not need to match the chart's actual antialiasing settings exactly because we only
  // care about approximate text extents, not pixel-perfect rendering.
  private static final FontRenderContext FRC = new FontRenderContext(null, false, false);

  private static final Font FALLBACK_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

  private final @NotNull Supplier<@NotNull FxXYPlot> plotSupplier;
  private final @NotNull Supplier<@NotNull List<@NotNull Rectangle2D>> drawnBoundsSupplier;
  private final @NotNull DoubleSupplier bottomMarginSupplier;

  public XYLabelCollisionResolver(@NotNull Supplier<@NotNull FxXYPlot> plotSupplier,
      @NotNull Supplier<@NotNull List<@NotNull Rectangle2D>> drawnBoundsSupplier,
      @NotNull DoubleSupplier bottomMarginSupplier) {
    this.plotSupplier = plotSupplier;
    this.drawnBoundsSupplier = drawnBoundsSupplier;
    this.bottomMarginSupplier = bottomMarginSupplier;
  }

  /**
   * @return {@code true} when the label fits inside the allowed area and does not intersect any
   * previously accepted label. As a side effect the label's bounds are appended to the cache. If
   * the data area is not yet available (first paint pass before {@link PlotRenderingInfo} is
   * populated), returns {@code true} without modifying the cache so JFreeChart still draws the
   * label and the next render cycle produces real bounds.
   */
  public boolean tryAcceptLabel(@NotNull XYDataset dataset, final int series, final int item,
      @NotNull String label) {
    final FxXYPlot plot = plotSupplier.get();
    final Rectangle2D dataArea = currentDataArea(plot);
    if (dataArea == null || dataArea.isEmpty()) {
      return true;
    }

    final Rectangle2D labelBounds = computeLabelBounds(plot, dataset, series, item, label,
        dataArea);
    if (labelBounds == null) {
      return false;
    }

    // Only allow labels that fit fully inside the dataArea, shrunk by the configured bottom margin.
    // Reasons to reject:
    //   - top: a very tall peak's label would extend above the plot area;
    //   - left/right: a peak at the chart edge would have its label clipped horizontally;
    //   - bottom: low-intensity peaks whose label would sit inside the excluded bottom strip
    //     (e.g. on top of the axis labels) get filtered out by the bottom-margin shrink.
    final double bottomMargin = Math.max(0.0, bottomMarginSupplier.getAsDouble());
    final double allowedHeight = Math.max(0.0, dataArea.getHeight() - bottomMargin);
    final Rectangle2D allowedArea = new Rectangle2D.Double(dataArea.getX(), dataArea.getY(),
        dataArea.getWidth(), allowedHeight);
    if (allowedHeight <= 0.0 || !allowedArea.contains(labelBounds)) {
      return false;
    }

    final List<@NotNull Rectangle2D> cache = drawnBoundsSupplier.get();
    for (final Rectangle2D existing : cache) {
      if (existing.intersects(labelBounds)) {
        return false;
      }
    }

    cache.add(labelBounds);
    return true;
  }

  private static @Nullable Rectangle2D currentDataArea(@NotNull FxXYPlot plot) {
    final ChartRenderingInfo info = plot.getRenderingInfo();
    if (info == null) {
      return null;
    }
    final PlotRenderingInfo plotInfo = info.getPlotInfo();
    if (plotInfo == null) {
      return null;
    }
    return plotInfo.getDataArea();
  }

  private static @Nullable Rectangle2D computeLabelBounds(@NotNull XYPlot plot,
      @NotNull XYDataset dataset, final int series, final int item, @NotNull String label,
      @NotNull Rectangle2D dataArea) {

    final double xValue = dataset.getXValue(series, item);
    final double yValue = dataset.getYValue(series, item);
    if (!Double.isFinite(xValue) || !Double.isFinite(yValue)) {
      return null;
    }

    final double xPx = plot.getDomainAxis()
        .valueToJava2D(xValue, dataArea, plot.getDomainAxisEdge());
    final double yPx = plot.getRangeAxis().valueToJava2D(yValue, dataArea, plot.getRangeAxisEdge());
    if (!Double.isFinite(xPx) || !Double.isFinite(yPx)) {
      return null;
    }

    final XYItemRenderer renderer = plot.getRendererForDataset(dataset);
    final Font font = renderer != null ? renderer.getItemLabelFont(series, item) : FALLBACK_FONT;
    final Font resolvedFont = font != null ? font : FALLBACK_FONT;

    final Rectangle2D textBounds = resolvedFont.getStringBounds(label, FRC);
    final double width = textBounds.getWidth() + 2 * PADDING;
    final double height = textBounds.getHeight() + 2 * PADDING;

    // assumption: vertical bars with default ItemLabelPosition.OUTSIDE12 — label centered above
    // the bar top. ColoredXYBarRenderer keeps that default, and line renderers labeling the
    // series-max point sit close enough that the same anchor works for collision detection.
    final double x = xPx - width / 2.0;
    final double y = yPx - height - LABEL_GAP;
    return new Rectangle2D.Double(x, y, width, height);
  }
}
