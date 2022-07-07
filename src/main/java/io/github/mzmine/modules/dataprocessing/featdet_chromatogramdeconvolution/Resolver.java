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
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.datamodel.featuredata.TimeSeries;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a {@link IntensitySeries}-and-{@link TimeSeries} in time dimension and/or a {@link
 * IntensitySeries}-and-{@link MobilitySeries} in mobility dimension.
 *
 * @author SteffenHeu https://github.com/SteffenHeu
 */
public interface Resolver {

  /**
   * Resolves a series in time dimension.
   *
   * @param series The series.
   * @return A list of time ranges.
   */
  @NotNull <T extends IntensitySeries & TimeSeries> List<Range<Double>> resolveRt(
      @NotNull final T series);

  /**
   * Resolves a series in mobility dimension.
   *
   * @param series The series.
   * @return A list of mobility ranges.
   */
  @NotNull <T extends IntensitySeries & MobilitySeries> List<Range<Double>> resolveMobility(
      @NotNull final T series);

   @NotNull List<Range<Double>> resolve(final double[] x, final double[] y);

  /**
   * Resolves a series (EICs) into individual series (features).
   *
   * @param series  The EIC
   * @param storage A storage for the resolved features.
   * @return A list of resolved series.
   */
  @NotNull <T extends IonTimeSeries<? extends Scan>> List<T> resolve(@NotNull T series,
      @Nullable MemoryMapStorage storage);

  public RawDataFile getRawDataFile();

  @NotNull Class<? extends MZmineModule> getModuleClass();
}
