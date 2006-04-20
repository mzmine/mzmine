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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.text.DecimalFormat;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.RepaintManager;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.RtMethodGenerator;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.obsoletedatastructures.RawDataAtClient;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.FormatCoordinates;
import net.sf.mzmine.util.TransferableImage;
import net.sf.mzmine.visualizers.RawDataVisualizer;

/**
 * This class defines the total ion chromatogram visualizer for raw data
 */
public class TICVisualizer extends JInternalFrame implements RawDataVisualizer,
        TaskListener, Printable, InternalFrameListener {

    // Components of the window
    private JPanel bottomPnl, leftPnl, rightPnl, topPnl;
    private TICPlot ticPlot;

    private RawDataFile rawDataFile;
    private int msLevel;

    private boolean xicMode = false;

    private double retentionTimes[], intensities[];

    private double mzRangeMin, mzRangeMax;
    private double zoomRTMin, zoomRTMax, zoomIntensityMin, zoomIntensityMax;
    private double cursorPosition = -1;

    /**
     * scan numbers of selected MS level
     */
    private int scanNumbers[];

    /**
     * Constructor for total ion chromatogram visualizer
     * 
     */
    public TICVisualizer(RawDataFile rawDataFile, int msLevel) {

        super("", true, true, true, true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Build this visualizer
        getContentPane().setLayout(new BorderLayout());

        bottomPnl = new TICXAxis(this);

        getContentPane().add(bottomPnl, java.awt.BorderLayout.SOUTH);

        topPnl = new JPanel();
        topPnl.setMinimumSize(new Dimension(getWidth(), 5));
        topPnl.setPreferredSize(new Dimension(getWidth(), 5));
        topPnl.setBackground(Color.white);
        getContentPane().add(topPnl, java.awt.BorderLayout.NORTH);

        leftPnl = new TICYAxis(this);

        getContentPane().add(leftPnl, java.awt.BorderLayout.WEST);

        rightPnl = new JPanel();
        rightPnl.setMinimumSize(new Dimension(5, getHeight()));
        rightPnl.setPreferredSize(new Dimension(5, getHeight()));
        rightPnl.setBackground(Color.white);
        getContentPane().add(rightPnl, java.awt.BorderLayout.EAST);

        ticPlot = new TICPlot(this);
        ticPlot.setBackground(Color.white);
        getContentPane().add(ticPlot, java.awt.BorderLayout.CENTER);

        pack();

        addInternalFrameListener(this);

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

        zoomRTMin = newFile.getDataMinRT();
        zoomRTMax = newFile.getDataMaxRT();
        zoomIntensityMin = 0;
        zoomIntensityMax = newFile.getDataMaxTotalIonCurrent(msLevel);

        Task updateTask = new TICVisualizerDataRetrievalTask(rawDataFile,
                scanNumbers, this);
        TaskController.getInstance().addTask(updateTask, this);
        
        updateTitle();
        
    }

    boolean getXicMode() {
        return xicMode;
    }
    
    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setMZRange(double,
     *      double)
     */
    public void setMZRange(double mzMin, double mzMax) {
        xicMode = true;
        mzRangeMin = mzMin;
        mzRangeMax = mzMax;
        updateTitle();
        Task updateTask = new TICVisualizerDataRetrievalTask(rawDataFile,
                scanNumbers, this, mzMin, mzMax);
        TaskController.getInstance().addTask(updateTask, this);
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#resetMZRange()
     */
    public void resetMZRange() {
        if (xicMode) {
            xicMode = false;
            updateTitle();
            Task updateTask = new TICVisualizerDataRetrievalTask(rawDataFile,
                    scanNumbers, this);
            TaskController.getInstance().addTask(updateTask, this);
        }
    }
    
    private void updateTitle() {
        FormatCoordinates formatCoordinates = new FormatCoordinates(
                MainWindow.getInstance().getParameterStorage()
                        .getGeneralParameters());
        DecimalFormat intFormat = new DecimalFormat("0.####E0");
        
        StringBuffer title = new StringBuffer();
        title.append(rawDataFile.toString());
        title.append(": ");
        if (xicMode) title.append("Extracted"); else title.append("Total");
        title.append(" ion chromatogram, MS level ");
        title.append(msLevel);
        if (xicMode) title.append(", MZ range " + mzRangeMin + " - " + mzRangeMax);
        title.append(", RT " + formatCoordinates.formatRTValue(zoomRTMin) + " - " + formatCoordinates.formatRTValue(zoomRTMax));
        title.append(", IC " + intFormat.format(zoomIntensityMin) + " - " + intFormat.format(zoomIntensityMax));
        setTitle(title.toString());
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setRTRange(double,
     *      double)
     */
    public void setRTRange(double rtMin, double rtMax) {
        zoomRTMin = rtMin;
        zoomRTMax = rtMax;
        updateTitle();
        repaint();
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#resetRTRange()
     */
    public void resetRTRange() {
        zoomRTMin = rawDataFile.getDataMinRT();
        zoomRTMax = rawDataFile.getDataMaxRT();
        updateTitle();
        repaint();
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setIntensityRange(double,
     *      double)
     */
    public void setIntensityRange(double intensityMin, double intensityMax) {
        zoomIntensityMin = intensityMin;
        zoomIntensityMax = intensityMax;
        updateTitle();
        repaint();
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#resetIntensityRange()
     */
    public void resetIntensityRange() {
        zoomIntensityMin = 0;
        zoomIntensityMax = rawDataFile.getDataMaxTotalIonCurrent(msLevel);
        updateTitle();
        repaint();
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
        cursorPosition = rt;
        repaint();
    }

    /**
     * @return Returns the cursorPosition.
     */
    double getCursorPosition() {
        return cursorPosition;
    }

    /**
     * @return Returns the zoomIntensityMax.
     */
    double getZoomIntensityMax() {
        return zoomIntensityMax;
    }

    /**
     * @return Returns the zoomIntensityMin.
     */
    double getZoomIntensityMin() {
        return zoomIntensityMin;
    }

    /**
     * @return Returns the zoomRTMax.
     */
    double getZoomRTMax() {
        return zoomRTMax;
    }

    /**
     * @return Returns the zoomRTMin.
     */
    double getZoomRTMin() {
        return zoomRTMin;
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

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#attachVisualizer(net.sf.mzmine.visualizers.RawDataVisualizer)
     */
    public void attachVisualizer(RawDataVisualizer visualizer) {
        // TODO Auto-generated method stub

    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#detachVisualizer(net.sf.mzmine.visualizers.RawDataVisualizer)
     */
    public void detachVisualizer(RawDataVisualizer visualizer) {
        // TODO Auto-generated method stub

    }

    void updateData(int position, double retentionTime, double intensity) {

 /*       if (intensity > zoomIntensityMax)
            zoomIntensityMax = intensity;
        if (retentionTime > zoomRTMax)
            zoomRTMax = retentionTime;
*/
        retentionTimes[position] = retentionTime;
        intensities[position] = intensity;
        repaint();
        
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

    public void internalFrameOpened(InternalFrameEvent e) {
    }

    public void internalFrameIconified(InternalFrameEvent e) {
    }

    public void internalFrameDeiconified(InternalFrameEvent e) {
    }

    public void internalFrameDeactivated(InternalFrameEvent e) {
    }

    public void internalFrameClosing(InternalFrameEvent e) {
    }

    public void internalFrameClosed(InternalFrameEvent e) {
    }

    /**
     * Implementation of methods in InternalFrameListener
     */
    public void internalFrameActivated(InternalFrameEvent e) {

        MainWindow.getInstance().getItemSelector()
                .setActiveRawData(rawDataFile);

    }

}
