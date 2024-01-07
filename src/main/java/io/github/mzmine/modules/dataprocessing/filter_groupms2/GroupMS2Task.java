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
import io.github.mzmine.datamodel.features.types.numbers.RtMs2ApexDistanceType;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.modules.dataprocessing.filter_groupms2_refine.GroupedMs2RefinementTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.combowithinput.RtLimitsFilter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.FragmentScanSelection.IncludeInputSpectra;
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
 * Groups fragmentation scans with features in range
 */
public class GroupMS2Task extends AbstractTask {

  private static final Logger logger = Logger.getLogger(GroupMS2Task.class.getName());

  private final ParameterSet parameters;
  private final Double minMs2IntensityAbs;
  private final boolean combineTimsMS2;
  private final Double minMs2IntensityRel;
  private final FeatureList list;
  private final MZTolerance mzTol;
  private final boolean lockToFeatureMobilityRange;
  private final int minimumSignals;
  private final Double minimumRelativeFeatureHeight;
  private final int totalRows;
  private final RtLimitsFilter rtFilter;
  private final FragmentScanSelection timsFragmentScanSelection;
  private int processedRows;
  private GroupedMs2RefinementTask refineTask;

  /**
   * Create the task.
   *
   * @param list         feature list to process.
   * @param parameterSet task parameters.
   */
  public GroupMS2Task(final FeatureList list, final ParameterSet parameterSet,
      @NotNull Instant moduleCallDate) {
    super(((ModularFeatureList) list).getMemoryMapStorage(),
        moduleCallDate); // use storage from feature list to store merged ms2 spectra.

    parameters = parameterSet;
    // RT has two options / tolerance is only provided for second option
    rtFilter = parameters.getValue(GroupMS2Parameters.rtFilter);

    mzTol = parameters.getValue(GroupMS2Parameters.mzTol);
    combineTimsMS2 = parameterSet.getValue(GroupMS2Parameters.combineTimsMsMs);
    lockToFeatureMobilityRange = parameterSet.getValue(GroupMS2Parameters.limitMobilityByFeature);
    minMs2IntensityAbs = parameterSet.getEmbeddedParameterValueIfSelectedOrElse(
        GroupMS2Parameters.outputNoiseLevel, null);
    minMs2IntensityRel = parameterSet.getEmbeddedParameterValueIfSelectedOrElse(
        GroupMS2Parameters.outputNoiseLevelRelative, null);

    // if active, only features with min relative height get MS2
    minimumRelativeFeatureHeight = parameterSet.getEmbeddedParameterValueIfSelectedOrElse(
        GroupMS2Parameters.minimumRelativeFeatureHeight, null);

    // 0 is deactivated
    minimumSignals = parameters.getEmbeddedParameterValueIfSelectedOrElse(
        GroupMS2Parameters.minRequiredSignals, 0);

    // only used for tims, keeping input spectra is important for later merging.
    timsFragmentScanSelection = new FragmentScanSelection(SpectraMerging.pasefMS2MergeTol, true,
        IncludeInputSpectra.ALL, IntensityMergingType.MAXIMUM, MsLevelFilter.ALL_LEVELS,
        getMemoryMapStorage());

    this.list = list;
    processedRows = 0;
    totalRows = list.getNumberOfRows();
  }

  @Override
  public double getFinishedPercentage() {
    if (refineTask != null) {
      return refineTask.getFinishedPercentage();
    }
    return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    if (refineTask != null) {
      return refineTask.getTaskDescription();
    }
    return "Grouping MS2 scans to their features in list " + list.getName();
  }

  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);

      processFeatureList(this);
      if (isCanceled()) {
        return;
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

  public void processFeatureList(AbstractTask parentTask) {
    // for all features
    for (FeatureListRow row : list.getRows()) {
      if (parentTask.isCanceled()) {
        return;
      }

      processRow(row);
      processedRows++;
    }

    // refine MS2 groupings with features that are at least X % of the highest feature that was grouped with each MS2
    if (minimumRelativeFeatureHeight != null) {
      refineTask = new GroupedMs2RefinementTask(list, minimumRelativeFeatureHeight, 0d);
      refineTask.processFeatureList(parentTask);
    }
  }

  /**
   * Group all MS2 scans with the corresponding features (per raw data file)
   *
   * @param row does this for each feature in this row
   */
  public void processRow(FeatureListRow row) {
    for (ModularFeature feature : row.getFeatures()) {
      List<Scan> scans;
      if (MobilityType.TIMS.isTypeOfBackingRawData(feature)) {
        scans = findFragmentScansForTimsFeature(feature);
      } else {
        scans = findFragmentScans(feature);
      }

      scans = filterByMinimumSignals(scans);
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
    return rtFilter.accept(feature, scan.getRetentionTime()) && precursorMZ != 0
        && mzTol.checkWithinTolerance(feature.getMZ(), precursorMZ);
  }


  /**
   * Process tims features. Merge within Frames and optionally merge across frames
   *
   * @param feature feature from TIMS data
   * @return list of fragmentation scans
   */
  @NotNull
  private List<Scan> findFragmentScansForTimsFeature(ModularFeature feature) {

    double fmz = feature.getMZ();
    Float mobility = feature.getMobility();

    final List<? extends Scan> scans = feature.getRawDataFile().getScanNumbers(2).stream()
        .filter(scan -> rtFilter.accept(feature, scan.getRetentionTime()))
        .collect(Collectors.toList());

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
          getMemoryMapStorage(), mobilityLimits, minMs2IntensityAbs, minMs2IntensityRel, null);
      if (spectrum != null) {
        msmsSpectra.add(spectrum);
      }
    }

    if (!msmsSpectra.isEmpty() && combineTimsMS2) {
      return timsFragmentScanSelection.getAllFragmentSpectra(msmsSpectra);
    }
    return msmsSpectra;
  }


  /**
   * remove all scans with less than minimumSignals in mass list
   *
   * @param scans returns a filtered list or the input list if no filter is applied
   */
  private List<Scan> filterByMinimumSignals(final List<Scan> scans) {
    if (minimumSignals <= 0) {
      return scans;
    }

    for (final Scan scan : scans) {
      if (scan.getMassList() == null) {
        throw new MissingMassListException(scan);
      }
    }

    return scans.stream()
        .filter(scan -> scan.getMassList().getNumberOfDataPoints() >= minimumSignals).toList();
  }
}
