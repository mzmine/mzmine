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
package net.sf.mzmine.visualizers.rawdata.spectra;

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
import java.io.IOException;
import java.text.DecimalFormat;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.RepaintManager;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.Scan;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.FormatCoordinates;
import net.sf.mzmine.util.TransferableImage;
import net.sf.mzmine.visualizers.RawDataVisualizer;

public class SpectrumVisualizer extends JInternalFrame implements
        RawDataVisualizer, Printable {

    private JPanel bottomPnl, leftPnl, rightPnl, topPnl;
    private ScanPlot scanPlot;

    private RawDataFile rawDataFile;
    
    private double zoomMZMin, zoomMZMax, zoomIntensityMin, zoomIntensityMax;
       
    private Scan[] scans;

   
    public SpectrumVisualizer(RawDataFile rawDataFile, int scanNumber) {
        this(rawDataFile, new int[] { scanNumber });
    }


    public SpectrumVisualizer(RawDataFile rawDataFile, int[] scanNumbers) {

        super("", true, true, true, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        getContentPane().setLayout(new BorderLayout());

        bottomPnl = new ScanXAxis(this);
        getContentPane().add(bottomPnl, java.awt.BorderLayout.SOUTH);

        topPnl = new JPanel();
        topPnl.setMinimumSize(new Dimension(getWidth(), 5));
        topPnl.setPreferredSize(new Dimension(getWidth(), 5));
        topPnl.setBackground(Color.white);
        getContentPane().add(topPnl, java.awt.BorderLayout.NORTH);

        leftPnl = new ScanYAxis(this);
        getContentPane().add(leftPnl, java.awt.BorderLayout.WEST);

        rightPnl = new JPanel();
        rightPnl.setMinimumSize(new Dimension(5, getHeight()));
        rightPnl.setPreferredSize(new Dimension(5, getHeight()));
        rightPnl.setBackground(Color.white);
        getContentPane().add(rightPnl, java.awt.BorderLayout.EAST);

        scanPlot = new ScanPlot(this);

        getContentPane().add(scanPlot, java.awt.BorderLayout.CENTER);

        scanPlot.setVisible(true);

        pack();
        
        this.rawDataFile = rawDataFile;
        
        // TODO: create a task for this
        scans = new Scan[scanNumbers.length];
        try {
            for (int i = 0; i < scanNumbers.length; i++)
                scans[i] = rawDataFile.getScan(scanNumbers[i]);
        } catch (IOException e) {
            MainWindow.getInstance().displayErrorMessage("Error while loading scan data: " + e);
            dispose(); // TODO: is this correct?
            return;
        }
        
        resetMZRange();
        resetIntensityRange();
        updateTitle();

    }

    public void printMe() {
        PrinterJob printJob = PrinterJob.getPrinterJob();

        HashPrintRequestAttributeSet pSet = new HashPrintRequestAttributeSet();
        pSet.add(OrientationRequested.LANDSCAPE);

        if (printJob.printDialog(pSet)) {
            printJob.setPrintable(this);
            try {
                printJob.print(pSet);
            } catch (Exception PrintException) {
            }
        }

    }

    public int print(Graphics g, PageFormat pf, int pi) {
        double sx, sy;
        final int titleHeight = 30;

        if (pi > 0) {
            return NO_SUCH_PAGE;
        } else {

            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());

            g2.drawString(this.getTitle(), 0, titleHeight - 5);

            g2.translate(0, titleHeight);

            sx = (double) pf.getImageableWidth()
                    / (double) getContentPane().getWidth();
            sy = (double) (pf.getImageableHeight() - titleHeight)
                    / (double) getContentPane().getHeight();

            g2.transform(AffineTransform.getScaleInstance(sx, sy));

            RepaintManager currentManager = RepaintManager
                    .currentManager(getContentPane());
            currentManager.setDoubleBufferingEnabled(false);

            getContentPane().paint(g2);

            currentManager.setDoubleBufferingEnabled(true);
            return Printable.PAGE_EXISTS;
        }
    }

    public void copyMe() {
        // Initialize clipboard
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Clipboard clipboard = toolkit.getSystemClipboard();

        // Draw visualizer graphics
        int w = getContentPane().getWidth();
        int h = getContentPane().getHeight();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        // g.setTransform(AffineTransform.getTranslateInstance(-r.x, -r.y);
        getContentPane().paint(g);

        // Put image to clipboard
        clipboard.setContents(new TransferableImage(bi), null);

    }

    Scan[] getScans() {
        return scans;
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
    double getZoomMZMax() {
        return zoomMZMax;
    }

    /**
     * @return Returns the zoomRTMin.
     */
    double getZoomMZMin() {
        return zoomMZMin;
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
        // do nothing
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setMZRange(double,
     *      double)
     */
    public void setMZRange(double mzMin, double mzMax) {
        zoomMZMin = mzMin;
        zoomMZMax = mzMax;
        updateTitle();
        repaint();

    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setRTRange(double,
     *      double)
     */
    public void setRTRange(double rtMin, double rtMax) {
        // TODO Auto-generated method stub

    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setMZPosition(double)
     */
    public void setMZPosition(double mz) {
        // TODO Auto-generated method stub

    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setRTPosition(double)
     */
    public void setRTPosition(double rt) {
        // TODO Auto-generated method stub

    }



    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#resetMZRange()
     */
    public void resetMZRange() {
        zoomMZMin = scans[0].getMZRangeMin();
        zoomMZMax = scans[0].getMZRangeMax();       
        for (int i = 1; i < scans.length; i++) {
            if (scans[i].getMZRangeMin() < zoomMZMin)
                zoomMZMin = scans[i].getMZRangeMin();
            if (scans[i].getMZRangeMax() > zoomMZMax)
                zoomMZMax = scans[i].getMZRangeMax();
        }
        updateTitle();
        repaint();
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#resetRTRange()
     */
    public void resetRTRange() {
        // TODO Auto-generated method stub

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
        zoomIntensityMax = scans[0].getBasePeakIntensity();       
        for (int i = 1; i < scans.length; i++) {
            if (scans[i].getBasePeakIntensity() > zoomIntensityMax)
                zoomIntensityMax = scans[i].getBasePeakIntensity();
        }
        updateTitle();
        repaint();
    }
    
    private void updateTitle() {
        
        StringBuffer title = new StringBuffer();
        title.append(rawDataFile.toString());
        title.append(": ");
        
        if (scans.length == 1) {
            title.append("Scan #");
            title.append(scans[0].getScanNumber());
            title.append(", RT ");
            title.append(FormatCoordinates.formatRTValue(scans[0].getRetentionTime()));

        } else {
            title.append("Combination of spectra, RT ");
            title.append(FormatCoordinates.formatRTValue(scans[0].getRetentionTime()));
            title.append(" - ");            
            title.append(FormatCoordinates.formatRTValue(scans[scans.length - 1].getRetentionTime()));
        }
        title.append(", MS level ");
        title.append(scans[0].getMSLevel());
     
        title.append(", m/z " + FormatCoordinates.formatMZValue(zoomMZMin)
                + " - " + FormatCoordinates.formatMZValue(zoomMZMax));
        title.append(", IC " + FormatCoordinates.formatIntensityValue(zoomIntensityMin) + " - "
                + FormatCoordinates.formatIntensityValue(zoomIntensityMax));
        
        setTitle(title.toString());
        
    }

}