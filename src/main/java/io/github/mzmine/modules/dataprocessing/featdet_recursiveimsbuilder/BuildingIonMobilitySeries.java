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

package io.github.mzmine.modules.dataprocessing.featdet_recursiveimsbuilder;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BuildingIonMobilitySeries extends SimpleIonMobilitySeries {

  protected final double summedIntensity;
  protected final double avgMZ;
  protected final Frame frame;

  /**
   * @param storage         May be null if forceStoreInRam is true.
   * @param mzValues
   * @param intensityValues
   * @param scans
   */
  public BuildingIonMobilitySeries(@Nullable MemoryMapStorage storage, @NotNull double[] mzValues,
      @NotNull double[] intensityValues,
      @NotNull List<MobilityScan> scans) {
    super(storage, mzValues, intensityValues, scans);

    frame = scans.get(0).getFrame();
    summedIntensity = ArrayUtils.sum(intensityValues);
    avgMZ = MathUtils.calcCenter(CenterMeasure.AVG, mzValues, intensityValues, Weighting.LINEAR);
  }

  public double getSummedIntensity() {
    return summedIntensity;
  }

  public double getAvgMZ() {
    return avgMZ;
  }

  public Frame getFrame() {
    return frame;
  }
}
