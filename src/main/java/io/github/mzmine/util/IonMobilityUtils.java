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
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.Property;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IonMobilityUtils {

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

  public static boolean isRowWithinMzMobilityRegion(@Nonnull ModularFeatureListRow row,
      @Nonnull final Collection<Path2D> regions) {
      Property<Float> mobility = row.get(MobilityType.class);
      if (mobility != null) {
        Point2D point = new Point2D.Double(row.getAverageMZ(), mobility.getValue().doubleValue());
        for (Path2D region : regions) {
          if (region.contains(point)) {
            return true;
          }
        }
      }
    return false;
  }

  public static boolean isRowWithinMzCCSRegion(@Nonnull ModularFeatureListRow feature,
      @Nonnull final Collection<Path2D> regions) {
      Float ccs = feature.getAverageCCS();
      if (ccs != null) {
        Point2D point = new Point2D.Double(feature.getAverageMZ() * feature.getRowCharge(),
            ccs.doubleValue());
        for (Path2D region : regions) {
          if (region.contains(point)) {
            return true;
          }
        }
      }
    return false;
  }

  /**
   * Builds a mobilogram for the given mz range in the frame. Should only be used for previews and
   * visualisations, less perfomant than a ims feature detector.
   *
   * @param frame The frame
   * @param mzRange The mz/Range of the mobilogram
   * @param type basepeak or tic (summed)
   * @param storage The storage to use
   * @return The built mobilogram.
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
        if(bp != null) {
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

  public enum MobilogramType {
    BASE_PEAK, TIC
  }
}
