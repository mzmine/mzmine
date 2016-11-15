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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MassList;
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

class Ms2SearchResult {
    private double score;
    private String searchType;
    private List<DataPoint> matchedIons;
    
    public Ms2SearchResult(double score, String searchType, List<DataPoint> matchedIons)
    {
    this.score = score;
    this.searchType = searchType;
    this.matchedIons = matchedIons;
    }
    
    public double getScore()
    {
        return this.score;
    }
    
    public int getNumIonsMatched()
    {
        return matchedIons.size();
    }
    
    public String getSearchType()
    {
        return this.searchType;
    }
    
    public List<DataPoint> getMatchedIons()
    {
        return this.matchedIons;
    }
    
    public String getMatchedIonsAsString()
    {
        //Return the matched ions as a string with the following format:
        //10.2312_20.4324_55.1231
        String returnString = new String();
        for (int i = 0; i < this.matchedIons.size(); i++)
        {
        returnString = returnString  + String.format("%.4f",this.matchedIons.get(i).getMZ()) + "_";  
        }
        return returnString.substring(0,returnString.length()-1); //Some hackery to remove the last "_" 
    }
      
 }

class Ms2SearchTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private int finishedRows, totalRows;
    private PeakList peakList1;
    private PeakList peakList2;

    private MZTolerance mzTolerance;
    private ParameterSet parameters;
    private double scoreThreshold;
    private double intensityThreshold;
    private int minimumIonsMatched;
    private String massListName;

    /**
     * @param parameters
     * @param peakList
     */
    public Ms2SearchTask(ParameterSet parameters, PeakList peakList1,
            PeakList peakList2) {

        this.peakList1 = peakList1;
        this.peakList2 = peakList2;
        this.parameters = parameters;

        mzTolerance = parameters.getParameter(Ms2SearchParameters.mzTolerance)
                .getValue();
        
        scoreThreshold = parameters.getParameter(Ms2SearchParameters.scoreThreshold)
                .getValue();
        
        intensityThreshold = parameters.getParameter(Ms2SearchParameters.intensityThreshold)
                .getValue();
        
        minimumIonsMatched = parameters.getParameter(Ms2SearchParameters.minimumIonsMatched)
                .getValue();
        
        massListName = parameters.getParameter(Ms2SearchParameters.massList)
                .getValue();

        
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
        return "MS2 similarity comparison between " + peakList1 + " and "
                + peakList2;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        setStatus(TaskStatus.PROCESSING);

        logger.info("Starting MS2 similarity search between " + peakList1 + "and"
                + peakList2 + "with mz tolerance:"+mzTolerance.getPpmTolerance());

        Ms2SearchResult searchResult;
        PeakListRow rows1[] = peakList1.getRows();
        PeakListRow rows2[] = peakList2.getRows();
        
        int rows1Length = rows1.length;
        int rows2Length = rows2.length;
        
        totalRows = rows1Length;
        
        for (int i = 0; i < rows1Length; i++) {
            for (int j = 0; j < rows2Length; j++) {
                Feature featureA = rows1[i].getBestPeak();
                Feature featureB = rows2[j].getBestPeak();
                
                searchResult = simpleMS2similarity(featureA,
                        featureB, intensityThreshold, mzTolerance, massListName);
                
                //Report the final score to the peaklist identity
                if (searchResult != null 
                        && searchResult.getScore() > scoreThreshold 
                        && searchResult.getNumIonsMatched() >= minimumIonsMatched)
                    addFragmentClusterIdentity(rows1[i],featureA,featureB,searchResult);
              
                if (isCanceled())
                    return;
            }
            
        //Update progress bar
        finishedRows++;
        }

        // Add task description to peakList
        ((SimplePeakList) peakList1).addDescriptionOfAppliedTask(
                new SimplePeakListAppliedMethod("Identification of similar MS2s",
                        parameters));

        // Repaint the window to reflect the change in the peak list
        Desktop desktop = MZmineCore.getDesktop();
        if (!(desktop instanceof HeadLessDesktop))
            desktop.getMainWindow().repaint();

        setStatus(TaskStatus.FINISHED);

        logger.info("Finished MS2 similarity search for " + peakList1 + "against"
                + peakList2);

    }
    
    private Ms2SearchResult simpleMS2similarity(Feature featureA, Feature featureB,
            double intensityThreshold, MZTolerance mzRange, String massList) {

        double runningScoreTotal = 0.0;
        double mzRangePPM = mzRange.getPpmTolerance();
        
        List<DataPoint> matchedIons = new ArrayList<DataPoint>();

        // Fetch 1st feature MS2 scan.
        int ms2ScanNumberA = featureA.getMostIntenseFragmentScanNumber();
        Scan scanMS2A = featureA.getDataFile().getScan(ms2ScanNumberA);
        //RawDataFile featureADataFile = featureA.getDataFile();

        // Fetch 2nd feature MS2 scan.
        int ms2ScanNumberB = featureB.getMostIntenseFragmentScanNumber();
        Scan scanMS2B = featureB.getDataFile().getScan(ms2ScanNumberB);
        //RawDataFile peak2DataFile = featureB.getDataFile();
        
        if (scanMS2A == null || scanMS2B == null)
        {
            return null;
        }

        //Fetch centroided data
        MassList massListA = scanMS2A.getMassList(massListName);
        MassList massListB = scanMS2B.getMassList(massListName);
        
        if (massListA == null)
        {
            //Will this work properly? As this function isn't directly the task?
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Scan " + scanMS2A.getDataFile().getName() + " #" + scanMS2A.getScanNumber()
                    + " does not have a mass list " + massListName);
            return null;        
        }
        
        if (massListB == null)
        {
            //Will this work properly? As this function isn't directly the task?
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Scan " + scanMS2B.getDataFile().getName() + " #" + scanMS2B.getScanNumber()
                    + " does not have a mass list " + massListName);
            return null;        
        }
     
        DataPoint[] ionsA = null;
        DataPoint[] ionsB = null;
     
        ionsA = massListA.getDataPoints();
        ionsB = massListB.getDataPoints();
        
        if (ionsA == null || ionsB == null || ionsA.length == 0 || ionsB.length == 0)
        {
            //Fall back to profile data?
             //Profile / raw data.
            //ionsA = scanMS2A.getDataPointsOverIntensity(intensityThreshold);
            //ionsB = scanMS2B.getDataPointsOverIntensity(intensityThreshold);
         return null;   
        }
                
        // Compare every ion peak in MS2 scan A, to every ion peak in MS2 scan B.
        double ionsBMaxMZ = ionsB[ionsB.length - 1].getMZ();
        for (int i = 0; i < ionsA.length; i++) {
            
            double iMZ = ionsA[i].getMZ();
            double mzRangeAbsolute = iMZ * 1e-6 * mzRangePPM;
            
            if (iMZ - mzRangeAbsolute > ionsBMaxMZ)
                break; //Potential speedup heuristic. If any i is greater than the max of j, no more matches are possible.
            
            for (int j = 0; j < ionsB.length; j++) {
               
                double jMZ = ionsB[j].getMZ();
                
                if (iMZ < jMZ - mzRangeAbsolute)
                    break; //Potential speedup heuristic. iMZ smaller than jMZ.  Skip the rest of the j's as they can only increase.
                    
                if (Math.abs(iMZ - jMZ) < mzRangeAbsolute) {
                    runningScoreTotal += ionsA[i].getIntensity()
                            * ionsB[j].getIntensity();
                    matchedIons.add(ionsA[i]);
                }

            }
        }
        Ms2SearchResult result = new Ms2SearchResult(runningScoreTotal,"simple",matchedIons);
        return result;
    }

    /**
     * Add new identity based on fragmentation similarity to the row
     * 
     * @param mainRow
     * @param fragmentRow
     */
    private void addFragmentClusterIdentity(PeakListRow row1, Feature peakA,
            Feature peakB, Ms2SearchResult searchResult) {
        Ms2Identity newIdentity = new Ms2Identity(peakA, peakB, searchResult);
        row1.addPeakIdentity(newIdentity, false);

        // Notify the GUI about the change in the project
        MZmineCore.getProjectManager().getCurrentProject()
                .notifyObjectChanged(row1, false);
    }
}
    