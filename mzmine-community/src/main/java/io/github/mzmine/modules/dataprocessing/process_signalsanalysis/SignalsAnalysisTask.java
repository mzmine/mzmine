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

package io.github.mzmine.modules.dataprocessing.process_signalsanalysis;

import static io.github.mzmine.util.DataPointUtils.removePrecursorMz;
import static io.github.mzmine.util.scans.ScanUtils.findAllMS2FragmentScans;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.numbers.CommonSignalsType;
import io.github.mzmine.datamodel.features.types.numbers.UniqueFragmentedPrecursorsType;
import io.github.mzmine.datamodel.features.types.numbers.UniqueMs1SignalsType;
import io.github.mzmine.datamodel.features.types.numbers.UniqueMs2SignalsType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The SignalsAnalysisTask is responsible for analyzing the signals from feature lists. The task
 * will be scheduled by the TaskController and progress is calculated from the
 * finishedItems/totalItems. It compares all MS1 signals present in the feature mass spectrum to the
 * MS2 signals present in all the fragment scans corresponding to precursors corresponding to MS1
 * signals.
 */
class SignalsAnalysisTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(SignalsAnalysisTask.class.getName());
  private final List<FeatureList> featureLists;
  private final boolean useMassList;
  private final MZTolerance tolerance;

  /**
   * Constructor to initialize the task with necessary parameters.
   *
   * @param project        The MZmine project.
   * @param featureLists   The feature lists to process.
   * @param parameters     User parameters.
   * @param storage        Optional memory map storage.
   * @param moduleCallDate The date the module was called.
   * @param moduleClass    The class of the calling module.
   */
  public SignalsAnalysisTask(MZmineProject project, List<FeatureList> featureLists,
      ParameterSet parameters, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.featureLists = featureLists;
    this.totalItems = featureLists.stream().mapToInt(FeatureList::getNumberOfRows).sum();
    var scanDataType = parameters.getValue(SignalsAnalysisParameters.scanDataType);
    this.useMassList = scanDataType == ScanDataType.MASS_LIST;
    this.tolerance = parameters.getValue(SignalsAnalysisParameters.tolerance);
  }

  /**
   * Streams MS1 scans from the grouped scans.
   *
   * @param groupedScans The grouped scans.
   * @return A stream of MS1 scans.
   */
  private static @NotNull Stream<Scan> streamMs1Scans(final List<GroupedSignalScans> groupedScans) {
    return groupedScans.stream().map(GroupedSignalScans::ms1Scans).flatMap(Collection::stream);
  }

  @Override
  protected void process() {
    List<GroupedSignalScans> groupedScans = collectSpectra();
    logger.info("Collected spectra - now starting to analyze the grouped scans.");
  }

  /**
   * Collects spectra from feature lists.
   *
   * @return A list of grouped signal scans.
   */
  private List<GroupedSignalScans> collectSpectra() {
    List<GroupedSignalScans> groupingResults = new ArrayList<>();
    for (FeatureList featureList : featureLists) {
      for (var row : featureList.getRows()) {
        GroupedSignalScans result = processRow(row);
        groupingResults.add(result);
      }
    }
    return groupingResults;
  }

  /**
   * Processes a single row from the feature list.
   *
   * @param row The feature list row.
   * @return The grouped signal scans.
   */
  private GroupedSignalScans processRow(FeatureListRow row) {
    List<Scan> ms1Scans = new ArrayList<>();
    List<Scan> ms2Scans = new ArrayList<>();
    for (final ModularFeature feature : row.getFeatures()) {
      try {
        List<Scan> exportedScans = processFeature(feature);
        for (Scan scan : exportedScans) {
          if (scan.getMSLevel() == 1) {
            ms1Scans.add(scan);
          } else if (scan.getMSLevel() == 2) {
            ms2Scans.add(scan);
          }
        }
      } catch (Exception ex) {
        logger.log(Level.WARNING, ex.getMessage(), ex);
      }
    }
    SignalsResults results = countUniqueSignalsBetweenMs1AndMs2(ms1Scans, ms2Scans, tolerance);
    row.set(UniqueFragmentedPrecursorsType.class, results.precursorsCount);
    row.set(UniqueMs1SignalsType.class, results.ms1Count);
    row.set(UniqueMs2SignalsType.class, results.ms2Count);
    row.set(CommonSignalsType.class, results.commonCount);

    return new GroupedSignalScans(row, ms1Scans, ms2Scans);
  }

  /**
   * Processes a single feature.
   *
   * @param feature The feature to process.
   * @return The list of exported scans.
   */
  private List<Scan> processFeature(final Feature feature) {
    List<Scan> scansToExport = new ArrayList<>();
    Scan bestMs1 = feature.getRepresentativeScan();
    if (bestMs1 != null) {
      scansToExport.add(bestMs1);
      // TODO: This could be a parameter to get it over the whole file instead
      Range<Float> rawRtRange = feature.getRawDataPointsRTRange();
      RawDataFile raw = feature.getRawDataFile();
      DataPoint[] dataPoints = ScanUtils.extractDataPoints(bestMs1, useMassList);
      for (DataPoint dp : dataPoints) {
        double ms1Signal = dp.getMZ();
        Scan[] fragmentScansBroad = findAllMS2FragmentScans(raw, rawRtRange,
            tolerance.getToleranceRange(ms1Signal));
        for (Scan ms2 : fragmentScansBroad) {
          if (ms2 != null) {
            scansToExport.add(ms2);
          }
        }
      }
    }
    feature.getAllMS2FragmentScans().stream().filter(scan -> scan.getMSLevel() == 2)
        .forEach(scansToExport::add);
    return scansToExport;
  }

  @Override
  public String getTaskDescription() {
    return "Signals analysis task running on " + featureLists;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return featureLists;
  }

  /**
   * Counts unique signals between MS1 and MS2 scans.
   *
   * @param ms1Scans  The MS1 scans.
   * @param ms2Scans  The MS2 scans.
   * @param tolerance The MZ tolerance.
   * @return The results of the signal count.
   */
  private SignalsResults countUniqueSignalsBetweenMs1AndMs2(List<Scan> ms1Scans,
      List<Scan> ms2Scans, MZTolerance tolerance) {
    Set<Double> uniqueMs1 = collectUniqueSignalsFromScans(ms1Scans, tolerance);
    Set<Double> uniqueMs2 = collectUniqueSignalsFromScans(ms2Scans, tolerance);
    List<Double> precursors = new ArrayList<>();
    for (Scan scan : ms2Scans) {
      precursors.add(scan.getPrecursorMz());
    }
    int precursorsCount = collectUniqueSignals(precursors, tolerance).size();
    int commonCount = countUniquePairs(uniqueMs1, uniqueMs2, tolerance);
    int ms1Count = uniqueMs1.size();
    int ms2Count = uniqueMs2.size();

    return new SignalsResults(commonCount, ms1Count, ms2Count, precursorsCount);
  }

  /**
   * Collects unique signals from scans.
   *
   * @param scans     The scans.
   * @param tolerance The MZ tolerance.
   * @return A set of unique signals.
   */
  private Set<Double> collectUniqueSignalsFromScans(List<Scan> scans, MZTolerance tolerance) {
    Set<Double> uniqueSignals = new HashSet<>();
    for (Scan scan : scans) {
      DataPoint[] dataPoints = ScanUtils.extractDataPoints(scan, useMassList);
      if (scan.getMSLevel() > 1) {
        // TODO arbitrarily removing 1 around precursor for now
        dataPoints = removePrecursorMz(dataPoints, scan.getPrecursorMz(), 1);
      }
      List<Double> mzs = new ArrayList<>();
      for (DataPoint dp : dataPoints) {
        double mz = dp.getMZ();
        mzs.add(mz);
      }
      uniqueSignals.addAll(collectUniqueSignals(mzs, tolerance));
    }
    return uniqueSignals;
  }

  /**
   * Collects unique signals from a list of MZ values.
   *
   * @param mzValues  The MZ values.
   * @param tolerance The MZ tolerance.
   * @return A set of unique signals.
   */
  private Set<Double> collectUniqueSignals(List<Double> mzValues, MZTolerance tolerance) {
    Set<Double> uniqueSignals = new HashSet<>();
    for (double mz : mzValues) {
      boolean isUnique = true;
      for (double uniqueMz : uniqueSignals) {
        if (tolerance.checkWithinTolerance(mz, uniqueMz)) {
          isUnique = false;
          break;
        }
      }
      if (isUnique) {
        uniqueSignals.add(mz);
      }
    }
    return uniqueSignals;
  }

  /**
   * Counts unique pairs between two sets of MZ values.
   *
   * @param uniqueMs1 The unique MS1 signals.
   * @param uniqueMs2 The unique MS2 signals.
   * @param tolerance The MZ tolerance.
   * @return The count of unique pairs.
   */
  private int countUniquePairs(Set<Double> uniqueMs1, Set<Double> uniqueMs2,
      MZTolerance tolerance) {
    int uniqueCount = 0;
    for (double mz1 : uniqueMs1) {
      for (double mz2 : uniqueMs2) {
        if (tolerance.checkWithinTolerance(mz1, mz2)) {
          uniqueCount++;
          break; // Move to next mz1
        }
      }
    }
    return uniqueCount;
  }

  /**
   * A container for the results of the signal counting.
   */
  private static class SignalsResults {

    private final int commonCount;
    private final int ms1Count;
    private final int ms2Count;
    private final int precursorsCount;

    private SignalsResults(int commonCount, int ms1Count, int ms2Count, int precursorsCount) {
      this.commonCount = commonCount;
      this.ms1Count = ms1Count;
      this.ms2Count = ms2Count;
      this.precursorsCount = precursorsCount;
    }

    public int getCommonCount() {
      return commonCount;
    }

    public int getMs1Count() {
      return ms1Count;
    }

    public int getMs2Count() {
      return ms2Count;
    }

    public int getPrecursorsCount() {
      return precursorsCount;
    }
  }
}
