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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
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
import io.github.mzmine.datamodel.impl.MultiChargeStateIsotopePattern;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.IsotopeFinderParameters.ScanRange;
import io.github.mzmine.modules.tools.msmsspectramerge.MergedDataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.IsotopesUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.time.Instant;
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
  private final String isotopes;
  private final ScanRange scanRange;
  private int processedRows, totalRows;

  IsotopeFinderTask(MZmineProject project, ModularFeatureList featureList, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(featureList.getMemoryMapStorage(), moduleCallDate);

    this.featureList = featureList;
    this.parameters = parameters;

    isotopeElements = parameters.getValue(IsotopeFinderParameters.elements);
    scanRange = parameters.getValue(IsotopeFinderParameters.scanRange);
    isotopeMaxCharge = parameters.getValue(IsotopeFinderParameters.maxCharge);
    isoMzTolerance = parameters.getValue(IsotopeFinderParameters.isotopeMzTolerance);
    isotopes = isotopeElements.stream().map(Objects::toString).collect(Collectors.joining(","));
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
    DoubleArrayList[] isoMzDiffsForCharge = IsotopesUtils.getIsotopesMzDiffsForCharge(
        isotopeElements, isotopeMaxCharge);
    if (isoMzDiffsForCharge.length == 0 || isoMzDiffsForCharge[0].isEmpty()) {
      setErrorMessage("No isotopes found for elements: " + isotopes);
      setStatus(TaskStatus.ERROR);
      return;
    }
    // get maximum difference per charge state
    double[] maxIsoMzDiff = new double[isotopeMaxCharge];
    for (int i = 0; i < isotopeMaxCharge; i++) {
      for (double diff : isoMzDiffsForCharge[i]) {
        if (diff > maxIsoMzDiff[i]) {
          maxIsoMzDiff[i] = diff;
        }
      }
      // add some to the max diff to include more search space
      maxIsoMzDiff[i] += 10 * isoMzTolerance.getMzToleranceForMass(maxIsoMzDiff[i]);
    }

    // start processing
    totalRows = featureList.getNumberOfRows();
    processedRows = 0;
    RawDataFile raw = featureList.getRawDataFile(0);

    // Loop through all rows
    ScanDataAccess scans = EfficientDataAccess.of(raw, ScanDataType.CENTROID,
        featureList.getSeletedScans(raw));

    int missingValues = 0;
    int detected = 0;

    // find for all rows the isotope pattern
    for (FeatureListRow row : featureList.getRows()) {
      if (isCanceled()) {
        return;
      }

      // start at max intensity signal
      Feature feature = row.getFeature(raw);
      double mz = feature.getMZ();
      Scan maxScan = feature.getRepresentativeScan();
      int scanIndex = scans.indexOf(maxScan);
      scans.jumpToIndex(scanIndex);

      // find candidate isotope pattern in max scan
      // for each charge state to determine best charge
      // merge afterward to get one isotope patten with all possible isotopes
      int maxFoundIsotopes = 0;
      int bestCharge = 0;
      IsotopePattern pattern = null;

      for (int i = 0; i < isotopeMaxCharge; i++) {
        // charge is zero indexed but always starts at 1 -> max charge
        final int charge = i + 1;
        final DoubleArrayList currentChargeDiffs = isoMzDiffsForCharge[i];
        final double currentMaxDiff = maxIsoMzDiff[i];
        List<DataPoint> candidates = IsotopesUtils.findIsotopesInScan(currentChargeDiffs,
            currentMaxDiff, isoMzTolerance, scans, new SimpleDataPoint(mz, feature.getHeight()));

        if (!candidates.isEmpty()) {
          IsotopePattern newPattern = new SimpleIsotopePattern(candidates.toArray(new DataPoint[0]),
              charge, IsotopePatternStatus.DETECTED, IsotopeFinderModule.MODULE_NAME);
          if (pattern == null) {
            pattern = newPattern;
          } else if (pattern instanceof SimpleIsotopePattern) {
            // combine 2 isotope pattern
            pattern = new MultiChargeStateIsotopePattern(pattern, newPattern);
          } else if (pattern instanceof MultiChargeStateIsotopePattern multi) {
            // add next patterns
            multi.addPattern(newPattern);
          } else {
            throw new IllegalStateException("Isotope pattern type is not handled.");
          }

          if (candidates.size() > maxFoundIsotopes) {
            maxFoundIsotopes = candidates.size();
            // charge is zero indexed but always starts at 1 -> max charge
            bestCharge = charge;
          }
        }
      }
      if (pattern == null) {
        // no pattern found
        continue;
      }

      if (scanRange == ScanRange.SINGLE_MOST_INTENSE) {
        // add isotope pattern and charge
        feature.setIsotopePattern(pattern);
        feature.setCharge(bestCharge);
        detected++;
      } else {
        // find pattern in FWHM
        //      Float fwhmDiff = feature.getFWHM();
        //      if (fwhmDiff != null) {
        //        fwhmDiff /= 2f;
        //
        //        if (candidates.size() > 1) {
        //          int next = 1;
        //          while (scanIndex + next < totalScans || scanIndex - next >= 0) {
        //            if (scanIndex + next < totalScans) {
        //              scans.jumpToIndex(scanIndex + next);
        //              if (checkRetentionTime(scans.getCurrentScan(), maxRT, fwhmDiff)) {
        //                checkCandidatesInScan(scans, candidates);
        //              }
        //            }
        //            if (scanIndex - next >= 0) {
        //              scans.jumpToIndex(scanIndex - next);
        //              if (checkRetentionTime(scans.getCurrentScan(), maxRT, fwhmDiff)) {
        //                checkCandidatesInScan(scans, candidates);
        //              }
        //            }
        //            next++;
        //          }
        //        }
        //        // all scans in FWHMN checked... add isotope pattern
        //        if (candidates.size() > 1) {
        //          feature.setIsotopePattern(new SimpleIsotopePattern(
        //              candidates.stream().map(d -> new SimpleDataPoint(d.getMZ(), d.getIntensity()))
        //                  .toArray(DataPoint[]::new), IsotopePatternStatus.DETECTED, "Pattern finder"));
        //          detected++;
        //        }
        //      } else {
        //        // missing FWHM
        //        missingValues++;
        //      }
      }
      processedRows++;
    }

    if (missingValues > 0) {
      logger.info(String.format("There were %d missing FWHM values in %d features", missingValues,
          totalRows));
    }
    if (detected > 0) {
      logger.info(String.format("Found %d isotope pattern in %s", detected, featureList));
    }
    // Add task description to peakList
    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Isotope finder module", IsotopeFinderModule.class,
            parameters, getModuleCallDate()));

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

}
