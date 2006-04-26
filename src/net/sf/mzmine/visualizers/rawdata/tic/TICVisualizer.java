/*
 * Copyright 2005 VTT Biotechnology
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
package net.sf.mzmine.visualizers.rawdata.tic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.RepaintManager;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.RawDataFile.PreloadLevel;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.components.XAxis;
import net.sf.mzmine.userinterface.components.YAxis;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.TransferableImage;
import net.sf.mzmine.util.format.IntensityValueFormat;
import net.sf.mzmine.util.format.RetentionTimeValueFormat;
import net.sf.mzmine.util.format.ValueFormat;
import net.sf.mzmine.visualizers.RawDataVisualizer;
import net.sf.mzmine.visualizers.rawdata.spectra.SpectrumVisualizer;

/**
 * This class defines the total ion chromatogram visualizer for raw data
 */
public class TICVisualizer extends JInternalFrame implements RawDataVisualizer,
        TaskListener, Printable, ActionListener {

    private TICToolBar toolBar;
    private TICPopupMenu popupMenu;
    private TICPlot ticPlot;

    private JLabel titleLabel;
    private XAxis xAxis;
    private YAxis yAxis;

    private RawDataFile rawDataFile;
    private int msLevel;

    private boolean xicMode = false;

    private double retentionTimes[], intensities[];

    private double mzRangeMin, mzRangeMax;
    private double zoomRTMin, zoomRTMax, zoomIntensityMin, zoomIntensityMax;

    private ValueFormat rtFormat, intensityFormat;

    /**
     * cursor posititon represented by an index to retentionTimes[]
     */
    private int cursorPosition = -1;

    /**
     * scan numbers of selected MS level
     */
    private int scanNumbers[];

    /**
     * Constructor for total ion chromatogram visualizer
     *
     */
    public TICVisualizer(RawDataFile rawDataFile, int msLevel) {

        super(rawDataFile.toString() + ": TIC", true, true, true, true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        rtFormat = new RetentionTimeValueFormat();
        intensityFormat = new IntensityValueFormat();

        popupMenu = new TICPopupMenu(this);

        setLayout(new BorderLayout());
        setBackground(Color.white);

        titleLabel = new JLabel(rawDataFile.toString(), JLabel.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        titleLabel.setFont(titleLabel.getFont().deriveFont(11.0f));
        add(titleLabel, BorderLayout.NORTH);

        toolBar = new TICToolBar(this);
        add(toolBar, BorderLayout.EAST);

        yAxis = new YAxis(0, 0, 0, 0, intensityFormat);
        add(yAxis, BorderLayout.WEST);

        xAxis = new XAxis(0, 0, (int) yAxis.getPreferredSize().getWidth(),
                (int) toolBar.getPreferredSize().getWidth(), rtFormat);
        add(xAxis, BorderLayout.SOUTH);

        ticPlot = new TICPlot(this);
        getContentPane().add(ticPlot, BorderLayout.CENTER);

        getInputMap().put(KeyStroke.getKeyStroke("RIGHT"), "moveCursorRight");
        getActionMap().put("moveCursorRight", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                if ((cursorPosition >= 0)
                        && (cursorPosition < retentionTimes.length - 1)) {
                    cursorPosition++;
                    repaint();
                }
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
        retentionTimes = new double[scanNumbers.length];
        intensities = new double[scanNumbers.length];

        resetRTRange();
        resetIntensityRange();
        toolBar.setZoomOutButton(false);
        toolBar.setXicButton(true);
        popupMenu.setZoomOutMenuItem(false);

        Task updateTask = new TICDataRetrievalTask(rawDataFile, scanNumbers,
                this);

        /*
         * if the file data is preloaded in memory, we can update the visualizer
         * in this thread, otherwise start a task
         */
        if (newFile.getPreloadLevel() == PreloadLevel.PRELOAD_ALL_SCANS)
            updateTask.run();
        else
            TaskController.getInstance().addTask(updateTask, this);

    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setMZRange(double,
     *      double)
     */
    public void setMZRange(double mzMin, double mzMax) {
        xicMode = true;
        toolBar.setXicButton(false);
        mzRangeMin = mzMin;
        mzRangeMax = mzMax;
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

        StringBuffer title = new StringBuffer();
        if (xicMode) {
            title.append("XIC");
            title.append(" m/z range " + mzRangeMin + " - " + mzRangeMax);
        } else
            title.append("TIC");
        title.append(", MS level ");
        title.append(msLevel);
        title.append(", RT " + rtFormat.format(zoomRTMin) + " - "
                + rtFormat.format(zoomRTMax));
        title.append(", IC " + intensityFormat.format(zoomIntensityMin) + " - "
                + intensityFormat.format(zoomIntensityMax));

        titleLabel.setText(title.toString());
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setRTRange(double,
     *      double)
     */
    public void setRTRange(double rtMin, double rtMax) {
        toolBar.setZoomOutButton(true);
        popupMenu.setZoomOutMenuItem(true);
        zoomRTMin = rtMin;
        zoomRTMax = rtMax;
        xAxis.setRange(zoomRTMin, zoomRTMax);
        ticPlot.setRTRange(zoomRTMin, zoomRTMax);
        updateTitle();
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#resetRTRange()
     */
    public void resetRTRange() {
        zoomRTMin = rawDataFile.getDataMinRT();
        zoomRTMax = rawDataFile.getDataMaxRT();
        xAxis.setRange(zoomRTMin, zoomRTMax);
        ticPlot.setRTRange(zoomRTMin, zoomRTMax);
        updateTitle();
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setIntensityRange(double,
     *      double)
     */
    public void setIntensityRange(double intensityMin, double intensityMax) {
        toolBar.setZoomOutButton(true);
        popupMenu.setZoomOutMenuItem(true);
        zoomIntensityMin = intensityMin;
        zoomIntensityMax = intensityMax;
        yAxis.setRange(zoomIntensityMin, zoomIntensityMax);
        ticPlot.setIntensityRange(zoomIntensityMin, zoomIntensityMax);
        updateTitle();
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#resetIntensityRange()
     */
    public void resetIntensityRange() {
        zoomIntensityMin = 0;
        zoomIntensityMax = rawDataFile.getDataMaxTotalIonCurrent(msLevel);
        yAxis.setRange(zoomIntensityMin, zoomIntensityMax);
        ticPlot.setIntensityRange(zoomIntensityMin, zoomIntensityMax);
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

        if (rt < retentionTimes[0]) {
            cursorPosition = -1;
        } else {
            // find the first scan number with RT higher than given rt
            int index;
            for (index = 1; index < retentionTimes.length; index++) {
                if (retentionTimes[index] > rt)
                    break;
            }
            if (index == retentionTimes.length) {
                cursorPosition = -1;
            } else if (rt - retentionTimes[index - 1] < retentionTimes[index]
                    - rt)
                cursorPosition = index - 1;
            else
                cursorPosition = index;
        }
        toolBar.setSpectraButton(cursorPosition != -1);
        repaint();
    }

    /**
     * @return Returns the cursorPosition.
     */
    int getCursorPosition() {
        return cursorPosition;
    }

    /**
     * @return Returns the intensities.
     */
    double[] getIntensities() {
        return intensities;
    }

    /**
     * @return Returns the retentionTimes.
     */
    double[] getRetentionTimes() {
        return retentionTimes;
    }

    int[] getScanNumbers() {
        return scanNumbers;
    }

    TICPopupMenu getPopupMenu() {
        return popupMenu;
    }

    void updateData(int position, double retentionTime, double intensity) {

        retentionTimes[position] = retentionTime;
        intensities[position] = intensity;
        if (ticPlot.getWidth() > 0) {
            if (position % (retentionTimes.length / ticPlot.getWidth()) == 0)
                repaint();
        }

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
     * This method is used for drawing the window contents in a separate buffer
     * which will then go to printer.
     */
    public int print(Graphics g, PageFormat pf, int pi) {
        double sx, sy;
        final int titleHeight = 30;

        // Since this visualizer will be printed on a single page, don't try to
        // print pages 2,3,4,...
        if (pi > 0) {
            return NO_SUCH_PAGE;
        }

        // Prepare given buffer for drawing the plot in it
        Graphics2D g2 = (Graphics2D) g;
        g2.translate(pf.getImageableX(), pf.getImageableY());

        // Print title of this visualizer
        g2.drawString(this.getTitle(), 0, titleHeight - 5);

        // Setup transform so that plot will fit on page
        g2.translate(0, titleHeight);

        sx = (double) pf.getImageableWidth()
                / (double) getContentPane().getWidth();
        sy = (double) (pf.getImageableHeight() - titleHeight)
                / (double) getContentPane().getHeight();

        g2.transform(AffineTransform.getScaleInstance(sx, sy));

        // Disabling double buffering increases print quality
        RepaintManager currentManager = RepaintManager
                .currentManager(getContentPane());
        currentManager.setDoubleBufferingEnabled(false);

        // Draw this visualizer to the buffer
        getContentPane().paint(g2);

        // Enable double buffering again (good for screen output)
        currentManager.setDoubleBufferingEnabled(true);

        // Return page ready status
        return Printable.PAGE_EXISTS;

    }

    /**
     * Implementation of the printMe() method (Visualizer interface)
     */
    public void printMe() {

        // Set default printer parameters
        PrinterJob printJob = PrinterJob.getPrinterJob();
        HashPrintRequestAttributeSet pSet = new HashPrintRequestAttributeSet();
        pSet.add(OrientationRequested.LANDSCAPE);

        // Open print dialog and initiate print job if user confirms
        if (printJob.printDialog(pSet)) {
            printJob.setPrintable(this);
            try {
                printJob.print(pSet);
            } catch (Exception PrintException) {
            }
        }
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
            popupMenu.setZoomOutMenuItem(false);
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
                popupMenu.setTicXicMenuItem("Switch to XIC");
            } else {

                popupMenu.setTicXicMenuItem("Switch to TIC");
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

}
