/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.MobilityUnitType;
import io.github.mzmine.datamodel.impl.MultiChargeStateIsotopePattern;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.IsotopeFinderParameters.ScanRange;
import io.github.mzmine.modules.dataprocessing.id_ccscalc.CCSUtils;
import io.github.mzmine.modules.tools.msmsspectramerge.MergedDataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.IsotopesUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    final ScanDataAccess scans = EfficientDataAccess.of(raw, ScanDataType.MASS_LIST,
        featureList.getSeletedScans(raw));

    final MobilityScanDataAccess mobScans = initMobilityScanDataAccess(raw);

    int missingValues = 0;
    int detected = 0;

    try {
      // find for all rows the isotope pattern
      for (FeatureListRow row : featureList.getRows()) {
        if (isCanceled()) {
          return;
        }

        // start at max intensity signal
        Feature feature = row.getFeature(raw);
        Scan scan = feature.getRepresentativeScan();
        // no MS1 scan available
        if (scan == null) {
          continue;
        }

        double mz = feature.getMZ();
        scan = findBestScanOrMobilityScan(scans, mobScans, feature);

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
          final SimpleDataPoint featureDp = new SimpleDataPoint(mz, feature.getHeight());
          List<DataPoint> candidates = IsotopesUtils.findIsotopesInScan(currentChargeDiffs,
              currentMaxDiff, isoMzTolerance, scan, featureDp);

          if (scan instanceof MobilityScan && !candidates.isEmpty()) {
            candidates = normalizeImsIntensities(candidates, scan, featureDp);
          }

          if (candidates.size() > 1) { // feature itself is always in cadidates
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
          //Final CCS Calculation
          RawDataFile data = feature.getRawDataFile();
          Float mobility = feature.getMobility();
          MobilityType mobilityType = feature.getMobilityUnit();
          if (data instanceof IMSRawDataFile imsfile) {
            if (CCSUtils.hasValidMobilityType(imsfile) && mobility != null && bestCharge > 0 && mobilityType != null) {
              Float ccs = CCSUtils.calcCCS(mz, mobility, mobilityType, bestCharge, imsfile);
              if (ccs != null) {
                feature.setCCS(ccs);
              }
            }
          }//end
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
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Error in isotope finder "+ ex.getMessage(), ex);
      setStatus(TaskStatus.ERROR);
      return;
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

  private List<DataPoint> normalizeImsIntensities(List<DataPoint> candidates, Scan scan,
      SimpleDataPoint featureDp) {
    final int i = scan.binarySearch(featureDp.getMZ(), true);
    if (i < 0) {
      // did not find the expected feature data point
      return candidates;
    }

    final double intensity = scan.getIntensityValue(i);
    final double normalisationFactor = featureDp.getIntensity() / intensity;

    final List<DataPoint> newCandidates = new ArrayList<>(candidates.size());
    for (DataPoint candidate : candidates) {
      if (!candidate.equals(featureDp)) {
        newCandidates.add(
            new SimpleDataPoint(candidate.getMZ(), candidate.getIntensity() * normalisationFactor));
      } else {
        newCandidates.add(featureDp);
      }
    }

    return newCandidates;
  }

  @NotNull
  private Scan findBestScanOrMobilityScan(ScanDataAccess scans, MobilityScanDataAccess mobScans,
      Feature feature) {

    final Scan maxScan = feature.getRepresentativeScan();
    final int scanIndex = scans.indexOf(maxScan);
    scans.jumpToIndex(scanIndex);

    final boolean mobility = feature.getMobility() != null;
    MobilityScan mobilityScan = null;
    if (mobility && mobScans != null) {
      final MobilityScan bestMobilityScan = IonMobilityUtils.getBestMobilityScan(feature);
      if (bestMobilityScan != null) {
        mobilityScan = mobScans.jumpToMobilityScan(bestMobilityScan);
      }
    }

    return mobilityScan != null ? mobScans : scans;
  }

  @Nullable
  private MobilityScanDataAccess initMobilityScanDataAccess(RawDataFile raw) {
    return raw instanceof IMSRawDataFile imsFile && featureList.getFeatureTypes()
        .containsKey(MobilityUnitType.class) ? new MobilityScanDataAccess(imsFile,
        MobilityScanDataType.MASS_LIST, (List<Frame>) featureList.getSeletedScans(imsFile)) : null;
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
