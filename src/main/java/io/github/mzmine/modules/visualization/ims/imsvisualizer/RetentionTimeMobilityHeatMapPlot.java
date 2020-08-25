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

package io.github.mzmine.modules.visualization.ims.imsvisualizer;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizePaintScales;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYZDataset;

import java.awt.*;
import java.util.Arrays;
import java.util.logging.Logger;

public class RetentionTimeMobilityHeatMapPlot extends EChartViewer {

    private final XYPlot plot;
    static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
    private PaintScaleLegend legend;
    public XYBlockPixelSizeRenderer pixelRenderer;
    public XYBlockRenderer blockRenderer;

    public RetentionTimeMobilityHeatMapPlot(XYZDataset dataset, String paintScaleStyle) {

        super(
                ChartFactory.createScatterPlot(
                        "", "retention time", "mobility", dataset, PlotOrientation.VERTICAL, true, true, true));

        JFreeChart chart = getChart();
        // copy and sort z-Values for min and max of the paint scale
        double[] copyZValues = new double[dataset.getItemCount(0)];
        for (int i = 0; i < dataset.getItemCount(0); i++) {
            copyZValues[i] = dataset.getZValue(0, i);
        }
        Arrays.sort(copyZValues);

        // copy and sort x-values.
        double[] copyXValues = new double[dataset.getItemCount(0)];
        for (int i = 0; i < dataset.getItemCount(0); i++) {
            copyXValues[i] = dataset.getXValue(0, i);
        }
        Arrays.sort(copyXValues);

        // copy and sort y-values.
        double[] copyYValues = new double[dataset.getItemCount(0)];
        for (int i = 0; i < dataset.getItemCount(0); i++) {
            copyYValues[i] = dataset.getYValue(0, i);
        }
        Arrays.sort(copyYValues);

        // get index in accordance to percentile windows
        int minIndexScale = 0;
        int maxIndexScale = copyZValues.length - 1;
        double min = copyZValues[minIndexScale];
        double max = copyZValues[maxIndexScale];
        Paint[] contourColors =
                XYBlockPixelSizePaintScales.getPaintColors(
                        "", Range.closed(min, max), paintScaleStyle);
        LookupPaintScale scale = new LookupPaintScale(min, max, Color.BLACK);

        double[] scaleValues = new double[contourColors.length];
        double delta = (max - min) / (contourColors.length - 1);
        double value = min;
        for (int i = 0; i < contourColors.length; i++) {
            scaleValues[i] = value;
            scale.add(value, contourColors[i]);
            value = value + delta;
        }

        plot = chart.getXYPlot();
        EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
        theme.apply(chart);

        // set the pixel renderer
        setPixelRenderer(copyXValues, copyYValues, scale);
        //set the legend
        prepareLegend(min, max, scale);

        blockRenderer.setPaintScale(scale);
        plot.setRenderer(blockRenderer);
        plot.setBackgroundPaint(Color.black);
        plot.setRangeGridlinePaint(Color.black);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setOutlinePaint(Color.black);
        chart.addSubtitle(legend);
        plot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

    }

    void setPixelRenderer(double[] copyXValues, double[] copyYValues, LookupPaintScale scale) {
        pixelRenderer = new XYBlockPixelSizeRenderer();
        pixelRenderer.setPaintScale(scale);
        // set the block renderer
        blockRenderer = new XYBlockRenderer();
        double retentionWidth = 0.0;
        double mobilityWidth = 0.0;

        for (int i = 0; i + 1 < copyXValues.length; i++) {
            if (copyXValues[i] != copyXValues[i + 1]) {
                retentionWidth = copyXValues[i + 1] - copyXValues[i];
                break;
            }
        }
        for (int i = 0; i + 1 < copyYValues.length; i++) {
            if (copyYValues[i] != copyYValues[i + 1]) {
                mobilityWidth = copyYValues[i + 1] - copyYValues[i];
                break;
            }
        }

        if (mobilityWidth <= 0.0 || retentionWidth <= 0.0) {
            throw new IllegalArgumentException(
                    "there must be atleast two unique value of retentio time and mobility");
        }
        blockRenderer.setBlockHeight(mobilityWidth);
        blockRenderer.setBlockWidth(retentionWidth);
    }

    void prepareLegend(double min, double max, LookupPaintScale scale) {
        // Legend
        NumberAxis scaleAxis = new NumberAxis("Intensity");
        scaleAxis.setRange(min, max);
        scaleAxis.setAxisLinePaint(Color.white);
        scaleAxis.setTickMarkPaint(Color.white);
        legend = new PaintScaleLegend(scale, scaleAxis);

        legend.setStripOutlineVisible(false);
        legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
        legend.setAxisOffset(5.0);
        legend.setMargin(new RectangleInsets(3, 3, 3, 3));
        legend.setFrame(new BlockBorder(Color.white));
        legend.setPadding(new RectangleInsets(5, 5, 5, 5));
        legend.setStripWidth(5);
        legend.setPosition(RectangleEdge.TOP);
        legend.getAxis().setLabelFont(legendFont);
        legend.getAxis().setTickLabelFont(legendFont);
    }

    public XYPlot getPlot() {
        return plot;
    }
}
