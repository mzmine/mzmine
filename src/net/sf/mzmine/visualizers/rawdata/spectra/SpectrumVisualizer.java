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

package net.sf.mzmine.visualizers.rawdata.spectra;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;

import net.sf.mzmine.interfaces.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.visualizers.RawDataVisualizer;
import net.sf.mzmine.visualizers.rawdata.spectra.SpectrumPlot.PlotMode;

import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;

/**
 * TODO: implement combination of spectra
 *
 */
public class SpectrumVisualizer extends JInternalFrame implements
        RawDataVisualizer, ActionListener {

    private SpectrumToolBar toolBar;
    private SpectrumPlot spectrumPlot;

    private RawDataFile rawDataFile;

    private Scan[] scans;

    public SpectrumVisualizer(RawDataFile rawDataFile, int scanNumber) {
        this(rawDataFile, new int[] { scanNumber });
    }
    
    private DefaultTableXYDataset dataset;
    
    // TODO: get these from parameter storage
    private static DateFormat rtFormat = new SimpleDateFormat("m:ss");

    public SpectrumVisualizer(RawDataFile rawDataFile, int[] scanNumbers) {

        super(rawDataFile.toString(), true, true, true, true);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        toolBar = new SpectrumToolBar(this);
        add(toolBar, BorderLayout.EAST);

        this.rawDataFile = rawDataFile;
        
        dataset = new DefaultTableXYDataset();
        // set minimum width for peak line (in fact, a bar)
        dataset.setIntervalWidth(Double.MIN_VALUE);
        XYSeries series = new XYSeries(rawDataFile.toString(), false, false);
        dataset.addSeries(series);

        // TODO: create a task for this?
        scans = new Scan[scanNumbers.length];
        try {
            for (int i = 0; i < scanNumbers.length; i++) {
                scans[i] = rawDataFile.getScan(scanNumbers[i]);
                double mzValues[] = scans[i].getMZValues();
                double intValues[] = scans[i].getIntensityValues();
                for (int j = 0; j < mzValues.length; j++)
                    series.addOrUpdate(mzValues[j], intValues[j]);
            }
        } catch (IOException e) {
            MainWindow.getInstance().displayErrorMessage(
                    "Error while loading scan data: " + e);
            dispose(); // TODO: is this correct?
            return;
        }
        
        spectrumPlot = new SpectrumPlot(this, dataset);
        add(spectrumPlot, BorderLayout.CENTER);
        
        pack();
        
        // if the scans are centroided, switch to centroid mode
        if (scans[0].isCentroided()) {
            spectrumPlot.setPlotMode(PlotMode.CENTROID);
            toolBar.setCentroidButton(false);
        } else {
            spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
            toolBar.setCentroidButton(true);
        }
        
        updateTitle();

    }

    Scan[] getScans() {
        return scans;
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setMZRange(double,
     *      double)
     */
    public void setMZRange(double mzMin, double mzMax) {
        spectrumPlot.getPlot().getDomainAxis().setRange(mzMin, mzMax);
    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setRTRange(double,
     *      double)
     */
    public void setRTRange(double rtMin, double rtMax) {
        // do nothing

    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#setIntensityRange(double,
     *      double)
     */
    public void setIntensityRange(double intensityMin, double intensityMax) {
        spectrumPlot.getPlot().getRangeAxis().setRange(intensityMin, intensityMax);
    }



    private void updateTitle() {

        StringBuffer title = new StringBuffer();
        title.append(rawDataFile.toString());
        title.append(": ");

        if (scans.length == 1) {
            title.append("Scan #");
            title.append(scans[0].getScanNumber());
            title.append(", RT ");
            title.append(rtFormat.format(scans[0]
                    .getRetentionTime() * 1000));

        } else {
            title.append("Combination of spectra, RT ");
            title.append(rtFormat.format(scans[0]
                    .getRetentionTime() * 1000));
            title.append(" - ");
            title.append(rtFormat.format(scans[scans.length - 1].getRetentionTime() * 1000));
        }
        setTitle(title.toString());

        title.append(", MS");
        title.append(scans[0].getMSLevel());

        spectrumPlot.setTitle(title.toString());

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("SHOW_DATA_POINTS")) {
            spectrumPlot.switchDataPointsVisible();
        }

        if (command.equals("SHOW_ANNOTATIONS")) {
            spectrumPlot.switchItemLabelsVisible();
        }

        if (command.equals("TOGGLE_PLOT_MODE")) {
            if (spectrumPlot.getPlotMode() == PlotMode.CONTINUOUS) {
                spectrumPlot.setPlotMode(PlotMode.CENTROID);
                toolBar.setCentroidButton(false);
            } else {
                spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
                toolBar.setCentroidButton(true);
            }
        }

    }

    /**
     * @see net.sf.mzmine.visualizers.RawDataVisualizer#getRawDataFiles()
     */
    public RawDataFile[] getRawDataFiles() {
        return new RawDataFile[] { rawDataFile };
    }

}