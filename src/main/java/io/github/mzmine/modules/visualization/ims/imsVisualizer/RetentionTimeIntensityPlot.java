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
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

public class RetentionTimeIntensityPlot extends EChartViewer {

  private XYPlot plot;
  private JFreeChart chart;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
  private double selectedRetention;
  private ValueMarker marker;
  private EStandardChartTheme theme;

  public RetentionTimeIntensityPlot(
      XYDataset dataset,
      ImsVisualizerTask imsVisualizerTask,
      RetentionTimeMobilityHeatMapPlot retentionTimeMobilityHeatMapPlot) {

    super(
        ChartFactory.createXYLineChart(
            "",
            "retention time",
            "intensity",
            dataset,
            PlotOrientation.VERTICAL,
            false,
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
    retentionTimeMobilityHeatMapPlot.getPlot().clearDomainMarkers();
    retentionTimeMobilityHeatMapPlot.getPlot().addDomainMarker(marker);

    addChartMouseListener(
        new ChartMouseListenerFX() {
          @Override
          public void chartMouseClicked(ChartMouseEventFX event) {


            ChartEntity chartEntity = event.getEntity();
            // If entity is not selected then calculate the nearest entity to selected one.
            if (chartEntity == null || !(chartEntity instanceof XYItemEntity)) {
              int x = (int) ((event.getTrigger().getX() - getInsets().getLeft()) / getScaleX());
              int y = (int) ((event.getTrigger().getY() - getInsets().getRight()) / getScaleY());
              Point2D point2d = new Point2D.Double(x, y);
              double minDistance = Integer.MAX_VALUE;
              Collection entities = getRenderingInfo().getEntityCollection().getEntities();

              for (Iterator iter = entities.iterator(); iter.hasNext(); ) {
                ChartEntity element = (ChartEntity) iter.next();

                if (isDataEntity(element)) {
                  Rectangle rect = element.getArea().getBounds();
                  Point2D centerPoint = new Point2D.Double(rect.getCenterX(), rect.getCenterY());

                  if (point2d.distance(centerPoint) < minDistance) {
                    minDistance = point2d.distance(centerPoint);
                    chartEntity = element;
                  }
                }
              }
            }
            // Now entity must be selected.
            if (chartEntity != null) {
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
                retentionTimeMobilityHeatMapPlot.getPlot().clearDomainMarkers();
                retentionTimeMobilityHeatMapPlot.getPlot().addDomainMarker(marker);
              }
            }

          }

          protected boolean isDataEntity(ChartEntity entity) {
            return ((entity instanceof XYItemEntity));
          }

          @Override
          public void chartMouseMoved(ChartMouseEventFX event) {}
        });
  }
}
