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

package io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Checks if a precursor isolation range was contaminated with outer signals in an MS1 spectrum -
 * leading to chimeric MS2 spectra
 */
public class ChimericPrecursorChecker {

  private static final Logger logger = Logger.getLogger(ChimericPrecursorChecker.class.getName());


  /**
   * Score if the MS1 spectrum, within the precursor isolation range, contains more signals
   * contaminating an MS2 spectrum
   *
   * @param row             check all fragment scans of this row. Will check the corresponding MS1
   *                        scan
   * @param mainSignalMzTol search for the main signal in this range, should be lower than the
   *                        isolation tolerance, depending on the mass accuracy and resolution
   * @param isolationMzTol  precursor isolation tolerance might be higher than the actually set
   *                        window. Depending on the performance of the instrument
   * @param minimumPurity   minimum precursor purity as main signal intensity/sum intensity
   * @return {@link ChimericPrecursorResults} that describes the precursor purity and adds flags if
   * criteria were matched
   */
  @NotNull
  public static Map<Scan, ChimericPrecursorResults> checkChimericPrecursorIsolation(
      final FeatureListRow row, final MZTolerance mainSignalMzTol, final MZTolerance isolationMzTol,
      final double minimumPurity) {
    return checkChimericPrecursorIsolation(row.getAverageMZ(), row.getAllFragmentScans(),
        mainSignalMzTol, isolationMzTol, minimumPurity);
  }

  /**
   * Score if the MS1 spectrum, within the precursor isolation range, contains more signals
   * contaminating an MS2 spectrum
   *
   * @param precursorMz     the precursor m/z (may be different from the one specifid with the
   *                        scan)
   * @param fragmentScans   the fragment scans to check. Will check the corresponding MS1 scan
   * @param mainSignalMzTol search for the main signal in this range, should be lower than the
   *                        isolation tolerance, depending on the mass accuracy and resolution
   * @param isolationMzTol  precursor isolation tolerance might be higher than the actually set
   *                        window. Depending on the performance of the instrument
   * @param minimumPurity   minimum precursor purity as main signal intensity/sum intensity
   * @return {@link ChimericPrecursorResults} that describes the precursor purity and adds flags if
   * criteria were matched
   */
  @NotNull
  public static Map<Scan, ChimericPrecursorResults> checkChimericPrecursorIsolation(
      final double precursorMz, final List<Scan> fragmentScans, final MZTolerance mainSignalMzTol,
      final MZTolerance isolationMzTol, final double minimumPurity) {
    // all data files from fragment scans to score if is chimeric
    Map<Scan, ChimericPrecursorResults> chimericMap = new HashMap<>();
    for (Scan scan : fragmentScans) {
      chimericMap.computeIfAbsent(scan,
          key -> scoreChimericIsolation(precursorMz, scan, mainSignalMzTol, isolationMzTol,
              minimumPurity));
    }
    return chimericMap;
  }

  /**
   * Score if the MS1 spectrum, within the precursor isolation range, contains more signals
   * contaminating an MS2 spectrum
   *
   * @param fragmentScan    the fragment scan to check. Will check the corresponding MS1 scan
   * @param mainSignalMzTol search for the main signal in this range, should be lower than the
   *                        isolation tolerance, depending on the mass accuracy and resolution
   * @param isolationMzTol  precursor isolation tolerance might be higher than the actually set
   *                        window. Depending on the performance of the instrument
   * @param minimumPurity   minimum precursor purity as main signal intensity/sum intensity
   * @return {@link ChimericPrecursorResults} that describes the precursor purity and adds flags if
   * criteria were matched
   */
  @NotNull
  public static ChimericPrecursorResults scoreChimericIsolation(final double precursorMz,
      final Scan fragmentScan, final MZTolerance mainSignalMzTol, final MZTolerance isolationMzTol,
      final double minimumPurity) {
    // retrieve preceding ms1 scan
    final Scan ms1;
    if (fragmentScan instanceof MergedMsMsSpectrum msms) {
      ms1 = ScanUtils.findPrecursorScanForMerged(msms, SpectraMerging.defaultMs1MergeTol);
    } else {
      ms1 = ScanUtils.findPrecursorScan(fragmentScan);
    }

    if (ms1 == null) {
      // maybe there was no MS1 before that?
      logger.finest(() -> String.format("Could not find MS1 before this scan: %s", fragmentScan));
      return ChimericPrecursorResults.MISSING_MS1_SCAN;
    }

    MassList massList = ms1.getMassList();
    if (massList == null) {
      throw new MissingMassListException(ms1);
    }

    // check for signals in isolation range
    return ChimericPrecursorChecker.checkMs1(massList, precursorMz, mainSignalMzTol, isolationMzTol,
        minimumPurity);
  }


