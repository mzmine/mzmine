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
import java.io.IOException;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.RepaintManager;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.Scan;
import net.sf.mzmine.obsoletedatastructures.FormatCoordinates;
import net.sf.mzmine.userinterface.components.XAxis;
import net.sf.mzmine.userinterface.components.YAxis;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.TransferableImage;
import net.sf.mzmine.util.format.IntensityValueFormat;
import net.sf.mzmine.util.format.MZValueFormat;
import net.sf.mzmine.util.format.ValueFormat;
import net.sf.mzmine.visualizers.RawDataVisualizer;
import net.sf.mzmine.visualizers.rawdata.spectra.SpectrumPlot.PlotMode;

public class SpectrumVisualizer extends JInternalFrame implements
        RawDataVisualizer, Printable, ActionListener {

    private SpectrumToolBar toolBar;
    private SpectrumPopupMenu popupMenu;
    private SpectrumPlot spectrumPlot;

    private JLabel titleLabel;
    private XAxis xAxis;
    private YAxis yAxis;

    private RawDataFile rawDataFile;

    private double zoomMZMin, zoomMZMax, zoomIntensityMin, zoomIntensityMax;

    private ValueFormat mzFormat, intensityFormat;

    private Scan[] scans;

    public SpectrumVisualizer(RawDataFile rawDataFile, int scanNumber) {
        this(rawDataFile, new int[] { scanNumber });
    }

    public SpectrumVisualizer(RawDataFile rawDataFile, int[] scanNumbers) {

        super(rawDataFile.toString(), true, true, true, true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        mzFormat = new MZValueFormat();
        intensityFormat = new IntensityValueFormat();

        popupMenu = new SpectrumPopupMenu(this);

        setLayout(new BorderLayout());
        setBackground(Color.white);

        titleLabel = new JLabel(rawDataFile.toString(), JLabel.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        titleLabel.setFont(titleLabel.getFont().deriveFont(11.0f));
        add(titleLabel, BorderLayout.NORTH);

        toolBar = new SpectrumToolBar(this);
        add(toolBar, BorderLayout.EAST);

        yAxis = new YAxis(0, 0, 0, 0, intensityFormat);
        add(yAxis, BorderLayout.WEST);

        xAxis = new XAxis(0, 0, (int) yAxis.getPreferredSize().getWidth(),
                (int) toolBar.getPreferredSize().getWidth(), mzFormat);
        add(xAxis, BorderLayout.SOUTH);

        spectrumPlot = new SpectrumPlot(this);
        add(spectrumPlot, BorderLayout.CENTER);

        pack();

        this.rawDataFile = rawDataFile;

        // TODO: create a task for this
        scans = new Scan[scanNumbers.length];
        try {
            for (int i = 0; i < scanNumbers.length; i++)
                scans[i] = rawDataFile.getScan(scanNumbers[i]);
        } catch (IOException e) {
            MainWindow.getInstance().displayErrorMessage(
                    "Error while loading scan data: " + e);
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

    SpectrumPopupMenu getPopupMenu() {
        return popupMenu;
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
        toolBar.setZoomOutButtonEnabled(true);
        popupMenu.setZoomOutMenuItem(true);
        zoomMZMin = mzMin;
        zoomMZMax = mzMax;
        xAxis.setRange(mzMin, mzMax);
        spectrumPlot.setMZRange(mzMin, mzMax);
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
        xAxis.setRange(zoomMZMin, zoomMZMax);
        spectrumPlot.setMZRange(zoomMZMin, zoomMZMax);
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
        toolBar.setZoomOutButtonEnabled(true);
        popupMenu.setZoomOutMenuItem(true);
        zoomIntensityMin = intensityMin;
        zoomIntensityMax = intensityMax;
        yAxis.setRange(zoomIntensityMin, zoomIntensityMax);
        spectrumPlot.setIntensityRange(zoomIntensityMin, zoomIntensityMax);
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
        yAxis.setRange(zoomIntensityMin, zoomIntensityMax);
        spectrumPlot.setIntensityRange(zoomIntensityMin, zoomIntensityMax);
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
            title.append(FormatCoordinates.formatRTValue(scans[0]
                    .getRetentionTime()));

        } else {
            title.append("Combination of spectra, RT ");
            title.append(FormatCoordinates.formatRTValue(scans[0]
                    .getRetentionTime()));
            title.append(" - ");
            title.append(FormatCoordinates
                    .formatRTValue(scans[scans.length - 1].getRetentionTime()));
        }
        setTitle(title.toString());
        title.setLength(0);

        title.append("MS level ");
        title.append(scans[0].getMSLevel());

        title.append(", m/z " + mzFormat.format(zoomMZMin) + " - "
                + mzFormat.format(zoomMZMax));
        title.append(", intensity " + intensityFormat.format(zoomIntensityMin)
                + " - " + intensityFormat.format(zoomIntensityMax));

        titleLabel.setText(title.toString());

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("ZOOM_OUT")) {
            resetMZRange();
            resetIntensityRange();
            toolBar.setZoomOutButtonEnabled(false);
            popupMenu.setZoomOutMenuItem(false);
        }

        if (command.equals("SHOW_DATA_POINTS")) {
            if (spectrumPlot.getShowDataPoints()) {
                spectrumPlot.setShowDataPoints(false);
                popupMenu.setDataPointsMenuItem("Show data points");
            } else {
                spectrumPlot.setShowDataPoints(true);
                popupMenu.setDataPointsMenuItem("Hide data points");
            }
        }

        if (command.equals("SET_PLOT_MODE")) {
            if (spectrumPlot.getPlotMode() == PlotMode.CENTROID) {
                spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
                popupMenu.setPlotModeMenuItem("Show as centroid");
                toolBar.setCentroidButton(true);
            } else {
                spectrumPlot.setPlotMode(PlotMode.CENTROID);
                popupMenu.setPlotModeMenuItem("Show as continuous");
                toolBar.setCentroidButton(false);
            }

        }

        if (command.equals("SHOW_ANNOTATIONS")) {
            if (spectrumPlot.getShowAnnotations()) {
                spectrumPlot.setShowAnnotations(false);
                popupMenu.setAnnotationsMenuItem("Show base peak values");
            } else {
                spectrumPlot.setShowAnnotations(true);
                popupMenu.setAnnotationsMenuItem("Hide base peak values");
            }
        }

    }

}