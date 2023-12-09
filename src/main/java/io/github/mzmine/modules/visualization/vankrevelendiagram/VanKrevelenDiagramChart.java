package io.github.mzmine.modules.visualization.vankrevelendiagram;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.XYCirclePixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.MathUtils;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.text.DecimalFormat;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;

public class VanKrevelenDiagramChart extends EChartViewer {

  private final String colorScaleLabel;
  private final Color legendBg = new Color(0, 0, 0, 0);

  public VanKrevelenDiagramChart(String title, String xAxisLabel, String yAxisLabel,
      String colorScaleLabel, VanKrevelenDiagramXYZDataset dataset) {
    super(ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, dataset,
        PlotOrientation.VERTICAL, false, true, true));
    this.colorScaleLabel = colorScaleLabel;

    EStandardChartTheme defaultChartTheme = MZmineCore.getConfiguration().getDefaultChartTheme();
    defaultChartTheme.apply(this);
    double[] colorScaleValues = dataset.getColorScaleValues();
    final double[] quantiles = MathUtils.calcQuantile(colorScaleValues, new double[]{0.00, 1.00});
    PaintScale paintScale = MZmineCore.getConfiguration().getDefaultPaintScalePalette()
        .toPaintScale(PaintScaleTransform.LINEAR, Range.closed(quantiles[0], quantiles[1]));
    getChart().getXYPlot().getRangeAxis()
        .setRange(Arrays.stream(dataset.getyValues()).min().orElse(0.0),
            Arrays.stream(dataset.getyValues()).max().orElse(0.0));
    getChart().getXYPlot().getDomainAxis()
        .setRange(Arrays.stream(dataset.getxValues()).min().orElse(0.0),
            Arrays.stream(dataset.getxValues()).max().orElse(0.0));
    XYCirclePixelSizeRenderer renderer = new XYCirclePixelSizeRenderer();
    renderer.setPaintScale(paintScale);
    PaintScaleLegend legend = generateLegend(paintScale);
    getChart().addSubtitle(legend);
    this.getChart().getXYPlot().setRenderer(renderer);
  }

  private PaintScaleLegend generateLegend(@NotNull PaintScale scale) {
    Paint axisPaint = this.getChart().getXYPlot().getDomainAxis().getAxisLinePaint();
    Font axisLabelFont = this.getChart().getXYPlot().getDomainAxis().getLabelFont();
    Font axisTickLabelFont = this.getChart().getXYPlot().getDomainAxis().getTickLabelFont();

    NumberAxis scaleAxis = new NumberAxis(null);
    scaleAxis.setRange(scale.getLowerBound(),
        Math.max(scale.getUpperBound(), scale.getUpperBound()));
    scaleAxis.setAxisLinePaint(axisPaint);
    scaleAxis.setTickMarkPaint(axisPaint);
    scaleAxis.setNumberFormatOverride(new DecimalFormat("0.#"));
    scaleAxis.setLabelFont(axisLabelFont);
    scaleAxis.setLabelPaint(axisPaint);
    scaleAxis.setTickLabelFont(axisTickLabelFont);
    scaleAxis.setTickLabelPaint(axisPaint);
    if (colorScaleLabel != null) {
      scaleAxis.setLabel(colorScaleLabel);
    }
    PaintScaleLegend newLegend = new PaintScaleLegend(scale, scaleAxis);
    newLegend.setPadding(5, 0, 5, 0);
    newLegend.setStripOutlineVisible(false);
    newLegend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    newLegend.setAxisOffset(5.0);
    newLegend.setSubdivisionCount(500);
    newLegend.setPosition(RectangleEdge.RIGHT);
    newLegend.setBackgroundPaint(legendBg);
    return newLegend;
  }

}
