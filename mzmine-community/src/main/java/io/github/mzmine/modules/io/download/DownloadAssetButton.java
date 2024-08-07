/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.components.factories.FxIconButtonBuilder;
import io.github.mzmine.javafx.components.factories.MenuItems;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskService;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

public class DownloadAssetButton extends HBox {

  private static final Logger logger = Logger.getLogger(DownloadAssetButton.class.getName());
  private final BooleanProperty isDownloading = new SimpleBooleanProperty(false);
  private final DoubleProperty progress = new SimpleDoubleProperty(0);
  private final ButtonBase downloadButton;

  private Consumer<File> onDownloadFinished;
  private FileDownloadTask task;


  public DownloadAssetButton(@NotNull final ExternalAsset asset,
      @NotNull final List<DownloadAsset> assets) {
    downloadButton = addDownloadLinksButton(assets);

    ProgressBar progressBar = new ProgressBar();
    progressBar.progressProperty().bind(progress);
    var progressPane = new HBox(4, progressBar,
        FxIconUtil.newIconButton(FxIcons.X_CIRCLE, this::cancelCurrentTask));
    progressPane.setAlignment(Pos.CENTER);

    BorderPane firstPane = new BorderPane();
    firstPane.centerProperty()
        .bind(isDownloading.map(isDownloading -> isDownloading ? progressPane : downloadButton));
    // final layout
    setSpacing(FxLayout.DEFAULT_SPACE);
    if (!assets.isEmpty()) {
      getChildren().add(firstPane);
    }

    String downloadInfoPage = asset.getDownloadInfoPage();
    if (downloadInfoPage != null) {
      getChildren().add(FxIconUtil.newIconButton(FxIcons.WEBSITE, "Open asset website",
          () -> DesktopService.getDesktop().openWebPage(asset.getDownloadInfoPage())));
    }
  }

  private void cancelCurrentTask() {
    if (task != null) {
      task.cancel();
    }
  }

  /**
   * Action called once download is finished
   */
  public void setOnDownloadFinished(final Consumer<File> onDownloadFinished) {
    this.onDownloadFinished = onDownloadFinished;
  }

  private ButtonBase addDownloadLinksButton(final List<DownloadAsset> assets) {
    FxIconButtonBuilder buttonBuilder;
    if (assets.size() == 1) {
      DownloadAsset asset = assets.getFirst();
      buttonBuilder = new FxIconButtonBuilder(new Button(), FxIcons.DOWNLOAD);
      buttonBuilder.onAction(() -> download(assets.getFirst()));
      buttonBuilder.tooltip(asset.getDownloadDescription());
    } else {
      // opens drop down menu with selection of items
      MenuButton menuButton = new MenuButton();
      var menuItems = assets.stream()
          .map(asset -> MenuItems.create(asset.getLabel(false), () -> download(asset))).toList();
      menuButton.getItems().setAll(FXCollections.observableList(menuItems));
      buttonBuilder = new FxIconButtonBuilder(menuButton, FxIcons.DOWNLOAD);
    }
    var downloadButton = buttonBuilder.build();
    downloadButton.disableProperty().bind(isDownloading);
    return downloadButton;
  }

  /**
   * Starts a download task and blocks any further download attempts
   */
  private void download(final DownloadAsset asset) {
    if (isDownloading.get()) {
      logger.fine("Already downloading");
      return;
    }
    // check if file exists
    File finalFile = asset.getEstimatedFinalFile();
    if (finalFile.exists()) {
      Optional<ButtonType> resultButton = DialogLoggerUtil.showDialog(AlertType.CONFIRMATION,
          "File already exists", "Use existing file or download again",
          new ButtonType("Use existing", ButtonData.CANCEL_CLOSE),
          new ButtonType("Download", ButtonData.APPLY));
      // cancel or use existing will just set the filename
      if (resultButton.isEmpty() || resultButton.get().getButtonData() == ButtonData.CANCEL_CLOSE) {
        onDownloadFinished.accept(finalFile);
        return;
      }
      // otherwise download
    }

    isDownloading.set(true);
    task = new FileDownloadTask(asset);
    task.addTaskStatusListener((_, _, _) -> {
      if (task.isFinished() || task.isCanceled()) {
        FxThread.runLater(() -> isDownloading.set(false));
      }
      if (task.isFinished() && onDownloadFinished != null) {
        // search for main file and set it to parameter
        task.getDownloadedFiles().stream().filter(
                file -> asset.mainFileName() == null || file.getName()
                    .equalsIgnoreCase(asset.mainFileName())).findFirst()
            .ifPresent(file -> onDownloadFinished.accept(file));
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

}
