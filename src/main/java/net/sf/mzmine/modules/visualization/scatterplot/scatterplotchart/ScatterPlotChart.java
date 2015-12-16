/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.scatterplot.scatterplotchart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.scatterplot.ScatterPlotAxisSelection;
import net.sf.mzmine.modules.visualization.scatterplot.ScatterPlotTopPanel;
import net.sf.mzmine.modules.visualization.scatterplot.ScatterPlotWindow;
import net.sf.mzmine.modules.visualization.tic.TICPlotType;
import net.sf.mzmine.modules.visualization.tic.TICVisualizerModule;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.SaveImage;
import net.sf.mzmine.util.SaveImage.FileType;
import net.sf.mzmine.util.SearchDefinition;
import net.sf.mzmine.util.components.ComponentToolTipManager;
import net.sf.mzmine.util.components.ComponentToolTipProvider;
import net.sf.mzmine.util.components.PeakSummaryComponent;
import net.sf.mzmine.util.dialogs.AxesSetupDialog;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleInsets;

import com.google.common.collect.Range;

public class ScatterPlotChart extends ChartPanel implements
        ComponentToolTipProvider {

    private static final long serialVersionUID = 1L;

    // grid color
    private static final Color gridColor = Color.lightGray;

    // crosshair (selection) color
    private static final Color crossHairColor = Color.gray;

    // crosshair stroke
    private static final BasicStroke crossHairStroke = new BasicStroke(1,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {
                    5, 3 }, 0);

    private JFreeChart chart;
    private XYPlot plot;

    // Renderers
    private ScatterPlotRenderer mainRenderer;
    private DiagonalLineRenderer diagonalLineRenderer;

    // Data sets
    private ScatterPlotDataSet mainDataSet;
    private DiagonalLineDataset diagonalLineDataset;

    private ScatterPlotTopPanel topPanel;
    private ComponentToolTipManager ttm;

    private ScatterPlotWindow window;
    private PeakList peakList;
    private ScatterPlotAxisSelection axisX, axisY;
    private int fold;

    public ScatterPlotChart(ScatterPlotWindow window,
            ScatterPlotTopPanel topPanel, PeakList peakList) {

        super(null, true);

        this.window = window;
        this.peakList = peakList;
        this.topPanel = topPanel;

        // initialize the chart by default time series chart from factory
        chart = ChartFactory.createXYLineChart("", // title
                "", // x-axis label
                "", // y-axis label
                null, // data set
                PlotOrientation.VERTICAL, // orientation
                false, // create legend
                false, // generate tooltips
                false // generate URLs
                );

        chart.setBackgroundPaint(Color.white);
        setChart(chart);

        // disable maximum size (we don't want scaling)
        setMaximumDrawWidth(Integer.MAX_VALUE);
        setMaximumDrawHeight(Integer.MAX_VALUE);

        // set the plot properties
        plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
        plot.setDomainGridlinePaint(gridColor);
        plot.setRangeGridlinePaint(gridColor);

        // Set the domain log axis
        LogAxis logAxisDomain = new LogAxis();
        logAxisDomain.setMinorTickCount(1);
        logAxisDomain.setNumberFormatOverride(MZmineCore.getConfiguration()
                .getIntensityFormat());
        logAxisDomain.setAutoRange(true);
        plot.setDomainAxis(logAxisDomain);

        // Set the range log axis
        LogAxis logAxisRange = new LogAxis();
        logAxisRange.setMinorTickCount(1);
        logAxisRange.setNumberFormatOverride(MZmineCore.getConfiguration()
                .getIntensityFormat());
        logAxisRange.setAutoRange(true);
        plot.setRangeAxis(logAxisRange);

        // Set crosshair properties
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.setDomainCrosshairPaint(crossHairColor);
        plot.setRangeCrosshairPaint(crossHairColor);
        plot.setDomainCrosshairStroke(crossHairStroke);
        plot.setRangeCrosshairStroke(crossHairStroke);

        // Create data sets;
        mainDataSet = new ScatterPlotDataSet(peakList);
        plot.setDataset(0, mainDataSet);
        diagonalLineDataset = new DiagonalLineDataset();
        plot.setDataset(1, diagonalLineDataset);

        // Create renderers
        mainRenderer = new ScatterPlotRenderer();
        plot.setRenderer(0, mainRenderer);
        diagonalLineRenderer = new DiagonalLineRenderer();
        plot.setRenderer(1, diagonalLineRenderer);

        // Set tooltip properties
        ttm = new ComponentToolTipManager();
        ttm.registerComponent(this);
        setDismissDelay(Integer.MAX_VALUE);
        setInitialDelay(0);

        // add items to popup menu TODO: add other Show... items
        JPopupMenu popupMenu = getPopupMenu();
        popupMenu.addSeparator();
        GUIUtils.addMenuItem(popupMenu, "Show Chromatogram", this, "TIC");

        // Add EMF and EPS options to the save as menu
        JMenuItem saveAsMenu = (JMenuItem) popupMenu.getComponent(3);
        GUIUtils.addMenuItem(saveAsMenu, "EMF...", this, "SAVE_EMF");
        GUIUtils.addMenuItem(saveAsMenu, "EPS...", this, "SAVE_EPS");

    }

    public JComponent getCustomToolTipComponent(MouseEvent event) {

        String index = this.getToolTipText(event);
        if (index == null) {
            return null;
        }
        String indexSplit[] = index.split(":");

        int series = Integer.parseInt(indexSplit[0]);
        int item = Integer.parseInt(indexSplit[1]);

        PeakListRow row = mainDataSet.getRow(series, item);

        PeakSummaryComponent newSummary = new PeakSummaryComponent(row,
                peakList.getRawDataFiles(), true, true, true, true, false,
                ComponentToolTipManager.bg);

        double xValue = mainDataSet.getXValue(series, item);
        double yValue = mainDataSet.getYValue(series, item);
        newSummary.setRatio(xValue, yValue);

        return newSummary;

    }

    /**
     * @see org.jfree.chart.event.ChartProgressListener#chartProgress(org.jfree.chart.event.ChartProgressEvent)
     */
    @Override
    public void chartProgress(ChartProgressEvent event) {
        super.chartProgress(event);

        // Whenever chart is repainted (e.g. after crosshair position changed),
        // we update the selected item name
        if (event.getType() == ChartProgressEvent.DRAWING_FINISHED) {
            double valueX = plot.getDomainCrosshairValue();
            double valueY = plot.getRangeCrosshairValue();
            PeakListRow selectedRow = mainDataSet.getRow(valueX, valueY);
            topPanel.updateItemNameText(selectedRow);
        }
    }

    public void actionPerformed(ActionEvent event) {

        super.actionPerformed(event);

        String command = event.getActionCommand();

        if (command.equals("SETUP_AXES")) {
            AxesSetupDialog dialog = new AxesSetupDialog(window, plot);
            dialog.setVisible(true);
            return;
        }

        if (command.equals("TIC")) {

            double valueX = plot.getDomainCrosshairValue();
            double valueY = plot.getRangeCrosshairValue();
            PeakListRow selectedRow = mainDataSet.getRow(valueX, valueY);

            if (selectedRow == null) {
                MZmineCore.getDesktop().displayErrorMessage(window,
                        "No peak is selected");
                return;
            }

            Feature[] peaks = selectedRow.getPeaks();
            Range<Double> rtRange = peakList.getRowsRTRange();
            Range<Double> mzRange = PeakUtils.findMZRange(peaks);

            // Label best peak with preferred identity.
            final Feature bestPeak = selectedRow.getBestPeak();
            final PeakIdentity peakIdentity = selectedRow
                    .getPreferredPeakIdentity();
            final Map<Feature, String> labelMap = new HashMap<Feature, String>(
                    1);
            if (bestPeak != null && peakIdentity != null) {

                labelMap.put(bestPeak, peakIdentity.getName());
            }

            ScanSelection scanSelection = new ScanSelection(rtRange, 1);

            TICVisualizerModule.showNewTICVisualizerWindow(
                    peakList.getRawDataFiles(), peaks, labelMap, scanSelection,
                    TICPlotType.BASEPEAK, mzRange);
        }

        if ("SAVE_EMF".equals(command)) {

            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "EMF Image", "EMF");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showSaveDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String file = chooser.getSelectedFile().getPath();
                if (!file.toLowerCase().endsWith(".emf"))
                    file += ".emf";

                int width = (int) this.getSize().getWidth();
                int height = (int) this.getSize().getHeight();

                // Save image
                SaveImage SI = new SaveImage(getChart(), file, width, height,
                        FileType.EMF);
                new Thread(SI).start();

            }
        }

        if ("SAVE_EPS".equals(command)) {

            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "EPS Image", "EPS");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showSaveDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String file = chooser.getSelectedFile().getPath();
                if (!file.toLowerCase().endsWith(".eps"))
                    file += ".eps";

                int width = (int) this.getSize().getWidth();
                int height = (int) this.getSize().getHeight();

                // Save image
                SaveImage SI = new SaveImage(getChart(), file, width, height,
                        FileType.EPS);
                new Thread(SI).start();

            }

        }
    }

    public void setDisplayedAxes(ScatterPlotAxisSelection axisX,
            ScatterPlotAxisSelection axisY, int fold) {

        // Save values
        this.axisX = axisX;
        this.axisY = axisY;
        this.fold = fold;

        // Update axes
        plot.getDomainAxis().setLabel(axisX.toString());
        plot.getRangeAxis().setLabel(axisY.toString());

        // Update data sets
        mainDataSet.setDisplayedAxes(axisX, axisY);
        diagonalLineDataset.updateDiagonalData(mainDataSet, fold);

        topPanel.updateNumOfItemsText(peakList, mainDataSet, axisX, axisY, fold);
    }

    public void setItemLabels(boolean enabled) {
        mainRenderer.setSeriesItemLabelsVisible(1, enabled);
    }

    public void updateSearchDefinition(SearchDefinition newSearch) {
        mainDataSet.updateSearchDefinition(newSearch);
        topPanel.updateNumOfItemsText(peakList, mainDataSet, axisX, axisY, fold);
    }

}