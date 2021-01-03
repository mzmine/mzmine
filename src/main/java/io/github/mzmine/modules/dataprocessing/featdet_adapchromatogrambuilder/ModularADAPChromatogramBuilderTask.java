/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 *
 * Edited and modified by Owen Myers (Oweenm@gmail.com)
 */

package io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder;


import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ADAPChromatogramSorter;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.FeatureConvertors;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;


public class ModularADAPChromatogramBuilderTask extends AbstractTask {

  RangeSet<Double> rangeSet = TreeRangeSet.create();
  // After each range is created it does not change so we can map the ranges (which will be uniqe)
  // to the chromatograms
  HashMap<Range, ADAPChromatogram> rangeToChromMap = new HashMap<Range, ADAPChromatogram>();

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private MZmineProject project;
  private RawDataFile dataFile;

  // scan counter
  // private int processedPoints = 0, totalPoints;
  private double progress = 0.0;
  private ScanSelection scanSelection;
  private int newFeatureID = 1;
  private Scan[] scans;

  // User parameters
  private String suffix, massListName;
  private MZTolerance mzTolerance;
  private double minimumHeight;
  private int minimumScanSpan;
  // Owen added User parameers;
  private double IntensityThresh2;
  private double minIntensityForStartChrom;

  private ModularFeatureList newFeatureList;

  /**
   * @param dataFile
   * @param parameters
   */
  public ModularADAPChromatogramBuilderTask(MZmineProject project, RawDataFile dataFile,
      ParameterSet parameters) {

    this.project = project;
    this.dataFile = dataFile;
    this.scanSelection =
        parameters.getParameter(ADAPChromatogramBuilderParameters.scanSelection).getValue();
    this.massListName =
        parameters.getParameter(ADAPChromatogramBuilderParameters.massList).getValue();

    this.mzTolerance =
        parameters.getParameter(ADAPChromatogramBuilderParameters.mzTolerance).getValue();
    this.minimumScanSpan =
        parameters.getParameter(ADAPChromatogramBuilderParameters.minimumScanSpan).getValue();
    // this.minimumHeight = parameters
    // .getParameter(ChromatogramBuilderParameters.minimumHeight)
    // .getValue();

    this.suffix = parameters.getParameter(ADAPChromatogramBuilderParameters.suffix).getValue();

    // Owen added parameters
    this.IntensityThresh2 =
        parameters.getParameter(ADAPChromatogramBuilderParameters.IntensityThresh2).getValue();
    this.minIntensityForStartChrom =
        parameters.getParameter(ADAPChromatogramBuilderParameters.startIntensity).getValue();
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Detecting chromatograms in " + dataFile;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {
    boolean writeChromCDF = true;

    setStatus(TaskStatus.PROCESSING);

    logger.info("Started chromatogram builder on " + dataFile);

    scans = scanSelection.getMatchingScans(dataFile);
    List<Float> rtListForChromCDF = new ArrayList<Float>();

    // Check if the scans are properly ordered by RT
    double prevRT = Double.NEGATIVE_INFINITY;
    for (Scan s : scans) {
      if (isCanceled()) {
        return;
      }

      if (writeChromCDF) {
        rtListForChromCDF.add(s.getRetentionTime());
      }

      if (s.getRetentionTime() < prevRT) {
        setStatus(TaskStatus.ERROR);
        final String msg = "Retention time of scan #" + s.getScanNumber()
            + " is smaller then the retention time of the previous scan."
            + " Please make sure you only use scans with increasing retention times."
            + " You can restrict the scan numbers in the parameters, or you can use the Crop filter module";
        setErrorMessage(msg);
        return;
      }
      prevRT = s.getRetentionTime();
    }

    // Check if the scans are MS1-only or MS2-only.
    int minMsLevel = Arrays.stream(scans).mapToInt(Scan::getMSLevel).min()
        .orElseThrow(() -> new IllegalStateException("Cannot find the minimum MS level"));

    int maxMsLevel = Arrays.stream(scans).mapToInt(Scan::getMSLevel).max()
        .orElseThrow(() -> new IllegalStateException("Cannot find the maximum MS level"));

    if (minMsLevel != maxMsLevel) {
      MZmineCore.getDesktop().displayMessage(null,
          "MZmine thinks that you are running ADAP Chromatogram builder on both MS1- and MS2-scans. "
              + "This will likely produce wrong results. "
              + "Please, set the scan filter parameter to a specific MS level");
    }

    // make a list of all the data points
    // sort data points by intensity
    // loop through list
    // add data point to chromatogrm or make new one
    // update mz avg and other stuff
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
        setErrorMessage("Mass list " + massListName + " does not contain m/z values for scan #"
            + scan.getScanNumber() + " of file " + dataFile);
        return;
      }

