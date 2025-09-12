/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.filter_diams2_nocorr;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.impl.SimplePseudoSpectrum;
import io.github.mzmine.datamodel.msms.DIAMsMsInfoImpl;
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiaMs2NoCorrTask extends AbstractFeatureListTask {

  private final FeatureList flist;
  private final boolean replaceExisting;
  private final ScanSelection scanSelection;

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   * @param parameters
   * @param moduleClass
   */
  protected DiaMs2NoCorrTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull ParameterSet parameters, @NotNull Class<? extends MZmineModule> moduleClass,
      FeatureList flist) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.flist = flist;
    replaceExisting = parameters.getValue(DiaMs2NoCorrParameters.replaceExisting);
    scanSelection = parameters.getValue(DiaMs2NoCorrParameters.ms2ScanSelection);

    totalItems = (long) flist.getNumberOfRows() * flist.getNumberOfRawDataFiles();
  }

  private static boolean isMatchingMs2Frame(Frame potentialFrame, Feature feature) {
    final Set<IonMobilityMsMsInfo> msmsInfos = potentialFrame.getImsMsMsInfos();

    if (msmsInfos.isEmpty()) {
      return true;
    }

    return getMatchingImsMsMsInfo(feature, msmsInfos) != null;
  }

  private static @Nullable IonMobilityMsMsInfo getMatchingImsMsMsInfo(Feature feature,
      Set<IonMobilityMsMsInfo> msmsInfos) {
    final Float mobility = feature.getMobility();
    return msmsInfos.stream().filter(info -> {
      final Range<Float> mobRange = info.getMobilityRange();
      final Range<Double> isolationWindow = info.getIsolationWindow();
      if ((mobRange != null && mobility != null && !mobRange.contains(mobility)) || (
          isolationWindow != null && !isolationWindow.contains(feature.getMZ()))) {
        return false;
      }
      return true;
    }).findFirst().orElse(null);
  }

  private static boolean featureMatchesScanIsolation(Scan potentialScan, Feature feature) {
    if (potentialScan.getMSLevel() == 1) {
      // if scan selection is set to ms1, probably some fancy user stuff is happening. or someone misconfigured the filter.
      return true;
    }

    if (potentialScan instanceof Frame potentialFrame && isMatchingMs2Frame(potentialFrame,
        feature)) {
      return true;
    } else {
      final MsMsInfo msMsInfo = potentialScan.getMsMsInfo();
      if (msMsInfo != null && msMsInfo.getIsolationWindow() != null && msMsInfo.getIsolationWindow()
          .contains(feature.getMZ())) {
        return true;
      }
    }
    return false;
  }

  private static Range<Float> getMobilityRange(@Nullable IonMobilityMsMsInfo matchingImsMsMsInfo,
      Feature feature) {
    if (feature.getFeatureData() instanceof IonMobilogramTimeSeries imts) {
      return IonMobilityUtils.getMobilityFWHM(imts.getSummedMobilogram());
    }

    return matchingImsMsMsInfo == null ? null : matchingImsMsMsInfo.getMobilityRange();
  }

  private static void putScanIntoCeMap(Feature feature, Scan potentialScan,
      Map<Float, Scan> ceToScanMap) {
    if (potentialScan.getMSLevel() == 1) {
      ceToScanMap.putIfAbsent(0f, potentialScan);
    } else if (potentialScan instanceof Frame potentialFrame) {
      final IonMobilityMsMsInfo info = getMatchingImsMsMsInfo(feature,
          potentialFrame.getImsMsMsInfos());
      if (info != null) {
        ceToScanMap.putIfAbsent(info.getActivationEnergy(), potentialFrame);
      }
    } else if (potentialScan.getMsMsInfo() != null) {
      ceToScanMap.putIfAbsent(potentialScan.getMsMsInfo().getActivationEnergy(), potentialScan);
    }
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  protected void process() {
    final List<FeatureListRow> rows = flist.getRowsCopy();

    final List<RawDataFile> files = flist.getRawDataFiles();
    for (RawDataFile file : files) {
      final List<Scan> eligibleScans = scanSelection.getMatchingScans(file.getScans());

      for (FeatureListRow row : rows) {

        final Feature feature = row.getFeature(file);
        if (feature == null || feature.getFeatureStatus() == FeatureStatus.UNKNOWN
            || feature.getRT() == null || (!feature.getAllMS2FragmentScans().isEmpty()
            && !replaceExisting)) {
          finishedItems.getAndIncrement();
          continue;
        }

        final List<Scan> ms2Scans = findMatchingMs2Scans(eligibleScans, feature);
        if (ms2Scans.isEmpty()) {
          finishedItems.getAndIncrement();
          continue;
        }

        final List<PseudoSpectrum> pseudoSpectra = new ArrayList<>();
        for (final Scan ms2Scan : ms2Scans) {
          if (ms2Scan instanceof Frame ms2Frame) {
            @Nullable final IonMobilityMsMsInfo msmsInfo = getMatchingImsMsMsInfo(feature,
                ms2Frame.getImsMsMsInfos());

            final Range<Float> mobilityRange = Objects.requireNonNullElse(
                getMobilityRange(msmsInfo, feature),
                RangeUtils.toFloatRange(ms2Frame.getMobilityRange()));

            final double[][] mzIntensity = SpectraMerging.calculatedMergedMzsAndIntensities(
                ms2Frame.getMobilityScans().stream()
                    .filter(s -> mobilityRange.contains((float) s.getMobility()))
                    .map(MobilityScan::getMassList).toList(), SpectraMerging.pasefMS2MergeTol,
                IntensityMergingType.SUMMED, SpectraMerging.DEFAULT_CENTER_FUNCTION, null, null, 2);

            final SimplePseudoSpectrum ms2 = new SimplePseudoSpectrum(file, ms2Frame.getMSLevel(),
                ms2Frame.getRetentionTime(), null, mzIntensity[0], mzIntensity[1],
                ms2Frame.getPolarity(), "Pseudo spectrum (uncorrelated, IMS filtered)",
                PseudoSpectrumType.UNCORRELATED);

            final DIAMsMsInfoImpl builtMs2Info = new DIAMsMsInfoImpl(
                msmsInfo != null ? msmsInfo.getActivationEnergy() : null, ms2, ms2.getMSLevel(),
                msmsInfo != null ? msmsInfo.getActivationMethod() : null,
                RangeUtils.toDoubleRange(mobilityRange));
            ms2.setMsMsInfo(builtMs2Info);
            pseudoSpectra.add(ms2);
          } else {

            final SimplePseudoSpectrum ms2 = new SimplePseudoSpectrum(file, ms2Scan.getMSLevel(),
                ms2Scan.getRetentionTime(), null, ms2Scan.getMzValues(new double[0]),
                ms2Scan.getIntensityValues(new double[0]), ms2Scan.getPolarity(),
                "Pseudo spectrum (uncorrelated)", PseudoSpectrumType.UNCORRELATED);
            pseudoSpectra.add(ms2);
          }
          feature.setAllMS2FragmentScans(ImmutableList.copyOf(pseudoSpectra));

          finishedItems.incrementAndGet();
        }
      }
    }
  }

  /**
   * Finds the closest ms2 scan that contains ms2 information that matches this feature. (DIA may
   * still have isolation windows (=SWATH/diaPASEF)) If no window information is supplied, it is
   * assumed that everything was fragmented and the closest scan will be used.
   *
   * @return null if no matching scan was found (isolation data supplied, but none matches)
   */
  private @NotNull List<Scan> findMatchingMs2Scans(List<Scan> eligibleScans, Feature feature) {

    final int closestScanIndex = BinarySearch.binarySearch(feature.getRT().doubleValue(),
        DefaultTo.CLOSEST_VALUE, eligibleScans.size(),
        i -> eligibleScans.get(i).getRetentionTime());

    if (closestScanIndex < 0) {
      return List.of();
    }

    final Map<Float, Scan> ceToScanMap = new HashMap<>();

    final Range<Float> featureRtRange = feature.getRawDataPointsRTRange();
    // find the scan that actually matches the isolation window
    for (int i = 0; i + closestScanIndex < eligibleScans.size() && closestScanIndex - i >= 0; i++) {

      boolean bothScansOutsideRange = true;

      // check next scan
      if (i + closestScanIndex < eligibleScans.size()) {
        final Scan potentialScan = eligibleScans.get(closestScanIndex + i);
        bothScansOutsideRange = !featureRtRange.contains(potentialScan.getRetentionTime());

        if (featureMatchesScanIsolation(potentialScan, feature)) {
          putScanIntoCeMap(feature, potentialScan, ceToScanMap);
        }
      }

      // check previous scan
      if (closestScanIndex - i >= 0) {
        final Scan potentialScan = eligibleScans.get(closestScanIndex - i);
        bothScansOutsideRange =
            bothScansOutsideRange && !featureRtRange.contains(potentialScan.getRetentionTime());

        if (featureMatchesScanIsolation(potentialScan, feature)) {
          putScanIntoCeMap(feature, potentialScan, ceToScanMap);
        }
      }

      // no scan found
      if (bothScansOutsideRange) {
        return ImmutableList.copyOf((ceToScanMap.values()));
      }
    }
    return ImmutableList.copyOf((ceToScanMap.values()));
  }

  @Override
  public String getTaskDescription() {
    return "Assigning closest MS2 scans to Features.";
  }
}
