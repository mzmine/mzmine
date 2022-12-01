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
