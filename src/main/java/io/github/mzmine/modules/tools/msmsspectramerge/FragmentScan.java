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

package io.github.mzmine.modules.tools.msmsspectramerge;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * An MS/MS scan with some statistics about its precursor in MS
 */
class FragmentScan {

  private static final double CHIMERIC_INTENSITY_THRESHOLD = 0.1d;
  /**
   * The raw data file this scans are derived from
   */
  protected final RawDataFile origin;

  /**
   * The feature this scans are derived from
   */
  protected final Feature feature;

  /**
   * the MS1 scan that comes before the first MS/MS
   */
  protected final Scan ms1ScanNumber;
  /**
   * the MS1 scan that comes after the last MS/MS
   */
  protected final Scan ms1SucceedingScanNumber;
  /**
   * all consecutive(!) MS/MS scans. There should ne no other MS1 scan between them
   */
  protected final Scan[] ms2ScanNumbers;
  /**
   * the intensity of the precursor peak in MS (left or right from MS/MS scans)
   */
  protected double precursorIntensityLeft, precursorIntensityRight;
  /**
   * the sumed up intensity of chimeric peaks (left or right from MS/MS scans)
   */
  protected double chimericIntensityLeft, chimericIntensityRight;

  /**
   * precursor charge of fragment scan
   */
  protected int precursorCharge;
  private PolarityType polarity;

  static FragmentScan[] getAllFragmentScansFor(Feature feature,
      Range<Double> isolationWindow, MZTolerance massAccuracy) {
    final RawDataFile file = feature.getRawDataFile();
    final Scan[] ms2 = feature.getAllMS2FragmentScans().stream()
        .sorted(Comparator.comparingInt(Scan::getScanNumber)).toArray(Scan[]::new).clone();
    final List<FragmentScan> fragmentScans = new ArrayList<>();
    // search for ms1 scans
    int i = 0;
    while (i < ms2.length) {
      Scan scan = ms2[i];
      Scan precursorScan = scan instanceof MergedMsMsSpectrum ?
          ScanUtils.findPrecursorScanForMerged((MergedMsMsSpectrum) scan, massAccuracy)
          : ScanUtils.findPrecursorScan(scan);
      Scan precursorScan2 = scan instanceof MergedMsMsSpectrum ?
          ScanUtils.findSucceedingPrecursorScanForMerged((MergedMsMsSpectrum) scan, massAccuracy)
          : ScanUtils.findSucceedingPrecursorScan(scan);

      int j = precursorScan2 == null ? ms2.length
          : Arrays.binarySearch(ms2, precursorScan2);
      if (j < 0)
        j = -j - 1;
      final Scan[] subms2 = new Scan[j - i];
      for (int k = i; k < j; ++k)
        subms2[k - i] = ms2[k];

      fragmentScans.add(new FragmentScan(file, feature, precursorScan, precursorScan2, subms2,
          isolationWindow, massAccuracy));
      i = j;
    }
    return fragmentScans.toArray(new FragmentScan[0]);
  }

  FragmentScan(RawDataFile origin, Feature feature, Scan ms1ScanNumber,
      Scan ms1ScanNumber2, Scan[] ms2ScanNumbers, Range<Double> isolationWindow,
      MZTolerance massAccuracy) {
    this.origin = origin;
    this.feature = feature;
    this.ms1ScanNumber = ms1ScanNumber;
    this.ms1SucceedingScanNumber = ms1ScanNumber2;
    this.ms2ScanNumbers = ms2ScanNumbers;
    double[] precInfo = new double[2];
    if (ms1ScanNumber != null) {
      detectPrecursor(ms1ScanNumber, feature.getMZ(), isolationWindow, massAccuracy, precInfo);
      this.precursorIntensityLeft = precInfo[0];
      this.chimericIntensityLeft = precInfo[1];
    } else {
      this.precursorIntensityLeft = 0d;
      this.chimericIntensityLeft = 0d;
    }
    if (ms1SucceedingScanNumber != null) {
      detectPrecursor(ms1SucceedingScanNumber, feature.getMZ(), isolationWindow, massAccuracy,
          precInfo);
      this.precursorIntensityRight = precInfo[0];
      this.chimericIntensityRight = precInfo[1];
    } else {
      this.precursorIntensityRight = 0d;
      this.chimericIntensityRight = 0d;
    }
  }

