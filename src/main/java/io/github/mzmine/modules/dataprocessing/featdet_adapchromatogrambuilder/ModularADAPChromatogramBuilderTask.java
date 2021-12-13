/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder;


import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureShapeType;
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
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ModularADAPChromatogramBuilderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      ModularADAPChromatogramBuilderTask.class.getName());
  private final MZmineProject project;
  private final RawDataFile dataFile;
  // User parameters
  private final String suffix;
  private final MZTolerance mzTolerance;
  private final int minimumScanSpan;
  // Owen added User parameers;
  private final double IntensityThresh2;
  private final double minIntensityForStartChrom;
  private RangeSet<Double> rangeSet = TreeRangeSet.create();
  // After each range is created it does not change so we can map the ranges (which will be uniqe)
  // to the chromatograms
  private HashMap<Range, ADAPChromatogram> rangeToChromMap = new HashMap<>();
  private double progress = 0.0;
  private ScanSelection scanSelection;
  private int newFeatureID = 1;
  private Scan[] scans;
  private double minimumHeight;
  private ModularFeatureList newFeatureList;
  private ParameterSet parameters;

  /**
   * @param dataFile
   * @param parameters
   */
  public ModularADAPChromatogramBuilderTask(MZmineProject project, RawDataFile dataFile,
      ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.project = project;
    this.dataFile = dataFile;
    this.scanSelection = parameters.getParameter(ADAPChromatogramBuilderParameters.scanSelection)
        .getValue();

    this.mzTolerance = parameters.getParameter(ADAPChromatogramBuilderParameters.mzTolerance)
        .getValue();
    this.minimumScanSpan = parameters.getParameter(
        ADAPChromatogramBuilderParameters.minimumScanSpan).getValue();
    // this.minimumHeight = parameters
    // .getParameter(ChromatogramBuilderParameters.minimumHeight)
    // .getValue();

    this.suffix = parameters.getParameter(ADAPChromatogramBuilderParameters.suffix).getValue();

    // Owen added parameters
    this.IntensityThresh2 = parameters.getParameter(
        ADAPChromatogramBuilderParameters.IntensityThresh2).getValue();
    this.minIntensityForStartChrom = parameters.getParameter(
        ADAPChromatogramBuilderParameters.startIntensity).getValue();
    this.parameters = parameters;
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

    setStatus(TaskStatus.PROCESSING);
    logger.info(() -> "Started chromatogram builder on " + dataFile);

    try {

      scans = scanSelection.getMatchingScans(dataFile);
      if (scans.length == 0) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("There are no scans satisfying filtering values. Consider updating filters "
                        + "with \"Set filters\" in the \"Scans\" parameter.");
        return;
      }

      // Check if the scans are properly ordered by RT
      double prevRT = Double.NEGATIVE_INFINITY;
      int msLevel = scans[0].getMSLevel();
      boolean sameMSLevels = true;
      for (Scan s : scans) {
        if (isCanceled()) {
          return;
        }

        if (msLevel != s.getMSLevel()) {
          sameMSLevels = false;
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

      if (!sameMSLevels) {
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
      List<ExpandedDataPoint> allMzValues = new ArrayList<>();

      ScanDataAccess scanData = EfficientDataAccess.of(dataFile, ScanDataType.CENTROID, scans);

      while (scanData.hasNextScan()) {
        if (isCanceled()) {
          return;
        }

        Scan scan = null;
        try {
          scan = scanData.nextScan();
        } catch (MissingMassListException e) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage(
              "Scan #" + scanData.getCurrentScan().getScanNumber() + " from " + dataFile.getName()
              + " does not have a mass list. Pleas run \"Raw data methods\" -> \"Mass detection\".");
          e.printStackTrace();
          return;
        }

        int dps = scanData.getNumberOfDataPoints();
        for (int i = 0; i < dps; i++) {
          ExpandedDataPoint curDatP = new ExpandedDataPoint(scanData.getMzValue(i),
              scanData.getIntensityValue(i), scan);
          allMzValues.add(curDatP);
        }
      }

      // sort data points by intensity
      allMzValues.sort(new DataPointSorter(SortingProperty.Intensity, SortingDirection.Descending));

      // Set<Chromatogram> buildingChromatograms;
      // buildingChromatograms = new LinkedHashSet<Chromatogram>();

      double maxIntensity = allMzValues.get(0).getIntensity();

      // count starts at 1 since we already have added one with a single point.

      // Stopwatch stopwatch = Stopwatch.createUnstarted();
      // stopwatch2 = Stopwatch.createUnstarted();
      // Stopwatch stopwatch3 = Stopwatch.createUnstarted();

      progress = 0.0;
      double progressStep = (allMzValues.size() > 0) ? 0.5 / allMzValues.size() : 0.0;

      for (ExpandedDataPoint mzFeature : allMzValues) {

        progress += progressStep;

        if (isCanceled()) {
          return;
        }

        if (mzFeature == null || Double.isNaN(mzFeature.getMZ()) || Double.isNaN(
            mzFeature.getIntensity())) {
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
          } else {
            throw new IllegalStateException(
                String.format("Incorrect range [%f, %f] for m/z %f", toBeLowerBound, toBeUpperBound,
                    mzFeature.getMZ()));
          }

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
      rangeSet = null; // free
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
        double numberOfContinuousPointsAboveNoise = chromatogram.findNumberOfContinuousPointsAboveNoise(
            IntensityThresh2);
        if (numberOfContinuousPointsAboveNoise < minimumScanSpan) {
          // System.out.println("skipping chromatogram because it does not meet the min point scan
          // requirements");
          continue;
        } else {
          buildingChromatograms.add(chromatogram);
        }

      }
      rangeToChromMap = null; // free

      buildingChromatograms.forEach(c -> c.addNZeros(1, 1));
      // Sort the final chromatograms by m/z
      buildingChromatograms.sort(
          new ADAPChromatogramSorter(SortingProperty.MZ, SortingDirection.Ascending));

      // Create new feature list
      newFeatureList = new ModularFeatureList(dataFile + " " + suffix, getMemoryMapStorage(),
          dataFile);
      // ensure that the default columns are available
      DataTypeUtils.addDefaultChromatographicTypeColumns(newFeatureList);

      // Add the chromatograms to the new feature list
      for (ADAPChromatogram finishedFeature : buildingChromatograms) {
        finishedFeature.setFeatureList(newFeatureList);
        ModularFeature modular = FeatureConvertors.ADAPChromatogramToModularFeature(
            finishedFeature);
        ModularFeatureListRow newRow = new ModularFeatureListRow(newFeatureList, newFeatureID,
            modular);
        newFeatureList.addRow(newRow);
        // activate shape for this row
        newRow.set(FeatureShapeType.class, true);
        newFeatureID++;
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Error in ADAP chromatogram builder: " + ex.getMessage(), ex);
      setErrorMessage("Error during chromatogram building " + ex.getMessage());
      setStatus(TaskStatus.ERROR);
      return;
    }

    newFeatureList.setSelectedScans(dataFile, Arrays.asList(scans));

    dataFile.getAppliedMethods().forEach(m -> newFeatureList.getAppliedMethods().add(m));
    // Add new feature list to the project
    newFeatureList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(ModularADAPChromatogramBuilderModule.class, parameters,
            getModuleCallDate()));
    project.addFeatureList(newFeatureList);

    progress = 1.0;

    clearObjects();
    setStatus(TaskStatus.FINISHED);

    logger.info(() -> "Finished chromatogram builder on " + dataFile);
  }

  private void clearObjects() {
    rangeSet = null;
    rangeToChromMap = null;

    scanSelection = null;
    scans = null;

    // User parameters
    newFeatureList = null;
    parameters = null;
  }

}

