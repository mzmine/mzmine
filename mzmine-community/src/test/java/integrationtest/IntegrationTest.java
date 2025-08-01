/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package integrationtest;

import static integrationtest.IntegrationTestUtils.urlToFile;

import io.github.mzmine.modules.tools.output_compare_csv.CheckResult;
import io.github.mzmine.util.ArrayUtils;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record IntegrationTest(@NotNull File batchFile, @Nullable File tempDir,
                              @Nullable File[] rawFiles, @Nullable File[] specLibs) {

  public static Builder builder(final @NotNull String baseDirectory, @NotNull String batchFile) {
    return new Builder(baseDirectory, batchFile);
  }

  public File runBatchGetCsvFile() {
    return IntegrationTestUtils.runBatchGetExportedCsv(this);
  }

  public List<CheckResult> runBatchGetCheckResults(String expectedResultsFullPath) {
    return IntegrationTestUtils.getCsvComparisonResults(expectedResultsFullPath,
        runBatchGetCsvFile(), batchFile().getName());
  }

  // -----------------------------------------------------------
  // Builder below
  // -----------------------------------------------------------

  public static final class Builder {

    private final @NotNull File batchFile;
    private final @NotNull String baseDir;
    private @Nullable File tempDir;
    private @Nullable File[] specLibs;
    private @Nullable File[] rawFiles;

    public Builder(final @NotNull String baseDirectory, final @NotNull String batchFile) {
      this.baseDir = baseDirectory;
      this.batchFile = getFileFromBaseDir(batchFile);
    }

    /**
     * Retrieves a file relative to the {@link Builder#baseDir}
     */
    private @NotNull File getFileFromBaseDir(@NotNull String file) {
      return urlToFile(getResourceFromBaseDir(file));
    }

    /**
     * Retrieves a URL relative to the {@link Builder#baseDir}
     */
    private @Nullable URL getResourceFromBaseDir(@NotNull String file) {
      // use / instead of File.separator, since it does not work
      final String path = baseDir + "/" + file;
      return getClass().getClassLoader().getResource(path);
    }

    /**
     * Retrieves a file relative to the main resources folder.
     */
    private @NotNull File getFileFullPath(@NotNull String path) {
      return urlToFile(getClass().getClassLoader().getResource(path));
    }

    public @NotNull Builder tempDir(@Nullable final File tempDir) {
      this.tempDir = tempDir;
      return this;
    }

    /**
     * Adds the raw data files relative to the base directory
     */
    public @NotNull Builder rawFiles(@Nullable final String... rawFiles) {
      if (rawFiles == null) {
        return this;
      }
      // attach base directory
      this.rawFiles = ArrayUtils.combine(
          Arrays.stream(rawFiles).filter(Objects::nonNull).map(this::getFileFromBaseDir)
              .toArray(File[]::new), this.rawFiles);
      return this;
    }

    /**
     * Adds the raw data files relative to the base directory
     */
    public @NotNull Builder rawFilesFullPath(@Nullable final String... rawFiles) {
      if (rawFiles == null) {
        return this;
      }
      // attach base directory
      this.rawFiles = ArrayUtils.combine(
          Arrays.stream(rawFiles).filter(Objects::nonNull).map(this::getFileFullPath)
              .toArray(File[]::new), this.rawFiles);
      return this;
    }

    /**
     * Adds spectra libraries relative to the base path
     */
    public @NotNull Builder specLibsRelative(@Nullable final String... specLibs) {
      if (specLibs == null) {
        return this;
      }
      // attach base directory
      this.specLibs = ArrayUtils.combine(this.specLibs,
          Arrays.stream(specLibs).filter(Objects::nonNull).map(this::getFileFromBaseDir)
              .toArray(File[]::new));
      return this;
    }

    /**
     * Adds spectral libraries as an absolute resource
     */
    public @NotNull Builder specLibsFullPath(@Nullable final String... specLibs) {
      if (specLibs == null) {
        return this;
      }
      // attach base directory
      this.specLibs = ArrayUtils.combine(
          Arrays.stream(specLibs).filter(Objects::nonNull).map(this::getFileFullPath)
              .toArray(File[]::new), this.specLibs);
      return this;
    }

    public IntegrationTest build() {
      return new IntegrationTest(batchFile, tempDir, rawFiles, specLibs);
    }
  }
}
