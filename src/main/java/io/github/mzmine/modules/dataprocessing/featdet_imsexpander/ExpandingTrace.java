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

package io.github.mzmine.modules.dataprocessing.featdet_imsexpander;

import com.google.common.collect.Range;
import gnu.trove.list.array.TDoubleArrayList;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ExpandingTrace {

  private final ModularFeatureListRow f;
  private final TDoubleArrayList mzs = new TDoubleArrayList();
  private final TDoubleArrayList intensities = new TDoubleArrayList();
  private final List<MobilityScan> scans = new ArrayList<>();
  private final Range<Float> rtRange;
  private final Range<Double> mzRange;

  public ModularFeatureListRow getRow() {
    return f;
  }

  ExpandingTrace(@NotNull final ModularFeatureListRow f, Range<Double> mzRange) {
    this.f = f;
    rtRange = f.getBestFeature().getRawDataPointsRTRange();
    this.mzRange = mzRange;
  }

  /**
   * Offers a data point to this trace.
   *
   * @param access
   * @param index
   * @return true if the data points is added to this trace.
   */
  public synchronized boolean offerDataPoint(@NotNull MobilityScanDataAccess access, int index) {
    if (!rtRange.contains(access.getRetentionTime()) || !mzRange
        .contains(access.getMzValue(index))) {
      return false;
    }

    if (!scans.isEmpty() && scans.get(scans.size() - 1) == access.getCurrentMobilityScan()) {
      return false;
    }

    scans.add(access.getCurrentMobilityScan());
    mzs.add(access.getMzValue(index));
    intensities.add(access.getIntensityValue(index));
    return true;
  }

  public IonMobilogramTimeSeries toIonMobilogramTimeSeries(MemoryMapStorage storage,
      BinningMobilogramDataAccess mobilogramDataAccess) {

    final List<IonMobilitySeries> mobilograms = new ArrayList<>();

    int scanStart = 0;
    int scanEnd = 0;
    Frame lastFrame = null;

    for (int i = 0; i < scans.size(); i++) {
      final MobilityScan scan = scans.get(i);
      if (lastFrame == null) {
        lastFrame = scan.getFrame();
      } else if (lastFrame != scan.getFrame()) {
        scanEnd = i;
        final IonMobilitySeries mobilogram = new SimpleIonMobilitySeries(null,
            mzs.toArray(scanStart, scanEnd - scanStart),
            intensities.toArray(scanStart, scanEnd - scanStart), scans.subList(scanStart, scanEnd));
        mobilograms.add(mobilogram);
        scanStart = i;
        lastFrame = scan.getFrame();
      }
    }

    if(scanStart <= scanEnd) {
      final IonMobilitySeries mobilogram = new SimpleIonMobilitySeries(null,
          mzs.toArray(scanStart, scans.size() - scanStart),
          intensities.toArray(scanStart, scans.size() - scanStart), scans.subList(scanStart, scans.size()));
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
    return scans.size();
  }
}
