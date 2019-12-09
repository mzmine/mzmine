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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockPixelSizeRenderer;
import io.github.mzmine.gui.chartbasics.gui.swing.EChartPanel;
import io.github.mzmine.gui.impl.WindowsMenu;
import io.github.mzmine.util.dialogs.FeatureOverviewWindow;

/**
 * Window for Van Krevelen Diagrams
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class VanKrevelenDiagramWindow extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;
    private VanKrevelenDiagramToolBar toolBar;
    private JFreeChart chart;

    public VanKrevelenDiagramWindow(JFreeChart chart, EChartPanel chartPanel,
            PeakListRow[] rows) {

        this.chart = chart;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        // Add toolbar
        toolBar = new VanKrevelenDiagramToolBar(this);
        add(toolBar, BorderLayout.EAST);

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

                if (plot.getDataset() instanceof VanKrevelenDiagramXYZDataset) {
                    VanKrevelenDiagramXYZDataset dataset = (VanKrevelenDiagramXYZDataset) plot
                            .getDataset();
                    double[] xValues = new double[dataset.getItemCount(0)];
                    for (int i = 0; i < xValues.length; i++) {
                        if ((event.getTrigger()
                                .getButton() == MouseEvent.BUTTON1)
                                && (event.getTrigger().getClickCount() == 2)) {
                            if (dataset.getX(0, i).doubleValue() == xValue
                                    && dataset.getY(0, i)
                                            .doubleValue() == yValue) {
                                new FeatureOverviewWindow(rows[i]);
                            }
                        }
                    }
                }
                if (plot.getDataset() instanceof VanKrevelenDiagramXYDataset) {
                    VanKrevelenDiagramXYDataset dataset = (VanKrevelenDiagramXYDataset) plot
                            .getDataset();
                    double[] xValues = new double[dataset.getItemCount(0)];
                    for (int i = 0; i < xValues.length; i++) {
                        if ((event.getTrigger()
                                .getButton() == MouseEvent.BUTTON1)
                                && (event.getTrigger().getClickCount() == 2)) {
                            if (dataset.getX(0, i).doubleValue() == xValue
                                    && dataset.getY(0, i)
                                            .doubleValue() == yValue) {
                                new FeatureOverviewWindow(rows[i]);
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
            XYBlockPixelSizeRenderer renderer = (XYBlockPixelSizeRenderer) plot
                    .getRenderer();
            if (plot.getBackgroundPaint() == Color.WHITE) {
                plot.setBackgroundPaint(Color.BLACK);
                renderer.setDefaultItemLabelPaint(Color.WHITE);
            } else {
                plot.setBackgroundPaint(Color.WHITE);
                renderer.setDefaultItemLabelPaint(Color.BLACK);
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
        }

    }

}
