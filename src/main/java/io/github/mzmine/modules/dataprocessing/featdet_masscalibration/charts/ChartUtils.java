/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration.charts;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.MassPeakMatch;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.function.Function2D;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;

public class ChartUtils {

  public static final BasicStroke DEFAULT_2P_STROKE = new BasicStroke(2.0f);

  public static double calculateRSquared(XYDataItem[] items, Function2D trend) {
    double yMean = Arrays.stream(items).mapToDouble(item -> item.getYValue()).average().orElse(0);
    double ssTot = Arrays.stream(items).mapToDouble(item -> Math.pow(item.getYValue() - yMean, 2))
        .sum();
    double ssRes = Arrays.stream(items)
        .mapToDouble(item -> Math.pow(item.getYValue() - trend.getValue(item.getXValue()), 2))
        .sum();
    double rSquared = 1 - ssRes / ssTot;
    return rSquared;
  }

  public static void cleanPlot(XYPlot plot) {
    for (int i = 0; i < plot.getDatasetCount(); i++) {
      plot.setDataset(i, null);
    }
    plot.clearRangeMarkers();
    plot.clearAnnotations();
  }

  public static void cleanPlotLabels(XYPlot plot) {
    plot.clearRangeMarkers();
    plot.clearAnnotations();
  }

  public static ValueMarker createValueMarker(String label, double value) {
    NumberFormat ppmFormat = MZmineCore.getConfiguration().getPPMFormat();
    ValueMarker valueMarker = new ValueMarker(value);
    valueMarker.setLabel(String.format("%s: %s", label, ppmFormat.format(value)));
    valueMarker.setPaint(Color.blue);
    valueMarker.setLabelTextAnchor(TextAnchor.BASELINE_LEFT);
    valueMarker.setLabelPaint(Color.blue);
    valueMarker.setLabelFont(new Font(null, 0, 11));
    return valueMarker;
  }

  public static String generateTooltipText(List<MassPeakMatch> matches, int item) {
    MassPeakMatch match = matches.get(item);
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    NumberFormat ppmFormat = MZmineCore.getConfiguration().getPPMFormat();
    String tooltipText = String.format(
        "Measured-matched m/z: %s-%s" + "\nMeasured-matched RT: %s-%s" + "\nMass error: %s %s"
            + "\nMass peak intensity: %s" + "\nScan number: %s",
        mzFormat.format(match.getMeasuredMzRatio()), mzFormat.format(match.getMatchedMzRatio()),
        rtFormat.format(match.getMeasuredRetentionTime()),
        match.getMatchedRetentionTime() == -1 ? "none"
            : rtFormat.format(match.getMatchedRetentionTime()),
        ppmFormat.format(match.getMzError()), match.getMzErrorType(),
        intensityFormat.format(match.getMeasuredDataPoint().getIntensity()), match.getScan());
    StringBuilder tooltipTextBuilder = new StringBuilder(tooltipText);
    if (match.getMatchedCalibrant().getMolecularFormula() != null) {
      tooltipTextBuilder.append(
          "\nIon formula: " + match.getMatchedCalibrant().getMolecularFormula());
    }
    if (match.getMatchedCalibrant().getName() != null) {
      tooltipTextBuilder.append(
          "\nMatched calibrant name: " + match.getMatchedCalibrant().getName());
    }
    return tooltipTextBuilder.toString();
  }

  public static XYToolTipGenerator createTooltipGenerator() {
    return new XYToolTipGenerator() {
      @Override
      public String generateToolTip(XYDataset dataset, int series, int item) {
        double yValue = dataset.getYValue(series, item);
        double xValue = dataset.getXValue(series, item);
        return String.format("x: %s, y: %s", xValue, yValue);
      }
    };
  }

  public static XYLineAndShapeRenderer createErrorsRenderer() {
    XYLineAndShapeRenderer errorsRenderer = new XYLineAndShapeRenderer(false, true);
    Shape circle = new Ellipse2D.Double(-2, -2, 4, 4);
    Color paintColor = new Color(230, 160, 30, 100);
    errorsRenderer.setSeriesShape(0, circle);
    errorsRenderer.setSeriesPaint(0, paintColor);
    errorsRenderer.setSeriesFillPaint(0, paintColor);
    errorsRenderer.setSeriesOutlinePaint(0, paintColor);
    errorsRenderer.setUseFillPaint(true);
    errorsRenderer.setUseOutlinePaint(true);

    errorsRenderer.setDefaultToolTipGenerator(createTooltipGenerator());

    return errorsRenderer;
  }

  public static XYLineAndShapeRenderer createTrendRenderer() {
    XYLineAndShapeRenderer trendRenderer = new XYLineAndShapeRenderer();
    Shape circle = new Ellipse2D.Double(-2, -2, 4, 4);
    Color paintColor = new Color(0, 0, 0, 150);
    Stroke stroke = new BasicStroke(2);
    trendRenderer.setSeriesShape(0, circle);
    trendRenderer.setSeriesPaint(0, paintColor);
    trendRenderer.setSeriesStroke(0, stroke);
    trendRenderer.setDefaultStroke(stroke);
    trendRenderer.setAutoPopulateSeriesStroke(false);
    trendRenderer.setAutoPopulateSeriesPaint(false);
    return trendRenderer;
  }
}
