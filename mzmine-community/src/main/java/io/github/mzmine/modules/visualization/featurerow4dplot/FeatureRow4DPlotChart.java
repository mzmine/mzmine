package io.github.mzmine.modules.visualization.featurerow4dplot;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.FxChartFactory;
import io.github.mzmine.gui.chartbasics.chartutils.ColoredBubbleDatasetRenderer;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.MathUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.AbstractXYZDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Chart for the {@link FeatureRow4DPlotController}. Hosts up to two plot datasets:
 * <ul>
 *   <li>index 0 — the {@link FeatureRow4DPlotDataset} (m/z × RT × color × bubble), rendered with a
 *       {@link ColoredBubbleDatasetRenderer};</li>
 *   <li>index 1 — a single-series overlay of the currently selected rows, drawn as outlined
 *       circles on top of the bubbles.</li>
 * </ul>
 * The constructor wires the chart with an empty dataset so the controller can build the view
 * before data is ready and then push a dataset later via {@link #applyData(FeatureRow4DPlotDataset)}.
 */
public class FeatureRow4DPlotChart extends EChartViewer {

  private static final int DATA_INDEX = 0;
  private static final int SELECTION_INDEX = 1;
  // Pixels. ColoredBubbleDatasetRenderer caps bubble size at 15px diameter (so radius 7.5), so an
  // outline radius of ~12 px leaves a clear gap around the largest bubble while still being a tight
  // ring around the smallest 3px bubble.
  private static final double SELECTION_OUTLINE_RADIUS = 12.0;
  private static final Color LEGEND_BG = new Color(0, 0, 0, 0);

  private @Nullable FeatureRow4DPlotDataset currentDataset;
  private @Nullable PaintScaleLegend currentLegend;
  private @NotNull String colorAxisLabel = "Color";

  public FeatureRow4DPlotChart() {
    super(FxChartFactory.createScatterPlot("", "m/z", "Retention time / min", new EmptyXYZDataset(),
        PlotOrientation.VERTICAL, false, true, true));
    setStickyZeroRangeAxis(false);
    ConfigService.getConfiguration().getDefaultChartTheme().apply(this);

    final XYPlot plot = getChart().getXYPlot();
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);
    if (plot.getDomainAxis() instanceof NumberAxis x) {
      x.setAutoRangeIncludesZero(false);
    }
    if (plot.getRangeAxis() instanceof NumberAxis y) {
      y.setAutoRangeIncludesZero(false);
    }
  }

  /**
   * Replaces the current data and rebuilds the paint scale + legend. Resets the selection overlay —
   * the controller should re-apply it after a data swap.
   */
  public void applyData(@NotNull final FeatureRow4DPlotDataset dataset) {
    final XYPlot plot = getChart().getXYPlot();
    final double[] zValues = dataset.getZValues();
    final double[] quantiles = zValues.length == 0 ? new double[]{0.0, 1.0}
        : MathUtils.calcQuantile(zValues, new double[]{0.00, 1.00});
    // decision: guarantee a non-degenerate range so the linear paint scale doesn't collapse to a
    // single colour when every row has the same z value (e.g. RT is constant).
    final double lower = quantiles[0];
    final double upper = quantiles[1] > quantiles[0] ? quantiles[1] : quantiles[0] + 1.0;

    final PaintScale paintScale = ConfigService.getConfiguration().getDefaultPaintScalePalette()
        .toPaintScale(PaintScaleTransform.LINEAR, Range.closed(lower, upper));

    final ColoredBubbleDatasetRenderer renderer = new ColoredBubbleDatasetRenderer();
    renderer.setPaintScale(paintScale);
    renderer.setDefaultItemLabelsVisible(false);
    renderer.setDefaultToolTipGenerator(new FeatureRow4DPlotToolTipGenerator(dataset));

    plot.setDataset(DATA_INDEX, dataset);
    plot.setRenderer(DATA_INDEX, renderer);

    currentDataset = dataset;
    colorAxisLabel = dataset.getColorAxisLabel();
    rebuildPaintScaleLegend(paintScale);
    // Clear any prior selection overlay — its indices may no longer be valid for the new dataset.
    plot.setDataset(SELECTION_INDEX, null);
    plot.setRenderer(SELECTION_INDEX, null);
  }

  public void clearData() {
    final XYPlot plot = getChart().getXYPlot();
    plot.setDataset(DATA_INDEX, new EmptyXYZDataset());
    plot.setRenderer(DATA_INDEX, null);
    plot.setDataset(SELECTION_INDEX, null);
    plot.setRenderer(SELECTION_INDEX, null);
    currentDataset = null;
    removeCurrentLegend();
  }

  public void setOrientation(@NotNull final PlotOrientation orientation) {
    getChart().getXYPlot().setOrientation(orientation);
  }

  /**
   * Updates the selection overlay to mark the supplied rows with an outlined circle. Rows that are
   * not present in the current dataset are silently skipped.
   */
  public void setSelectedRows(@NotNull final List<FeatureListRow> rows) {
    final XYPlot plot = getChart().getXYPlot();
    final FeatureRow4DPlotDataset dataset = currentDataset;
    if (dataset == null || rows.isEmpty()) {
      plot.setDataset(SELECTION_INDEX, null);
      plot.setRenderer(SELECTION_INDEX, null);
      return;
    }

    final XYSeries series = new XYSeries("Selected", false, true);
    for (final FeatureListRow row : rows) {
      final int i = dataset.indexOfRow(row);
      if (i < 0) {
        continue;
      }
      series.add(dataset.getXValue(0, i), dataset.getYValue(0, i));
    }
    if (series.isEmpty()) {
      plot.setDataset(SELECTION_INDEX, null);
      plot.setRenderer(SELECTION_INDEX, null);
      return;
    }

    final XYSeriesCollection selectionDataset = new XYSeriesCollection(series);
    final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
    final double diameter = SELECTION_OUTLINE_RADIUS * 2.0;
    renderer.setSeriesShape(0,
        new Ellipse2D.Double(-SELECTION_OUTLINE_RADIUS, -SELECTION_OUTLINE_RADIUS, diameter,
            diameter));
    renderer.setSeriesPaint(0, ConfigService.getDefaultColorPalette().getNegativeColorAWT());
    renderer.setSeriesStroke(0, new BasicStroke(2.0f));
    renderer.setDefaultShapesFilled(false);
    renderer.setUseFillPaint(false);
    renderer.setSeriesVisibleInLegend(0, false);

    plot.setDataset(SELECTION_INDEX, selectionDataset);
    plot.setRenderer(SELECTION_INDEX, renderer);
  }

  // --- internals -------------------------------------------------------------

  private void rebuildPaintScaleLegend(@NotNull final PaintScale scale) {
    final JFreeChart chart = getChart();
    removeCurrentLegend();

    final XYPlot plot = chart.getXYPlot();
    final Paint axisPaint = plot.getDomainAxis().getAxisLinePaint();
    final NumberAxis scaleAxis = new NumberAxis(colorAxisLabel);
    scaleAxis.setRange(scale.getLowerBound(), scale.getUpperBound());
    scaleAxis.setAxisLinePaint(axisPaint);
    scaleAxis.setTickMarkPaint(axisPaint);
    scaleAxis.setLabelPaint(axisPaint);
    scaleAxis.setTickLabelPaint(axisPaint);
    scaleAxis.setLabelFont(plot.getDomainAxis().getLabelFont());
    scaleAxis.setTickLabelFont(plot.getDomainAxis().getTickLabelFont());
    scaleAxis.setNumberFormatOverride(new DecimalFormat("0.##"));

    final PaintScaleLegend legend = new PaintScaleLegend(scale, scaleAxis);
    legend.setPadding(5, 0, 5, 0);
    legend.setStripOutlineVisible(false);
    legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    legend.setAxisOffset(5.0);
    legend.setSubdivisionCount(500);
    legend.setPosition(RectangleEdge.RIGHT);
    legend.setBackgroundPaint(LEGEND_BG);

    chart.addSubtitle(legend);
    currentLegend = legend;
  }

  private void removeCurrentLegend() {
    final JFreeChart chart = getChart();
    if (currentLegend != null) {
      chart.removeSubtitle(currentLegend);
      currentLegend = null;
      return;
    }
    // Defensive sweep — some upstream code paths could have replaced the subtitle list.
    for (int i = 0; i < chart.getSubtitleCount(); i++) {
      final Title t = chart.getSubtitle(i);
      if (t instanceof PaintScaleLegend) {
        chart.removeSubtitle(t);
        break;
      }
    }
  }

  /**
   * Placeholder dataset used until the controller pushes real data. Carries no items so the chart
   * renders an empty plot area without throwing.
   */
  private static final class EmptyXYZDataset extends AbstractXYZDataset {

    @Override
    public int getSeriesCount() {
      return 1;
    }

    @Override
    public Comparable<?> getSeriesKey(final int series) {
      return "empty";
    }

    @Override
    public int getItemCount(final int series) {
      return 0;
    }

    @Override
    public Number getX(final int series, final int item) {
      return 0;
    }

    @Override
    public Number getY(final int series, final int item) {
      return 0;
    }

    @Override
    public Number getZ(final int series, final int item) {
      return 0;
    }
  }
}
