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

import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peakpicking.recursivethreshold.RecursivePickerParameters;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.centroid.CentroidMassDetector;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.centroid.CentroidMassDetectorParameters;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass.ExactMassDetector;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass.ExactMassDetectorParameters;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.localmaxima.LocalMaxMassDetector;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.localmaxima.LocalMaxMassDetectorParameters;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.wavelet.WaveletMassDetector;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.wavelet.WaveletMassDetectorParameters;
import net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.simpleconnector.ConnectedPeak;
import net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.simpleconnector.SimpleConnector;
import net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.simpleconnector.SimpleConnectorParameters;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.Range;

/**
 * 
 */
class TwoStepPickerTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private RawDataFile dataFile;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;
    private int twoStepsConf = 0;

    // scan counter
    private int processedScans, totalScans;
    private int newPeakID = 1;
    private int[] scanNumbers;
    
    // User parameters
    private String suffix;

    // Mass Detectors parameters
    private CentroidMassDetector centroidMassDetector;
    private LocalMaxMassDetector localMaxMassDetector;
    private ExactMassDetector exactMassDetector;
    private WaveletMassDetector waveletMassDetector;

    
    // Mass Detectors parameters
    private CentroidMassDetectorParameters centroidMassDetectorParameters;
    private LocalMaxMassDetectorParameters localMaxMassDetectorParameters;
    private ExactMassDetectorParameters exactMassDetectorParameters;
    private WaveletMassDetectorParameters waveletMassDetectorParameters;
    private int massDetectorTypeNumber, peakBuilderTypeNumber;
    
    // Peak Builders 
    private SimpleConnector simpleConnector;
    
    // Peak Builders parameters
    private SimpleConnectorParameters simpleConnectorParameters;
    
    
    /**
     * @param dataFile
     * @param parameters
     */
    TwoStepPickerTask(RawDataFile dataFile, TwoStepPickerParameters parameters) {
    	
    	this.dataFile = dataFile;

        // Get minimum and maximum m/z values
        Range mzRange = dataFile.getDataMZRange(1);

        massDetectorTypeNumber = parameters.getMassDetectorTypeNumber();
    	peakBuilderTypeNumber = parameters.getPeakBuilderTypeNumber();
    	suffix = parameters.getSuffix();
    	scanNumbers = dataFile.getScanNumbers(1);
        totalScans = scanNumbers.length;
    	localMaxMassDetectorParameters = (LocalMaxMassDetectorParameters) parameters.getMassDetectorParameters(1);
    	localMaxMassDetector = new LocalMaxMassDetector(localMaxMassDetectorParameters, mzRange);
    	centroidMassDetectorParameters = (CentroidMassDetectorParameters) parameters.getMassDetectorParameters(0);
    	centroidMassDetector =  new CentroidMassDetector(centroidMassDetectorParameters);
    	exactMassDetectorParameters = (ExactMassDetectorParameters) parameters.getMassDetectorParameters(2); 
    	exactMassDetector = new ExactMassDetector(exactMassDetectorParameters);
    	waveletMassDetectorParameters = (WaveletMassDetectorParameters) parameters.getMassDetectorParameters(3);
    	waveletMassDetector =  new WaveletMassDetector(waveletMassDetectorParameters);
    	simpleConnectorParameters = (SimpleConnectorParameters) parameters.getPeakBuilderParameters(0);
    	simpleConnector =  new SimpleConnector(simpleConnectorParameters);
        
        // make an instance of selected mass detector and peak builder
        switch (massDetectorTypeNumber) {
        case 0:
        	twoStepsConf = 1000;
            break;
        case 1:
        	twoStepsConf = 2000;
            break;
        case 2:
        	twoStepsConf = 3000;
            break;
        case 3:
        	twoStepsConf = 4000;
        	break;
        }        

        switch (peakBuilderTypeNumber) {
        case 0:
        	twoStepsConf += 1;
            break; 
        }

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Two step peak detection on " + dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        if (totalScans == 0)
            return 0.0f;
        return (float) processedScans / (float) (2 * totalScans);
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    public RawDataFile getDataFile() {
        return dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;
        // Create new peak list
        SimplePeakList newPeakList = new SimplePeakList(
                dataFile + " " + suffix, dataFile);

        // MzPeak
        Vector<MzPeak> mzValues = new Vector<MzPeak>();
        Vector<ConnectedPeak> underConstructionPeaks= new Vector<ConnectedPeak>();
        Vector<Peak> peaks = new Vector<Peak>();
        
        // process each scan, detect masses, connect masses
         switch (twoStepsConf) {
        case 1001:
        	for (int i=0; i<totalScans; i++){
        		Scan scan= dataFile.getScan(scanNumbers[i]);  
        		mzValues = centroidMassDetector.getMassValues(scan);
        		peaks = simpleConnector.addScan(scan, mzValues, underConstructionPeaks, dataFile);
        		for (Peak finishedPeak: peaks){
            		SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
                    newPeakID++;
                    newRow.addPeak(dataFile, finishedPeak, finishedPeak);
                    newPeakList.addRow(newRow);
        		}
        		processedScans++;
        	}
            break;
        case 2001:
        	for (int i=0; i<totalScans; i++){
        		Scan scan= dataFile.getScan(scanNumbers[i]);  
        		mzValues = localMaxMassDetector.getMassValues(scan, i);
        		peaks = simpleConnector.addScan(scan, mzValues, underConstructionPeaks, dataFile);
        		for (Peak finishedPeak: peaks){
            		SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
                    newPeakID++;
                    newRow.addPeak(dataFile, finishedPeak, finishedPeak);
                    newPeakList.addRow(newRow);
        		}
        		processedScans++;
        	}
            break;
        case 3001:
        	for (int i=0; i<totalScans; i++){
        		Scan scan= dataFile.getScan(scanNumbers[i]);  
        		mzValues = exactMassDetector.getMassValues(scan);
        		peaks = simpleConnector.addScan(scan, mzValues, underConstructionPeaks, dataFile);
        		for (Peak finishedPeak: peaks){
            		SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
                    newPeakID++;
                    newRow.addPeak(dataFile, finishedPeak, finishedPeak);
                    newPeakList.addRow(newRow);
        		}
        		processedScans++;
        	}
            break;
        case 4001:
        	for (int i=0; i<totalScans; i++){
        		Scan scan= dataFile.getScan(scanNumbers[i]);  
        		mzValues = waveletMassDetector.getMassValues(scan);
        		peaks = simpleConnector.addScan(scan, mzValues, underConstructionPeaks, dataFile);
        		for (Peak finishedPeak: peaks){
            		SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
                    newPeakID++;
                    newRow.addPeak(dataFile, finishedPeak, finishedPeak);
                    newPeakList.addRow(newRow);
        		}
        		processedScans++;
        	}
        	break;
         }        
        
         
         for (ConnectedPeak ucPeak : underConstructionPeaks) {

         	// Finalize peak
         	ucPeak.finalizedAddingDatapoints(PeakStatus.DETECTED);
         	
             // Check length & height
             float ucLength = ucPeak.getRawDataPointsRTRange().getSize();
             float ucHeight = ucPeak.getHeight();
             float minimumPeakDuration = (Float) simpleConnectorParameters.getParameterValue
             		(RecursivePickerParameters.minimumPeakDuration);
             float minimumPeakHeight = (Float) simpleConnectorParameters.getParameterValue
     				(RecursivePickerParameters.minimumPeakHeight);
             
             if ((ucLength >= minimumPeakDuration)
                     && (ucHeight >= minimumPeakHeight)) {

                 // Good peak, add it to the peak list
                 SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
                 newPeakID++;
                 newRow.addPeak(dataFile, ucPeak, ucPeak);
                 newPeakList.addRow(newRow);
             }
         }
         
         // Add new peaklist to the project
         MZmineProject currentProject = MZmineCore.getCurrentProject();
         currentProject.addPeakList(newPeakList);

         status = TaskStatus.FINISHED;
    }
}
