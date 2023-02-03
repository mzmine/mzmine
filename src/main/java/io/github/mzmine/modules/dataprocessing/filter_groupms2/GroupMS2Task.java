/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_groupms2;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.MsMsInfoType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RtMs2ApexDistanceType;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.FragmentScanSorter;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Filters out feature list rows.
 */
public class GroupMS2Task extends AbstractTask {

  private static final Logger logger = Logger.getLogger(GroupMS2Task.class.getName());

  private final ParameterSet parameters;
  private final Double minMs2IntensityAbs;
  private final boolean combineTimsMS2;
  private final Double minMs2IntensityRel;
  private final FeatureList list;
  private final RTTolerance rtTol;
  private final MZTolerance mzTol;
  private final boolean lockToFeatureMobilityRange;
  private final int minimumSignals;
  private final FeatureLimitOptions rtFilter;
  private int processedRows, totalRows;

  /**
   * Create the task.
   *
   * @param list         feature list to process.
   * @param parameterSet task parameters.
   */
  public GroupMS2Task(final FeatureList list, final ParameterSet parameterSet,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    parameters = parameterSet;
    // RT has two options / tolerance is only provided for second option
    var rtFilterParam = parameters.getParameter(GroupMS2Parameters.rtFilter);
    rtFilter = rtFilterParam.getValue();
    rtTol = rtFilterParam.useEmbeddedParameter() ? rtFilterParam.getEmbeddedParameter().getValue()
        : null;

    mzTol = parameters.getValue(GroupMS2Parameters.mzTol);
    combineTimsMS2 = parameterSet.getValue(GroupMS2Parameters.combineTimsMsMs);
    lockToFeatureMobilityRange = parameterSet.getValue(GroupMS2Parameters.limitMobilityByFeature);
    minMs2IntensityAbs = parameterSet.getEmbeddedParameterValueIfSelectedOrElse(
        GroupMS2Parameters.outputNoiseLevel, null);
    minMs2IntensityRel = parameterSet.getEmbeddedParameterValueIfSelectedOrElse(
        GroupMS2Parameters.outputNoiseLevelRelative, null);

    // 0 is deactivated
    minimumSignals = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        GroupMS2Parameters.minRequiredSignals, 0);

