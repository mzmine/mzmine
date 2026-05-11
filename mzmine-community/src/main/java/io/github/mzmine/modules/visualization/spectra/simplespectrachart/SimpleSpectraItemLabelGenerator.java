package io.github.mzmine.modules.visualization.spectra.simplespectrachart;

import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * Spectra-specific label generator that performs screen-space collision detection. For every item
 * it estimates the {@link Rectangle2D} that the rendered label would occupy and checks against the
 * cache of already drawn label bounds on {@link SimpleSpectraChartModel#getDrawnLabelBounds()}. The
 * actual painting is still done by JFreeChart's {@code XYBarRenderer.drawItemLabel} (and the line
 * renderer equivalent) — this generator only decides which items get a label.
 *
 * <p>The cache is cleared by a {@code PlotChangeListener} registered in
 * {@link SimpleSpectraChartViewBuilder}.
 */
public class SimpleSpectraItemLabelGenerator implements XYItemLabelGenerator {

  // Padding around the text bounds and vertical gap between the data point and the label.
  // Matches ColoredBubbleDatasetRenderer's constants so spectra and bubble plots feel consistent.
  private static final double PADDING = 2.0;
  private static final double LABEL_GAP = 2.0;

  // assumption: a shared, immutable FontRenderContext is good enough for collision detection.
  // It does not need to match the chart's actual antialiasing settings exactly because we only
  // care about approximate text extents, not pixel-perfect rendering.
  private static final FontRenderContext FRC = new FontRenderContext(null, false, false);

  private static final Font FALLBACK_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10);

  private final @NotNull SimpleSpectraChartModel model;

  public SimpleSpectraItemLabelGenerator(final @NotNull SimpleSpectraChartModel model) {
    this.model = model;
  }

  @Override
  public @Nullable String generateLabel(final @NotNull XYDataset dataset, final int series,
      final int item) {
    if (!(dataset instanceof ColoredXYDataset cxy)) {
      return null;
    }

    final String label = cxy.getLabel(item);
    if (label == null || label.isBlank()) {
      return null;
    }

    final XYPlot plot = model.getXYPlot();
    final Rectangle2D dataArea = currentDataArea();
    if (dataArea == null || dataArea.isEmpty()) {
      // First paint pass before rendering info is populated. Show the label rather than swallow it;
      // the next render cycle will produce a real data area and proper collision detection.
      return label;
    }

    final Rectangle2D labelBounds = computeLabelBounds(plot, dataset, series, item, label,
        dataArea);
    if (labelBounds == null) {
      return null;
    }

    // Only allow labels that fit fully inside the dataArea, shrunk by the configured bottom margin.
    // Reasons to reject:
    //   - top: a very tall peak's label would extend above the plot area;
    //   - left/right: a peak at the chart edge would have its label clipped horizontally;
    //   - bottom: low-intensity peaks whose label would sit inside the excluded bottom strip
    //     (e.g. on top of the axis labels) get filtered out by the bottom-margin shrink.
    final double bottomMargin = Math.max(0.0, model.getBottomLabelMargin());
    final double allowedHeight = Math.max(0.0, dataArea.getHeight() - bottomMargin);
    final Rectangle2D allowedArea = new Rectangle2D.Double(dataArea.getX(), dataArea.getY(),
        dataArea.getWidth(), allowedHeight);
    if (allowedHeight <= 0.0 || !allowedArea.contains(labelBounds)) {
      return null;
    }

    // Single shared cache across all datasets — typical mzmine spectra plots stack several
    // single-series datasets (raw scan, peak overlays, predicted ions), and the user-facing goal
    // is that labels from any of them respect bounds from any other.
    final List<Rectangle2D> cache = model.getDrawnLabelBounds();
    for (final Rectangle2D existing : cache) {
      if (existing.intersects(labelBounds)) {
        return null;
      }
    }

    cache.add(labelBounds);
    return label;
  }

  private @Nullable Rectangle2D currentDataArea() {
    final ChartRenderingInfo info = model.getXYPlot().getRenderingInfo();
    if (info == null) {
      return null;
    }
    final PlotRenderingInfo plotInfo = info.getPlotInfo();
    if (plotInfo == null) {
      return null;
    }
    return plotInfo.getDataArea();
  }

  private @Nullable Rectangle2D computeLabelBounds(final @NotNull XYPlot plot,
      final @NotNull XYDataset dataset, final int series, final int item,
      final @NotNull String label, final @NotNull Rectangle2D dataArea) {

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
    // the bar top. ColoredXYBarRenderer keeps that default, and ColoredXYLineRenderer's labels are
    // close enough that the same anchor works for collision detection.
    final double x = xPx - width / 2.0;
    final double y = yPx - height - LABEL_GAP;
    return new Rectangle2D.Double(x, y, width, height);
  }
}
