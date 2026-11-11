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

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import java.io.File;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One dataset for {@link EstimateVsOptimumTest}: the raw files to optimize on plus the wizard
 * presets they should be processed with.
 * <p>
 * decision: the files are listed explicitly rather than discovered in a folder. A folder listing
 * would silently change what the benchmark measured whenever a file is added next to the data, and
 * the interesting datasets are a hand-picked set of QC injections inside a larger folder.
 *
 * @param name     label used in the printed table and the exported csv
 * @param files    paths to the raw files, in the order they should be imported. Absolute paths are
 *                 used as given; relative ones are resolved against {@value #DATA_ROOT_PROPERTY}.
 *                 Use forward slashes, they work on Windows too
 * @param metadata optional metadata file, resolved the same way as {@link #files}
 */
public record BenchmarkDataset(@NotNull String name, @NotNull List<String> files,
                               @NotNull IonInterfaceWizardParameterFactory ionInterface,
                               @NotNull MassSpectrometerWizardParameterFactory massSpectrometer,
                               @NotNull IonMobilityWizardParameterFactory ionMobility,
                               @Nullable String metadata) {

  /**
   * Optional parent folder for relative entries, so a dataset can be shared between machines that
   * keep the data in different places.
   */
  public static final String DATA_ROOT_PROPERTY = "mzmine.test.autoparam.dataRoot";

  /**
   * assumption: at least two files. The estimator derives sample to sample tolerances by aligning
   * the benchmark features across files, which a single file cannot produce.
   */
  private static final int MIN_FILES = 2;

  public @NotNull File[] rawFiles() {
    return files.stream().map(BenchmarkDataset::resolve).toArray(File[]::new);
  }

  public @Nullable File metadataFile() {
    return metadata == null ? null : resolve(metadata);
  }

  private static @NotNull File resolve(@NotNull String path) {
    final File asGiven = new File(path);
    final String root = System.getProperty(DATA_ROOT_PROPERTY);
    final File resolved =
        asGiven.isAbsolute() || root == null || root.isBlank() ? asGiven : new File(root, path);
    return resolved.exists() ? resolved : convertedFallback(resolved);
  }

  /**
   * Vendor files are often only kept as their mzML conversion, which imports the same data.
   */
  private static @NotNull File convertedFallback(@NotNull File missing) {
    final String name = missing.getName();
    if (!name.toLowerCase(Locale.ROOT).endsWith(".raw")) {
      return missing;
    }
    final File mzml = new File(missing.getParentFile(),
        name.substring(0, name.length() - ".raw".length()) + ".mzML");
    return mzml.exists() ? mzml : missing;
  }

  public boolean isAvailable() {
    return files.size() >= MIN_FILES && missingFiles().isEmpty();
  }

  /**
   * Listed files that do not exist. Reported rather than silently skipped, because a single typo in
   * a long path would otherwise look exactly like an absent dataset.
   */
  public @NotNull List<File> missingFiles() {
    return files.stream().map(BenchmarkDataset::resolve).filter(f -> !f.exists()).toList();
  }

  /**
   * @return why this dataset cannot run, or null when it can.
   */
  public @Nullable String unavailableReason() {
    if (files.size() < MIN_FILES) {
      return "%s lists %d file(s), the estimator needs at least %d".formatted(name, files.size(),
          MIN_FILES);
    }
    final List<File> missing = missingFiles();
    return missing.isEmpty() ? null
        : "%s is missing %d file(s): %s".formatted(name, missing.size(), missing);
  }
}
