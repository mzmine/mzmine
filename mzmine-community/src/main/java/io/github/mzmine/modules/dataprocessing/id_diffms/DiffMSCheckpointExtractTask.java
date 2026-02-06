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

import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extracts {@link DiffMSCheckpointFiles#DEFAULT_CHECKPOINT_FILE_NAME} from the
 * downloaded
 * {@code diffms_checkpoints.tar.gz} archive.
 */
public class DiffMSCheckpointExtractTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(DiffMSCheckpointExtractTask.class.getName());

  private final @NotNull File archiveTarGz;
  private final @NotNull File outputCheckpoint;
  private final @NotNull String wantedFileName;
  private final @Nullable Consumer<File> onFinished;

  private volatile String description = "DiffMS: preparing checkpoint";
  private volatile boolean done;

  public DiffMSCheckpointExtractTask(final @NotNull File archiveTarGz,
      final @NotNull File outputCheckpoint, final @NotNull String wantedFileName,
      final @Nullable Consumer<File> onFinished) {
    super(Instant.now());
    this.archiveTarGz = Objects.requireNonNull(archiveTarGz);
    this.outputCheckpoint = Objects.requireNonNull(outputCheckpoint);
    this.wantedFileName = Objects.requireNonNull(wantedFileName);
    this.onFinished = onFinished;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return done ? 1d : 0d;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try {
      if (outputCheckpoint.isFile() && outputCheckpoint.length() > 0) {
        description = "DiffMS: checkpoint ready";
        done = true;
        if (onFinished != null) {
          onFinished.accept(outputCheckpoint);
        }
        setStatus(TaskStatus.FINISHED);
        return;
      }

      description = "DiffMS: extracting checkpoint";
      DiffMSCheckpointFiles.extractWantedCheckpointFromTarGz(archiveTarGz, outputCheckpoint,
          wantedFileName, this::isCanceled);

      if (isCanceled()) {
        return;
      }

      description = "DiffMS: checkpoint ready";
      done = true;
      if (onFinished != null) {
        onFinished.accept(outputCheckpoint);
      }
      setStatus(TaskStatus.FINISHED);
    } catch (Exception e) {
      logger.log(Level.WARNING, "DiffMS: failed to extract checkpoint", e);
      setErrorMessage("DiffMS: failed to extract checkpoint: " + e.getMessage());
      setStatus(TaskStatus.ERROR);
    }
  }
}
