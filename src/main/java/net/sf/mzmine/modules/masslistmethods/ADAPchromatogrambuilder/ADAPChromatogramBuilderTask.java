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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Edited and modified by Owen Myers (Oweenm@gmail.com)
 */

package net.sf.mzmine.modules.masslistmethods.ADAPchromatogrambuilder;
import com.google.common.collect.TreeRangeSet;
import com.google.common.collect.RangeSet;


import java.util.Arrays;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

import net.sf.mzmine.util.DataPointSorter;
import com.google.common.collect.Range;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.lang.*;



public class ADAPChromatogramBuilderTask extends AbstractTask {

    RangeSet<Double> rangeSet = TreeRangeSet.create();
    // After each range is created it does not change so we can map the ranges (which will be uniqe)
    // to the chromatograms
    HashMap<Range,ADAPChromatogram> rangeToChromMap = new HashMap<Range,ADAPChromatogram>();

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private MZmineProject project;
    private RawDataFile dataFile;

    // scan counter
    private int processedPoints = 0, totalPoints;
    private ScanSelection scanSelection;
    private int newPeakID = 1;
    private Scan[] scans;

    // User parameters
    private String suffix, massListName;
    private MZTolerance mzTolerance;
    private double minimumHeight;
    private int minimumScanSpan;
    // Owen added User parameers;
    private double IntensityThresh2;
    private double minIntensityForStartChrom;

    private SimplePeakList newPeakList;


    /**
     * @param dataFile
     * @param parameters
     */
    public ADAPChromatogramBuilderTask(MZmineProject project, RawDataFile dataFile,
            ParameterSet parameters) {



        this.project = project;
        this.dataFile = dataFile;
        this.scanSelection = parameters
                .getParameter(ADAPChromatogramBuilderParameters.scanSelection)
                .getValue();
        this.massListName = parameters
                .getParameter(ADAPChromatogramBuilderParameters.massList)
                .getValue();

        this.mzTolerance = parameters
                .getParameter(ADAPChromatogramBuilderParameters.mzTolerance)
                .getValue();
        this.minimumScanSpan = parameters
                .getParameter(ADAPChromatogramBuilderParameters.minimumScanSpan)
                .getValue().intValue();
        //this.minimumHeight = parameters
        //        .getParameter(ChromatogramBuilderParameters.minimumHeight)
        //        .getValue();

        this.suffix = parameters
                .getParameter(ADAPChromatogramBuilderParameters.suffix).getValue();

        // Owen added parameters
        this.IntensityThresh2 = parameters
                .getParameter(ADAPChromatogramBuilderParameters.IntensityThresh2)
                .getValue();
        this.minIntensityForStartChrom = parameters
                .getParameter(ADAPChromatogramBuilderParameters.startIntensity)
                .getValue();


    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Detecting chromatograms in " + dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalPoints == 0)
            return 0;
        else
            return (double) processedPoints/ totalPoints;
    }

