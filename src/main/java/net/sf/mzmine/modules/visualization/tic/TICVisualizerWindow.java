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

package net.sf.mzmine.modules.visualization.tic;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.SimpleSorter;
import net.sf.mzmine.util.dialogs.LoadSaveFileChooser;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import com.google.common.base.Joiner;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

/**
 * Total ion chromatogram visualizer using JFreeChart library
 */
public class TICVisualizerWindow extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    // CSV extension.
    private static final String CSV_EXTENSION = "csv";

    private TICToolBar toolBar;
    private TICPlot ticPlot;

    // Data sets
    private Hashtable<RawDataFile, TICDataSet> ticDataSets;

    private TICPlotType plotType;
    private ScanSelection scanSelection;
    private Range<Double> mzRange;

    private Desktop desktop;

    // Export file chooser.
    private static LoadSaveFileChooser exportChooser = null;

    /**
     * Constructor for total ion chromatogram visualizer
     */
    public TICVisualizerWindow(RawDataFile dataFiles[], TICPlotType plotType,
            ScanSelection scanSelection, Range<Double> mzRange,
            Feature[] peaks, Map<Feature, String> peakLabels) {

        super("Chromatogram loading...");

        assert mzRange != null;

        this.desktop = MZmineCore.getDesktop();
        this.plotType = plotType;
        this.ticDataSets = new Hashtable<RawDataFile, TICDataSet>();
        this.scanSelection = scanSelection;
        this.mzRange = mzRange;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        ticPlot = new TICPlot(this);
        add(ticPlot, BorderLayout.CENTER);

        toolBar = new TICToolBar(ticPlot);
        add(toolBar, BorderLayout.EAST);

        // add all peaks
        if (peaks != null) {

            for (Feature peak : peaks) {

                if (peakLabels != null && peakLabels.containsKey(peak)) {

                    final String label = peakLabels.get(peak);
                    ticPlot.addLabelledPeakDataset(
                            new PeakDataSet(peak, label), label);

                } else {

                    ticPlot.addPeakDataset(new PeakDataSet(peak));
                }
            }
        }

        // add all data files
        for (RawDataFile dataFile : dataFiles) {
            addRawDataFile(dataFile);
        }

        // Add the Windows menu
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new WindowsMenu());
        setJMenuBar(menuBar);

        pack();

        // get the window settings parameter
        ParameterSet paramSet = MZmineCore.getConfiguration()
                .getModuleParameters(TICVisualizerModule.class);
        WindowSettingsParameter settings = paramSet
                .getParameter(TICVisualizerParameters.WINDOWSETTINGSPARAMETER);

        // update the window and listen for changes
        settings.applySettingsToWindow(this);
        this.addComponentListener(settings);

        // Listen for clicks on legend items
        ticPlot.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                ChartEntity entity = event.getEntity();
                XYPlot plot = (XYPlot) ticPlot.getChart().getPlot();

                if ((entity != null)
                        && entity instanceof LegendItemEntity
                        && plot.getRenderer().getClass().getName()
                                .indexOf(".TICPlotRenderer") > -1) {
                    LegendItemEntity itemEntity = (LegendItemEntity) entity;
                    XYLineAndShapeRenderer rendererAll = (XYLineAndShapeRenderer) plot
                            .getRenderer();

                    // Find index value
                    int index = -1;
                    for (int i = 0; i < plot.getDatasetCount(); i++) {
                        if (rendererAll.getLegendItem(i, 1) != null
                                && rendererAll.getLegendItem(i, 1)
                                        .getDescription()
                                        .equals(itemEntity.getSeriesKey())) {
                            index = i;
                            break;
                        }
                    }
                    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot
                            .getRenderer(index);

                    // Select or deselect dataset
                    Font font = new Font("Helvetica", Font.BOLD, 11);
                    BasicStroke stroke = new BasicStroke(4);
                    if (renderer.getBaseLegendTextFont() != null
                            && renderer.getBaseLegendTextFont().isBold()) {
                        font = new Font("Helvetica", Font.PLAIN, 11);
                        stroke = new BasicStroke(1);
                    }
                    renderer.setBaseLegendTextFont(font);
                    renderer.setSeriesStroke(0, stroke);
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent event) {
                ChartEntity entity = event.getEntity();
                XYPlot plot = (XYPlot) ticPlot.getChart().getPlot();
                if ((entity != null)
                        && entity instanceof LegendItemEntity
                        && plot.getRenderer().getClass().getName()
                                .indexOf(".TICPlotRenderer") > -1) {
                    ticPlot.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    ticPlot.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
                }
            }
        });

    }

    void updateTitle() {

        NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
        NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
        NumberFormat intensityFormat = MZmineCore.getConfiguration()
                .getIntensityFormat();

        StringBuffer mainTitle = new StringBuffer();
        StringBuffer subTitle = new StringBuffer();

        // If all data files have m/z range less than or equal to range of
        // the plot (mzMin, mzMax), then call this TIC, otherwise XIC
        Set<RawDataFile> fileSet = ticDataSets.keySet();
        String ticOrXIC = "TIC";

        // Enlarge range a bit to avoid rounding errors
        Range<Double> mzRange2 = Range
                .range(mzRange.lowerEndpoint() - 1, BoundType.CLOSED,
                        mzRange.upperEndpoint() + 1, BoundType.CLOSED);
        for (RawDataFile df : fileSet) {
            if (!mzRange2.encloses(df.getDataMZRange())) {
                ticOrXIC = "XIC";
                break;
            }
        }

        if (plotType == TICPlotType.BASEPEAK) {
            if (ticOrXIC.equals("TIC")) {
                mainTitle.append("Base peak chromatogram");
            } else {
                mainTitle.append("XIC (base peak)");
            }
        } else {
            if (ticOrXIC.equals("TIC")) {
                mainTitle.append("TIC");
            } else {
                mainTitle.append("XIC");
            }
        }

        mainTitle.append(", m/z: " + mzFormat.format(mzRange.lowerEndpoint())
                + " - " + mzFormat.format(mzRange.upperEndpoint()));

        CursorPosition pos = getCursorPosition();

        if (pos != null) {
            subTitle.append("Selected scan #");
            subTitle.append(pos.getScanNumber());
            if (ticDataSets.size() > 1) {
                subTitle.append(" (" + pos.getDataFile() + ")");
            }
            subTitle.append(", RT: " + rtFormat.format(pos.getRetentionTime()));
            if (plotType == TICPlotType.BASEPEAK) {
                subTitle.append(", base peak: "
                        + mzFormat.format(pos.getMzValue()) + " m/z");
            }
            subTitle.append(", IC: "
                    + intensityFormat.format(pos.getIntensityValue()));
        }

        // update window title
        RawDataFile files[] = ticDataSets.keySet().toArray(new RawDataFile[0]);
        Arrays.sort(files, new SimpleSorter());
        String dataFileNames = Joiner.on(",").join(files);
        setTitle("Chromatogram: [" + dataFileNames + "; "
                + mzFormat.format(mzRange.lowerEndpoint()) + " - "
                + mzFormat.format(mzRange.upperEndpoint()) + " m/z" + "]");

        // update plot title
        ticPlot.setTitle(mainTitle.toString(), subTitle.toString());

    }

    /**
     * @return Returns the plotType.
     */
    TICPlotType getPlotType() {
        return plotType;
    }

    TICDataSet[] getAllDataSets() {
        return ticDataSets.values().toArray(new TICDataSet[0]);
    }

    /**
     */
    public void setRTRange(Range<Double> rtRange) {
        ticPlot.getXYPlot().getDomainAxis()
                .setRange(rtRange.lowerEndpoint(), rtRange.upperEndpoint());
    }

    public void setAxesRange(double xMin, double xMax, double xTickSize,
            double yMin, double yMax, double yTickSize) {
        NumberAxis xAxis = (NumberAxis) ticPlot.getXYPlot().getDomainAxis();
        NumberAxis yAxis = (NumberAxis) ticPlot.getXYPlot().getRangeAxis();
        xAxis.setRange(xMin, xMax);
        xAxis.setTickUnit(new NumberTickUnit(xTickSize));
        yAxis.setRange(yMin, yMax);
        yAxis.setTickUnit(new NumberTickUnit(yTickSize));
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#setIntensityRange(double,
     *      double)
     */
    public void setIntensityRange(double intensityMin, double intensityMax) {
        ticPlot.getXYPlot().getRangeAxis().setRange(intensityMin, intensityMax);
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#getRawDataFiles()
     */
    public RawDataFile[] getRawDataFiles() {
        return ticDataSets.keySet().toArray(new RawDataFile[0]);
    }

    public void addRawDataFile(RawDataFile newFile) {

        final Scan scans[] = scanSelection.getMatchingScans(newFile);
        if (scans.length == 0) {
            desktop.displayErrorMessage(this, "No scans found.");
            return;
        }

        TICDataSet ticDataset = new TICDataSet(newFile, scans, mzRange, this);
        ticDataSets.put(newFile, ticDataset);
        ticPlot.addTICDataset(ticDataset);

    }

    public void removeRawDataFile(RawDataFile file) {
        TICDataSet dataset = ticDataSets.get(file);
        ticPlot.getXYPlot().setDataset(ticPlot.getXYPlot().indexOf(dataset),
                null);
        ticDataSets.remove(file);
    }

    /**
     * Export a file's chromatogram.
     *
     * @param file
     *            the file.
     */
    public void exportChromatogram(RawDataFile file) {

        // Get the data set.
        final TICDataSet dataSet = ticDataSets.get(file);
        if (dataSet != null) {

            // Create the chooser if necessary.
            if (exportChooser == null) {

                exportChooser = new LoadSaveFileChooser(
                        "Select Chromatogram File");
                exportChooser
                        .addChoosableFileFilter(new FileNameExtensionFilter(
                                "Comma-separated values files", CSV_EXTENSION));
            }

            // Choose an export file.
            final File exportFile = exportChooser.getSaveFile(this,
                    file.getName(), CSV_EXTENSION);
            if (exportFile != null) {

                MZmineCore.getTaskController().addTask(
                        new ExportChromatogramTask(dataSet, exportFile));
            }
        }
    }

    /**
     * @return current cursor position
     */
    public CursorPosition getCursorPosition() {
        double selectedRT = (double) ticPlot.getXYPlot()
                .getDomainCrosshairValue();
        double selectedIT = (double) ticPlot.getXYPlot()
                .getRangeCrosshairValue();
        Enumeration<TICDataSet> e = ticDataSets.elements();
        while (e.hasMoreElements()) {
            TICDataSet dataSet = e.nextElement();
            int index = dataSet.getIndex(selectedRT, selectedIT);
            if (index >= 0) {
                double mz = 0;
                if (plotType == TICPlotType.BASEPEAK) {
                    mz = (double) dataSet.getZValue(0, index);
                }
                CursorPosition pos = new CursorPosition(selectedRT, mz,
                        selectedIT, dataSet.getDataFile(),
                        dataSet.getScanNumber(index));
                return pos;
            }
        }
        return null;
    }

    /**
     * @return current cursor position
     */
    public void setCursorPosition(CursorPosition newPosition) {
        ticPlot.getXYPlot().setDomainCrosshairValue(
                newPosition.getRetentionTime(), false);
        ticPlot.getXYPlot().setRangeCrosshairValue(
                newPosition.getIntensityValue());
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("SHOW_SPECTRUM")) {
            CursorPosition pos = getCursorPosition();
            if (pos != null) {
                SpectraVisualizerModule.showNewSpectrumWindow(
                        pos.getDataFile(), pos.getScanNumber());
            }
        }

        if (command.equals("MOVE_CURSOR_LEFT")) {
            CursorPosition pos = getCursorPosition();
            if (pos != null) {
                TICDataSet dataSet = ticDataSets.get(pos.getDataFile());
                int index = dataSet.getIndex(pos.getRetentionTime(),
                        pos.getIntensityValue());
                if (index > 0) {
                    index--;
                    pos.setRetentionTime((double) dataSet.getXValue(0, index));
                    pos.setIntensityValue((double) dataSet.getYValue(0, index));
                    setCursorPosition(pos);

                }
            }
        }

        if (command.equals("MOVE_CURSOR_RIGHT")) {
            CursorPosition pos = getCursorPosition();
            if (pos != null) {
                TICDataSet dataSet = ticDataSets.get(pos.getDataFile());
                int index = dataSet.getIndex(pos.getRetentionTime(),
                        pos.getIntensityValue());
                if (index >= 0) {
                    index++;
                    if (index < dataSet.getItemCount(0)) {
                        pos.setRetentionTime((double) dataSet.getXValue(0,
                                index));
                        pos.setIntensityValue((double) dataSet.getYValue(0,
                                index));
                        setCursorPosition(pos);
                    }
                }
            }
        }

    }

    public void dispose() {

        // If the window is closed, we want to cancel all running tasks of the
        // data sets
        Task tasks[] = this.ticDataSets.values().toArray(new Task[0]);

        for (Task task : tasks) {
            TaskStatus status = task.getStatus();
            if ((status == TaskStatus.WAITING)
                    || (status == TaskStatus.PROCESSING)) {
                task.cancel();
            }

        }
        super.dispose();

    }

}
