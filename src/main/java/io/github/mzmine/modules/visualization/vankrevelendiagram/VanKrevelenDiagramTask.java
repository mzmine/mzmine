/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.vankrevelendiagram;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;
import com.google.common.collect.Range;

import io.github.mzmine.datamodel.PeakIdentity;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.gui.chartbasics.chartutils.NameItemLabelGenerator;
import io.github.mzmine.gui.chartbasics.chartutils.ScatterPlotToolTipGenerator;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizePaintScales;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.gui.swing.EChartPanel;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

/**
 * Task to create a Van Krevelen Diagram of selected features of a selected
 * feature list
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class VanKrevelenDiagramTask extends AbstractTask {

    static final Font legendFont = new Font("SansSerif", Font.PLAIN, 12);
    static final Font titleFont = new Font("SansSerif", Font.PLAIN, 12);

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private JFreeChart chart;
    private PeakList peakList;
    private String zAxisLabel;
    private String zAxisScaleType;
    private Range<Double> zScaleRange;
    private String paintScaleStyle;
    private PeakListRow rows[];
    private PeakListRow filteredRows[];
    private String title;
    private int displayedFeatures;
    private int featuresWithoutFormula;
    private int featuresWithFormulasWithoutCHO;
    private int totalSteps = 3, appliedSteps = 0;

    public VanKrevelenDiagramTask(ParameterSet parameters) {
        peakList = parameters
                .getParameter(VanKrevelenDiagramParameters.peakList).getValue()
                .getMatchingPeakLists()[0];
        zAxisLabel = parameters
                .getParameter(VanKrevelenDiagramParameters.zAxisValues)
                .getValue().toString();
        zAxisScaleType = parameters
                .getParameter(VanKrevelenDiagramParameters.zScaleType)
                .getValue();
        zScaleRange = parameters
                .getParameter(VanKrevelenDiagramParameters.zScaleRange)
                .getValue();
        paintScaleStyle = parameters
                .getParameter(VanKrevelenDiagramParameters.paintScale)
                .getValue();
        rows = parameters
                .getParameter(VanKrevelenDiagramParameters.selectedRows)
                .getMatchingRows(peakList);
        filteredRows = filterSelectedRows(rows);
        title = "Van Krevelen Diagram [" + peakList + "]";
    }

    @Override
    public String getTaskDescription() {
        return "Van Krevelen Diagram for " + peakList;
    }

    @Override
    public double getFinishedPercentage() {
        return totalSteps == 0 ? 0 : (double) appliedSteps / totalSteps;
    }

    @Override
    public void run() {
        try {
            setStatus(TaskStatus.PROCESSING);
            logger.info("Create Van Krevelen diagram of " + peakList);
            // Task canceled?
            if (isCanceled())
                return;
            JFreeChart chart = null;
            // 2D, if no third dimension was selected
            if (zAxisLabel.equals("none")) {
                chart = create2DVanKrevelenDiagram();
            }
            // 3D, if a third dimension was selected
            else {
                chart = create3DVanKrevelenDiagram();
            }

            chart.setBackgroundPaint(Color.white);

            // create chart JPanel
            EChartPanel chartPanel = new EChartPanel(chart, true, true, true,
                    true, false);

            // Create Van Krevelen Diagram window
            VanKrevelenDiagramWindow frame = new VanKrevelenDiagramWindow(chart,
                    chartPanel, filteredRows);

            // create chart JPanel
            frame.add(chartPanel, BorderLayout.CENTER);

            // set title properties
            TextTitle chartTitle = chart.getTitle();
            chartTitle.setMargin(5, 0, 0, 0);
            chartTitle.setFont(titleFont);
            LegendTitle legend = chart.getLegend();
            legend.setVisible(false);
            frame.setTitle(title);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setBackground(Color.white);
            frame.setVisible(true);
            frame.pack();
            logger.info(
                    "Finished creating van Krevelen diagram of " + peakList);
            JOptionPane.showMessageDialog(frame, "Results summary:\n"
                    + displayedFeatures
                    + " feature list rows are displayed in the Van Krevelen diagram.\n"
                    + featuresWithFormulasWithoutCHO
                    + " feature list rows are not displayed, because the annotated molecular formula does not contain the elements C, H, and O.\n"
                    + featuresWithoutFormula
                    + " feature list rows are not displayed, because no molecular formula was assigned.");
            setStatus(TaskStatus.FINISHED);
        } catch (Throwable t) {
            setErrorMessage(
                    "Nothing to plot here or some peaks have other identities than molecular formulas.\n"
                            + "Have you annotated your features with molecular formulas?\n"
                            + "You can use the feature list method \"Formula prediction\" to handle the task.");
            setStatus(TaskStatus.ERROR);
        }
    }

    /**
     * create 2D Van Krevelen Diagram chart
     */
    private JFreeChart create2DVanKrevelenDiagram() {
        logger.info("Creating new 2D chart instance");
        appliedSteps++;

        // load dataset
        VanKrevelenDiagramXYDataset dataset2D = new VanKrevelenDiagramXYDataset(
                filteredRows);

        // create chart
        chart = ChartFactory.createScatterPlot(title, "O/C", "H/C", dataset2D,
                PlotOrientation.VERTICAL, true, true, false);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainCrosshairPaint(Color.GRAY);
        plot.setRangeCrosshairPaint(Color.GRAY);
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        appliedSteps++;

        // set renderer
        XYBlockPixelSizeRenderer renderer = new XYBlockPixelSizeRenderer();

        // calc block sizes
        double maxX = plot.getDomainAxis().getRange().getUpperBound();
        double maxY = plot.getRangeAxis().getRange().getUpperBound();

        renderer.setBlockWidth(0.001);
        renderer.setBlockHeight(renderer.getBlockWidth() / (maxX / maxY));

        // set tooltip generator
        ScatterPlotToolTipGenerator tooltipGenerator = new ScatterPlotToolTipGenerator(
                "O/C", "H/C", zAxisLabel, filteredRows);
        renderer.setSeriesToolTipGenerator(0, tooltipGenerator);
        plot.setRenderer(renderer);

        // set item label generator
        NameItemLabelGenerator generator = new NameItemLabelGenerator(
                filteredRows);
        renderer.setDefaultItemLabelGenerator(generator);
        renderer.setDefaultItemLabelsVisible(false);
        renderer.setDefaultItemLabelFont(legendFont);
        renderer.setDefaultPositiveItemLabelPosition(
                new ItemLabelPosition(ItemLabelAnchor.CENTER,
                        TextAnchor.TOP_RIGHT, TextAnchor.TOP_RIGHT, -45),
                true);

        return chart;
    }

    /**
     * create 3D Van Krevelen Diagram chart
     */
    private JFreeChart create3DVanKrevelenDiagram() {
        logger.info("Creating new 3D chart instance");
        appliedSteps++;
        // load data set
        VanKrevelenDiagramXYZDataset dataset3D = new VanKrevelenDiagramXYZDataset(
                zAxisLabel, filteredRows);

        // copy and sort z-Values for min and max of the paint scale
        double[] copyZValues = new double[dataset3D.getItemCount(0)];
        for (int i = 0; i < dataset3D.getItemCount(0); i++) {
            copyZValues[i] = dataset3D.getZValue(0, i);
        }
        Arrays.sort(copyZValues);
        // get index in accordance to percentile windows
        int minScaleIndex = 0;
        int maxScaleIndex = copyZValues.length - 1;
        double min = 0;
        double max = 0;

        if (zAxisScaleType.equals("percentile")) {
            minScaleIndex = (int) Math.floor(
                    copyZValues.length * (zScaleRange.lowerEndpoint() / 100));
            maxScaleIndex = copyZValues.length
                    - (int) (Math.ceil(copyZValues.length
                            * ((100 - zScaleRange.upperEndpoint()) / 100)));
            if (zScaleRange.upperEndpoint() == 100) {
                maxScaleIndex = copyZValues.length - 1;
            }
            if (zScaleRange.lowerEndpoint() == 0) {
                minScaleIndex = 0;
            }
            min = copyZValues[minScaleIndex];
            max = copyZValues[maxScaleIndex];
        }
        if (zAxisScaleType.equals("custom")) {
            min = zScaleRange.lowerEndpoint();
            max = zScaleRange.upperEndpoint();
        }

        // create paint scale for third dimension
        Paint[] contourColors = XYBlockPixelSizePaintScales
                .getPaintColors(zAxisScaleType, zScaleRange, paintScaleStyle);
        LookupPaintScale scale = null;
        scale = new LookupPaintScale(copyZValues[0],
                copyZValues[copyZValues.length - 1], new Color(0, 0, 0));
        double[] scaleValues = new double[contourColors.length];
        double delta = (max - min) / (contourColors.length - 1);
        double value = min;
        for (int i = 0; i < contourColors.length; i++) {
            scale.add(value, contourColors[i]);
            scaleValues[i] = value;
            value = value + delta;
        }

        // create chart
        chart = ChartFactory.createScatterPlot(title, "O/C", "H/C", dataset3D,
                PlotOrientation.VERTICAL, true, true, false);
        // set renderer
        XYBlockPixelSizeRenderer renderer = new XYBlockPixelSizeRenderer();

        XYPlot plot = chart.getXYPlot();
        appliedSteps++;
        renderer.setPaintScale(scale);
        double maxX = plot.getDomainAxis().getRange().getUpperBound();
        double maxY = plot.getRangeAxis().getRange().getUpperBound();

        renderer.setBlockWidth(0.001);
        renderer.setBlockHeight(renderer.getBlockWidth() / (maxX / maxY));

        // set tooltip generator
        ScatterPlotToolTipGenerator tooltipGenerator = new ScatterPlotToolTipGenerator(
                "O/C", "H/C", zAxisLabel, filteredRows);
        renderer.setSeriesToolTipGenerator(0, tooltipGenerator);
        renderer.setDefaultPositiveItemLabelPosition(
                new ItemLabelPosition(ItemLabelAnchor.CENTER,
                        TextAnchor.TOP_RIGHT, TextAnchor.TOP_RIGHT, -45),
                true);

        // set item label generator
        NameItemLabelGenerator generator = new NameItemLabelGenerator(
                filteredRows);
        renderer.setDefaultItemLabelGenerator(generator);
        renderer.setDefaultItemLabelsVisible(false);
        renderer.setDefaultItemLabelFont(legendFont);

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setOutlinePaint(Color.black);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainCrosshairPaint(Color.GRAY);
        plot.setRangeCrosshairPaint(Color.GRAY);
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);

        // Legend
        NumberAxis scaleAxis = new NumberAxis(zAxisLabel);
        scaleAxis.setRange(min, max);
        scaleAxis.setAxisLinePaint(Color.white);
        scaleAxis.setTickMarkPaint(Color.white);
        PaintScaleLegend legend = new PaintScaleLegend(scale, scaleAxis);

        legend.setStripOutlineVisible(false);
        legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
        legend.setAxisOffset(5.0);
        legend.setMargin(new RectangleInsets(5, 5, 5, 5));
        legend.setFrame(new BlockBorder(Color.white));
        legend.setPadding(new RectangleInsets(10, 10, 10, 10));
        legend.setStripWidth(10);
        legend.setPosition(RectangleEdge.LEFT);
        legend.getAxis().setLabelFont(legendFont);
        legend.getAxis().setTickLabelFont(legendFont);
        chart.addSubtitle(legend);
        appliedSteps++;

        return chart;
    }

    /*
     * This method removes all feature list rows, that cannot be plot in a Van
     * Krevelen diagram
     */
    private PeakListRow[] filterSelectedRows(PeakListRow[] selectedRows) {
        ArrayList<PeakListRow> rows = new ArrayList<PeakListRow>();
        for (PeakListRow peakListRow : selectedRows) {
            boolean hasIdentity = false;
            boolean isFormula = false;
            boolean hasSuitableElements = false;

            // check for identity
            if (peakListRow.getPreferredPeakIdentity() != null)
                hasIdentity = true;

            // check for formula
            if (hasIdentity && peakListRow.getPreferredPeakIdentity()
                    .getPropertyValue(PeakIdentity.PROPERTY_FORMULA) != null) {
                isFormula = true;
            } else {
                featuresWithoutFormula++;
            }

            // check if formula is suitable for Van Krevelen diagram (needs
            // elements C, H, and O)
            if (isFormula) {
                String s = peakListRow.getPreferredPeakIdentity()
                        .getPropertyValue(PeakIdentity.PROPERTY_FORMULA);
                if (s.contains("C") && s.contains("H") && s.contains("O")) {
                    hasSuitableElements = true;
                    displayedFeatures++;
                } else {
                    logger.log(Level.WARNING, "Warning, " + s
                            + " cannot be plottet in a Van_Krevelen diagram");
                    featuresWithFormulasWithoutCHO++;
                }
            }

            // add peakListRow
            if (hasIdentity && isFormula && hasSuitableElements)
                rows.add(peakListRow);
        }
        if (rows.size() > 0) {
            return rows.stream().toArray(PeakListRow[]::new);
        } else
            return null;
    }

}
