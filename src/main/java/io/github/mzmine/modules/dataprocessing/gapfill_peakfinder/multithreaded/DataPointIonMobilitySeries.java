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

package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded;

import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.GapDataPoint;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.SpectraMerging;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mobilogram implementation that also serves as a GapDataPoint for gap filling.
 *
 * @author https://github.com/SteffenHeu
 */
class DataPointIonMobilitySeries extends SimpleIonMobilitySeries implements GapDataPoint {

  private final double mz;
  private final double intensity;

  /**
   * @param storage         May be null if forceStoreInRam is true.
   * @param mzValues
   * @param intensityValues
   * @param scans
   */
  public DataPointIonMobilitySeries(@Nullable MemoryMapStorage storage, @NotNull double[] mzValues,
      @NotNull double[] intensityValues, @NotNull List<MobilityScan> scans) {
    super(storage, mzValues, intensityValues, scans);

    mz = MathUtils.calcCenter(SpectraMerging.DEFAULT_CENTER_MEASURE, mzValues, intensityValues,
        SpectraMerging.DEFAULT_WEIGHTING);
    intensity = ArrayUtils.sum(intensityValues);
  }

  @Override
  public double getMZ() {
    return mz;
  }

  @Override
  public double getIntensity() {
    return intensity;
  }

  @Override
  public double getRT() {
    return getSpectrum(0).getRetentionTime();
  }

  @Override
  public Scan getScan() {
    return getSpectrum(0).getFrame();
  }
}
