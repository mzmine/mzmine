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
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ModularADAPChromatogramBuilderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      ModularADAPChromatogramBuilderTask.class.getName());

  private final MZmineProject project;
  private final RawDataFile dataFile;
  private final ScanSelection scanSelection;
  // User parameters
  private final String suffix;
  private final MZTolerance mzTolerance;
  private final int minimumScanSpan;
  // Owen added User parameers;
  private final double minGroupIntensity;
  private final double minHighestPoint;
  private final ParameterSet parameters;
  private double progress = 0.0;
  private int newFeatureID = 1;
  private ModularFeatureList newFeatureList;

  /**
   *
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

    this.suffix = parameters.getParameter(ADAPChromatogramBuilderParameters.suffix).getValue();

    // Owen added parameters
    this.minGroupIntensity = parameters.getParameter(
        ADAPChromatogramBuilderParameters.minGroupIntensity).getValue();
    this.minHighestPoint = parameters.getParameter(
        ADAPChromatogramBuilderParameters.minHighestPoint).getValue();
    this.parameters = parameters;
  }

  @Override
  public String getTaskDescription() {
    return "Detecting chromatograms in " + dataFile;
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    logger.info(() -> "Started chromatogram builder on " + dataFile);

    Scan[] scans = scanSelection.getMatchingScans(dataFile);
    if (scans.length == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("There are no scans satisfying filtering values. Consider updating filters "
                      + "with \"Set filters\" in the \"Scans\" parameter.");
      return;
    }

    // Check if the scans are properly ordered by RT
    double prevRT = Double.NEGATIVE_INFINITY;
    for (Scan s : scans) {
      if (isCanceled()) {
        return;
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
    int level = scans[0].getMSLevel();
    for (int i = 1; i < scans.length; i++) {
      if (level != scans[i].getMSLevel()) {
        MZmineCore.getDesktop().displayMessage(null,
            "MZmine thinks that you are running ADAP Chromatogram builder on both MS1- and MS2-scans. "
            + "This will likely produce wrong results. "
            + "Please, set the scan filter parameter to a specific MS level");
      }
    }

    // make a list of all the data points
    // sort data points by intensity
    // loop through list
    // add data point to chromatogrm or make new one
    // update mz avg and other stuff
    //

    // map the mz tolerance to chromatograms
    RangeMap<Double, ADAPChromatogram> rangeToChromMap = TreeRangeMap.create();

    // make a list of all the data points
    List<ExpandedDataPoint> allMzValues = new ArrayList<>();

    ScanDataAccess scanData = EfficientDataAccess.of(dataFile, ScanDataType.CENTROID,
        scanSelection);

    while (scanData.hasNextScan()) {
      if (isCanceled()) {
        return;
      }

      Scan scan;
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

    // count starts at 1 since we already have added one with a single point.
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

      final Entry<Range<Double>, ADAPChromatogram> existing = rangeToChromMap.getEntry(
          mzFeature.getMZ());
      if (existing != null) {
        // add data point to chromatogram
        existing.getValue().addMzFeature(mzFeature.getScan(), mzFeature);
      } else {
        // skip it entierly if the intensity is not high enough
        if (mzFeature.getIntensity() < minHighestPoint) {
          continue;
        }
        // add a new chromatogram to the range map - limit ranges to avoid overlap
        startNewChromatogramLimitMzRanges(rangeToChromMap, mzFeature);
      }
    }

    // finish chromatograms sorted by m/z
    final Map<Range<Double>, ADAPChromatogram> finalRangeMap = rangeToChromMap.asMapOfRanges();

    int numChromatograms = finalRangeMap.size();
    progressStep = numChromatograms > 0 ? 0.5 / numChromatograms : 0.0;

    // Create new feature list
    newFeatureList = new ModularFeatureList(dataFile + " " + suffix, getMemoryMapStorage(),
        dataFile);
    // ensure that the default columns are available
    DataTypeUtils.addDefaultChromatographicTypeColumns(newFeatureList);

    // add chromatograms that match criteria
    for (ADAPChromatogram chromatogram : finalRangeMap.values()) {
      if (isCanceled()) {
        return;
      }

      progress += progressStep;

      // And remove chromatograms who dont have a certian number of continous points above the
      // IntensityThresh2 level.
      if (chromatogram.matchesMinContinuousDataPoints(scans, minGroupIntensity, minimumScanSpan,
          minHighestPoint)) {
        // add zeros to edges
        chromatogram.addNZeros(scans, 1, 1);

        // add to list
        ModularFeature modular = FeatureConvertors.ADAPChromatogramToModularFeature(newFeatureList,
            dataFile, chromatogram);
        ModularFeatureListRow newRow = new ModularFeatureListRow(newFeatureList, newFeatureID,
            modular);
        newFeatureList.addRow(newRow);
        // activate shape for this row
        newRow.set(FeatureShapeType.class, true);
        newFeatureID++;
      }
    }

    newFeatureList.setSelectedScans(dataFile, Arrays.asList(scans));

    dataFile.getAppliedMethods().forEach(m -> newFeatureList.getAppliedMethods().add(m));
    // Add new feature list to the project
    newFeatureList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(ModularADAPChromatogramBuilderModule.class, parameters,
            getModuleCallDate()));
    project.addFeatureList(newFeatureList);

    progress = 1.0;

    setStatus(TaskStatus.FINISHED);

    logger.info(() -> "Finished chromatogram builder on " + dataFile);
  }

  /**
   * Starts a new chromatogram and limits its range so that it does not overlap with existing m/z
   * ranges
   *
   * @param rangeToChromMap started chromatograms with their non overlapping m/z range
   * @param mzFeature       current tested data point
   */
  @SuppressWarnings("UnstableApiUsage")
  private void startNewChromatogramLimitMzRanges(RangeMap<Double, ADAPChromatogram> rangeToChromMap,
      ExpandedDataPoint mzFeature) {
    // start new chromatogram and create new range (subract overlapping existing ranges)
    Range<Double> toleranceRange = mzTolerance.getToleranceRange(mzFeature.getMZ());

    // look +- mz tolerance to see if ther is a range near by.
    // If there is use the proper boundry of that range for the
    // new range to insure than NON OF THE RANGES OVERLAP.
    final Entry<Range<Double>, ADAPChromatogram> minusRange = rangeToChromMap.getEntry(
        toleranceRange.lowerEndpoint());
    final Entry<Range<Double>, ADAPChromatogram> plusRange = rangeToChromMap.getEntry(
        toleranceRange.upperEndpoint());

    // If both of the above ranges are null then we make the new range spaning the full
    // mz tolerance range.
    // If one or both are not null we need to properly modify the range of the new
    // chromatogram so that none of the points are overlapping.
    Double toBeLowerBound =
        minusRange == null ? toleranceRange.lowerEndpoint() : minusRange.getKey().upperEndpoint();
    Double toBeUpperBound =
        plusRange == null ? toleranceRange.upperEndpoint() : plusRange.getKey().lowerEndpoint();

    if (toBeLowerBound < toBeUpperBound) {
      // use closed open so that every value may be captured by rangeMap
      Range<Double> newRange = Range.closedOpen(toBeLowerBound, toBeUpperBound);
      ADAPChromatogram newChrom = new ADAPChromatogram();
      newChrom.addMzFeature(mzFeature.getScan(), mzFeature);

      rangeToChromMap.put(newRange, newChrom);
    } else if (toBeLowerBound.equals(toBeUpperBound) && plusRange != null) {
      plusRange.getValue().addMzFeature(mzFeature.getScan(), mzFeature);
    } else {
      throw new IllegalStateException(
          String.format("Incorrect range [%f, %f] for m/z %f", toBeLowerBound, toBeUpperBound,
              mzFeature.getMZ()));
    }
  }

}

