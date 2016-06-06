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

package net.sf.mzmine.modules.peaklistmethods.identification.ms2search;

import java.util.Arrays;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

import com.google.common.collect.Range;

public class Ms2SearchTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private int finishedRows, totalRows;
    private PeakList peakList1;
    private PeakList peakList2;

    private RTTolerance rtTolerance;
    private MZTolerance mzTolerance;
    private double maxComplexHeight;
    private IonizationType ionType;
    private ParameterSet parameters;

    /**
     * @param parameters
     * @param peakList
     */
    public Ms2SearchTask(ParameterSet parameters, PeakList peakList1, PeakList peakList2) {

	this.peakList1 = peakList1;
	this.peakList2 = peakList2;
	this.parameters = parameters;

	ionType = parameters.getParameter(
		Ms2SearchParameters.ionizationMethod).getValue();
	rtTolerance = parameters.getParameter(
		Ms2SearchParameters.rtTolerance).getValue();
	mzTolerance = parameters.getParameter(
		Ms2SearchParameters.mzTolerance).getValue();
	maxComplexHeight = parameters.getParameter(
		Ms2SearchParameters.maxComplexHeight).getValue();
	
	
	
	

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
	if (totalRows == 0)
	    return 0;
	return ((double) finishedRows) / totalRows;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
	return "MS2 similarity comparison between " + peakList1 + "and" + peakList2;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

	setStatus(TaskStatus.PROCESSING);

	logger.info("Starting MS2 similarity search in " + peakList1 + "and" + peakList2);

	double result;
	PeakListRow rows1[] = peakList1.getRows();
	PeakListRow rows2[] = peakList2.getRows();
	int rows1Length = rows1.length;
	int rows2Length = rows2.length;
	for (int i = 0; i < rows1Length; i++){
	    for (int j = 0; j < rows2Length; j++)
	    {
	        result = simpleMS2similarity(rows1[i].getBestPeak(),rows2[j].getBestPeak(), 1E3, 5);
	    }
	    
	}

	// Add task description to peakList
	((SimplePeakList) peakList1)
		.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
			"Identification of complexes", parameters));

        // Repaint the window to reflect the change in the peak list
        Desktop desktop = MZmineCore.getDesktop();
        if (!(desktop instanceof HeadLessDesktop))
            desktop.getMainWindow().repaint();

	setStatus(TaskStatus.FINISHED);

	logger.info("Finished MS2 similarity search in " + peakList1 + "and" + peakList2);

    }

    /**
     * Check if candidate peak may be a possible complex of given two peaks
     * 
     */
    private double simpleMS2similarity(Feature peak1, Feature peak2,double intensityThreshold, double mzRangePPM) {
        //This could probably return some sort of key:value dictionary (say scanHash:similarity) for the similarity scores, 
        //instead of the current double[] return
        double runningScoreTotal = 0.0;

        //Fetch 1st peak MS2 scan.
        int ms2ScanNumberA = peak1.getMostIntenseFragmentScanNumber();
        Scan peakMS2A = peak1.getDataFile().getScan(ms2ScanNumberA);
        RawDataFile peak1DataFile = peak1.getDataFile();
        int peak1ID = peak1.hashCode(); //Use this for the key:value dictionary?
        
        //Fetch 2nd peak MS2 scan.
        int ms2ScanNumberB = peak2.getMostIntenseFragmentScanNumber();
        Scan peakMS2B = peak2.getDataFile().getScan(ms2ScanNumberB);
        RawDataFile peak2DataFile = peak2.getDataFile();
        int peak2ID = peak2.hashCode(); //Use this for the key:value dictionary?

        DataPoint[] peaksA = null;
        DataPoint[] peaksB = null;
        
        peaksA = peakMS2A.getDataPointsOverIntensity(intensityThreshold);
        peaksB = peakMS2B.getDataPointsOverIntensity(intensityThreshold); 
        
        //Compare every peak in MS2 scan A, to MS2 scan B.
        for ( int i = 0; i < peaksA.length; i++)
        {
            for ( int j = 0; j < peaksB.length; j++)
            {
                if (Math.abs( peaksA[i].getMZ() - peaksB[j].getMZ() ) < peaksA[i].getMZ()*1e-6*mzRangePPM)
                { 
                    runningScoreTotal += peaksA[i].getIntensity()*peaksB[j].getIntensity();  
                }
            }
        }
        
       
       return runningScoreTotal;

    }

    /**
     * Add new identity to the complex row
     * 
     * @param mainRow
     * @param fragmentRow
     */
    private void addComplexInfo(PeakListRow complexRow, PeakListRow row1,
	    PeakListRow row2) {
	Ms2Identity newIdentity = new Ms2Identity(row1, row2);
	complexRow.addPeakIdentity(newIdentity, false);

	// Notify the GUI about the change in the project
	MZmineCore.getProjectManager().getCurrentProject()
		.notifyObjectChanged(complexRow, false);
    }

}
