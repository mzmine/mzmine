/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.ims.imsVisualizer;

import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerTask;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.util.logging.Logger;

public class IntensityRetentionTimePlot extends EChartViewer {

  private XYPlot plot;
  private JFreeChart chart;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
  private double selectedRetention;
  private ValueMarker marker;
  private EStandardChartTheme theme;

  public IntensityRetentionTimePlot(
      XYDataset dataset,
      ImsVisualizerTask imsVisualizerTask,
      MobilityRetentionHeatMapPlot mobilityRetentionHeatMapPlot) {

    super(
        ChartFactory.createXYLineChart(
            "",
            "retention time",
            "intensity",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false));
    chart = getChart();
    theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(chart);
    plot = chart.getXYPlot();
    this.selectedRetention = imsVisualizerTask.getSelectedRetentionTime();
    var renderer = new XYLineAndShapeRenderer(true, true);
    renderer.setSeriesPaint(0, Color.GREEN);
    renderer.setSeriesShapesVisible(0, false);
    renderer.setSeriesStroke(0, new BasicStroke(1.0f));

    plot.setRenderer(renderer);
    plot.setBackgroundPaint(Color.WHITE);
    plot.setRangeGridlinePaint(Color.WHITE);
    plot.setDomainGridlinePaint(Color.WHITE);
    chart.getLegend().setFrame(BlockBorder.NONE);

    plot.clearDomainMarkers();
    marker = new ValueMarker(selectedRetention);
    marker.setPaint(Color.red);
    marker.setLabelFont(legendFont);
    marker.setStroke(new BasicStroke(2));
    marker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
    marker.setLabelTextAnchor(TextAnchor.BASELINE_CENTER);
    plot.addDomainMarker(marker);

    imsVisualizerTask.setSelectedRetentionTime(selectedRetention);
    imsVisualizerTask.updateMobilityGroup();
    //  marker to the mobility-retention time heatmap plot.
    mobilityRetentionHeatMapPlot.getPlot().clearDomainMarkers();
    mobilityRetentionHeatMapPlot.getPlot().addDomainMarker(marker);

    addChartMouseListener(
        new ChartMouseListenerFX() {
          @Override
          public void chartMouseClicked(ChartMouseEventFX event) {
            ChartEntity chartEntity = event.getEntity();
            if (chartEntity instanceof XYItemEntity) {
              XYItemEntity entity = (XYItemEntity) chartEntity;
              int serindex = entity.getSeriesIndex();
              int itemindex = entity.getItem();
              selectedRetention = dataset.getXValue(serindex, itemindex);
              // Get controller
              imsVisualizerTask.setSelectedRetentionTime(selectedRetention);
              imsVisualizerTask.updateMobilityGroup();

              // setting the marker at seleted range.
              plot.clearDomainMarkers();
              marker = new ValueMarker(selectedRetention);
              marker.setPaint(Color.red);
              marker.setLabelFont(legendFont);
              marker.setStroke(new BasicStroke(2));
              marker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
              marker.setLabelTextAnchor(TextAnchor.BASELINE_CENTER);
              plot.addDomainMarker(marker);

              //  marker to the mobility-retention time heatmap plot.
              mobilityRetentionHeatMapPlot.getPlot().clearDomainMarkers();
              mobilityRetentionHeatMapPlot.getPlot().addDomainMarker(marker);
            }
          }

          @Override
          public void chartMouseMoved(ChartMouseEventFX event) {}
        });
  }

  public void setLegend(PaintScaleLegend legend) {
    chart.addSubtitle(legend);
  }
}
