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
    private double scoreThreshold;
    private double intensityThreshold;

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
        return "MS2 similarity comparison between " + peakList1 + "and"
                + peakList2;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        setStatus(TaskStatus.PROCESSING);

        logger.info("Starting MS2 similarity search between " + peakList1 + "and"
                + peakList2 + "with mz tolerance:"+mzTolerance.getPpmTolerance());

        double score;
        int numIonsMatched;
        PeakListRow rows1[] = peakList1.getRows();
        PeakListRow rows2[] = peakList2.getRows();
        
        int rows1Length = rows1.length;
        int rows2Length = rows2.length;
        
        totalRows = rows1Length;
        
        for (int i = 0; i < rows1Length; i++) {
            for (int j = 0; j < rows2Length; j++) {
                Feature featureA = rows1[i].getBestPeak();
                Feature featureB = rows2[j].getBestPeak();
                
                int featureAID = featureA.hashCode();
                int featureBID = featureB.hashCode();
                
                score = simpleMS2similarity(featureA,
                        featureB, intensityThreshold, mzTolerance.getPpmTolerance());
                
                if (score > scoreThreshold)
                    addFragmentClusterIdentity(rows1[i],featureA,featureB,score);
                
                //featureA.appendFragmentSimilarityScore(featureBID,result);
              
                if (isCanceled())
                    return;
            }
        finishedRows++;
        }

        // Add task description to peakList
        ((SimplePeakList) peakList1).addDescriptionOfAppliedTask(
                new SimplePeakListAppliedMethod("Identification of complexes",
                        parameters));

        // Repaint the window to reflect the change in the peak list
        Desktop desktop = MZmineCore.getDesktop();
        if (!(desktop instanceof HeadLessDesktop))
            desktop.getMainWindow().repaint();

        setStatus(TaskStatus.FINISHED);

        logger.info("Finished MS2 similarity search for " + peakList1 + "against"
                + peakList2);

    }

    /**
     * Simple cosine like calculation of similarity between ions of two fragmentation scans.
     * 
     */
    private double simpleMS2similarity(Feature featureA, Feature featureB,
            double intensityThreshold, double mzRangePPM) {
        // This could probably return some sort of key:value dictionary (say
        // scanHash:similarity) for the similarity scores,
        // instead of the current double[] return
        double runningScoreTotal = 0.0;

        // Fetch 1st peak MS2 scan.
        int ms2ScanNumberA = featureA.getMostIntenseFragmentScanNumber();
        Scan scanMS2A = featureA.getDataFile().getScan(ms2ScanNumberA);
        RawDataFile featureADataFile = featureA.getDataFile();

        // Fetch 2nd peak MS2 scan.
        int ms2ScanNumberB = featureB.getMostIntenseFragmentScanNumber();
        Scan scanMS2B = featureB.getDataFile().getScan(ms2ScanNumberB);
        RawDataFile peak2DataFile = featureB.getDataFile();
        
        if (scanMS2A == null || scanMS2B == null)
        {
            return 0.0;
        }

        DataPoint[] ionsA = null;
        DataPoint[] ionsB = null;

        ionsA = scanMS2A.getDataPointsOverIntensity(intensityThreshold);
        ionsB = scanMS2B.getDataPointsOverIntensity(intensityThreshold);

        int numIonsMatched = 0;
        // Compare every ion peak in MS2 scan A, to MS2 scan B.
        for (int i = 0; i < ionsA.length; i++) {
            for (int j = 0; j < ionsB.length; j++) {
                if (Math.abs(ionsA[i].getMZ() - ionsB[j].getMZ()) < ionsA[i]
                        .getMZ() * 1e-6 * mzRangePPM) {
                    runningScoreTotal += ionsA[i].getIntensity()
                            * ionsB[j].getIntensity();
                    numIonsMatched++;
                }

            }
        }

        return runningScoreTotal;

    }

    /**
     * Add new identity based on fragmentation similarity to the row
     * 
     * @param mainRow
     * @param fragmentRow
     */
    private void addFragmentClusterIdentity(PeakListRow complexRow, Feature peakA,
            Feature peakB, double score) {
        Ms2Identity newIdentity = new Ms2Identity(peakA, peakB, score);
        complexRow.addPeakIdentity(newIdentity, false);

        // Notify the GUI about the change in the project
        MZmineCore.getProjectManager().getCurrentProject()
                .notifyObjectChanged(complexRow, false);
    }
}
    