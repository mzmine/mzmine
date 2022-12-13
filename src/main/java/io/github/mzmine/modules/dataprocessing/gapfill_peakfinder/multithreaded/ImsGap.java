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

package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded;

import com.google.common.collect.Range;
import gnu.trove.list.array.TDoubleArrayList;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.Gap;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.GapDataPoint;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * A gap in an IMS Raw data file.
 *
 * @author https://github.com/SteffenHeu
 */
public class ImsGap extends Gap {

  private final Range<Float> mobilityRange;
  private final BinningMobilogramDataAccess mobilogramBinning;

  /**
   * Constructor: Initializes an empty gap
   *
   * @param peakListRow
   * @param rawDataFile
   * @param mzRange           M/Z coordinate of this empty gap
   * @param rtRange           RT coordinate of this empty gap
   * @param intTolerance
   * @param mobilogramBinning
   */
  public ImsGap(@NotNull FeatureListRow peakListRow, @NotNull RawDataFile rawDataFile,
      @NotNull Range<Double> mzRange, @NotNull Range<Float> rtRange,
      @NotNull Range<Float> mobilityRange, double intTolerance,
      @NotNull BinningMobilogramDataAccess mobilogramBinning) {
    this(peakListRow, rawDataFile, mzRange, rtRange, mobilityRange, intTolerance, mobilogramBinning,
        true);
  }

  public ImsGap(@NotNull FeatureListRow peakListRow, @NotNull RawDataFile rawDataFile,
      @NotNull Range<Double> mzRange, @NotNull Range<Float> rtRange,
      @NotNull Range<Float> mobilityRange, double intTolerance,
      @NotNull BinningMobilogramDataAccess mobilogramBinning, boolean validateRtShape) {
    super(peakListRow, rawDataFile, mzRange, rtRange, intTolerance, validateRtShape);
    this.mobilityRange = mobilityRange;
    this.mobilogramBinning = mobilogramBinning;
  }

  @Override
  public void offerNextScan(Scan scan) {
    if (!(scan instanceof MobilityScanDataAccess access)) {
      throw new IllegalArgumentException("Scan is not a MobilityScanDataAccess");
    }
    double scanRT = scan.getRetentionTime();

    // If not yet inside the RT range
    // If we have passed the RT range and finished processing last peak
    if (scanRT < rtRange.lowerEndpoint() || (scanRT > rtRange.upperEndpoint()) && (
        currentPeakDataPoints == null)) {
      return;
    }

    DataPointIonMobilitySeries mobilogram = findDataPoint(access);

    if (mobilogram == null) {
      return;
    }

    if (currentPeakDataPoints == null) {
      currentPeakDataPoints = new ArrayList<>();
      currentPeakDataPoints.add(mobilogram);
      return;
    }

    // Check if this continues previous peak?
    if (checkRTShape(mobilogram)) {
      // Yes, continue this peak.
      currentPeakDataPoints.add(mobilogram);
    } else {
      checkCurrentPeak();
      currentPeakDataPoints = null;
    }

  }

  private DataPointIonMobilitySeries findDataPoint(@NotNull final MobilityScanDataAccess access) {

    final Frame frame = access.getFrame();
    final MobilityType mobilityType = frame.getMobilityType();
//    final double featureMz = peakListRow.getAverageMZ();
    final double featureMz = RangeUtils.rangeCenter(mzRange);

    /*if (frame.getRetentionTime() < rtRange.lowerEndpoint()) {
      return null;
    } else if (frame.getRetentionTime() > rtRange.upperEndpoint()) {
      return null;
    }*/

    final List<MobilityScan> mobilogramScans = new ArrayList<>();
    final TDoubleArrayList mzValues = new TDoubleArrayList();
    final TDoubleArrayList intensityValues = new TDoubleArrayList();

    while (access.hasNextMobilityScan()) {
      final MobilityScan scan;
      try {
        scan = access.nextMobilityScan();
      } catch (MissingMassListException e) {
        e.printStackTrace();
        return null;
      }

      if ((mobilityType != MobilityType.TIMS && scan.getMobility() < mobilityRange.lowerEndpoint())
          || (mobilityType == MobilityType.TIMS
          && scan.getMobility() > mobilityRange.upperEndpoint())) {
        continue;
      } else if (
          (mobilityType != MobilityType.TIMS && scan.getMobility() > mobilityRange.upperEndpoint())
              || (mobilityType == MobilityType.TIMS
              && scan.getMobility() < mobilityRange.lowerEndpoint())) {
        break;
      }

      int bestIndex = -1;
      double bestDelta = Double.POSITIVE_INFINITY;
      for (int i = 0; i < access.getNumberOfDataPoints(); i++) {
        final double mz = access.getMzValue(i);
        if (mz < mzRange.lowerEndpoint()) {
          continue;
        } else if (mz > mzRange.upperEndpoint()) {
          break;
        }

        final double delta = Math.abs(mz - featureMz);
        if (delta < bestDelta) {
          bestDelta = delta;
          bestIndex = i;
        }
      }

      if (bestIndex != -1) {
        mzValues.add(access.getMzValue(bestIndex));
        intensityValues.add(access.getIntensityValue(bestIndex));
        mobilogramScans.add(scan);
      }
    }

    if (!mobilogramScans.isEmpty()) {
      return new DataPointIonMobilitySeries(null, mzValues.toArray(), intensityValues.toArray(),
          mobilogramScans);
    }
    return null;
  }

  @Override
  protected boolean addFeatureToRow() {
    final IonMobilogramTimeSeries trace = IonMobilogramTimeSeriesFactory.of(
        ((ModularFeatureList) featureListRow.getFeatureList()).getMemoryMapStorage(),
        (List<IonMobilitySeries>) (List<? extends IonMobilitySeries>) (List<? extends GapDataPoint>) bestPeakDataPoints,
        mobilogramBinning);

    ModularFeature f = new ModularFeature((ModularFeatureList) featureListRow.getFeatureList(),
        rawDataFile, trace, FeatureStatus.ESTIMATED);

    featureListRow.addFeature(rawDataFile, f, false);
    return true;
  }
}
