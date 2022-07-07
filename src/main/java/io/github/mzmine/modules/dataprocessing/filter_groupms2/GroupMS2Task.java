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

package io.github.mzmine.modules.dataprocessing.filter_groupms2;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
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
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.FragmentScanSorter;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.MergingType;
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

  // Logger.
  private static final Logger logger = Logger.getLogger(GroupMS2Task.class.getName());
  // Feature lists.
  private final MZmineProject project;
  // Parameters.
  private final ParameterSet parameters;
  private final Double minMs2Intensity;
  private final boolean combineTimsMS2;
  // Processed rows counter
  private int processedRows, totalRows;
  private FeatureList list;
  private RTTolerance rtTol;
  private MZTolerance mzTol;
  private boolean limitRTByFeature;
  private boolean lockToFeatureMobilityRange;

  /**
   * Create the task.
   *
   * @param list         feature list to process.
   * @param parameterSet task parameters.
   */
  public GroupMS2Task(final MZmineProject project, final FeatureList list,
      final ParameterSet parameterSet, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    // Initialize.
    this.project = project;
    parameters = parameterSet;
    rtTol = parameters.getParameter(GroupMS2Parameters.rtTol).getValue();
    mzTol = parameters.getParameter(GroupMS2Parameters.mzTol).getValue();
    limitRTByFeature = parameters.getParameter(GroupMS2Parameters.limitRTByFeature).getValue();
    combineTimsMS2 = parameterSet.getParameter(GroupMS2Parameters.combineTimsMsMs).getValue();
    lockToFeatureMobilityRange = parameterSet.getParameter(
        GroupMS2Parameters.lockMS2ToFeatureMobilityRange).getValue();
    minMs2Intensity = parameterSet.getParameter(GroupMS2Parameters.outputNoiseLevel).getValue()
        ? parameterSet.getParameter(GroupMS2Parameters.outputNoiseLevel).getEmbeddedParameter()
        .getValue() : null;
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
    for (ModularFeature f : row.getFeatures()) {
      if (f != null && f.getFeatureStatus() != FeatureStatus.UNKNOWN && (
          f.getMobilityUnit() == io.github.mzmine.datamodel.MobilityType.TIMS || (
              f.getRawDataFile() instanceof IMSRawDataFile imsfile
                  && imsfile.getMobilityType() == MobilityType.TIMS))) {
        processTimsFeature(f);
      } else if (f != null && !f.getFeatureStatus().equals(FeatureStatus.UNKNOWN)) {
        RawDataFile raw = f.getRawDataFile();
        float frt = f.getRT();
        double fmz = f.getMZ();
        Range<Float> rtRange = f.getRawDataPointsRTRange();

        List<Scan> scans = raw.stream().filter(scan -> scan.getMSLevel() > 1)
            .filter(scan -> filterScan(scan, frt, fmz, rtRange))
            .sorted(FragmentScanSorter.DEFAULT_TIC).toList();

        // set list to feature and sort
        f.setAllMS2FragmentScans(scans);
      }
    }
  }

  private boolean filterScan(Scan scan, float frt, double fmz, Range<Float> featureRtRange) {
    final double precursorMZ;
    if (scan.getMsMsInfo() instanceof MSnInfoImpl msn) {
      precursorMZ = msn.getMS2PrecursorMz();
    } else if (scan.getMsMsInfo() instanceof DDAMsMsInfo info) {
      precursorMZ = info.getIsolationMz();
    } else {
      precursorMZ = Objects.requireNonNullElse(scan.getPrecursorMz(), 0d);
    }
    return (!limitRTByFeature || featureRtRange.contains(scan.getRetentionTime()))
           && rtTol.checkWithinTolerance(frt, scan.getRetentionTime()) && precursorMZ != 0
           && mzTol.checkWithinTolerance(fmz, precursorMZ);
  }

  private void processTimsFeature(ModularFeature feature) {

    float frt = feature.getRT();
    double fmz = feature.getMZ();
    Range<Float> rtRange = feature.getRawDataPointsRTRange();
    Float mobility = feature.getMobility();

    final List<? extends Scan> scans = feature.getRawDataFile().getScanNumbers(2).stream().filter(
            scan -> (!limitRTByFeature && rtTol.checkWithinTolerance(frt, scan.getRetentionTime())) || (
                limitRTByFeature && rtRange.contains(scan.getRetentionTime())))
        .collect(Collectors.toList());

    if (scans.isEmpty() || !(scans.get(0) instanceof Frame)) {
      return;
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
      return;
    }
    feature.set(MsMsInfoType.class, eligibleMsMsInfos);

    List<MergedMsMsSpectrum> msmsSpectra = new ArrayList<>();
    for (MsMsInfo info : eligibleMsMsInfos) {
      MergedMsMsSpectrum spectrum = SpectraMerging.getMergedMsMsSpectrumForPASEF(
          (PasefMsMsInfo) info, SpectraMerging.pasefMS2MergeTol, MergingType.SUMMED,
          ((ModularFeatureList) list).getMemoryMapStorage(),
          lockToFeatureMobilityRange && feature.getMobilityRange() != null
              ? feature.getMobilityRange() : null, minMs2Intensity);
      if (spectrum != null) {
        msmsSpectra.add(spectrum);
      }
    }

    if (!msmsSpectra.isEmpty()) {
      if (combineTimsMS2) {
        List<Scan> sameCEMerged = SpectraMerging.mergeMsMsSpectra(msmsSpectra,
            SpectraMerging.pasefMS2MergeTol, MergingType.SUMMED,
            ((ModularFeatureList) list).getMemoryMapStorage());
        feature.setAllMS2FragmentScans(sameCEMerged, true);

      } else {
        feature.setAllMS2FragmentScans((List<Scan>) (List<? extends Scan>) msmsSpectra, true);
      }
    }
  }
}
