/*
 * Copyright 2006-2022 The MZmine Development Team
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
package io.github.mzmine.modules.dataprocessing.featdet_msn_tree;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PrecursorIonTree;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.dataprocessing.featdet_msn_tree.SimpleFullChromatogram.IntensityMode;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class MsnTreeFeatureDetectionTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      MsnTreeFeatureDetectionTask.class.getName());
  private final MZmineProject project;
  private final RawDataFile dataFile;
  private final ScanSelection scanSelection;
  private final MZTolerance mzTol;
  private final ParameterSet parameterSet;
  private final ModularFeatureList newFeatureList;
  private int processedScans, totalScans;

  public MsnTreeFeatureDetectionTask(MZmineProject project, RawDataFile dataFile,
      ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.dataFile = dataFile;

    scanSelection = parameters.getParameter(MsnTreeFeatureDetectionParameters.scanSelection)
        .getValue();
    mzTol = parameters.getParameter(MsnTreeFeatureDetectionParameters.mzTol).getValue();
    newFeatureList = new ModularFeatureList(dataFile.getName() + " MSn trees",
        getMemoryMapStorage(), dataFile);
    this.parameterSet = parameters;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalScans == 0) {
      return 0f;
    }
    return (double) processedScans / totalScans;
  }

  @Override
  public String getTaskDescription() {
    return "Building MSn trees from " + dataFile;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    var scans = List.of(scanSelection.getMatchingScans(dataFile));

    totalScans = scans.size();

    // No scans in selection range.
    if (totalScans == 0) {
      setStatus(TaskStatus.ERROR);
      final String msg = "No scans detected in scan selection for " + dataFile.getName();
      setErrorMessage(msg);
      return;
    }

    // get trees sorted ascending
    final List<PrecursorIonTree> trees = new ArrayList<>(
        ScanUtils.getMSnFragmentTrees(dataFile, mzTol));
    trees.sort(Comparator.comparingDouble(PrecursorIonTree::getPrecursorMz));
    List<Range<Double>> mzRanges = trees.stream().mapToDouble(PrecursorIonTree::getPrecursorMz)
        .mapToObj(mzTol::getToleranceRange).toList();

    SimpleFullChromatogram[] chromatograms = extractChromatograms(dataFile, mzRanges, scanSelection,
        this);
    if (isCanceled()) {
      return;
    }
    if (chromatograms == null) {
      setErrorMessage("No MSn tree EIC found in file " + dataFile.getName());
      setStatus(TaskStatus.ERROR);
      return;
    }

    int id = 0;
    for (int i = 0; i < chromatograms.length; i++) {
      final SimpleFullChromatogram eic = chromatograms[i];
      ModularFeature f = new ModularFeature(newFeatureList, dataFile,
          eic.toIonTimeSeries(storage, scans), FeatureStatus.DETECTED);
      f.setAllMS2FragmentScans(trees.get(i).getAllFragmentScans());
      ModularFeatureListRow row = new ModularFeatureListRow(newFeatureList, id, f);
      newFeatureList.addRow(row);
      id++;
    }

    newFeatureList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(MsnTreeFeatureDetectionModule.class, parameterSet,
            getModuleCallDate()));

    // Add new feature list to the project
    project.addFeatureList(newFeatureList);

    logger.info("Finished MSn Tree feature builder on " + dataFile);

    setStatus(TaskStatus.FINISHED);
  }

  public SimpleFullChromatogram[] extractChromatograms(final RawDataFile dataFile,
      final List<Range<Double>> mzRanges, final ScanSelection scanSelection,
      final AbstractTask parentTask) {
    var dataAccess = EfficientDataAccess.of(dataFile, ScanDataType.CENTROID, scanSelection);
    // store data points for each range
    SimpleFullChromatogram[] chromatograms = new SimpleFullChromatogram[mzRanges.size()];
    for (int i = 0; i < chromatograms.length; i++) {
      chromatograms[i] = new SimpleFullChromatogram(dataAccess.getNumberOfScans(),
          IntensityMode.HIGHEST);
    }

    int currentScan = -1;
    while (dataAccess.nextScan() != null) {
      int currentTree = 0;
      currentScan++;
      processedScans++;

      // Canceled?
      if (parentTask != null && parentTask.isCanceled()) {
        return null;
      }
      // check value for tree and for all next trees in range
      int nDataPoints = dataAccess.getNumberOfDataPoints();
      for (int dp = 0; dp < nDataPoints; dp++) {
        double mz = dataAccess.getMzValue(dp);
        // all next trees
        for (int t = currentTree; t < mzRanges.size(); t++) {
          if (mz > mzRanges.get(t).upperEndpoint()) {
            // out of bounds for current tree
            currentTree++;
          } else if (mz < mzRanges.get(t).lowerEndpoint()) {
            break;
          } else {
            // found match
            double intensity = dataAccess.getIntensityValue(dp);
            chromatograms[t].addValue(currentScan, mz, intensity);
          }
        }
        // all trees done
        if (currentTree >= mzRanges.size()) {
          break;
        }
      }
    }
    return chromatograms;
  }

}
