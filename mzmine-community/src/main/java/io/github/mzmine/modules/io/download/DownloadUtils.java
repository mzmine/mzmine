/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.io.download;

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskService;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

public class DownloadUtils {

  private static final Logger logger = Logger.getLogger(DownloadUtils.class.getName());

  /**
   * Starts a download task and blocks any further download attempts
   */
  static void download(final DownloadAsset asset, final BooleanProperty isDownloading,
      final ObjectProperty<Consumer<List<File>>> onDownloadFinished, final DoubleProperty progress,
      final ObjectProperty<FileDownloadTask> taskProperty) {
    if (isDownloading.get()) {
      logger.fine("Already downloading");
      return;
    }
    // check if file exists
    List<File> finalFile = asset.getEstimatedFinalFiles();
    List<File> existing = finalFile.stream().filter(File::exists).toList();
    boolean skipExisting = false;
    if (!existing.isEmpty()) {
      var result = showUseExistingFilesDialog(existing);
      skipExisting = result == HandleExistingFiles.USE_EXISTING;
      if (!skipExisting) {
        // download all again
        existing = List.of();
      }
    }
    if (skipExisting) {
      if (existing.size() == finalFile.size()) {
        // all are existing
        var consumer = onDownloadFinished.get();
        if (consumer != null) {
          consumer.accept(finalFile);
        }
        return;
      }
    }

    isDownloading.set(true);
    final var task = new FileDownloadTask(asset);
    if (skipExisting) {
      task.setSkipFiles(existing);
    }
    taskProperty.set(task);
    task.addTaskStatusListener((_, _, _) -> {
      if (task.isFinished() || task.isCanceled()) {
        FxThread.runLater(() -> {
          isDownloading.set(false);
          taskProperty.set(null);
        });
      }
      if (task.isFinished() && onDownloadFinished != null) {
        // search for main file and set it to parameter
        List<File> files = task.getDownloadedFiles();
        if (asset.mainFileName() != null) {
          files = files.stream()
              .filter(file -> file.getName().equalsIgnoreCase(asset.mainFileName())).toList();
        }
        var consumer = onDownloadFinished.get();
        if (consumer != null) {
          consumer.accept(files);
        }
      }
    });
    TaskService.getController().addTask(task, TaskPriority.HIGH);

    // check for task progress updates periodically
    PauseTransition progressUpdater = new PauseTransition(Duration.millis(500));
    progressUpdater.setOnFinished(_ -> {
      progress.set(task.getFinishedPercentage());
      if (!(task.isFinished() || task.isCanceled())) {
        // still running
        progressUpdater.playFromStart();
      }
    });
    progressUpdater.playFromStart();
  }

  static void cancelCurrentTask(final ObjectProperty<FileDownloadTask> taskProperty) {
    FileDownloadTask task = taskProperty.get();
    if (task != null) {
      task.cancel();
      taskProperty.set(null);
    }
  }

  public enum HandleExistingFiles {
    USE_EXISTING, DOWNLOAD_AGAIN
  }

  /**
   * Will call on FxThread and wait for user to close the dialog
   *
   * @param existing existing files
   * @return either use existing or download again as result
   */
  public static HandleExistingFiles showUseExistingFilesDialog(@NotNull Collection<File> existing) {
    final AtomicReference<HandleExistingFiles> result = new AtomicReference<>(
        HandleExistingFiles.USE_EXISTING);
    FxThread.runOnFxThreadAndWait(() -> {
      Optional<ButtonType> resultButton = DialogLoggerUtil.showDialog(AlertType.CONFIRMATION,
          "File(s) already exists",
          "Use existing file(s) or download again\n" + existing.stream().map(File::getAbsolutePath)
              .collect(Collectors.joining("\n")),
          new ButtonType("Use existing", ButtonData.CANCEL_CLOSE),
          new ButtonType("Download", ButtonData.APPLY));
      // cancel or use existing will just set the filename
      result.set(
          (resultButton.isEmpty() || resultButton.get().getButtonData() == ButtonData.CANCEL_CLOSE)
              ? HandleExistingFiles.USE_EXISTING : HandleExistingFiles.DOWNLOAD_AGAIN);
    });
    return result.get();
  }
}
