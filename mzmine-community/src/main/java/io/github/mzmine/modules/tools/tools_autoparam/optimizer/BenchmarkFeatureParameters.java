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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer;

import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.BenchmarkFeatureLoader;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.FeatureRecord;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.ImportTypeParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.util.files.ExtensionFilters;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Optional csv file with additional benchmark features for the optimizer, embedded in
 * {@link OptimizerParameters#benchmarkFeatures}.
 */
public class BenchmarkFeatureParameters extends SimpleParameterSet {

  public static final FileNameParameter benchmarkFeaturesFile = new FileNameParameter(
      "Benchmark file", "File with additional benchmark features.", ExtensionFilters.CSV_TSV_IMPORT,
      FileSelectionType.OPEN);
  static final List<ImportType<?>> DEFAULT_IMPORT_TYPES = List.of(
      new ImportType<>(true, "mz", new MZType()), new ImportType<>(true, "rt", new RTType()),
      new ImportType<>(false, "mobility", new MobilityType()));
  public static final ImportTypeParameter benchmarkFeatureTypes = new ImportTypeParameter(
      "CSV column names", "", DEFAULT_IMPORT_TYPES);

  public BenchmarkFeatureParameters() {
    super(benchmarkFeaturesFile, benchmarkFeatureTypes);
  }

  /**
   * @param optimizerParameters the {@link OptimizerParameters}
   * @return the benchmark features of the file, empty if no file is selected
   */
  public static @NotNull List<FeatureRecord> loadBenchmarkFeatures(
      @NotNull ParameterSet optimizerParameters) {
    if (!optimizerParameters.getValue(OptimizerParameters.benchmarkFeatures)) {
      return List.of();
    }
    final ParameterSet benchmark = optimizerParameters.getParameter(
        OptimizerParameters.benchmarkFeatures).getEmbeddedParameters();
    return BenchmarkFeatureLoader.fromFile(null, benchmark.getValue(benchmarkFeaturesFile),
        benchmark.getValue(benchmarkFeatureTypes));
  }

  @Override
  public boolean checkParameterValues(@NotNull final Collection<String> errorMessages,
      final boolean skipRawDataAndFeatureListParameters) {
    final boolean superCheck = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);

    final long mzAndRtColumns = getValue(benchmarkFeatureTypes).stream()
        .filter(ImportType::isSelected)
        .filter(i -> i.getDataType().equals(new MZType()) || i.getDataType().equals(new RTType()))
        .count();
    if (mzAndRtColumns < 2) {
      errorMessages.add("RT and MZ values must be imported from the benchmark features file.");
    }

    return superCheck && errorMessages.isEmpty();
  }
}