      for (DataPoint mzFeature : mzValues) {
        ExpandedDataPoint curDatP = new ExpandedDataPoint(mzFeature, scan);
        allMzValues.add(curDatP);
        // corespondingScanNum.add(scan.getScanNumber());
      }
    }

    // Integer[] simpleCorespondingScanNums = new Integer[corespondingScanNum.size()];
    // corespondingScanNum.toArray(simpleCorespondingScanNums );

    ExpandedDataPoint[] simpleAllMzVals = new ExpandedDataPoint[allMzValues.size()];
    allMzValues.toArray(simpleAllMzVals);

    // sort data points by intensity
    Arrays.sort(simpleAllMzVals,
        new DataPointSorter(SortingProperty.Intensity, SortingDirection.Descending));

    // Set<Chromatogram> buildingChromatograms;
    // buildingChromatograms = new LinkedHashSet<Chromatogram>();

    double maxIntensity = simpleAllMzVals[0].getIntensity();

    // count starts at 1 since we already have added one with a single point.

    // Stopwatch stopwatch = Stopwatch.createUnstarted();
    // stopwatch2 = Stopwatch.createUnstarted();
    // Stopwatch stopwatch3 = Stopwatch.createUnstarted();


    progress = 0.0;
    double progressStep = (simpleAllMzVals.length > 0) ? 0.5 / simpleAllMzVals.length : 0.0;

    for (ExpandedDataPoint mzFeature : simpleAllMzVals) {

      progress += progressStep;

      if (isCanceled()) {
        return;
      }

      if (mzFeature == null || Double.isNaN(mzFeature.getMZ()) || Double.isNaN(mzFeature.getIntensity())) {
        continue;
      }

      //////////////////////////////////////////////////

      Range<Double> containsPointRange = rangeSet.rangeContaining(mzFeature.getMZ());

      Range<Double> toleranceRange = mzTolerance.getToleranceRange(mzFeature.getMZ());
      if (containsPointRange == null) {
        // skip it entierly if the intensity is not high enough
        if (mzFeature.getIntensity() < minIntensityForStartChrom) {
          continue;
        }
        // look +- mz tolerance to see if ther is a range near by.
        // If there is use the proper boundry of that range for the
        // new range to insure than NON OF THE RANGES OVERLAP.
        Range<Double> plusRange = rangeSet.rangeContaining(toleranceRange.upperEndpoint());
        Range<Double> minusRange = rangeSet.rangeContaining(toleranceRange.lowerEndpoint());
        Double toBeLowerBound;
        Double toBeUpperBound;

        double cur_max_testing_mz = mzFeature.getMZ();

        // If both of the above ranges are null then we make the new range spaning the full
        // mz tolerance range.
        // If one or both are not null we need to properly modify the range of the new
        // chromatogram so that none of the points are overlapping.
        if ((plusRange == null) && (minusRange == null)) {
          toBeLowerBound = toleranceRange.lowerEndpoint();
          toBeUpperBound = toleranceRange.upperEndpoint();
        } else if ((plusRange == null) && (minusRange != null)) {
          // the upper end point of the minus range will be the lower
          // range of the new one
          toBeLowerBound = minusRange.upperEndpoint();
          toBeUpperBound = toleranceRange.upperEndpoint();

        } else if ((minusRange == null) && (plusRange != null)) {
          toBeLowerBound = toleranceRange.lowerEndpoint();
          toBeUpperBound = plusRange.lowerEndpoint();
          // double tmp_this = plusRange.upperEndpoint();
          // System.out.println("tmp_this");
        } else if ((minusRange != null) && (plusRange != null)) {
          toBeLowerBound = minusRange.upperEndpoint();
          toBeUpperBound = plusRange.lowerEndpoint();
        } else {
          toBeLowerBound = 0.0;
          toBeUpperBound = 0.0;
        }

        if (toBeLowerBound < toBeUpperBound) {
          Range<Double> newRange = Range.open(toBeLowerBound, toBeUpperBound);
          ADAPChromatogram newChrom = new ADAPChromatogram(dataFile, scans);

          newChrom.addMzFeature(mzFeature.getScan(), mzFeature);

          newChrom.setHighPointMZ(mzFeature.getMZ());


          rangeToChromMap.put(newRange, newChrom);
          // also need to put it in the set -> this is where the range can be efficiently found.

          rangeSet.add(newRange);
        } else if (toBeLowerBound.equals(toBeUpperBound) && plusRange != null) {
          ADAPChromatogram curChrom = rangeToChromMap.get(plusRange);
          curChrom.addMzFeature(mzFeature.getScan(), mzFeature);
        } else
          throw new IllegalStateException(String.format("Incorrect range [%f, %f] for m/z %f",
              toBeLowerBound, toBeUpperBound, mzFeature.getMZ()));

      } else {
        // In this case we do not need to update the rangeSet

        ADAPChromatogram curChrom = rangeToChromMap.get(containsPointRange);

        curChrom.addMzFeature(mzFeature.getScan(), mzFeature);

        // update the entry in the map
        rangeToChromMap.put(containsPointRange, curChrom);
      }
    }

    // System.out.println("search chroms (ms): " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
    // System.out.println("making new chrom (ms): " + stopwatch2.elapsed(TimeUnit.MILLISECONDS));

    // finish chromatograms
    Set<Range<Double>> ranges = rangeSet.asRanges();
    Iterator<Range<Double>> RangeIterator = ranges.iterator();

    List<ADAPChromatogram> buildingChromatograms = new ArrayList<ADAPChromatogram>();

    progressStep = (ranges.size() > 0) ? 0.5 / ranges.size() : 0.0;
    while (RangeIterator.hasNext()) {
      if (isCanceled()) {
        return;
      }

      progress += progressStep;

      Range<Double> curRangeKey = RangeIterator.next();

      ADAPChromatogram chromatogram = rangeToChromMap.get(curRangeKey);

      chromatogram.finishChromatogram();

      // And remove chromatograms who dont have a certian number of continous points above the
      // IntensityThresh2 level.
      double numberOfContinuousPointsAboveNoise =
          chromatogram.findNumberOfContinuousPointsAboveNoise(IntensityThresh2);
      if (numberOfContinuousPointsAboveNoise < minimumScanSpan) {
        // System.out.println("skipping chromatogram because it does not meet the min point scan
        // requirements");
        continue;
      } else {
        buildingChromatograms.add(chromatogram);
      }

    }

    ADAPChromatogram[] chromatograms = buildingChromatograms.toArray(new ADAPChromatogram[0]);

    // Sort the final chromatograms by m/z
    Arrays.sort(chromatograms, new ADAPChromatogramSorter(SortingProperty.MZ, SortingDirection.Ascending));

    // Create new feature list
    newFeatureList = new ModularFeatureList(dataFile + " " + suffix, dataFile);
    // ensure that the default columns are available
    DataTypeUtils.addDefaultChromatographicTypeColumns(newFeatureList);

    // Add the chromatograms to the new feature list
    for (ADAPChromatogram finishedFeature : chromatograms) {
      finishedFeature.setFeatureList(newFeatureList);
      ModularFeature modular = FeatureConvertors.ADAPChromatogramToModularFeature(finishedFeature);
      ModularFeatureListRow newRow =
          new ModularFeatureListRow(newFeatureList, newFeatureID, dataFile, modular);
      newFeatureList.addRow(newRow);
      newFeatureID++;
    }

    // Add new feature list to the project
    project.addFeatureList(newFeatureList);

    progress = 1.0;

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished chromatogram builder on " + dataFile);
  }

}


