/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.peakpicking.twostep;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.PeakListDataSet;
import net.sf.mzmine.modules.visualization.spectra.PlotMode;
import net.sf.mzmine.modules.visualization.spectra.ScanDataSet;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.SpectraToolBar;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;

/**
 * Spectrum visualizer using JFreeChart library
 */
class MassDetectorSetupDialog extends ParameterSetupDialog implements ActionListener {

    private static final float zoomCoefficient = 1.2f;
    public static final int TEXTFIELD_COLUMNS = 10;
    
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private SpectraToolBar toolBar;
    private SpectraPlot spectrumPlot;

    private RawDataFile currentDataFile, previewDataFile;
    private RawDataFile[] dataFiles;

    private JPanel pnlPlotXY, pnlFileNameScanNumber, pnlLocal, pnlLabels, pnlFields, pnlSpace;
    private JComboBox comboDataFileName, comboScanNumber;
    
    // Currently loaded scan
    private Scan currentScan;
    private PeakList previewPeakList;
    private String[] currentScanNumberlist;
    private int[] listScans;
    
    private SimpleParameterSet parameters;

    // Current scan data set
    private ScanDataSet scanDataSet;

    private NumberFormat rtFormat = MZmineCore.getRTFormat();
    private NumberFormat mzFormat = MZmineCore.getMZFormat();
    private NumberFormat intensityFormat = MZmineCore.getIntensityFormat();
    private String [] fileNames;
    private int indexComboFileName;
    
    MassDetectorSetupDialog(RawDataFile dataFile, SimpleParameterSet parameters, String name) {

    	super (name + "'s parameter Set Up Dialog & preVisualizer", parameters);
        this.currentDataFile = dataFile;
        this.previewDataFile = dataFile;
        this.parameters = parameters;
        
        listScans = dataFile.getScanNumbers(1);
        currentScanNumberlist = new String [listScans.length];
        for (int i = 0; i < listScans.length; i++)
        	 currentScanNumberlist[i] = String.valueOf(listScans[i]);
 
		MZmineProjectImpl project = (MZmineProjectImpl) MZmineCore.getCurrentProject();
		dataFiles = project.getDataFiles();
		fileNames = new String[dataFiles.length];

        for (int i = 0; i < dataFiles.length; i++) {
        	fileNames[i]= dataFiles[i].getFileName();
            if (fileNames[i].equals(dataFile.getFileName()))
            	indexComboFileName = i;
        }

        // panels for mass detectors & peak builders
        pnlLocal = new JPanel(new BorderLayout());
        pnlLabels = new JPanel(new GridLayout(0, 1));
        pnlFields = new JPanel(new GridLayout(0, 1));
        pnlSpace = new JPanel(new GridLayout(0, 1));

        pnlFields.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        pnlLabels.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        pnlSpace.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        
        
        pnlFileNameScanNumber = new JPanel(new BorderLayout());
        JLabel lblFileSelected = new JLabel("Data file ");
        JLabel lblScanNumber = new JLabel("Scan number ");
        JLabel lblSpace = new JLabel("            ");
        comboDataFileName = new JComboBox(fileNames);
        comboDataFileName.setSelectedIndex(indexComboFileName);
        comboDataFileName.setBackground(Color.WHITE);
        comboDataFileName.addActionListener(this);
        comboScanNumber = new JComboBox(currentScanNumberlist);
        comboScanNumber.setSelectedIndex(0);
        comboScanNumber.setBackground(Color.WHITE);
        comboScanNumber.addActionListener(this);        
        
        pnlLabels.add(lblFileSelected);
        pnlLabels.add(lblScanNumber);
        pnlFields.add(comboDataFileName);
        pnlFields.add(comboScanNumber);
        pnlSpace.add(lblSpace);
        
        pnlFileNameScanNumber.add(pnlLabels, BorderLayout.WEST);
        pnlFileNameScanNumber.add(pnlFields, BorderLayout.CENTER);
        pnlFileNameScanNumber.add(pnlSpace, BorderLayout.EAST);
        
        pnlLocal.add(pnlFileNameScanNumber, BorderLayout.NORTH);        
        
        pnlPlotXY = new JPanel(new BorderLayout());
        Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
        Border two = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        pnlPlotXY.setBorder(BorderFactory.createCompoundBorder(one, two));
        pnlPlotXY.setBackground(Color.white);

        spectrumPlot = new SpectraPlot(this);
        pnlPlotXY.add(spectrumPlot, BorderLayout.CENTER);

        toolBar = new SpectraToolBar(this);
        toolBar.setAxesButtonEnabled(false);
        pnlPlotXY.add(toolBar, BorderLayout.EAST);
        pnlLocal.add(pnlPlotXY, BorderLayout.SOUTH);

        pnlAll.add(pnlLocal, BorderLayout.EAST);        
      
        loadScan(listScans[0]);

        pack();
        
        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
        
        
    }
    
