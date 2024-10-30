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
import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import org.jetbrains.annotations.NotNull;

public class DownloadUtils {

  public enum Result {
    USE_EXISTING, DOWNLOAD_AGAIN
  }

  /**
   * Will call on FxThread and wait for user to close the dialog
   *
   * @param existing existing files
   * @return either use existing or download again as result
   */
  public static Result showUseExistingFilesDialog(@NotNull Collection<File> existing) {
    final AtomicReference<Result> result = new AtomicReference<>(Result.USE_EXISTING);
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
              ? Result.USE_EXISTING : Result.DOWNLOAD_AGAIN);
    });
    return result.get();
  }
}
