/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.ims.imsvisualizer;
/*

import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerTask;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
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

    private final XYPlot plot;
    static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
    private double selectedRetention;
    private ValueMarker marker;

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
        JFreeChart chart = getChart();
        EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
        theme.apply(chart);
        plot = chart.getXYPlot();
        this.selectedRetention = imsVisualizerTask.getSelectedRetentionTime();
        var renderer = new XYLineAndShapeRenderer(true, true);
        renderer.setSeriesPaint(0, Color.BLACK);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesStroke(0, new BasicStroke(1.0f));

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.WHITE);
        plot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());


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
                    public void chartMouseMoved(ChartMouseEventFX event) {
                    }
                });
    }
}
*/