    private void loadScan(final int scanNumber) {

        logger.finest("Loading scan #" + scanNumber + " from " + previewDataFile
                + " for spectra visualizer");

        // Perform the actual loading of the data in a new thread, so we don't
        // block the GUI
        Runnable newThreadRunnable = new Runnable() {

            public void run() {

                // Synchronize over data file so the threads don't fight each
                // other while updating the GUI components
                synchronized (previewDataFile) {

                    currentScan = previewDataFile.getScan(scanNumber);
                    scanDataSet = new ScanDataSet(currentScan);

                    PeakListDataSet peaksDataSet = null;

                    toolBar.setPeaksButtonEnabled(true);
                    //peaksDataSet = new PeakListDataSet(dataFile,
                      //          scanNumber, previewPeakList);

                    // Set plot data sets
                    spectrumPlot.setDataSets(scanDataSet, peaksDataSet);

                    // Set plot mode only if it hasn't been set before
                    if (spectrumPlot.getPlotMode() == PlotMode.UNDEFINED)
                        // if the scan is centroided, switch to centroid mode
                        if (currentScan.isCentroided()) {
                            spectrumPlot.setPlotMode(PlotMode.CENTROID);
                            toolBar.setCentroidButton(false);
                        } else {
                            spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
                            toolBar.setCentroidButton(true);
                        }

                    // Set window and plot titles

                    String title = "[" + previewDataFile.toString() + "] scan #"
                            + currentScan.getScanNumber();

                    String subTitle = "MS" + currentScan.getMSLevel() + ", RT "
                            + rtFormat.format(currentScan.getRetentionTime());

                    DataPoint basePeak = currentScan.getBasePeak();
                    if (basePeak != null) {
                        subTitle += ", base peak: "
                                + mzFormat.format(basePeak.getMZ())
                                + " m/z ("
                                + intensityFormat.format(basePeak.getIntensity())
                                + ")";
                    }

                    //setTitle(title);
                    spectrumPlot.setTitle(title, subTitle);
                }
            }

        };

        Thread newThread = new Thread(newThreadRunnable);
        newThread.start();

    }

    public void setAxesRange(float xMin, float xMax, float xTickSize,
            float yMin, float yMax, float yTickSize) {
        NumberAxis xAxis = (NumberAxis) spectrumPlot.getXYPlot().getDomainAxis();
        NumberAxis yAxis = (NumberAxis) spectrumPlot.getXYPlot().getRangeAxis();
        xAxis.setRange(xMin, xMax);
        xAxis.setTickUnit(new NumberTickUnit(xTickSize));
        yAxis.setRange(yMin, yMax);
        yAxis.setTickUnit(new NumberTickUnit(yTickSize));
    }

    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();
        String command = event.getActionCommand();

        if (src == btnOK) {
        	super.actionPerformed(event);
        }

        if (src == btnCancel) {
            dispose();
        }

        if (src == comboScanNumber) {
        	int ind = comboScanNumber.getSelectedIndex();
        	loadScan(listScans[ind]);
        }

        if (src == comboDataFileName) {
        	int ind = comboDataFileName.getSelectedIndex();
        	if (ind >= 0){
        		previewDataFile = dataFiles[ind];
                listScans = previewDataFile.getScanNumbers(1);
                currentScanNumberlist = new String [listScans.length];
                for (int i = 0; i < listScans.length; i++)
                	 currentScanNumberlist[i] = String.valueOf(listScans[i]);
                ComboBoxModel model = new DefaultComboBoxModel(currentScanNumberlist);
                comboScanNumber.setModel(model);
                comboScanNumber.setSelectedIndex(0);
                loadScan(listScans[0]);
        	}
        }
        
        if (command.equals("SHOW_DATA_POINTS")) {
            spectrumPlot.switchDataPointsVisible();
        }

        if (command.equals("SHOW_ANNOTATIONS")) {
            spectrumPlot.switchItemLabelsVisible();
        }

        if (command.equals("SHOW_PICKED_PEAKS")) {
            spectrumPlot.switchPickedPeaksVisible();
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

        if (command.equals("ZOOM_IN")) {
            spectrumPlot.getXYPlot().getDomainAxis().resizeRange(
                    1 / zoomCoefficient);
        }

        if (command.equals("ZOOM_OUT")) {
            spectrumPlot.getXYPlot().getDomainAxis().resizeRange(
                    zoomCoefficient);
        }
    }
    
    private void displayMessage(String msg) {
        try {
            logger.info(msg);
            JOptionPane.showMessageDialog(this, msg, "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception exce) {
        }
    }
}