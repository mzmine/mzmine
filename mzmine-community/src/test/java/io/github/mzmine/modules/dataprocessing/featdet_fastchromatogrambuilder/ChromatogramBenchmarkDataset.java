/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One data set of the chromatogram builder benchmarks, {@link ChromatogramBuilderBenchmark} and
 * {@link ToleranceEstimationBenchmark}, with the settings of its batch wizard. See
 * {@link ChromatogramBenchmarkDatasets} for the list.
 *
 * @param name           shown in the report, unique
 * @param paths          raw data files, absolute or test resources
 * @param detector       MS1 mass detection
 * @param noise          noise level or factor of the mass detection
 * @param minConsecutive builder parameter
 * @param minGroup       builder parameter
 * @param minHeight      builder parameter and min height of the resolver
 * @param preset         tolerance of the batch wizard preset for this instrument
 * @param ppmGrid        relative tolerances of the sweep
 * @param cropRt         retention time range of the builders, null for all scans
 * @param polarity       polarity of the builder scans, {@link PolarityType#ANY} for all
 */
record ChromatogramBenchmarkDataset(@NotNull String name, @NotNull List<String> paths,
                                    @NotNull MassDetectorWizardOptions detector, double noise,
                                    int minConsecutive, double minGroup, double minHeight,
                                    @NotNull MZTolerance preset, @NotNull double[] ppmGrid,
                                    @Nullable Range<Float> cropRt, @NotNull PolarityType polarity) {

  static final String WITHOUT_NOISE_FILTER = ", no noise filter";

  /**
   * @return MS1 scans in the retention time range and of the polarity, the scans of the builders in
   * the batch wizard
   */
  @NotNull ScanSelection scanSelection() {
    return new ScanSelection(1, cropRt, polarity);
  }

  /**
   * The builder settings stay, so only more weak signals reach the builders.
   *
   * @return the same data set without noise filter in the mass detection: factor 1 of the lowest
   * signal and absolute noise level 0 keep all signals
   */
  @NotNull ChromatogramBenchmarkDataset withoutNoiseFilter() {
    final double noNoise = detector == MassDetectorWizardOptions.FACTOR_OF_LOWEST_SIGNAL ? 1d : 0d;
    return new ChromatogramBenchmarkDataset(name + WITHOUT_NOISE_FILTER, paths, detector, noNoise,
        minConsecutive, minGroup, minHeight, preset, ppmGrid, cropRt, polarity);
  }

  /**
   * @return the same data set with at most maxFiles of its files
   */
  @NotNull ChromatogramBenchmarkDataset limitFiles(int maxFiles) {
    if (paths.size() <= maxFiles) {
      return this;
    }
    return new ChromatogramBenchmarkDataset(name, paths.subList(0, maxFiles), detector, noise,
        minConsecutive, minGroup, minHeight, preset, ppmGrid, cropRt, polarity);
  }

  /**
   * @return mass detection of all scans on import, fragment scans with factor 2.5 of the lowest
   * signal or the absolute MS1 noise level
   */
  @NotNull AdvancedSpectraImportParameters importParameters() {
    final double ms2Noise =
        detector == MassDetectorWizardOptions.FACTOR_OF_LOWEST_SIGNAL ? 2.5d : noise;
    return AdvancedSpectraImportParameters.create(detector, noise, ms2Noise, null,
        ScanSelection.ALL_SCANS, false);
  }

  /**
   * @return one line for the report
   */
  @NotNull String describe() {
    return """
        %d files, MS1 mass detection %s %.1f, min consecutive %d, min group intensity %.0f, \
        min height %.0f, builder scans %s""".formatted(paths.size(), detector, noise,
        minConsecutive, minGroup, minHeight, scanSelection().toShortDescription());
  }

  /**
   * Parameterized tests show the name.
   */
  @Override
  public String toString() {
    return name;
  }
}
