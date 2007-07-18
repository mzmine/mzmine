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

package net.sf.mzmine.modules.visualization.tic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import javax.swing.JInternalFrame;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.io.util.RawDataAcceptor;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MultipleRawDataVisualizer;
import net.sf.mzmine.modules.visualization.spectra.SpectraSetupDialog;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerWindow;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.util.CursorPosition;

import org.jfree.data.xy.DefaultXYDataset;

/**
 * Total ion chromatogram visualizer using JFreeChart library
 */
public class TICVisualizerWindow extends JInternalFrame implements
        MultipleRawDataVisualizer, TaskListener, ActionListener {

    public static enum PlotType { TIC, BASE_PEAK };
    
    private TICToolBar toolBar;
    private TICPlot plot;

    private Hashtable<RawDataFile, TICDataSet> dataFiles;
    private PlotType plotType;
    private int msLevel;
    private float rtMin, rtMax, mzMin, mzMax;
    
    private Peak[] peaks;
    
    private static final double zoomCoefficient = 1.2;

    private Desktop desktop;
    
    /**
     * Constructor for total ion chromatogram visualizer
     * 
     */
    public TICVisualizerWindow(RawDataFile dataFile, PlotType plotType,
            int msLevel,
            float rtMin, float rtMax,
            float mzMin, float mzMax, Peak[] peaks) {
        
        super(null, true, true, true, true);
        
        this.desktop = MZmineCore.getDesktop();
        this.plotType = plotType;
        this.msLevel = msLevel;
        this.dataFiles = new Hashtable<RawDataFile, TICDataSet>();
        this.rtMin = rtMin;
        this.rtMax = rtMax;
        this.mzMin = mzMin;
        this.mzMax = mzMax;
        this.peaks = peaks;
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        toolBar = new TICToolBar(this);
        add(toolBar, BorderLayout.EAST);

        plot = new TICPlot(this);
        add(plot, BorderLayout.CENTER);

        addRawDataFile(dataFile);

        pack();

    }
    
    void updateTitle() {

        NumberFormat rtFormat = desktop.getRTFormat();
        NumberFormat mzFormat = desktop.getMZFormat();
        NumberFormat intensityFormat = desktop.getIntensityFormat();
        
        StringBuffer title = new StringBuffer();
        title.append(dataFiles.keySet().toString());
        title.append(": ");
        if (plotType == PlotType.BASE_PEAK) title.append("base peak");
            else { 
            	// If all datafiles have m/z range less than or equal to range of the plot (mzMin, mzMax), then call this TIC, otherwise XIC 
            	Set<RawDataFile> fileSet = dataFiles.keySet();
            	String ticOrXIC = "TIC";
            	for (RawDataFile orf : fileSet) {
            		if ( (orf.getDataMinMZ(msLevel)<mzMin) || (orf.getDataMinMZ(msLevel)>mzMax) ) {
            			ticOrXIC = "XIC";
            			break;
            		}
            	}
            	title.append(ticOrXIC); 
            }
        
        title.append(" MS" + msLevel);

        setTitle(title.toString());
        
        title.append(", m/z: " + mzFormat.format(mzMin) + " - " + mzFormat.format(mzMax));

        CursorPosition pos = getCursorPosition();

        if (pos != null) {
            title.append(", scan #");
            title.append(pos.getScanNumber());
            if (dataFiles.size() > 1)
                title.append(" (" + pos.getDataFile() + ")");
            title.append(", RT: " + rtFormat.format(pos.getRetentionTime()));
            if (plotType == PlotType.BASE_PEAK)
                title.append(", base peak: " + mzFormat.format(pos.getMzValue()) + " m/z");
            title.append(", IC: " + intensityFormat.format(pos.getIntensityValue()));
        }
        
        if (peaks != null) {
        	if (peaks.length>1) {
        		title.append(", " + peaks.length + " peaks");
        	} else {
            	title.append(", peak m/z: " + mzFormat.format(peaks[0].getMZ()));
        	}

        }

        plot.setTitle(title.toString());

    }

    
    /**
     * @return Returns the plotType.
     */
    PlotType getPlotType() {
        return plotType;
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#setMZRange(double,
     *      double)
     */
    public void setMZRange(double mzMin, double mzMax) {
        // do nothing
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#setRTRange(double,
     *      double)
     */
    public void setRTRange(double rtMin, double rtMax) {
        plot.getXYPlot().getDomainAxis().setRange(rtMin, rtMax);
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#setIntensityRange(double,
     *      double)
     */
    public void setIntensityRange(double intensityMin, double intensityMax) {
        plot.getXYPlot().getRangeAxis().setRange(intensityMin, intensityMax);
    }

    /**
     * @see net.sf.mzmine.modules.RawDataVisualizer#getRawDataFiles()
     */
    public RawDataFile[] getRawDataFiles() {
        return dataFiles.keySet().toArray(new RawDataFile[0]);
    }

    public void addRawDataFile(RawDataFile newFile) {
        
        int scanNumbers[] = newFile.getScanNumbers(msLevel, rtMin, rtMax);
        if (scanNumbers.length == 0) {
            desktop.displayErrorMessage("No scans found at MS level " + msLevel + " within given retention time range.");
            return;
        }
        
        Vector<RawDataAcceptor> dataSets = new Vector<RawDataAcceptor>();
        
        // Initialize data sets
        
        // i) Normal TIC
        TICDataSet ticDataset = new TICDataSet(newFile, scanNumbers, mzMin, mzMax, this);
        dataSets.add(ticDataset);
        dataFiles.put(newFile, ticDataset);
        plot.addTICDataset(ticDataset);
        
        Desktop desktop = MZmineCore.getDesktop();
        NumberFormat mzFormat = desktop.getMZFormat();

        // ii) Another dataset for integrated peak area, if peak is given
        if (peaks!=null) {
        	// TODO: Needs work for multi-peak
        	
            DefaultXYDataset integratedPeakAreaDataset = new DefaultXYDataset();
            
            double[] peakLabelsX = new double[peaks.length];
            double[] peakLabelsY = new double[peaks.length];
            String[] peakLabelsString = new String[peaks.length];
            
            int peakNumber = 0;
            for (Peak p : peaks) {
                     	
            	int[] peakScanNumbers = p.getScanNumbers();
            	double[][] data = new double[2][peakScanNumbers.length];
            	          	
            	double maxHeight = 0.0;
            	double maxHeightRT = 0.0;
            	for (int i=0; i< peakScanNumbers.length; i++) {
            		
            		int scanNumber = peakScanNumbers[i];

                    float rt = newFile.getScan(scanNumber).getRetentionTime();
            		float[][] datapoints = p.getRawDatapoints(scanNumber);
            		// Choose maximum height data point
            		double height = 0.0;
            		for (float[] mzHeight : datapoints)
            			if (mzHeight[1]>height) height = mzHeight[1];
           		    
            		data[0][i] = rt;
            		data[1][i] = height;
            		
            		if (height>=maxHeight) {
            			maxHeight = height;
            			maxHeightRT = rt;
            		}
            		           	
            	}
            	
            	peakLabelsX[peakNumber] = maxHeightRT;
            	peakLabelsY[peakNumber] = (0.90)*maxHeight;
            	peakLabelsString[peakNumber] = mzFormat.format(p.getMZ());           	
            	
            	integratedPeakAreaDataset.addSeries(peakNumber, data);
            	peakNumber++;
            	
            }
            
            
            DefaultXYDataset peakAreaLabelDataset = new DefaultXYDataset();
            double[][] labelData = new double[2][peakLabelsX.length];
            labelData[0] = peakLabelsX;
            labelData[1] = peakLabelsY;
            peakAreaLabelDataset.addSeries(0, labelData);
            
            plot.addIntegratedPeakAreaDataset(integratedPeakAreaDataset, peakAreaLabelDataset, peakLabelsString);      	
        	
        }
        
        
        
        // Start-up the refresh task
        new TICRawDataAcceptor(newFile, scanNumbers, dataSets.toArray(new RawDataAcceptor[0]), this);
        
        if (dataFiles.size() > 1) {
            // when displaying more than one file, show a legend
            plot.showLegend(true);
        } else {
            // when adding first file, set the retention time range
            setRTRange(rtMin, rtMax);
        }

    }

    public void removeRawDataFile(RawDataFile file) {
        TICDataSet dataset = dataFiles.get(file);
        plot.getXYPlot().setDataset(plot.getXYPlot().indexOf(dataset), null);
        dataFiles.remove(file);

        // when displaying less than two files, hide a legend
        if (dataFiles.size() < 2) {
            plot.showLegend(false);
        }

    }


    /**
     * @return current cursor position
     */
    public CursorPosition getCursorPosition() {
        float selectedRT = (float) plot.getXYPlot().getDomainCrosshairValue();
        float selectedIT = (float) plot.getXYPlot().getRangeCrosshairValue();
        Enumeration<TICDataSet> e = dataFiles.elements();
        while (e.hasMoreElements()) {
            TICDataSet dataSet = e.nextElement();
            int index = dataSet.getIndex(selectedRT, selectedIT);
            if (index >= 0) {
                float mz = 0;
                if (plotType == PlotType.BASE_PEAK) mz = (float) dataSet.getZValue(0, index);
                CursorPosition pos = new CursorPosition(selectedRT, mz,
                        selectedIT, dataSet.getDataFile(), dataSet.getScanNumber(index));
                return pos;
            }
        }
        return null;
    }

    /**
     * @return current cursor position
     */
    public void setCursorPosition(CursorPosition newPosition) {
        plot.getXYPlot().setDomainCrosshairValue(
                newPosition.getRetentionTime(), false);
        plot.getXYPlot().setRangeCrosshairValue(
                newPosition.getIntensityValue());
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {
        if (task.getStatus() == TaskStatus.ERROR) {
            desktop.displayErrorMessage(
                    "Error while updating TIC visualizer: "
                            + task.getErrorMessage());
        }

    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskStarted(Task task) {
        // if we have not added this frame before, do it now
        if (getParent() == null)
            desktop.addInternalFrame(this);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals("SHOW_DATA_POINTS")) {
            plot.switchDataPointsVisible();
        }

        if (command.equals("SHOW_ANNOTATIONS")) {
            plot.switchItemLabelsVisible();
        }

        if (command.equals("SHOW_SPECTRUM")) {
            CursorPosition pos = getCursorPosition();
            if (pos != null) {
                new SpectraVisualizerWindow(pos.getDataFile(), pos
                        .getScanNumber());
            }
        }

        if (command.equals("MOVE_CURSOR_LEFT")) {
            CursorPosition pos = getCursorPosition();
            if (pos != null) {
                TICDataSet dataSet = dataFiles.get(pos.getDataFile());
                int index = dataSet.getIndex(pos.getRetentionTime(), pos
                        .getIntensityValue());
                if (index > 0) {
                    index--;
                    pos.setRetentionTime((float) dataSet.getXValue(0, index));
                    pos.setIntensityValue((float) dataSet.getYValue(0, index));
                    setCursorPosition(pos);

                }
            }
        }

        if (command.equals("MOVE_CURSOR_RIGHT")) {
            CursorPosition pos = getCursorPosition();
            if (pos != null) {
                TICDataSet dataSet = dataFiles.get(pos.getDataFile());
                int index = dataSet.getIndex(pos.getRetentionTime(), pos
                        .getIntensityValue());
                if (index >= 0) {
                    index++;
                    if (index < dataSet.getItemCount(0)) {
                        pos.setRetentionTime((float) dataSet.getXValue(0, index));
                        pos.setIntensityValue((float) dataSet.getYValue(0, index));
                        setCursorPosition(pos);
                    }
                }
            }
        }

        if (command.equals("ZOOM_IN")) {
            plot.getXYPlot().getDomainAxis().resizeRange(1 / zoomCoefficient);
        }

        if (command.equals("ZOOM_OUT")) {
            plot.getXYPlot().getDomainAxis().resizeRange(zoomCoefficient);
        }

        if (command.equals("SHOW_MULTIPLE_SPECTRA")) {
            CursorPosition pos = getCursorPosition();
            if (pos != null) {
                SpectraSetupDialog dialog = new SpectraSetupDialog(pos.getDataFile(), msLevel, pos.getScanNumber());
                dialog.setVisible(true);
            }
        }        

    }

}
