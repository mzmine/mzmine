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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stable filename suffix for one benchmark configuration.
 */
final class BenchmarkCampaign {

  static final String OUTPUT_DIRECTORY_PROPERTY = "mzmine.test.autoparam.outputDir";

  private final @NotNull String id;

  private BenchmarkCampaign(@NotNull String id) {
    this.id = sanitize(id);
  }

  static @NotNull BenchmarkCampaign create(@NotNull String optimizer, @NotNull String metric,
      @NotNull String sampling, int batchBudget, @NotNull List<Long> seeds,
      @Nullable String selectedDatasets, @Nullable String requestedId) {
    if (requestedId != null && !requestedId.isBlank()) {
      return new BenchmarkCampaign(requestedId);
    }

    final String seedId = seeds.stream().map(Object::toString).collect(Collectors.joining("-"));
    final String datasetId =
        selectedDatasets == null || selectedDatasets.isBlank() ? "all" : selectedDatasets;
    return new BenchmarkCampaign(
        "%s-%s-%s-b%d-s%s-d%s".formatted(optimizer, metric, sampling, batchBudget, seedId,
            datasetId));
  }

  @NotNull File outputFile(@NotNull String stem) {
    return outputDirectory().resolve("%s-%s.csv".formatted(stem, id)).toFile().getAbsoluteFile();
  }

  static @NotNull Path outputDirectory() {
    final Path directory = Path.of(
        System.getProperty(OUTPUT_DIRECTORY_PROPERTY, "build/autoparam-benchmarks"));
    try {
      return Files.createDirectories(directory);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create benchmark output directory " + directory, e);
    }
  }

  @NotNull String id() {
    return id;
  }

  private static @NotNull String sanitize(@NotNull String value) {
    final String replaced = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    final String trimmed = replaced.replaceAll("^-+|-+$", "");
    if (trimmed.isBlank()) {
      throw new IllegalArgumentException("campaign id must contain a letter or digit");
    }
    return trimmed;
  }
}
