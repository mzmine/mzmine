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
import java.util.zip.GZIPInputStream;
import org.jetbrains.annotations.NotNull;

/**
 * Loads the committed benchmark JSONL corpus and maps each line to a runnable
 * {@link GroundTruthCase}.
 * <p>
 * decision: the corpus is committed GZIPPED. It is a 12 MB text file that would otherwise sit in
 * the git history of every clone forever; gzip takes it to well under a tenth of that while keeping
 * the "pin the exact inputs the baselines were measured on" property that made committing it the
 * right call in the first place. Decompression is a few hundred milliseconds against a benchmark run
 * of minutes.
 */
public final class BenchmarkCorpusLoader {

  /**
   * Classpath location of the committed (gzipped) corpus.
   */
  public static final String RESOURCE = "isotopefinder/corpus/patterns.jsonl.gz";

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
    try (final BufferedReader reader = newReader(in)) {
      return parse(reader);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to read benchmark corpus: " + RESOURCE, e);
    }
  }

  /**
   * Load the corpus from a filesystem path (gzipped, see {@link #RESOURCE}).
   */
  @NotNull
  public static List<GroundTruthCase> load(@NotNull final Path path) {
    try (final BufferedReader reader = newReader(Files.newInputStream(path))) {
      return parse(reader);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to read benchmark corpus: " + path, e);
    }
  }

  /**
   * @param in the raw gzipped stream; closed together with the returned reader.
   * @return a UTF-8 line reader over the decompressed corpus.
   */
  @NotNull
  private static BufferedReader newReader(@NotNull final InputStream in) throws IOException {
    return new BufferedReader(
        new InputStreamReader(new GZIPInputStream(in), StandardCharsets.UTF_8));
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
