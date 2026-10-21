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

package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/**
 * A raw data file of the batch before it is imported.
 *
 * @param file       the raw data file or folder (e.g. Bruker .d)
 * @param sampleType guessed from the file name, see {@link SampleType#guessFromName(String)}
 * @param sizeBytes  size on disk, the sum of all files for folders
 */
record EstimationInputFile(@NotNull File file, @NotNull SampleType sampleType,
                                  long sizeBytes) {

  /**
   * Reads the size on disk. Walks folders, so call this from a task and never from the FX thread.
   */
  static @NotNull EstimationInputFile of(@NotNull final File file) {
    return new EstimationInputFile(file, SampleType.guessFromName(file.getName()),
        sizeOnDisk(file.toPath()));
  }

  private static long sizeOnDisk(@NotNull final Path path) {
    if (!Files.isDirectory(path)) {
      return path.toFile().length();
    }
    try (final Stream<Path> files = Files.walk(path)) {
      return files.filter(Files::isRegularFile).mapToLong(p -> p.toFile().length()).sum();
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read size of " + path, e);
    }
  }
}
