/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IonMobilityUtils {

  private static Logger logger = Logger.getLogger(IonMobilityUtils.class.getName());

  public static double getSmallestMobilityDelta(Frame frame) {
    double minDelta = Double.MAX_VALUE;

    List<MobilityScan> scans = frame.getMobilityScans();
    double lastMobility = scans.get(0).getMobility();
    for (int i = 1; i < scans.size(); i++) {
      final double currentMobility = scans.get(i).getMobility();
      final double delta = Math.abs(currentMobility - lastMobility);
      if (delta < minDelta) {
        minDelta = delta;
      }
      lastMobility = currentMobility;
    }
    return minDelta;
  }

  public static Map<Range<Double>, Frame> getUniqueMobilityRanges(
      @Nonnull final IMSRawDataFile file) {
    Map<Range<Double>, Frame> ranges = new HashMap<>();
    for (Frame frame : file.getFrames()) {
      ranges.putIfAbsent(frame.getMobilityRange(), frame);
    }
    return ranges;
  }

  public static List<ModularFeature> getFeaturesWithinRegion(Collection<ModularFeature> features,
      List<Path2D> regions) {

    List<ModularFeature> contained = new ArrayList<>();
    for (ModularFeature feature : features) {
      if (isFeatureWithinMzMobilityRegion(feature, regions)) {
        contained.add(feature);
      }
    }
    return contained;
  }

  public static boolean isFeatureWithinMzMobilityRegion(@Nonnull ModularFeature feature,
      @Nonnull final Collection<Path2D> regions) {
    if (feature != null) {
      Property<Float> mobility = feature.get(MobilityType.class);
      if (mobility != null) {
        Point2D point = new Point2D.Double(feature.getMZ(), mobility.getValue().doubleValue());
        for (Path2D region : regions) {
          if (region.contains(point)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public static boolean isFeatureWithinMzCCSRegion(@Nonnull ModularFeature feature,
      @Nonnull final Collection<Path2D> regions) {
    if (feature != null) {
      Float ccs = feature.getCCS();
      if (ccs != null) {
        Point2D point = new Point2D.Double(feature.getMZ() * feature.getCharge(),
            ccs.doubleValue());
        for (Path2D region : regions) {
          if (region.contains(point)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Builds a mobilogram for the given mz range in the frame. Should only be used for previews and
   * visualisations, less perfomant than a ims feature detector.
   *
   * @param frame
   * @param mzRange
   * @param type
   * @param storage
   * @return
   */
  public static IonMobilitySeries buildMobilogramForMzRange(@Nonnull final Frame frame,
      @Nonnull final Range<Double> mzRange, @Nonnull final MobilogramType type,
      @Nullable final MemoryMapStorage storage) {

    final int numScans = frame.getNumberOfMobilityScans();
    final double rangeCenter = RangeUtils.rangeCenter(mzRange);

    final double[] intensities = new double[frame.getNumberOfMobilityScans()];
    final double[] mzs = new double[frame.getNumberOfMobilityScans()];
    final double[] mobilities = new double[frame.getNumberOfMobilityScans()];
    frame.getMobilities().get(0, mobilities, 0, mobilities.length);

    final List<MobilityScan> mobilityScans = frame.getMobilityScans();

    // todo replace with method introduced in PR mzmine3#238
    final int maxNumDataPoints = mobilityScans.stream()
        .mapToInt(MobilityScan::getNumberOfDataPoints).max().orElse(0);

    final double[] intensitiesBuffer = new double[maxNumDataPoints];
    final double[] mzsBuffer = new double[maxNumDataPoints];

    for (int i = 0; i < numScans; i++) {
      final MobilityScan scan = mobilityScans.get(i);
      scan.getMzValues(mzsBuffer);
      scan.getIntensityValues(intensitiesBuffer);

      if (type == MobilogramType.BASE_PEAK) {
        DataPoint bp = ScanUtils
            .findBasePeak(mzsBuffer, intensitiesBuffer, mzRange, scan.getNumberOfDataPoints());
        if (bp != null) {
          mzs[i] = bp.getMZ();
          intensities[i] = bp.getIntensity();
        }
      } else if (type == MobilogramType.TIC) {
        mzs[i] = rangeCenter;
        intensities[i] = ScanUtils
            .calculateTIC(mzsBuffer, intensitiesBuffer, mzRange, scan.getNumberOfDataPoints());
      }
    }

    return new SimpleIonMobilitySeries(storage, mzs, intensities, mobilityScans);
  }

  /**
   * Extracts the mobility scan with the highest intensity this feature was detected in.
   *
   * @param f The feature.
   * @return The mobility scan. Null if this feature does not possess a mobility dimension.
   */
  @Nullable
  public static MobilityScan getBestMobilityScan(@Nonnull final ModularFeature f) {
    Scan bestScan = f.getRepresentativeScan();
    if (!(bestScan instanceof Frame bestFrame)) {
      return null;
    }

    final IonTimeSeries<? extends Scan> featureData = f.getFeatureData();
    if (!(featureData instanceof IonMobilogramTimeSeries trace)) {
      return null;
    }

    final IonMobilitySeries bestMobilogram = trace.getMobilogram(bestFrame);
    if (bestMobilogram == null) {
      return null;
    }

    MobilityScan bestMobilityScan = null;
    double maxIntensity = 0d;
    for (int i = 0; i < bestMobilogram.getNumberOfValues(); i++) {
      if (bestMobilogram.getIntensity(i) > maxIntensity) {
        maxIntensity = bestMobilogram.getIntensity(i);
        bestMobilityScan = bestMobilogram.getSpectrum(i);
      }
    }
    return bestMobilityScan;
  }

  /**
   * @param series The series. Sorted by ascending mobility. Note that raw {@link IonMobilitySeries}
   *               from {@link io.github.mzmine.datamodel.MobilityType#TIMS} measurements can be
   *               sorted by descending mobility. {@link io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries}
   *               are guaranteed to be sorted by ascending mobility.
   * @return The FWHM range or null.
   */
  public static <T extends IntensitySeries & MobilitySeries> Range<Float> getMobilityFWHM(
      T series) {
    final int mostIntenseIndex = FeatureDataUtils.getMostIntenseIndex(series);
    if (mostIntenseIndex == -1) {
      return null;
    }

    final double maxIntensity = series.getIntensity(mostIntenseIndex);
    final double halfIntensity = maxIntensity / 2;

    int before = 0;
    int after = series.getNumberOfValues() - 1;

    for (int i = 0; i < mostIntenseIndex; i++) {
      if (series.getIntensity(i) > halfIntensity) {
        before = Math.max(0, i - 1);
        break;
      }
    }

    for (int i = mostIntenseIndex; i < series.getNumberOfValues(); i++) {
      if (series.getIntensity(i) < halfIntensity) {
        after = i;
        break;
      }
    }

    final double startMobility = MathUtils
        .twoPointGetXForY(series.getMobility(before), series.getIntensity(before),
            series.getMobility(Math.min(before + 1, series.getNumberOfValues() - 1)),
            series.getIntensity(Math.min(before + 1, series.getNumberOfValues() - 1)), halfIntensity);

    final double endMobility = MathUtils
        .twoPointGetXForY(series.getMobility(Math.max(after - 1, 0)),
            series.getIntensity(Math.max(after - 1, 0)),
            series.getMobility(after), series.getIntensity(after), halfIntensity);

//    logger.finest(() -> "Determined FWHM from " + startMobility + " to " + endMobility);
    return Range.closed((float) startMobility, (float) endMobility);
  }

  public enum MobilogramType {
    BASE_PEAK, TIC
  }

}