    this.list = list;
    processedRows = 0;
    totalRows = 0;
  }

  @Override
  public double getFinishedPercentage() {

    return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {

    return "Adding all MS2 scans to their features in list " + list.getName();
  }

  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);

      totalRows = list.getNumberOfRows();
      // for all features
      for (FeatureListRow row : list.getRows()) {
        if (isCanceled()) {
          return;
        }

        processRow(row);
        processedRows++;
      }

      list.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(GroupMS2Module.class, parameters,
              getModuleCallDate()));
      setStatus(TaskStatus.FINISHED);
      logger.info("Finished adding all MS2 scans to their features in " + list.getName());

    } catch (Throwable t) {
      t.printStackTrace();
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
      logger.log(Level.SEVERE, "Error while adding all MS2 scans to their feautres", t);
    }
  }

  /**
   * Group all MS2 scans with the corresponding features (per raw data file)
   *
   * @param row
   */
  public void processRow(FeatureListRow row) {
    for (ModularFeature feature : row.getFeatures()) {
      final List<Scan> scans;
      if (MobilityType.TIMS.isTypeOfBackingRawData(feature)) {
        scans = findFragmentScansForTimsFeature(feature);
      } else {
        scans = findFragmentScans(feature);
      }

      filterByMinimumSignals(scans);
      feature.setAllMS2FragmentScans(scans.isEmpty() ? null : scans, true);
      // get proximity
      setRtApexProximity(feature, scans);
    }
  }

  /**
   * Find all fragment scans for this feature applying RT and mz filters
   *
   * @return list of fragment scans
   */
  @NotNull
  private List<Scan> findFragmentScans(final ModularFeature feature) {
    final List<Scan> scans;
    RawDataFile raw = feature.getRawDataFile();

    scans = raw.stream().filter(scan -> scan.getMSLevel() > 1)
        .filter(scan -> filterScan(scan, feature)).sorted(FragmentScanSorter.DEFAULT_TIC).toList();
    return scans;
  }

  /**
   * Calculate and set the RT proximity
   *
   * @param f     feature
   * @param scans feature's fragment scans
   */
  private void setRtApexProximity(final ModularFeature f, final List<Scan> scans) {
    if (scans.isEmpty()) {
      return;
    }
    float apexDistance = Float.MAX_VALUE;
    for (Scan s : scans) {
      float dist = s.getRetentionTime() - f.getRT();
      if (dist < apexDistance) {
        apexDistance = dist;
      }
    }
    f.set(RtMs2ApexDistanceType.class, apexDistance);
  }

  /**
   * Filter scans based on rt and mz
   *
   * @param scan tested scan
   * @return true if matches all criteria
   */
  private boolean filterScan(Scan scan, ModularFeature feature) {
    // minimum signals
    if (minimumSignals > 0) {
      MassList massList = scan.getMassList();
      if (massList == null) {
        throw new MissingMassListException(scan);
      }
      if (massList.getNumberOfDataPoints() < minimumSignals) {
        return false;
      }
    }
    //
    final double precursorMZ;
    if (scan.getMsMsInfo() instanceof MSnInfoImpl msn) {
      precursorMZ = msn.getMS2PrecursorMz();
    } else if (scan.getMsMsInfo() instanceof DDAMsMsInfo info) {
      precursorMZ = info.getIsolationMz();
    } else {
      precursorMZ = Objects.requireNonNullElse(scan.getPrecursorMz(), 0d);
    }
    return matchesRtFilter(scan, feature) && precursorMZ != 0 && mzTol.checkWithinTolerance(
        feature.getMZ(), precursorMZ);
  }

  /**
   * @return true if feature contains no retention time or if all filters match
   */
  private boolean matchesRtFilter(final Scan scan, ModularFeature feature) {
//    (!limitRTByFeature || )
//      && rtTol.checkWithinTolerance(frt, scan.getRetentionTime())
    return switch (rtFilter) {
      case USE_FEATURE_EDGES -> {
        // dont use shorcut as this returns a non null singleton range
//        Range<Float> rtRange = feature.getRawDataPointsRTRange();
        // true if no range means that there was no retention time like in IMS-MS data without time component
        Range<Float> rtRange = feature.get(RTRangeType.class);
        yield rtRange == null || rtRange.contains(scan.getRetentionTime());
      }
      case USE_TOLERANCE -> {
        Float rt = feature.getRT();
        yield rt == null || rtTol.checkWithinTolerance(rt, scan.getRetentionTime());
      }
    };
  }

  /**
   * Process tims features. Merge within Frames and optionally merge across frames
   *
   * @param feature feature from TIMS data
   * @return list of fragmentation scans
   */
  @NotNull
  private List<Scan> findFragmentScansForTimsFeature(ModularFeature feature) {

    float frt = feature.getRT();
    double fmz = feature.getMZ();
    Range<Float> rtRange = feature.getRawDataPointsRTRange();
    Float mobility = feature.getMobility();

    final List<? extends Scan> scans = feature.getRawDataFile().getScanNumbers(2).stream()
        .filter(scan -> matchesRtFilter(scan, feature)).collect(Collectors.toList());

    if (scans.isEmpty() || !(scans.get(0) instanceof Frame)) {
      return List.of();
    }

    final List<Frame> frames = (List<Frame>) scans;
    final List<MsMsInfo> eligibleMsMsInfos = new ArrayList<>();
    for (Frame frame : frames) {
      frame.getImsMsMsInfos().forEach(imsMsMsInfo -> {
        if (mzTol.checkWithinTolerance(fmz, imsMsMsInfo.getIsolationMz())) {
          // if we have a mobility (=processed by IMS workflow), we can check for the correct range during assignment.
          if (mobility != null) {
            // todo: maybe revisit this for a more sophisticated range check
            int mobilityScannumberOffset = frame.getMobilityScan(0).getMobilityScanNumber();
            float mobility1 = (float) frame.getMobilityForMobilityScanNumber(
                imsMsMsInfo.getSpectrumNumberRange().lowerEndpoint() - mobilityScannumberOffset);
            float mobility2 = (float) frame.getMobilityForMobilityScanNumber(
                imsMsMsInfo.getSpectrumNumberRange().upperEndpoint() - mobilityScannumberOffset);
            if (Range.singleton(mobility1).span(Range.singleton(mobility2)).contains(mobility)) {
              eligibleMsMsInfos.add(imsMsMsInfo);
            }
          } else {
            // if we don't have a mobility, we can simply add the msms info.
            eligibleMsMsInfos.add(imsMsMsInfo);
          }
        }
      });
    }

    if (eligibleMsMsInfos.isEmpty()) {
      return List.of();
    }
    feature.set(MsMsInfoType.class, eligibleMsMsInfos);

    List<Scan> msmsSpectra = new ArrayList<>();
    for (MsMsInfo info : eligibleMsMsInfos) {
      Range<Float> mobilityLimits = lockToFeatureMobilityRange && feature.getMobilityRange() != null
          ? feature.getMobilityRange() : null;
      MergedMsMsSpectrum spectrum = SpectraMerging.getMergedMsMsSpectrumForPASEF(
          (PasefMsMsInfo) info, SpectraMerging.pasefMS2MergeTol, IntensityMergingType.SUMMED,
          ((ModularFeatureList) list).getMemoryMapStorage(), mobilityLimits, minMs2IntensityAbs,
          minMs2IntensityRel, null);
      if (spectrum != null) {
        msmsSpectra.add(spectrum);
      }
    }

    if (!msmsSpectra.isEmpty() && combineTimsMS2) {
      return SpectraMerging.mergeMsMsSpectra(msmsSpectra, SpectraMerging.pasefMS2MergeTol,
          IntensityMergingType.SUMMED, ((ModularFeatureList) list).getMemoryMapStorage());
    }
    return msmsSpectra;
  }


  /**
   * remove all scans with less than minimumSignals in mass list
   *
   * @param scans input list, is changed
   */
  private void filterByMinimumSignals(final List<Scan> scans) {
    if (minimumSignals <= 0) {
      return;
    }

    for (final Scan scan : scans) {
      if (scan.getMassList() == null) {
        throw new MissingMassListException(scan);
      }
    }

    scans.removeIf(scan -> scan.getMassList().getNumberOfDataPoints() < minimumSignals);
  }
}