  /**
   * interpolate the precursor intensity and chimeric intensity of the MS1 scans linearly by
   * retention time to estimate this values for the MS2 scans
   *
   * @return two arrays, one for precursor intensities, one for chimeric intensities, for all MS2
   *         scans
   */
  protected double[][] getInterpolatedPrecursorAndChimericIntensities() {
    final double[][] values = new double[2][ms2ScanNumbers.length];
    if (ms1ScanNumber == null) {
      Arrays.fill(values[0], precursorIntensityRight);
      Arrays.fill(values[1], chimericIntensityRight);
    } else if (ms1SucceedingScanNumber == null) {
      Arrays.fill(values[0], precursorIntensityLeft);
      Arrays.fill(values[1], chimericIntensityLeft);
    } else {
      Scan left = ms1ScanNumber;
      Scan right = ms1SucceedingScanNumber;
      for (int k = 0; k < ms2ScanNumbers.length; ++k) {
        Scan ms2 = ms2ScanNumbers[k];
        float rtRange = (ms2.getRetentionTime() - left.getRetentionTime())
            / (right.getRetentionTime() - left.getRetentionTime());
        if (rtRange >= 0 && rtRange <= 1) {
          values[0][k] =
              (1d - rtRange) * precursorIntensityLeft + (rtRange) * precursorIntensityRight;
          values[1][k] =
              (1d - rtRange) * chimericIntensityLeft + (rtRange) * chimericIntensityRight;
        } else {
          Logger.getLogger(FragmentScan.class.getName())
              .warning("Retention time is non-monotonic within scan numbers.");
          values[0][k] = precursorIntensityLeft + precursorIntensityRight;
          values[1][k] = chimericIntensityLeft + chimericIntensityRight;
        }
      }
    }
    return values;
  }

  /**
   * search for precursor peak in MS1
   */
  private void detectPrecursor(Scan spectrum, double precursorMass, Range<Double> isolationWindow,
      MZTolerance massAccuracy, double[] precInfo) {
    this.precursorCharge = Objects.requireNonNullElse(spectrum.getPrecursorCharge(), 0);

    this.polarity = spectrum.getPolarity();
    Range<Double> mzRange = Range.closed(precursorMass + isolationWindow.lowerEndpoint(),
        precursorMass + isolationWindow.upperEndpoint());
    DataPoint[] dps =
        ScanUtils.selectDataPointsByMass(ScanUtils.extractDataPoints(spectrum), mzRange);
    // for simplicity, just use the most intense peak within massAccuracy
    // range
    int bestPeak = -1;
    double highestIntensity = 0d;
    for (int mppm = 1; mppm < 3; ++mppm) {
      final double maxDiff = massAccuracy.getMzToleranceForMass(precursorMass) * mppm;
      for (int i = 0; i < dps.length; ++i) {
        final DataPoint p = dps[i];
        if (p.getIntensity() <= highestIntensity)
          continue;
        final double mzdiff = Math.abs(p.getMZ() - precursorMass);
        if (mzdiff <= maxDiff) {
          highestIntensity = p.getIntensity();
          bestPeak = i;
        }
      }
      if (bestPeak >= 0)
        break;
    }
    // now sum up all remaining intensities. Leave out isotopes. leave out
    // peaks with intensity below 10%
    // of the precursor. They won't contaminate fragment scans anyways
    precInfo[0] = highestIntensity;
    precInfo[1] = 0d;
    final double threshold = highestIntensity * CHIMERIC_INTENSITY_THRESHOLD;
    foreachpeak: for (int i = 0; i < dps.length; ++i) {
      if (i != bestPeak && dps[i].getIntensity() > threshold) {
        // check for isotope peak
        final double maxDiff = massAccuracy.getMzToleranceForMass(precursorMass) + 0.03;
        for (int k = 1; k < 5; ++k) {
          final double isoMz = precursorMass + k * 1.0015;
          final double diff = isoMz - dps[i].getMZ();
          if (Math.abs(diff) <= maxDiff) {
            continue foreachpeak;
          } else if (diff > 0.5) {
            break;
          }
        }
        precInfo[1] += dps[i].getIntensity();
      }
    }
  }
}
