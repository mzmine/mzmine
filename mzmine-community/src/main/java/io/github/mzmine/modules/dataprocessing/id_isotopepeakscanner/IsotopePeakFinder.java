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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.MergedMassSpectrum.MergingType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class IsotopePeakFinder {


  private final Map<RawDataFile, ScanDataAccess> dataAccessMap = new HashMap<>();

  /**
   * @param featureList
   * @param row
   * @param calculatedDataPoints
   * @param mzTolerance
   * @param minHeight
   * @param mzRangeOfPattern
   * @param resolvedMobility
   * @param charge
   * @return the isotope signals found that match the isotope distribution searched for
   */

  public IsotopePattern detectedIsotopePattern (FeatureList featureList, FeatureListRow row,
      DataPoint [] calculatedDataPoints, MZTolerance mzTolerance, double minHeight, Range<Double> mzRangeOfPattern,
      boolean resolvedMobility, int charge){
    DataPoint[] detectedPatternDPs = searchForIsotopePatternDataPoints(featureList, row, calculatedDataPoints,
        mzTolerance, minHeight, mzRangeOfPattern, resolvedMobility);
    return  new SimpleIsotopePattern(detectedPatternDPs, charge,
        IsotopePatternStatus.DETECTED, "");
  }


  // Scanning for isotope signals in MS1Scan or MobilityScan.
  // Takes the signal with the highest intensity within the mass range.

  public DataPoint[] searchForIsotopePatternDataPoints (FeatureList featureList, FeatureListRow row,
      DataPoint [] calculatedDataPoints, MZTolerance mzTolerance, double minHeight, Range<Double> mzRangeOfPattern,
      boolean resolvedMobility){

    var ms1Scan = row.getBestFeature().getRepresentativeScan();

    final DataPoint[] ms1ScanPattern = new DataPoint[calculatedDataPoints.length];
    DataPoint[] detectedDps;
    RawDataFile raw = featureList.getRawDataFile(0);

    ScanDataAccess scans = dataAccessMap.computeIfAbsent(raw,
        r -> EfficientDataAccess.of(raw, ScanDataType.MASS_LIST, featureList.getSeletedScans(raw)));

    if (ms1Scan != null) {
      if (resolvedMobility) {
        ms1Scan = findBestScanOrMobilityScan(Objects.requireNonNull(row.getFeature(raw)),
            mzTolerance);
      }

      MassList massList = ms1Scan.getMassList();
      if (massList == null) {
        throw new MissingMassListException(ms1Scan);
      }

      DataPoint[] allData = ScanUtils.extractDataPoints(ms1Scan, true);

      detectedDps = ScanUtils.getDataPointsByMass(allData, mzRangeOfPattern);
    } else {
      return null;
    }

    for (int i = 0; i < calculatedDataPoints.length; i++) {
      DataPoint dp = calculatedDataPoints[i];
      for (DataPoint detectedDp : detectedDps) {
        if (mzTolerance.checkWithinTolerance(dp.getMZ(), detectedDp.getMZ())
            && detectedDp.getIntensity() > minHeight) {
          if (ms1ScanPattern[i] == null) {
            ms1ScanPattern[i] = detectedDp;
          } else if (detectedDp.getIntensity() > ms1ScanPattern[i].getIntensity()) {
            ms1ScanPattern[i] = detectedDp;
          }
        }
      }
    }
    for (int i = 0; i < ms1ScanPattern.length; i++) {
      DataPoint isotope = ms1ScanPattern[i];
      if (isotope == null) {
        SimpleDataPoint nullPoint = new SimpleDataPoint(calculatedDataPoints[i].getMZ(), 0);
        ms1ScanPattern[i] = nullPoint;
      }
    }
    return ms1ScanPattern;
  }

  /**
   * @param feature
   * @param mzTolerance
   * @return Scan in which the isotope signals are searched for; in the case of mobility-resolved data,
   * a merged mobility scan is used
   */
  private static Scan findBestScanOrMobilityScan(Feature feature,
      MZTolerance mzTolerance) {

    final boolean mobility = feature.getMobility() != null;
    final IonTimeSeries<? extends Scan> featureData = feature.getFeatureData();
    if (mobility && featureData instanceof IonMobilogramTimeSeries imsData) {
      MergedMassSpectrum mergedMobilityScan = null;
      final Range<Float> mobilityFWHM = IonMobilityUtils.getMobilityFWHM(imsData.getSummedMobilogram());
      final List<MobilityScan> mobilityScans = imsData.getMobilograms().stream()
          .flatMap(s -> (s.getSpectra().stream())).filter(m -> {
            assert mobilityFWHM != null;
            return mobilityFWHM.contains((float) m.getMobility());
          }).toList();

      if (!mobilityScans.isEmpty()) {
        mergedMobilityScan = SpectraMerging.mergeSpectra(mobilityScans, mzTolerance,
            MergingType.ALL_ENERGIES, null);
        return mergedMobilityScan;
      }
    }
    return feature.getRepresentativeScan();
  }

}
