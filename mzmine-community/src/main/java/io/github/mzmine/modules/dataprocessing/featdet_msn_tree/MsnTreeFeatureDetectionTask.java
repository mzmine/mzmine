/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mzmine.modules.dataprocessing.featdet_msn_tree;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PrecursorIonTree;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.dataprocessing.featdet_extract_mz_ranges.ExtractMzRangesIonSeriesFunction;
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
  private ExtractMzRangesIonSeriesFunction extractorFunction;

  public MsnTreeFeatureDetectionTask(MZmineProject project, RawDataFile dataFile,
      ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.dataFile = dataFile;

    scanSelection = parameters.getValue(MsnTreeFeatureDetectionParameters.scanSelection);
    mzTol = parameters.getValue(MsnTreeFeatureDetectionParameters.mzTol);
    String suffix = parameters.getValue(MsnTreeFeatureDetectionParameters.suffix);
    newFeatureList = new ModularFeatureList(dataFile.getName() + " " + suffix,
        getMemoryMapStorage(), dataFile);
    this.parameterSet = parameters;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  public double getFinishedPercentage() {
    return extractorFunction == null ? 0 : extractorFunction.getFinishedPercentage();
  }

  @Override
  public String getTaskDescription() {
    return "Building MSn trees from " + dataFile;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    var scans = List.of(scanSelection.getMatchingScans(dataFile));

    // No scans in selection range.
    if (scans.isEmpty()) {
      setStatus(TaskStatus.ERROR);
      final String msg = "No scans detected in scan selection for " + dataFile.getName();
      setErrorMessage(msg);
      return;
    }

    boolean anyNoDataFeatures = false;

    // get trees sorted ascending
    final List<PrecursorIonTree> trees = new ArrayList<>(
        ScanUtils.getMSnFragmentTrees(dataFile, mzTol));
    trees.sort(Comparator.comparingDouble(PrecursorIonTree::getPrecursorMz));
    List<Range<Double>> mzRangesSorted = trees.stream()
        .mapToDouble(PrecursorIonTree::getPrecursorMz).mapToObj(mzTol::getToleranceRange).toList();

    extractorFunction = new ExtractMzRangesIonSeriesFunction(dataFile, scanSelection,
        mzRangesSorted, ScanDataType.MASS_LIST, this);
    BuildingIonSeries[] chromatograms = extractorFunction.get();

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
      var mstree = trees.get(i);
      final BuildingIonSeries eic = chromatograms[i];
      var hasData = eic.hasNonZeroData();
      var featureData =
          hasData ? eic.toIonTimeSeriesWithLeadingAndTrailingZero(storage, scans) : null;
      var f = new ModularFeature(newFeatureList, dataFile, featureData, FeatureStatus.DETECTED);
      // need to set mz if data was empty
      if (!hasData) {
        f.setMZ(mstree.getPrecursorMz());
        f.setHeight(0f);
        f.setArea(0f);
        f.setRT(0f);
        anyNoDataFeatures = true;
        // should we discard the feature in that case? it is possible that the precursor ion is
        // not found in the ms2 spectra at all but the ms2s still contain information
      }
      f.setAllMS2FragmentScans(mstree.getAllFragmentScans());
      ModularFeatureListRow row = new ModularFeatureListRow(newFeatureList, id, f);
      newFeatureList.addRow(row);
      id++;
    }

    if (anyNoDataFeatures) {
      DialogLoggerUtil.showWarningNotification("MSn tree feature detection",
          "For some MSn features no precursor ion signals were detected in the provided scan selection. This may indicate too high noise levels.");
    }

    newFeatureList.setSelectedScans(dataFile, scans);
    newFeatureList.getAppliedMethods().addAll(dataFile.getAppliedMethods());
    newFeatureList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(MsnTreeFeatureDetectionModule.class, parameterSet,
            getModuleCallDate()));

    // Add new feature list to the project
    project.addFeatureList(newFeatureList);

    logger.info("Finished MSn Tree feature builder on " + dataFile);

    setStatus(TaskStatus.FINISHED);
  }

}
