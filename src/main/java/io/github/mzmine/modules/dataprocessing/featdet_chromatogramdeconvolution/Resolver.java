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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.datamodel.featuredata.TimeSeries;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Resolver {

  /**
   * Creates a new resolver with the given parameters. A single instance is not thread safe.
   *
   * @param param The parameter set.
   * @param flist The feature list this resolver will be used for.
   * @return The resolver or null if it could not be initialised.
   */
  public Resolver newInstance(ParameterSet param, ModularFeatureList flist);

  @NotNull <T extends IntensitySeries & TimeSeries> List<Range<Double>> resolveRt(
      @NotNull final T series);

  @NotNull <T extends IntensitySeries & MobilitySeries> List<Range<Double>> resolveMobility(
      @NotNull final T series);

  @NotNull <T extends IonTimeSeries<? extends Scan>> List<T> resolve(@NotNull T series,
      @Nullable MemoryMapStorage storage);

  @NotNull Class<? extends MZmineModule> getModuleClass();
}
