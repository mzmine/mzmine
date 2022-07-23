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

package io.github.mzmine.modules.dataprocessing.featdet_imsexpander2;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.NotNull;

public class ExpandingTrace2 {

  private final ModularFeatureListRow f;
  private final Range<Float> rtRange;
  private final Range<Double> mzRange;

  private final Map<MobilityScan, DataPoint> dataPoints = new HashMap<>();

  ExpandingTrace2(@NotNull final ModularFeatureListRow f, Range<Double> mzRange) {
    this.f = f;
    rtRange = f.getBestFeature().getRawDataPointsRTRange();
    this.mzRange = mzRange;
  }

  public ModularFeatureListRow getRow() {
    return f;
  }

  /**
   * Offers a data point to this trace.
   *
   * @param access
   * @param index
   * @return true if the data points is added to this trace.
   */
  public boolean offerDataPoint(@NotNull MobilityScanDataAccess access, int index) {
    if (!rtRange.contains(access.getRetentionTime()) || !mzRange.contains(
        access.getMzValue(index))) {
      return false;
    }

    synchronized (dataPoints) {
      return dataPoints.putIfAbsent(access.getCurrentMobilityScan(),
          new SimpleDataPoint(access.getMzValue(index), access.getIntensityValue(index))) == null;
    }
  }

  public IonMobilogramTimeSeries toIonMobilogramTimeSeries(MemoryMapStorage storage,
      BinningMobilogramDataAccess mobilogramDataAccess) {

    final List<IonMobilitySeries> mobilograms = new ArrayList<>();

    int scanStart = 0;
    int scanEnd = 0;
    Frame lastFrame = null;

    final List<MobilityScan> scans = dataPoints.keySet().stream().sorted().toList();
    final List<DataPoint> dataPointsList = this.dataPoints.entrySet().stream()
        .sorted(Comparator.comparing(Entry::getKey)).map(Entry::getValue).toList();

    for (int i = 0; i < scans.size(); i++) {
      final MobilityScan scan = scans.get(i);
      if (lastFrame == null) {
        lastFrame = scan.getFrame();
      } else if (lastFrame != scan.getFrame()) {
        scanEnd = i;

        final double[][] dps = DataPointUtils.getDataPointsAsDoubleArray(
            dataPointsList.subList(scanStart, scanEnd));

        final IonMobilitySeries mobilogram = new SimpleIonMobilitySeries(null, dps[0], dps[1],
            scans.subList(scanStart, scanEnd));
        mobilograms.add(mobilogram);
        scanStart = i;
        lastFrame = scan.getFrame();
      }
    }

    if (scanStart <= scanEnd) {
      final double[][] dps = DataPointUtils.getDataPointsAsDoubleArray(
          dataPointsList.subList(scanStart, dataPoints.size()));
      final IonMobilitySeries mobilogram = new SimpleIonMobilitySeries(null, dps[0], dps[1],
          scans.subList(scanStart, scans.size()));
      mobilograms.add(mobilogram);
    }

    return IonMobilogramTimeSeriesFactory.of(storage, mobilograms, mobilogramDataAccess);
  }

  public Range<Float> getRtRange() {
    return rtRange;
  }

  public Range<Double> getMzRange() {
    return mzRange;
  }

  public int getNumberOfMobilityScans() {
    return dataPoints.size();
  }
}