  /**
   * Score if the MS1 spectrum, within the precursor isolation range, contains more signals
   * contaminating an MS2 spectrum
   *
   * @param ms1             the MS1 scan where the precursor was isolated from
   * @param precursorMz     the precursor ion m/z that was isolated. The center
   * @param mainSignalMzTol search for the main signal in this range, should be lower than the
   *                        isolation tolerance, depending on the mass accuracy and resolution
   * @param isolationMzTol  precursor isolation tolerance might be higher than the actually set
   *                        window. Depending on the performance of the instrument
   * @param minimumPurity   minimum precursor purity as main signal intensity/sum intensity
   * @return {@link ChimericPrecursorResults} that describes the precursor purity and adds flags if
   * criteria were matched
   */
  @NotNull
  public static ChimericPrecursorResults checkMs1(final MassList ms1, final double precursorMz,
      MZTolerance mainSignalMzTol, MZTolerance isolationMzTol, final double minimumPurity) {

    // find starting point in spectrum
    int index = ms1.binarySearch(precursorMz, true);
    if (index == -1) {
      return ChimericPrecursorResults.MISSING_MAIN_SIGNAL;
    }

    Range<Double> mainMzRange = mainSignalMzTol.getToleranceRange(precursorMz);
    //  isolation window should be larger than main mz range
    Range<Double> isolationMzRange = isolationMzTol.getToleranceRange(precursorMz)
        .span(mainMzRange);

    double mainMz = -1;
    double mainIntensity = 0;
    double sumIntensity = 0;
    // left side
    for (int i = index; i >= 0; i--) {
      double mz = ms1.getMzValue(i);
      if (mz < isolationMzRange.lowerEndpoint()) {
        break;
      }

      double intensity = ms1.getIntensityValue(i);
      if (mainMzRange.contains(mz) && (mainMz < 0 || intensity > mainIntensity)) {
        mainMz = ms1.getMzValue(i);
        mainIntensity = intensity;
      }
      if (isolationMzRange.contains(mz)) {
        sumIntensity += intensity;
      }
    }
    // right side
    for (int i = index + 1; i < ms1.getNumberOfDataPoints(); i++) {
      double mz = ms1.getMzValue(i);
      if (mz > isolationMzRange.upperEndpoint()) {
        break;
      }
      double intensity = ms1.getIntensityValue(i);
      if (mainMzRange.contains(mz) && (mainMz < 0 || intensity > mainIntensity)) {
        mainMz = ms1.getMzValue(i);
        mainIntensity = intensity;
      }
      if (isolationMzRange.contains(mz)) {
        sumIntensity += intensity;
      }
    }
    //
    if (mainMz < 0) {
      logger.finest(
          "No signal found in main mz range during chimerics detection for " + precursorMz);
      return ChimericPrecursorResults.MISSING_MAIN_SIGNAL;
    }

    double purity = mainIntensity / sumIntensity;
    var flag =
        purity < minimumPurity ? ChimericPrecursorFlag.CHIMERIC : ChimericPrecursorFlag.PASSED;
    return new ChimericPrecursorResults(purity, flag);
  }
}
