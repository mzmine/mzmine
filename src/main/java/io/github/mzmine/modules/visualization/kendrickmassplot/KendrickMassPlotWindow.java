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

package io.github.mzmine.modules.visualization.kendrickmassplot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.PaintScaleLegend;

import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.gui.swing.EChartPanel;
import io.github.mzmine.gui.impl.WindowsMenu;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.dialogs.FeatureOverviewWindow;

/**
 * Window for Kendrick mass plots
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class KendrickMassPlotWindow extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;
    private KendrickMassPlotToolBar kendrickToolBar;
    private JFreeChart chart;
    private PeakListRow selectedRows[];
    private String xAxisKMBase;
    private String zAxisKMBase;
    private String customYAxisKMBase;
    private String customXAxisKMBase;
    private String customZAxisKMBase;
    private boolean useCustomXAxisKMBase;
    private boolean useCustomZAxisKMBase;
    private boolean useRKM_X;
    private boolean useRKM_Y;
    private boolean useRKM_Z;
    private double xAxisShift;
    private double yAxisShift;
    private double zAxisShift;
    private int yAxisCharge;
    private int xAxisCharge;
    private int zAxisCharge;
    private int yAxisDivisor;
    private int xAxisDivisor;
    private int zAxisDivisor;

    public KendrickMassPlotWindow(JFreeChart chart, ParameterSet parameters,
            EChartPanel chartPanel) {

        PeakList peakList = parameters
                .getParameter(KendrickMassPlotParameters.peakList).getValue()
                .getMatchingPeakLists()[0];

        this.selectedRows = parameters
                .getParameter(KendrickMassPlotParameters.selectedRows)
                .getMatchingRows(peakList);

        this.customYAxisKMBase = parameters
                .getParameter(
                        KendrickMassPlotParameters.yAxisCustomKendrickMassBase)
                .getValue();

        this.useCustomXAxisKMBase = parameters
                .getParameter(
                        KendrickMassPlotParameters.xAxisCustomKendrickMassBase)
                .getValue();

        if (useCustomXAxisKMBase == true) {
            this.customXAxisKMBase = parameters.getParameter(
                    KendrickMassPlotParameters.xAxisCustomKendrickMassBase)
                    .getEmbeddedParameter().getValue();
        } else {
            this.xAxisKMBase = parameters
                    .getParameter(KendrickMassPlotParameters.xAxisValues)
                    .getValue();
        }

        this.useCustomZAxisKMBase = parameters
                .getParameter(
                        KendrickMassPlotParameters.zAxisCustomKendrickMassBase)
                .getValue();

        if (useCustomZAxisKMBase == true) {
            this.customZAxisKMBase = parameters.getParameter(
                    KendrickMassPlotParameters.zAxisCustomKendrickMassBase)
                    .getEmbeddedParameter().getValue();
        } else {
            this.zAxisKMBase = parameters
                    .getParameter(KendrickMassPlotParameters.zAxisValues)
                    .getValue();
        }

        this.chart = chart;
        this.yAxisCharge = 1;
        this.xAxisCharge = 1;
        this.zAxisCharge = 1;
        if (customYAxisKMBase != null)
            this.yAxisDivisor = getDivisorKM(customYAxisKMBase);
        else
            this.yAxisDivisor = 1;
        if (customXAxisKMBase != null)
            this.xAxisDivisor = getDivisorKM(customXAxisKMBase);
        else
            this.xAxisDivisor = 1;
        if (customZAxisKMBase != null)
            this.zAxisDivisor = getDivisorKM(customZAxisKMBase);
        else
            this.zAxisDivisor = 1;
        this.xAxisShift = 0;
        this.yAxisShift = 0;
        this.zAxisShift = 0;
        this.useRKM_X = false;
        this.useRKM_Y = false;
        this.useRKM_Z = false;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        // Add toolbar
        kendrickToolBar = new KendrickMassPlotToolBar(this, xAxisShift,
                yAxisShift, zAxisShift, xAxisCharge, yAxisCharge, zAxisCharge,
                xAxisDivisor, yAxisDivisor, zAxisDivisor, useCustomXAxisKMBase,
                useCustomZAxisKMBase, useRKM_X, useRKM_Y, useRKM_Z);
        add(kendrickToolBar, BorderLayout.EAST);

        // set tooltips
        setTooltips();

        // Add the Windows menu
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new WindowsMenu());
        setJMenuBar(menuBar);

        // mouse listener
        chartPanel.addChartMouseListener(new ChartMouseListener() {

            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                XYPlot plot = (XYPlot) chart.getPlot();
                double xValue = plot.getDomainCrosshairValue();
                double yValue = plot.getRangeCrosshairValue();

                if (plot.getDataset() instanceof KendrickMassPlotXYZDataset) {
                    KendrickMassPlotXYZDataset dataset = (KendrickMassPlotXYZDataset) plot
                            .getDataset();
                    double[] xValues = new double[dataset.getItemCount(0)];
                    for (int i = 0; i < xValues.length; i++) {
                        if ((event.getTrigger()
                                .getButton() == MouseEvent.BUTTON1)
                                && (event.getTrigger().getClickCount() == 2)) {
                            if (dataset.getX(0, i).doubleValue() == xValue
                                    && dataset.getY(0, i)
                                            .doubleValue() == yValue) {
                                new FeatureOverviewWindow(selectedRows[i]);
                            }
                        }
                    }
                }
                if (plot.getDataset() instanceof KendrickMassPlotXYDataset) {
                    KendrickMassPlotXYDataset dataset = (KendrickMassPlotXYDataset) plot
                            .getDataset();
                    double[] xValues = new double[dataset.getItemCount(0)];
                    for (int i = 0; i < xValues.length; i++) {
                        if ((event.getTrigger()
                                .getButton() == MouseEvent.BUTTON1)
                                && (event.getTrigger().getClickCount() == 2)) {
                            if (dataset.getX(0, i).doubleValue() == xValue
                                    && dataset.getY(0, i)
                                            .doubleValue() == yValue) {
                                new FeatureOverviewWindow(selectedRows[i]);
                            }
                        }
                    }
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent event) {
            }
        });
        pack();
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("TOGGLE_BLOCK_SIZE")) {
            XYPlot plot = chart.getXYPlot();
            XYBlockPixelSizeRenderer renderer = (XYBlockPixelSizeRenderer) plot
                    .getRenderer();
            int height = (int) renderer.getBlockHeightPixel();

            if (height == 1) {
                height++;
            } else if (height == 5) {
                height = 1;
            } else if (height < 5 && height != 1) {
                height++;
            }
            renderer.setBlockHeightPixel(height);
            renderer.setBlockWidthPixel(height);
        }

        if (command.equals("TOGGLE_BACK_COLOR")) {
            XYPlot plot = chart.getXYPlot();
            if (plot.getBackgroundPaint() == Color.WHITE) {
                plot.setBackgroundPaint(Color.BLACK);
            } else {
                plot.setBackgroundPaint(Color.WHITE);
            }

        }

        if (command.equals("TOGGLE_GRID")) {
            XYPlot plot = chart.getXYPlot();
            if (plot.getDomainGridlinePaint() == Color.BLACK) {
                plot.setDomainGridlinePaint(Color.WHITE);
                plot.setRangeGridlinePaint(Color.WHITE);
            } else {
                plot.setDomainGridlinePaint(Color.BLACK);
                plot.setRangeGridlinePaint(Color.BLACK);
            }

        }

        if (command.equals("TOGGLE_ANNOTATIONS")) {
            XYPlot plot = chart.getXYPlot();
            XYBlockPixelSizeRenderer renderer = (XYBlockPixelSizeRenderer) plot
                    .getRenderer();
            Boolean itemNameVisible = renderer.getDefaultItemLabelsVisible();
            if (itemNameVisible == false) {
                renderer.setDefaultItemLabelsVisible(true);
            } else {
                renderer.setDefaultItemLabelsVisible(false);
            }
            if (plot.getBackgroundPaint() == Color.BLACK) {
                renderer.setDefaultItemLabelPaint(Color.WHITE);
            } else {
                renderer.setDefaultItemLabelPaint(Color.BLACK);
            }
        }

        // y axis commands
        if (command.equals("SHIFT_KMD_UP_Y")) {
            Double shiftValue = 0.01;
            yAxisShift = yAxisShift + shiftValue;
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("SHIFT_KMD_DOWN_Y")) {
            Double shiftValue = -0.01;
            yAxisShift = yAxisShift + shiftValue;
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("CHANGE_CHARGE_UP_Y")) {
            yAxisCharge = yAxisCharge + 1;
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("CHANGE_CHARGE_DOWN_Y")) {
            if (yAxisCharge > 1) {
                yAxisCharge = yAxisCharge - 1;
            } else
                yAxisCharge = 1;
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("CHANGE_DIVISOR_UP_Y")) {
            int minDivisor = getMinimumRecommendedDivisor(customYAxisKMBase);
            int maxDivisor = getMaximumRecommendedDivisor(customYAxisKMBase);
            if (yAxisDivisor >= minDivisor && yAxisDivisor < maxDivisor) {
                yAxisDivisor++;
                yAxisDivisor = checkDivisor(yAxisDivisor, useRKM_Y,
                        customYAxisKMBase, true);
            }
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("CHANGE_DIVISOR_DOWN_Y")) {
            int minDivisor = getMinimumRecommendedDivisor(customYAxisKMBase);
            int maxDivisor = getMaximumRecommendedDivisor(customYAxisKMBase);
            if (yAxisDivisor > minDivisor && yAxisDivisor <= maxDivisor) {
                yAxisDivisor--;
                yAxisDivisor = checkDivisor(yAxisDivisor, useRKM_Y,
                        customYAxisKMBase, false);
            }
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("TOGGLE_RKM_KMD_Y")) {
            XYPlot plot = chart.getXYPlot();
            if (useRKM_Y) {
                useRKM_Y = false;
                plot.getRangeAxis().setLabel("KMD(" + customYAxisKMBase + ")");
            } else {
                useRKM_Y = true;

                // if the divisor is round(R) switch to round(R)-1 for RKM plot
                yAxisDivisor = checkDivisor(yAxisDivisor, useRKM_Y,
                        customYAxisKMBase, false);
                plot.getRangeAxis().setLabel("RKM(" + customYAxisKMBase + ")");
            }
            kendrickVariableChanged(plot);
        }

        // x axis commands
        if (command.equals("SHIFT_KMD_UP_X")) {
            Double shiftValue = 0.01;
            xAxisShift = xAxisShift + shiftValue;
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("SHIFT_KMD_DOWN_X")) {
            Double shiftValue = -0.01;
            xAxisShift = xAxisShift + shiftValue;
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("CHANGE_CHARGE_UP_X")) {
            xAxisCharge = xAxisCharge + 1;
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("CHANGE_CHARGE_DOWN_X")) {
            if (xAxisCharge > 1) {
                xAxisCharge = xAxisCharge - 1;
            } else
                xAxisCharge = 1;
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("CHANGE_DIVISOR_UP_X")) {
            int minDivisor = getMinimumRecommendedDivisor(customXAxisKMBase);
            int maxDivisor = getMaximumRecommendedDivisor(customXAxisKMBase);
            if (xAxisDivisor >= minDivisor && xAxisDivisor < maxDivisor) {
                xAxisDivisor++;
                xAxisDivisor = checkDivisor(xAxisDivisor, useRKM_X,
                        customXAxisKMBase, true);
            }
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("CHANGE_DIVISOR_DOWN_X")) {
            int minDivisor = getMinimumRecommendedDivisor(customXAxisKMBase);
            int maxDivisor = getMaximumRecommendedDivisor(customXAxisKMBase);
            if (xAxisDivisor > minDivisor && xAxisDivisor <= maxDivisor) {
                xAxisDivisor--;
                xAxisDivisor = checkDivisor(xAxisDivisor, useRKM_X,
                        customXAxisKMBase, false);
            }
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("TOGGLE_RKM_KMD_X")) {
            XYPlot plot = chart.getXYPlot();
            if (useRKM_X) {
                useRKM_X = false;
                plot.getDomainAxis().setLabel("KMD(" + customXAxisKMBase + ")");
            } else {
                useRKM_X = true;

                // if the divisor is round(R) switch to round(R)-1 for RKM plot
                xAxisDivisor = checkDivisor(xAxisDivisor, useRKM_X,
                        customXAxisKMBase, false);
                plot.getDomainAxis().setLabel("RKM(" + customXAxisKMBase + ")");
            }
            kendrickVariableChanged(plot);
        }

        // z axis commands
        if (command.equals("SHIFT_KMD_UP_Z")) {

            Double shiftValue = 0.01;
            zAxisShift = zAxisShift + shiftValue;
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("SHIFT_KMD_DOWN_Z")) {

            Double shiftValue = -0.01;
            zAxisShift = zAxisShift + shiftValue;
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("CHANGE_CHARGE_UP_Z")) {
            zAxisCharge = zAxisCharge + 1;
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("CHANGE_CHARGE_DOWN_Z")) {
            if (zAxisCharge > 1) {
                zAxisCharge = zAxisCharge - 1;
            } else
                zAxisCharge = 1;
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("CHANGE_DIVISOR_UP_Z")) {
            int minDivisor = getMinimumRecommendedDivisor(customZAxisKMBase);
            int maxDivisor = getMaximumRecommendedDivisor(customZAxisKMBase);
            if (zAxisDivisor >= minDivisor && zAxisDivisor < maxDivisor) {
                zAxisDivisor++;
                zAxisDivisor = checkDivisor(zAxisDivisor, useRKM_Z,
                        customZAxisKMBase, true);

            }
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("CHANGE_DIVISOR_DOWN_Z")) {
            int minDivisor = getMinimumRecommendedDivisor(customZAxisKMBase);
            int maxDivisor = getMaximumRecommendedDivisor(customZAxisKMBase);
            if (zAxisDivisor > minDivisor && zAxisDivisor <= maxDivisor) {
                zAxisDivisor--;
                zAxisDivisor = checkDivisor(zAxisDivisor, useRKM_Z,
                        customZAxisKMBase, false);
            }
            XYPlot plot = chart.getXYPlot();
            kendrickVariableChanged(plot);
        }

        if (command.equals("TOGGLE_RKM_KMD_Z")) {
            XYPlot plot = chart.getXYPlot();
            if (plot.getDataset() instanceof KendrickMassPlotXYZDataset) {
                if (useRKM_Z) {
                    useRKM_Z = false;
                    PaintScaleLegend legend = (PaintScaleLegend) chart
                            .getSubtitle(1);
                    legend.getAxis().setLabel("KMD(" + customZAxisKMBase + ")");
                } else {
                    useRKM_Z = true;

                    // if the divisor is round(R) switch to round(R)-1 for RKM
                    // plot
                    zAxisDivisor = checkDivisor(zAxisDivisor, useRKM_Z,
                            customZAxisKMBase, false);
                    PaintScaleLegend legend = (PaintScaleLegend) chart
                            .getSubtitle(1);
                    legend.getAxis().setLabel("RKM(" + customZAxisKMBase + ")");
                }
                kendrickVariableChanged(plot);
            }
        }
    }

    /*
     * Method to calculate the data sets for a Kendrick mass plot
     */
    private void kendrickVariableChanged(XYPlot plot) {

        if (plot.getDataset() instanceof KendrickMassPlotXYDataset) {
            KendrickMassPlotXYDataset dataset = (KendrickMassPlotXYDataset) plot
                    .getDataset();
            double[] xValues = new double[dataset.getItemCount(0)];

            // Calc xValues
            xValues = new double[selectedRows.length];
            if (useCustomXAxisKMBase == true) {
                if (useRKM_X == false) {
                    for (int i = 0; i < selectedRows.length; i++) {
                        double unshiftedValue = Math.ceil(xAxisCharge
                                * selectedRows[i].getAverageMZ()
                                * getKendrickMassFactor(customXAxisKMBase,
                                        xAxisDivisor))
                                - xAxisCharge * selectedRows[i].getAverageMZ()
                                        * getKendrickMassFactor(
                                                customXAxisKMBase,
                                                xAxisDivisor);
                        xValues[i] = unshiftedValue + xAxisShift
                                - Math.floor(unshiftedValue + xAxisShift);
                    }
                } else {
                    for (int i = 0; i < selectedRows.length; i++) {
                        double unshiftedValue = (xAxisCharge
                                * (xAxisDivisor - Math.round(FormulaUtils
                                        .calculateExactMass(customXAxisKMBase)))
                                * selectedRows[i].getAverageMZ())
                                / FormulaUtils
                                        .calculateExactMass(customXAxisKMBase)//
                                - Math.floor((xAxisCharge
                                        * (xAxisDivisor - Math.round(
                                                FormulaUtils.calculateExactMass(
                                                        customXAxisKMBase)))
                                        * selectedRows[i].getAverageMZ())
                                        / FormulaUtils.calculateExactMass(
                                                customXAxisKMBase));
                        xValues[i] = unshiftedValue + xAxisShift
                                - Math.floor(unshiftedValue + xAxisShift);
                    }
                }
            } else {
                for (int i = 0; i < selectedRows.length; i++) {

                    // simply plot m/z values as x axis
                    if (xAxisKMBase.equals("m/z")) {
                        xValues[i] = selectedRows[i].getAverageMZ();
                    }

                    // plot Kendrick masses as x axis
                    else if (xAxisKMBase.equals("KM")) {
                        xValues[i] = selectedRows[i].getAverageMZ()
                                * getKendrickMassFactor(customYAxisKMBase,
                                        yAxisDivisor);
                    }
                }
            }

            // Calc yValues
            double[] yValues = new double[selectedRows.length];
            if (useRKM_Y == false) {
                for (int i = 0; i < selectedRows.length; i++) {
                    double unshiftedValue = Math
                            .ceil(yAxisCharge * (selectedRows[i].getAverageMZ())
                                    * getKendrickMassFactor(customYAxisKMBase,
                                            yAxisDivisor))
                            - yAxisCharge * (selectedRows[i].getAverageMZ())
                                    * getKendrickMassFactor(customYAxisKMBase,
                                            yAxisDivisor);
                    yValues[i] = unshiftedValue + yAxisShift
                            - Math.floor(unshiftedValue + yAxisShift);
                }
            } else {
                for (int i = 0; i < selectedRows.length; i++) {
                    double unshiftedValue = (yAxisCharge
                            * (yAxisDivisor - Math.round(FormulaUtils
                                    .calculateExactMass(customYAxisKMBase)))
                            * selectedRows[i].getAverageMZ())
                            / FormulaUtils.calculateExactMass(customYAxisKMBase)//
                            - Math.floor((yAxisCharge
                                    * (yAxisDivisor - Math.round(
                                            FormulaUtils.calculateExactMass(
                                                    customYAxisKMBase)))
                                    * selectedRows[i].getAverageMZ())
                                    / FormulaUtils.calculateExactMass(
                                            customYAxisKMBase));
                    yValues[i] = unshiftedValue + yAxisShift
                            - Math.floor(unshiftedValue + yAxisShift);
                }
            }
            dataset.setyValues(yValues);
            dataset.setxValues(xValues);
            chart.fireChartChanged();
            validate();
        } else if (plot.getDataset() instanceof KendrickMassPlotXYZDataset) {
            KendrickMassPlotXYZDataset dataset = (KendrickMassPlotXYZDataset) plot
                    .getDataset();
            double[] xValues = new double[dataset.getItemCount(0)];

            // Calc xValues
            xValues = new double[selectedRows.length];
            if (useCustomXAxisKMBase == true) {
                if (useRKM_X == false) {
                    for (int i = 0; i < selectedRows.length; i++) {
                        double unshiftedValue = Math.ceil(xAxisCharge
                                * selectedRows[i].getAverageMZ()
                                * getKendrickMassFactor(customXAxisKMBase,
                                        xAxisDivisor))
                                - xAxisCharge * selectedRows[i].getAverageMZ()
                                        * getKendrickMassFactor(
                                                customXAxisKMBase,
                                                xAxisDivisor);
                        xValues[i] = unshiftedValue + xAxisShift
                                - Math.floor(unshiftedValue + xAxisShift);
                    }
                } else {
                    for (int i = 0; i < selectedRows.length; i++) {
                        double unshiftedValue = (xAxisCharge
                                * (xAxisDivisor - Math.round(FormulaUtils
                                        .calculateExactMass(customXAxisKMBase)))
                                * selectedRows[i].getAverageMZ())
                                / FormulaUtils
                                        .calculateExactMass(customXAxisKMBase)//
                                - Math.floor((xAxisCharge
                                        * (xAxisDivisor - Math.round(
                                                FormulaUtils.calculateExactMass(
                                                        customXAxisKMBase)))
                                        * selectedRows[i].getAverageMZ())
                                        / FormulaUtils.calculateExactMass(
                                                customXAxisKMBase));
                        xValues[i] = unshiftedValue + xAxisShift
                                - Math.floor(unshiftedValue + xAxisShift);
                    }
                }
            } else {
                for (int i = 0; i < selectedRows.length; i++) {

                    // simply plot m/z values as x axis
                    if (xAxisKMBase.equals("m/z")) {
                        xValues[i] = selectedRows[i].getAverageMZ();
                    }

                    // plot Kendrick masses as x axis
                    else if (xAxisKMBase.equals("KM")) {
                        xValues[i] = selectedRows[i].getAverageMZ()
                                * getKendrickMassFactor(customYAxisKMBase,
                                        yAxisDivisor);
                    }
                }
            }

            // Calc yValues
            double[] yValues = new double[selectedRows.length];
            if (useRKM_Y == false) {
                for (int i = 0; i < selectedRows.length; i++) {
                    double unshiftedValue = Math
                            .ceil(yAxisCharge * (selectedRows[i].getAverageMZ())
                                    * getKendrickMassFactor(customYAxisKMBase,
                                            yAxisDivisor))
                            - yAxisCharge * (selectedRows[i].getAverageMZ())
                                    * getKendrickMassFactor(customYAxisKMBase,
                                            yAxisDivisor);
                    yValues[i] = unshiftedValue + yAxisShift
                            - Math.floor(unshiftedValue + yAxisShift);
                }
            } else {
                for (int i = 0; i < selectedRows.length; i++) {
                    double unshiftedValue = (yAxisCharge
                            * (yAxisDivisor - Math.round(FormulaUtils
                                    .calculateExactMass(customYAxisKMBase)))
                            * selectedRows[i].getAverageMZ())
                            / FormulaUtils.calculateExactMass(customYAxisKMBase)//
                            - Math.floor((yAxisCharge
                                    * (yAxisDivisor - Math.round(
                                            FormulaUtils.calculateExactMass(
                                                    customYAxisKMBase)))
                                    * selectedRows[i].getAverageMZ())
                                    / FormulaUtils.calculateExactMass(
                                            customYAxisKMBase));
                    yValues[i] = unshiftedValue + yAxisShift
                            - Math.floor(unshiftedValue + yAxisShift);
                }
            }

            // Calc zValues
            double[] zValues = new double[selectedRows.length];
            if (useCustomZAxisKMBase == true) {
                if (useRKM_Z == false) {
                    for (int i = 0; i < selectedRows.length; i++) {
                        double unshiftedValue = Math.ceil(zAxisCharge
                                * (selectedRows[i].getAverageMZ())
                                * getKendrickMassFactor(customZAxisKMBase,
                                        zAxisDivisor))
                                - zAxisCharge * (selectedRows[i].getAverageMZ())
                                        * getKendrickMassFactor(
                                                customZAxisKMBase,
                                                zAxisDivisor);
                        zValues[i] = unshiftedValue + zAxisShift
                                - Math.floor(unshiftedValue + zAxisShift);
                    }
                } else {
                    for (int i = 0; i < selectedRows.length; i++) {
                        double unshiftedValue = (zAxisCharge
                                * (zAxisDivisor - Math.round(FormulaUtils
                                        .calculateExactMass(customZAxisKMBase)))
                                * selectedRows[i].getAverageMZ())
                                / FormulaUtils
                                        .calculateExactMass(customZAxisKMBase)//
                                - Math.floor((zAxisCharge
                                        * (zAxisDivisor - Math.round(
                                                FormulaUtils.calculateExactMass(
                                                        customZAxisKMBase)))
                                        * selectedRows[i].getAverageMZ())
                                        / FormulaUtils.calculateExactMass(
                                                customZAxisKMBase));
                        zValues[i] = unshiftedValue + zAxisShift
                                - Math.floor(unshiftedValue + zAxisShift);
                    }
                }
            } else
                for (int i = 0; i < selectedRows.length; i++) {

                    // plot selected feature characteristic as z Axis
                    if (zAxisKMBase.equals("Retention time")) {
                        zValues[i] = selectedRows[i].getAverageRT();
                    } else if (zAxisKMBase.equals("Intensity")) {
                        zValues[i] = selectedRows[i].getAverageHeight();
                    } else if (zAxisKMBase.equals("Area")) {
                        zValues[i] = selectedRows[i].getAverageArea();
                    } else if (zAxisKMBase.equals("Tailing factor")) {
                        zValues[i] = selectedRows[i].getBestPeak()
                                .getTailingFactor();
                    } else if (zAxisKMBase.equals("Asymmetry factor")) {
                        zValues[i] = selectedRows[i].getBestPeak()
                                .getAsymmetryFactor();
                    } else if (zAxisKMBase.equals("FWHM")) {
                        zValues[i] = selectedRows[i].getBestPeak().getFWHM();
                    } else if (zAxisKMBase.equals("m/z")) {
                        zValues[i] = selectedRows[i].getBestPeak().getMZ();
                    }
                }
            dataset.setyValues(yValues);
            dataset.setxValues(xValues);
            dataset.setzValues(zValues);
            chart.fireChartChanged();
            validate();
        }

        // update toolbar
        this.remove(kendrickToolBar);
        kendrickToolBar = new KendrickMassPlotToolBar(this, xAxisShift,
                yAxisShift, zAxisShift, xAxisCharge, yAxisCharge, zAxisCharge,
                xAxisDivisor, yAxisDivisor, zAxisDivisor, useCustomXAxisKMBase,
                useCustomZAxisKMBase, useRKM_X, useRKM_Y, useRKM_Z);
        setTooltips();

        this.add(kendrickToolBar, BorderLayout.EAST);
        this.revalidate();
    }

    /*
     * Method to calculate the Kendrick mass factor for a given sum formula
     */
    private double getKendrickMassFactor(String formula, int divisor) {
        double exactMassFormula = FormulaUtils.calculateExactMass(formula);
        return Math.round(exactMassFormula / divisor)
                / (exactMassFormula / divisor);
    }

    /*
     * Method to calculate the divisor for Kendrick mass defect analysis
     */
    private int getDivisorKM(String formula) {
        double exactMass = FormulaUtils.calculateExactMass(formula);
        return (int) Math.round(exactMass);
    }

    /*
     * Method to calculate the recommended minimum of a divisor for Kendrick
     * mass defect analysis
     */
    private int getMinimumRecommendedDivisor(String formula) {
        double exactMass = FormulaUtils.calculateExactMass(formula);
        return (int) Math.round((2.0 / 3.0) * exactMass);
    }

    /*
     * Method to calculate the recommended maximum of a divisor for Kendrick
     * mass defect analysis
     */
    private int getMaximumRecommendedDivisor(String formula) {
        double exactMass = FormulaUtils.calculateExactMass(formula);
        return (int) Math.round(2.0 * exactMass);
    }

    private void setTooltips() {
        if (customYAxisKMBase != null)
            kendrickToolBar.getyAxisDivisorLabel().//
                    setToolTipText("The KM-Plot for divisor " + //
                            getDivisorKM(customYAxisKMBase)
                            + " is equal to a regular KM-Plot with divisor 1");
        if (customXAxisKMBase != null)
            kendrickToolBar.getxAxisDivisorLabel().//
                    setToolTipText("The KM-Plot for divisor " + //
                            getDivisorKM(customXAxisKMBase)
                            + " is equal to a regular KM-Plot with divisor 1");
        if (customZAxisKMBase != null)
            kendrickToolBar.getzAxisDivisorLabel().//
                    setToolTipText("The KM-Plot for divisor " + //
                            getDivisorKM(customZAxisKMBase)
                            + " is equal to a regular KM-Plot with divisor 1");
    }

    /*
     * Method to avoid round(R) as divisor for RKM plots All RKM values would be
     * 0 in that case
     */
    private int checkDivisor(int divisor, boolean useRKM, String kmdBase,
            boolean divisorUp) {
        if (useRKM && divisor == getDivisorKM(kmdBase) && divisorUp) {
            divisor++;
            return divisor;
        } else if (useRKM && divisor == getDivisorKM(kmdBase) && !divisorUp) {
            divisor--;
            return divisor;
        } else
            return divisor;
    }
}
