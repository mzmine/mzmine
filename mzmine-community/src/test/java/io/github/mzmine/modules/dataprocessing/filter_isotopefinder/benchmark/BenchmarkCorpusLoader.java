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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Loads the committed benchmark JSONL corpus and maps each line to a runnable
 * {@link GroundTruthCase}.
 */
public final class BenchmarkCorpusLoader {

  /**
   * Classpath location of the committed corpus.
   */
  public static final String RESOURCE = "isotopefinder/corpus/patterns.jsonl";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private BenchmarkCorpusLoader() {
  }

  /**
   * Load the corpus from the test classpath resource {@link #RESOURCE}.
   */
  @NotNull
  public static List<GroundTruthCase> load() {
    final InputStream in = BenchmarkCorpusLoader.class.getClassLoader()
        .getResourceAsStream(RESOURCE);
    if (in == null) {
      throw new IllegalStateException("Benchmark corpus not found on classpath: " + RESOURCE);
    }
    try (final BufferedReader reader = new BufferedReader(
        new InputStreamReader(in, StandardCharsets.UTF_8))) {
      return parse(reader);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to read benchmark corpus: " + RESOURCE, e);
    }
  }

  /**
   * Load the corpus from a filesystem path.
   */
  @NotNull
  public static List<GroundTruthCase> load(@NotNull final Path path) {
    try (final BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return parse(reader);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to read benchmark corpus: " + path, e);
    }
  }

  @NotNull
  private static List<GroundTruthCase> parse(@NotNull final BufferedReader reader)
      throws IOException {
    final List<GroundTruthCase> cases = new ArrayList<>();
    String line;
    while ((line = reader.readLine()) != null) {
      if (line.isBlank()) {
        continue;
      }
      final BenchmarkPattern pattern = MAPPER.readValue(line, BenchmarkPattern.class);
      cases.add(GroundTruthCase.fromPattern(pattern));
    }
    return cases;
  }
}
