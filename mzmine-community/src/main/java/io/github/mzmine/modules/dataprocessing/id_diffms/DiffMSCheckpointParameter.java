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

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.modules.io.download.AssetGroup;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameComponent;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskService;
import java.io.File;
import java.util.List;
import java.util.Locale;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link FileNameParameter} that provides a one-click download button for
 * DiffMS checkpoints.
 * <p>
 * The downloaded file from Zenodo is a {@code .tar.gz} archive; we
 * automatically extract
 * {@link DiffMSCheckpointFiles#DEFAULT_CHECKPOINT_FILE_NAME} and set the
 * parameter to that file.
 */
public class DiffMSCheckpointParameter extends FileNameParameter {

  public DiffMSCheckpointParameter(final @NotNull String name, final @NotNull String description,
      final @NotNull List<ExtensionFilter> filters, final @NotNull FileSelectionType type) {
    super(name, description, filters, type);
  }

  public DiffMSCheckpointParameter(final @NotNull String name, final @NotNull String description,
      final @NotNull List<ExtensionFilter> filters, final @NotNull FileSelectionType type,
      final boolean allowEmptyString) {
    super(name, description, filters, type, allowEmptyString);
  }

  @Override
  public FileNameComponent createEditingComponent() {
    final var comp = new FileNameComponent(getLastFiles(), getType(), filters, AssetGroup.DIFFMS,
        AssetGroup.DIFFMS.getDownloadAssets());

    // Override default behavior: after download, extract the actual .ckpt and set
    // it as value.
    comp.setOnDownloadFinished(files -> {
      if (files == null || files.isEmpty()) {
        return;
      }
      final File downloaded = files.getFirst();
      if (downloaded == null || !downloaded.isFile()) {
        return;
      }

      final String n = downloaded.getName().toLowerCase(Locale.ROOT);
      if (n.endsWith(".ckpt")) {
        FxThread.runLater(() -> comp.setValue(downloaded));
        return;
      }

      if (DiffMSCheckpointFiles.isTarGz(downloaded)) {
        final File out = DiffMSCheckpointFiles.getDefaultCheckpointFile();
        final var task = new DiffMSCheckpointExtractTask(downloaded, out,
            DiffMSCheckpointFiles.DEFAULT_CHECKPOINT_FILE_NAME,
            extracted -> FxThread.runLater(() -> comp.setValue(extracted)));
        TaskService.getController().addTask(task, TaskPriority.HIGH);
        return;
      }

      // Fallback: if we downloaded something unexpected, just set it so user can fix
      // manually.
      FxThread.runLater(() -> comp.setValue(downloaded));
    });

    return comp;
  }

  @Override
  public DiffMSCheckpointParameter cloneParameter() {
    final DiffMSCheckpointParameter copy = new DiffMSCheckpointParameter(name, description, filters,
        type, allowEmptyString);
    copy.setValue(this.getValue());
    copy.setLastFiles(new java.util.ArrayList<>(getLastFiles()));
    return copy;
  }
}
