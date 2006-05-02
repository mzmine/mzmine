/*
 * Copyright 2006 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.visualizers.rawdata.experimentaltic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFile.PreloadLevel;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.TransferableImage;
import net.sf.mzmine.util.format.IntensityValueFormat;
import net.sf.mzmine.util.format.ValueFormat;
import net.sf.mzmine.visualizers.RawDataVisualizer;
import net.sf.mzmine.visualizers.rawdata.spectra.SpectrumVisualizer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.RectangleInsets;

/**
 * This class defines the total ion chromatogram visualizer for raw data
 */
public class TICVisualizer extends JInternalFrame implements RawDataVisualizer,
        TaskListener, ActionListener {

    private TICToolBar toolBar;

    private JPanel ticPlot;

    private RawDataFile rawDataFile;
    private int msLevel;

    private boolean xicMode = false;

    private XYSeries series;
    
    private DefaultTableXYDataset dataset;

    /**
     * cursor posititon represented by an index to retentionTimes[]
     */
    private int cursorPosition = -1;

    /**
     * scan numbers of selected MS level
     */
    private int scanNumbers[];
    
    private ValueFormat intensityFormat = new IntensityValueFormat();

    /**
     * Constructor for total ion chromatogram visualizer
     * 
     */
    public TICVisualizer(RawDataFile rawDataFile, int msLevel) {

        super(rawDataFile.toString() + ": TIC", true, true, true, true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // setLayout(new BorderLayout());
        setBackground(Color.white);

        toolBar = new TICToolBar(this);
        add(toolBar, BorderLayout.EAST);

        dataset = new DefaultTableXYDataset();
        

        series = new XYSeries(1, true, false);
        dataset.addSeries(series);

        final JFreeChart chart = ChartFactory.createXYLineChart("TIC", // title
                "Retention time", // x-axis label
                "Intensity", // y-axis label
                dataset, // data
                PlotOrientation.VERTICAL, // orientation
                false, // create legend?
                true, // generate tooltips?
                false // generate URLs?
                );

        chart.setBackgroundPaint(Color.white);

        final XYPlot plot = chart.getXYPlot();

        plot.setBackgroundPaint(Color.white);

        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.getDomainAxis().setRange(rawDataFile.getDataMinRT(),
                rawDataFile.getDataMaxRT());
        plot.getRangeAxis().setRange(0,
                rawDataFile.getDataMaxTotalIonCurrent(msLevel) * 1.05);

        ticPlot = new ChartPanel(chart);
        add(ticPlot, BorderLayout.CENTER);
        
        XYItemRenderer r = plot.getRenderer();

        if (r instanceof XYLineAndShapeRenderer) {

            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;

            // renderer.setBaseShapesVisible(true);

            // renderer.setBaseShapesFilled(true);

        }
        
        XYItemLabelGenerator gener = new XYItemLabelGenerator() {

            public String generateLabel(XYDataset dataset, int series, int item) {

                final double originalX = dataset.getXValue(series, item);

                final double pointX = plot.getDomainAxis().getRange().getLength() / ticPlot.getWidth();
  
                for (int i = item - 1; i > 1; i--) {
                    if (dataset.getXValue(series, i) < (originalX - 40 * pointX)) break;
                    if (dataset.getYValue(series, item) < dataset.getYValue(
                            series, i))
                        return null;
                }
                for (int i = item + 1; i < dataset.getItemCount(series); i++) {
                    if (dataset.getXValue(series, i) > (originalX + 40 * pointX)) break;
                    if (dataset.getYValue(series, item) < dataset.getYValue(
                            series, i))
                        return null;
                }
                
                return intensityFormat.format(dataset.getYValue(series, item));
             
            }

        };

        r.setItemLabelGenerator(gener);
        r.setItemLabelsVisible(true);

        getInputMap().put(KeyStroke.getKeyStroke("RIGHT"), "moveCursorRight");
        getActionMap().put("moveCursorRight", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                /*
                 * if ((cursorPosition >= 0) && (cursorPosition <
                 * retentionTimes.length - 1)) { cursorPosition++; repaint(); }
                 */
            }
        });
        getInputMap().put(KeyStroke.getKeyStroke("LEFT"), "moveCursorLeft");
        getActionMap().put("moveCursorLeft", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                if (cursorPosition > 0) {
                    cursorPosition--;
                    repaint();
                }
            }
        });

        pack();

        this.msLevel = msLevel;

        setRawDataFile(rawDataFile);

    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#getRawDataFile()
     */
    public RawDataFile getRawDataFile() {
        return rawDataFile;
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setRawDataFile(net.sf.mzmine.io.RawDataFile)
     */
    public void setRawDataFile(RawDataFile newFile) {

        this.rawDataFile = newFile;

        /*
         * start the update task
         */
        scanNumbers = rawDataFile.getScanNumbers(msLevel);
        assert scanNumbers != null;
        // retentionTimes = new double[scanNumbers.length];
        // intensities = new double[scanNumbers.length];

        resetRTRange();
        resetIntensityRange();
        toolBar.setZoomOutButton(false);
        toolBar.setXicButton(true);

        Task updateTask = new TICDataRetrievalTask(rawDataFile, scanNumbers,
                this);

        /*
         * if the file data is preloaded in memory, we can update the visualizer
         * in this thread, otherwise start a task
         */
        if (newFile.getPreloadLevel() == PreloadLevel.PRELOAD_ALL_SCANS) {
            taskStarted(updateTask);
            updateTask.run();
            taskFinished(updateTask);
        } else
            TaskController.getInstance().addTask(updateTask, this);

    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setMZRange(double,
     *      double)
     */
    public void setMZRange(double mzMin, double mzMax) {
        xicMode = true;
        toolBar.setXicButton(false);
        // mzRangeMin = mzMin;
        // mzRangeMax = mzMax;
        updateTitle();
        setTitle(rawDataFile.toString() + ": XIC");
        Task updateTask = new TICDataRetrievalTask(rawDataFile, scanNumbers,
                this, mzMin, mzMax);
        /*
         * if the file data is preloaded in memory, we can update the visualizer
         * in this thread, otherwise start a task
         */
        if (rawDataFile.getPreloadLevel() == PreloadLevel.PRELOAD_ALL_SCANS)
            updateTask.run();
        else
            TaskController.getInstance().addTask(updateTask, this);

    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#resetMZRange()
     */
    public void resetMZRange() {

        xicMode = false;
        toolBar.setXicButton(true);
        updateTitle();
        setTitle(rawDataFile.toString() + ": TIC");
        Task updateTask = new TICDataRetrievalTask(rawDataFile, scanNumbers,
                this);
        /*
         * if the file data is preloaded in memory, we can update the visualizer
         * in this thread, otherwise start a task
         */
        if (rawDataFile.getPreloadLevel() == PreloadLevel.PRELOAD_ALL_SCANS)
            updateTask.run();
        else
            TaskController.getInstance().addTask(updateTask, this);

    }

    private void updateTitle() {

    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setRTRange(double,
     *      double)
     */
    public void setRTRange(double rtMin, double rtMax) {
        toolBar.setZoomOutButton(true);
        // zoomRTMin = rtMin;
        // zoomRTMax = rtMax;
        // xAxis.setRange(zoomRTMin, zoomRTMax);
        // ticPlot.setRTRange(zoomRTMin, zoomRTMax);
        updateTitle();
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#resetRTRange()
     */
    public void resetRTRange() {
        // zoomRTMin = rawDataFile.getDataMinRT();
        // zoomRTMax = rawDataFile.getDataMaxRT();
        // xAxis.setRange(zoomRTMin, zoomRTMax);
        // ticPlot.setRTRange(zoomRTMin, zoomRTMax);
        updateTitle();
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setIntensityRange(double,
     *      double)
     */
    public void setIntensityRange(double intensityMin, double intensityMax) {
        toolBar.setZoomOutButton(true);
        // zoomIntensityMin = intensityMin;
        // zoomIntensityMax = intensityMax;
        // yAxis.setRange(zoomIntensityMin, zoomIntensityMax);
        // ticPlot.setIntensityRange(zoomIntensityMin, zoomIntensityMax);
        updateTitle();
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#resetIntensityRange()
     */
    public void resetIntensityRange() {
        // zoomIntensityMin = 0;
        // zoomIntensityMax = rawDataFile.getDataMaxTotalIonCurrent(msLevel);
        // yAxis.setRange(zoomIntensityMin, zoomIntensityMax);
        // ticPlot.setIntensityRange(zoomIntensityMin, zoomIntensityMax);
        updateTitle();
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setMZPosition(double)
     */
    public void setMZPosition(double mz) {
        // do nothing
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setRTPosition(double)
     */
    public void setRTPosition(double rt) {
        /*
         * if (rt < retentionTimes[0]) { cursorPosition = -1; } else { // find
         * the first scan number with RT higher than given rt int index; for
         * (index = 1; index < retentionTimes.length; index++) { if
         * (retentionTimes[index] > rt) break; } if (index ==
         * retentionTimes.length) { cursorPosition = -1; } else if (rt -
         * retentionTimes[index - 1] < retentionTimes[index] - rt)
         * cursorPosition = index - 1; else cursorPosition = index; }
         * toolBar.setSpectraButton(cursorPosition != -1); repaint();
         */
    }

    /**
     * @return Returns the cursorPosition.
     */
    int getCursorPosition() {
        return cursorPosition;
    }

    void updateData(int position, double retentionTime, double intensity) {

        series.add(retentionTime, intensity, true);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {
        if (task.getStatus() == TaskStatus.ERROR) {
            MainWindow.getInstance().displayErrorMessage(
                    "Error while updating TIC visualizer: "
                            + task.getErrorMessage());
        }

    }

    public void taskStarted(Task task) {
        MainWindow.getInstance().addInternalFrame(this);
    }

    /**
     * Implementation of the copyMe() method (Visualizer interface)
     */
    public void copyMe() {
        // Initialize clipboard
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Clipboard clipboard = toolkit.getSystemClipboard();

        // Draw visualizer graphics in a buffered image
        int w = getContentPane().getWidth();
        int h = getContentPane().getHeight();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        getContentPane().paint(g);

        // Put image to clipboard
        clipboard.setContents(new TransferableImage(bi), null);
    }

   

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("ZOOM_OUT")) {
            resetRTRange();
            resetIntensityRange();
            toolBar.setZoomOutButton(false);

        }

        if (command.equals("SHOW_SPECTRUM")) {
            assert cursorPosition >= 0;
            SpectrumVisualizer specVis = new SpectrumVisualizer(rawDataFile,
                    scanNumbers[cursorPosition]);
            MainWindow.getInstance().addInternalFrame(specVis);
        }

        if (command.equals("CHANGE_XIC_TIC")) {

            if (xicMode) {
                resetMZRange();

            } else {

                // Default range is cursor location +- 0.25
                double ricMZ = 0; // getCursorPositionMZ();
                double ricMZDelta = (double) 0.25;

                // Show dialog
                XICSetupDialog psd = new XICSetupDialog(
                        "Please give centroid and delta MZ values for XIC",
                        ricMZ, ricMZDelta);
                psd.setVisible(true);
                // if cancel was clicked
                if (psd.getExitCode() == -1) {
                    MainWindow.getInstance().getStatusBar().setStatusText(
                            "Switch to XIC cancelled.");
                    return;
                }

                // Validate given parameter values

                ricMZ = psd.getXicMZ();
                if (ricMZ < 0) {
                    MainWindow.getInstance().getStatusBar().setStatusText(
                            "Error: incorrect parameter values.");
                    return;
                }

                ricMZDelta = psd.getXicMZDelta();
                if (ricMZDelta < 0) {
                    MainWindow.getInstance().getStatusBar().setStatusText(
                            "Error: incorrect parameter values.");
                    return;
                }

                setMZRange(ricMZ - ricMZDelta, ricMZ + ricMZDelta);

            }
        }

    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#printMe()
     */
    public void printMe() {
        
        
    }


}
