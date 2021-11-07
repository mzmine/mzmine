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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.modules.tools.msmsspectramerge.IntensityMergeMode;
import io.github.mzmine.modules.tools.msmsspectramerge.MergedDataPoint;
import io.github.mzmine.modules.tools.msmsspectramerge.MzMergeMode;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.IsotopesUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.Element;

/**
 *
 */
class IsotopeFinderTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(IsotopeFinderTask.class.getName());
  private final ModularFeatureList featureList;
  // parameter values
  private final ParameterSet parameters;
  private final MZTolerance isoMzTolerance;
  private final int isotopeMaxCharge;
  private final List<Element> isotopeElements;

  private int processedRows, totalRows;

  IsotopeFinderTask(MZmineProject project, ModularFeatureList featureList, ParameterSet parameters,
      @NotNull Date moduleCallDate) {
    super(featureList.getMemoryMapStorage(), moduleCallDate);

    this.featureList = featureList;
    this.parameters = parameters;

    isotopeElements = parameters.getParameter(IsotopeFinderParameters.elements).getValue();
    isotopeMaxCharge = parameters.getParameter(IsotopeFinderParameters.maxCharge).getValue();
    isoMzTolerance = parameters.getParameter(IsotopeFinderParameters.isotopeMzTolerance)
        .getValue();
  }

  @Override
  public String getTaskDescription() {
    return "Isotope pattern finder on " + featureList;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0.0d;
    }
    return (double) processedRows / (double) totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running isotope pattern finder on " + featureList);

    // We assume source peakList contains one datafile
    if (featureList.getRawDataFiles().size() > 1) {
      setErrorMessage("Cannot perform isotope finder on aligned feature list.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    // Update isotopesMzDiffs
    List<Double> isoMzDiffs = IsotopesUtils.getIsotopesMzDiffs(isotopeElements, isotopeMaxCharge);
    if (isoMzDiffs.isEmpty()) {
      setErrorMessage(
          "No isotopes found for elements: " + isotopeElements.stream().map(Objects::toString)
              .collect(
                  Collectors.joining(",")));
      setStatus(TaskStatus.ERROR);
      return;
    }
    double maxIsoMzDiff = Collections.max(isoMzDiffs);
    // add some to the max diff to include more search space
    maxIsoMzDiff += 10 * isoMzTolerance.getMzToleranceForMass(maxIsoMzDiff);

    // Loop through all peaks
    totalRows = featureList.getNumberOfRows();
    processedRows = 0;

    RawDataFile raw = featureList.getRawDataFile(0);
    ScanDataAccess scans = EfficientDataAccess
        .of(raw, ScanDataType.CENTROID, featureList.getSeletedScans(raw));
    final int totalScans = scans.getNumberOfScans();

    int missingValues = 0;
    int detected = 0;

    // find for all rows the isotope pattern
    for (FeatureListRow row : featureList.getRows()) {
      if (isCanceled()) {
        return;
      }
      // find pattern in FWHM
      Feature feature = row.getFeature(raw);
      double mz = feature.getMZ();
      Float fwhmDiff = feature.getFWHM();
      if (fwhmDiff != null) {
        fwhmDiff /= 2f;
        // start at max intensity signal
        Scan maxScan = feature.getRepresentativeScan();
        float maxRT = maxScan.getRetentionTime();
        int scanIndex = scans.indexOf(maxScan);
        scans.jumpToIndex(scanIndex);

        // find candidate isotope pattern in max scan
        List<MergedDataPoint> candidates = findCandidates(isoMzDiffs, maxIsoMzDiff, scans,
            new MergedDataPoint(MzMergeMode.WEIGHTED_AVERAGE, IntensityMergeMode.MAXIMUM,
                new SimpleDataPoint(mz, feature.getHeight())));

        if (candidates.size() > 1) {
          int next = 1;
          while (scanIndex + next < totalScans || scanIndex - next >= 0) {
            if (scanIndex + next < totalScans) {
              scans.jumpToIndex(scanIndex + next);
              if (checkRetentionTime(scans.getCurrentScan(), maxRT, fwhmDiff)) {
                checkCandidatesInScan(scans, candidates);
              }
            }
            if (scanIndex - next >= 0) {
              scans.jumpToIndex(scanIndex - next);
              if (checkRetentionTime(scans.getCurrentScan(), maxRT, fwhmDiff)) {
                checkCandidatesInScan(scans, candidates);
              }
            }
            next++;
          }
        }
        // all scans in FWHMN checked... add isotope pattern
        if (candidates.size() > 1) {
          feature.setIsotopePattern(new SimpleIsotopePattern(
              candidates.stream().map(d -> new SimpleDataPoint(d.getMZ(), d.getIntensity()))
                  .toArray(DataPoint[]::new), IsotopePatternStatus.DETECTED, "Pattern finder"));
          detected++;
        }
      } else {
        // missing FWHM
        missingValues++;
      }
      processedRows++;
    }

    if (missingValues > 0) {
      logger.info(String
          .format("There were %d missing FWHM values in %d features", missingValues, totalRows));
    }
    if (detected > 0) {
      logger.info(String.format("Found %d isotope pattern in %s", detected, featureList));
    }
    // Add task description to peakList
    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Isotope finder module",
            IsotopeFinderModule.class, parameters, getModuleCallDate()));

    logger.info("Finished isotope pattern finder on " + featureList);
    setStatus(TaskStatus.FINISHED);
  }

  private void checkCandidatesInScan(ScanDataAccess scans, List<MergedDataPoint> candidates,
      double maxIsoMzDiff) {
    double lastMZ = candidates.get(candidates.size() - 1).getMZ() + maxIsoMzDiff;
    double mz = 0;
    int index = 0;
    double currentMZ = candidates.get(index).getMZ();
    for (int dp = 0; dp < scans.getNumberOfDataPoints() && mz <= lastMZ; dp++) {
      mz = scans.getMzValue(dp);
      if (isoMzTolerance.checkWithinTolerance(mz, currentMZ)) {
        // check intensity and
        // use relative height
      }
    }
  }

  /**
   * Check if scan within fwhm difference
   *
   * @param scan     the current scan
   * @param maxRT    retention time of highest data point of feature
   * @param fwhmDiff the half of FWHM
   * @return true if within range
   */
  private boolean checkRetentionTime(Scan scan, float maxRT, Float fwhmDiff) {
    return scan != null && Math.abs(scan.getRetentionTime() - maxRT) <= fwhmDiff;
  }

  private List<DataPoint> findCandidates(List<Double> isoMzDiffs, double maxIsoMzDiff,
      ScanDataAccess scans, DataPoint target) {
    List<DataPoint> candidates = new ArrayList<>();
    candidates.add(target);
    double mz = target.getMZ();
    double lastMZ = mz;
    for (int dp = 0; dp < scans.getNumberOfDataPoints() && mz <= lastMZ + maxIsoMzDiff; dp++) {
      mz = scans.getMzValue(dp);
      if (IsotopesUtils.isPossibleIsotopeMz(mz, candidates, isoMzDiffs, isoMzTolerance)) {
        candidates.add(new SimpleDataPoint(mz, scans.getIntensityValue(dp)));
        lastMZ = mz;
      }
    }
    return candidates;
  }

}
