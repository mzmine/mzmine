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
public record EstimationInputFile(@NotNull File file, @NotNull SampleType sampleType,
                                  long sizeBytes) {

  /**
   * Reads the size on disk. Walks folders, so call this from a task and never from the FX thread.
   */
  public static @NotNull EstimationInputFile of(@NotNull final File file) {
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
