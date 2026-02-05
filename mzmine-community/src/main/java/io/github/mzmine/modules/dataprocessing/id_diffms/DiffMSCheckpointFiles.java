/*
 * Copyright (c) 2004-2026 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_diffms;

import io.github.mzmine.modules.io.download.AssetGroup;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * DiffMS checkpoint management.
 */
public final class DiffMSCheckpointFiles {

  /**
   * Zenodo record holding DiffMS checkpoints.
   */
  public static final String ZENODO_RECORD_ID = "15122968";

  /**
   * The archive filename as provided by the Zenodo record.
   */
  public static final String CHECKPOINTS_ARCHIVE_NAME = "diffms_checkpoints.tar.gz";

  /**
   * The checkpoint currently used by MZmine.
   */
  public static final String DEFAULT_CHECKPOINT_FILE_NAME = "diffms_msg.ckpt";

  private DiffMSCheckpointFiles() {
  }

  /**
   * Returns the directory where DiffMS checkpoints are stored.
   */
  public static @NotNull File getCheckpointDir() {
    return new File(AssetGroup.DIFFMS.getDownloadToDir(), "checkpoints");
  }

  /**
   * Returns the default checkpoint file location in the checkpoints subdirectory.
   */
  public static @NotNull File getDefaultCheckpointFile() {
    return new File(getCheckpointDir(), DEFAULT_CHECKPOINT_FILE_NAME);
  }

  public static boolean isTarGz(final @Nullable File file) {
    if (file == null) {
      return false;
    }
    final String n = file.getName().toLowerCase(Locale.ROOT);
    return n.endsWith(".tar.gz") || n.endsWith(".tgz");
  }

  /**
   * Extracts {@code wantedFileName} from {@code tarGzArchive} into
   * {@code outputFile}.
   * <p>
   * The archive is expected to be a .tar.gz file as provided by Zenodo.
   *
   * @return the extracted file (same as {@code outputFile})
   */
  public static @NotNull File extractWantedCheckpointFromTarGz(final @NotNull File tarGzArchive,
      final @NotNull File outputFile, final @NotNull String wantedFileName,
      final @Nullable BooleanSupplier isCanceled) throws IOException {

    Objects.requireNonNull(tarGzArchive);
    Objects.requireNonNull(outputFile);
    Objects.requireNonNull(wantedFileName);

    if (!tarGzArchive.isFile()) {
      throw new IOException("Checkpoint archive does not exist: " + tarGzArchive.getAbsolutePath());
    }

    final Path outPath = outputFile.toPath();
    final Path parent = outPath.getParent();
    if (parent == null) {
      throw new IOException("Invalid checkpoint output path (no parent): " + outputFile);
    }
    Files.createDirectories(parent);

    // Write to a temp file first to avoid partially-written checkpoints.
    final Path tmp = Files.createTempFile(parent, outputFile.getName(), ".part");
    boolean extracted = false;

    try (var fis = new FileInputStream(tarGzArchive);
        var bis = new BufferedInputStream(fis);
        var gzis = new GzipCompressorInputStream(bis);
        var tar = new TarArchiveInputStream(gzis)) {

      TarArchiveEntry entry;
      while ((entry = tar.getNextTarEntry()) != null) {
        if (isCanceled != null && isCanceled.getAsBoolean()) {
          throw new IOException("Canceled while extracting checkpoint.");
        }
        if (entry.isDirectory()) {
          continue;
        }
        final String name = entry.getName();
        if (name == null) {
          continue;
        }
        if (name.equals(wantedFileName) || name.endsWith("/" + wantedFileName)) {
          try (var out = new BufferedOutputStream(new FileOutputStream(tmp.toFile()))) {
            tar.transferTo(out);
          }
          extracted = true;
          break;
        }
      }
    } catch (IOException e) {
      // ensure temp file is cleaned up if anything fails
      try {
        Files.deleteIfExists(tmp);
      } catch (Exception ignored) {
      }
      throw e;
    }

    if (!extracted) {
      try {
        Files.deleteIfExists(tmp);
      } catch (Exception ignored) {
      }
      throw new IOException("Checkpoint '" + wantedFileName + "' not found in archive "
          + tarGzArchive.getAbsolutePath());
    }

    Files.move(tmp, outPath, StandardCopyOption.REPLACE_EXISTING);
    return outputFile;
  }
}