    public RawDataFile getDataFile() {
        return dataFile;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {
        boolean writeChromCDF = true;

        setStatus(TaskStatus.PROCESSING);

        logger.info("Started chromatogram builder on " + dataFile);

        scans = scanSelection.getMatchingScans(dataFile);
        int allScanNumbers[] = scanSelection.getMatchingScanNumbers(dataFile);

        List<Double> rtListForChromCDF = new ArrayList<Double>();

        // Check if the scans are properly ordered by RT
        double prevRT = Double.NEGATIVE_INFINITY;
        for (Scan s : scans) {
            if (isCanceled()){
                return;}

            if (writeChromCDF){
                rtListForChromCDF.add(s.getRetentionTime());
            }

            if (s.getRetentionTime() < prevRT) {
                setStatus(TaskStatus.ERROR);
                final String msg = "Retention time of scan #"
                        + s.getScanNumber()
                        + " is smaller then the retention time of the previous scan."
                        + " Please make sure you only use scans with increasing retention times."
                        + " You can restrict the scan numbers in the parameters, or you can use the Crop filter module";
                setErrorMessage(msg);
                return;
            }
            prevRT = s.getRetentionTime();
        }


        // Create new peak list
        newPeakList = new SimplePeakList(dataFile + " " + suffix, dataFile);

        // make a list of all the data points
        // sort data points by intensity
        // loop through list 
        //      add data point to chromatogrm or make new one
        //      update mz avg and other stuff
        // 

        
        // make a list of all the data points
        List<ExpandedDataPoint> allMzValues = new ArrayList<ExpandedDataPoint>();

        for (Scan scan : scans) {
            if (isCanceled())
                return;

            MassList massList = scan.getMassList(massListName);
            if (massList == null) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Scan " + dataFile + " #" + scan.getScanNumber()
                        + " does not have a mass list " + massListName);
                return;
            }

            DataPoint mzValues[] = massList.getDataPoints();

            if (mzValues == null) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Mass list " + massListName
                        + " does not contain m/z values for scan #"
                        + scan.getScanNumber() + " of file " + dataFile);
                return;
            }

            for (DataPoint mzPeak : mzValues){
                ExpandedDataPoint curDatP = new ExpandedDataPoint(mzPeak, scan.getScanNumber());
                allMzValues.add(curDatP );
                //corespondingScanNum.add(scan.getScanNumber());
            }

        }

        //Integer[] simpleCorespondingScanNums = new Integer[corespondingScanNum.size()];
        //corespondingScanNum.toArray(simpleCorespondingScanNums );

        ExpandedDataPoint[] simpleAllMzVals = new ExpandedDataPoint[allMzValues.size()];
        allMzValues.toArray(simpleAllMzVals);

        // sort data points by intensity
        Arrays.sort(simpleAllMzVals, new DataPointSorter(SortingProperty.Intensity,
                SortingDirection.Descending));


        //Set<Chromatogram> buildingChromatograms;
        //buildingChromatograms = new LinkedHashSet<Chromatogram>();



        double maxIntensity = simpleAllMzVals[0].getIntensity();

        // count starts at 1 since we already have added one with a single point.

        //Stopwatch stopwatch = Stopwatch.createUnstarted(); 
        // stopwatch2 = Stopwatch.createUnstarted(); 
        //Stopwatch stopwatch3 = Stopwatch.createUnstarted(); 


        processedPoints = 0;
        totalPoints = simpleAllMzVals.length;

        for (ExpandedDataPoint mzPeak : simpleAllMzVals){

            processedPoints++;

            if (isCanceled()){
                return;}

            if (mzPeak==null){
                //System.out.println("null Peak");
                continue;
            }

            //////////////////////////////////////////////////
            
            Range<Double> containsPointRange = rangeSet.rangeContaining(mzPeak.getMZ());
            
            Range<Double> toleranceRange =  mzTolerance.getToleranceRange(mzPeak.getMZ());
            if (containsPointRange==null){
                // skip it entierly if the intensity is not high enough
                if (mzPeak.getIntensity()<minIntensityForStartChrom) {
                    continue;
                }
                // look +- mz tolerance to see if ther is a range near by. 
                // If there is use the proper boundry of that range for the 
                // new range to insure than NON OF THE RANGES OVERLAP.
                Range<Double> plusRange = rangeSet.rangeContaining(toleranceRange.upperEndpoint());
                Range<Double> minusRange = rangeSet.rangeContaining(toleranceRange.lowerEndpoint());
                Double toBeLowerBound;
                Double toBeUpperBound;
                
                double cur_max_testing_mz = mzPeak.getMZ();
                
                
                // If both of the above ranges are null then we make the new range spaning the full
                // mz tolerance range. 
                // If one or both are not null we need to properly modify the range of the new
                // chromatogram so that none of the points are overlapping.
                if ((plusRange==null)&&(minusRange==null)){
                    toBeLowerBound = toleranceRange.lowerEndpoint();
                    toBeUpperBound = toleranceRange.upperEndpoint();
                }
                else if ((plusRange==null)&&(minusRange!=null)){
                    // the upper end point of the minus range will be the lower 
                    // range of the new one
                    toBeLowerBound = minusRange.upperEndpoint();
                    toBeUpperBound = toleranceRange.upperEndpoint();
            
                }
                else if ((minusRange==null)&&(plusRange!=null)){
                    toBeLowerBound = toleranceRange.lowerEndpoint();
                    toBeUpperBound = plusRange.lowerEndpoint(); 
//                    double tmp_this = plusRange.upperEndpoint();
//                    System.out.println("tmp_this");
                }
                else if ((minusRange!=null)&&(plusRange!=null)){
                    toBeLowerBound = minusRange.upperEndpoint();
                    toBeUpperBound = plusRange.lowerEndpoint(); 
                }
                else{
                    toBeLowerBound = 0.0;
                    toBeUpperBound = 0.0; 
                }
                Range<Double> newRange = Range.open(toBeLowerBound,toBeUpperBound);
                ADAPChromatogram newChrom = new ADAPChromatogram(dataFile, allScanNumbers);

                newChrom.addMzPeak(mzPeak.getScanNumber(),mzPeak);

                newChrom.setHighPointMZ(mzPeak.getMZ());
                
                

                rangeToChromMap.put(newRange,newChrom);
                // also need to put it in the set -> this is where the range can be efficiently found.
                
                rangeSet.add(newRange);
            }
            else{
                // In this case we do not need to update the rangeSet
                
                
                ADAPChromatogram curChrom = rangeToChromMap.get(containsPointRange);
                
                curChrom.addMzPeak(mzPeak.getScanNumber(),mzPeak);
                
                //update the entry in the map
                rangeToChromMap.put(containsPointRange,curChrom);
                
                
            }
        }
           
        //System.out.println("search chroms (ms): " +  stopwatch.elapsed(TimeUnit.MILLISECONDS));
        //System.out.println("making new chrom (ms): " +  stopwatch2.elapsed(TimeUnit.MILLISECONDS));

        // finish chromatograms
        Iterator<Range<Double>> RangeIterator = rangeSet.asRanges().iterator();

        List<ADAPChromatogram> buildingChromatograms = new ArrayList<ADAPChromatogram>();

        while (RangeIterator.hasNext()) {
            if (isCanceled()){
                return;}

            Range<Double> curRangeKey = RangeIterator.next();

            ADAPChromatogram chromatogram = rangeToChromMap.get(curRangeKey);

            chromatogram.finishChromatogram();

            // And remove chromatograms who dont have a certian number of continous points above the
            // IntensityThresh2 level.
            double numberOfContinuousPointsAboveNoise = chromatogram.findNumberOfContinuousPointsAboveNoise(IntensityThresh2);
            if (numberOfContinuousPointsAboveNoise < minimumScanSpan) {
                //System.out.println("skipping chromatogram because it does not meet the min point scan requirements");
                continue;
            }
            else{
                buildingChromatograms.add(chromatogram);
            }

        }

        ADAPChromatogram[] chromatograms = buildingChromatograms.toArray(new ADAPChromatogram[0]);
       

        // Sort the final chromatograms by m/z
        Arrays.sort(chromatograms,
                new PeakSorter(SortingProperty.MZ, SortingDirection.Ascending));


        // Add the chromatograms to the new peak list
        for (Feature finishedPeak : chromatograms) {
            SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
            newPeakID++;
            newRow.addPeak(dataFile, finishedPeak);
            newPeakList.addRow(newRow);

//            finishedPeak.outputChromToFile();
        }

        // Add new peaklist to the project
        project.addPeakList(newPeakList);

        // Add quality parameters to peaks
        QualityParameters.calculateQualityParameters(newPeakList);

        setStatus(TaskStatus.FINISHED);

        logger.info("Finished chromatogram builder on " + dataFile);
    }

}
