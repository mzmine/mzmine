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


package io.github.mzmine.modules.visualization.vankrevelendiagram;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.ColoredBubbleDatasetRenderer;
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

  public VanKrevelenDiagramChart(String title, String xAxisLabel, String yAxisLabel, String colorScaleLabel, VanKrevelenDiagramXYZDataset dataset) {
    super(ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, false, true, true));
    this.colorScaleLabel = colorScaleLabel;
    setStickyZeroRangeAxis(false);

    EStandardChartTheme defaultChartTheme = MZmineCore.getConfiguration().getDefaultChartTheme();
    defaultChartTheme.apply(this);
    double[] colorScaleValues = dataset.getColorScaleValues();
    final double[] quantiles = MathUtils.calcQuantile(colorScaleValues, new double[]{0.00, 1.00});
    PaintScale paintScale = MZmineCore.getConfiguration().getDefaultPaintScalePalette().toPaintScale(PaintScaleTransform.LINEAR, Range.closed(quantiles[0], quantiles[1]));
    getChart().getXYPlot().getRangeAxis().setRange(Arrays.stream(dataset.getyValues()).min().orElse(0.0),
        Arrays.stream(dataset.getyValues()).max().orElse(0.0));
    getChart().getXYPlot().getDomainAxis().setRange(Arrays.stream(dataset.getxValues()).min().orElse(0.0),
        Arrays.stream(dataset.getxValues()).max().orElse(0.0));
    ColoredBubbleDatasetRenderer renderer = new ColoredBubbleDatasetRenderer();
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
    scaleAxis.setRange(scale.getLowerBound(), Math.max(scale.getUpperBound(), scale.getUpperBound()));
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
