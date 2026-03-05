/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_linear;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class TotalRawSignalNormalizationTypeModule extends AbstractFactorNormalizationTypeModule {

  @Override
  public @NotNull String getName() {
    return "Total raw signal";
  }

  @Override
  protected double getNormalizationMetricForFile(@NotNull final RawDataFile file,
      @NotNull final ModularFeatureList featureList,
      @NotNull final ParameterSet linearNormalizerParameters,
      @NotNull final ParameterSet moduleSpecificParameters) {
    final double ticSum = file.stream().filter(scan -> scan.getMSLevel() == 1)
        .mapToDouble(scan -> Objects.requireNonNullElse(scan.getTIC(), 0d)).sum();
    if (Double.compare(ticSum, 0d) == 0) {
      throw new IllegalStateException("No TIC found for file: " + file.getName());
    }
    return ticSum;
  }
}
