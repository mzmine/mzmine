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

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.components.factories.FxIconButtonBuilder;
import io.github.mzmine.javafx.components.factories.FxIconButtonBuilder.EventHandling;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.modules.io.download.DownloadUtils.Result;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskService;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DownloadAssetButton extends HBox {

  private static final Logger logger = Logger.getLogger(DownloadAssetButton.class.getName());
  private final BooleanProperty isDownloading = new SimpleBooleanProperty(false);
  private final DoubleProperty progress = new SimpleDoubleProperty(0);
  private final ButtonBase downloadButton;

  private Consumer<List<File>> onDownloadFinished;
  private final ObjectProperty<FileDownloadTask> task = new SimpleObjectProperty<>();


  public DownloadAssetButton(@NotNull final List<DownloadAsset> assets) {
    this(null, assets);
  }

  public DownloadAssetButton(@Nullable final AssetGroup asset,
      @NotNull final List<DownloadAsset> assets) {
    downloadButton = addDownloadLinksButton(assets);

    ProgressBar progressBar = new ProgressBar();
    progressBar.progressProperty().bind(progress);
    var progressPane = new HBox(4, progressBar,
        FxIconUtil.newIconButton(FxIcons.X_CIRCLE, () -> cancelCurrentTask(this.task)));
    progressPane.setAlignment(Pos.CENTER);

    BorderPane firstPane = new BorderPane();
    firstPane.centerProperty()
        .bind(isDownloading.map(isDownloading -> isDownloading ? progressPane : downloadButton));
    // final layout
    setSpacing(FxLayout.DEFAULT_SPACE);
    if (!assets.isEmpty()) {
      getChildren().add(firstPane);
    }

    if (asset != null) {
      String downloadInfoPage = asset.getDownloadInfoPage();
      if (downloadInfoPage != null) {
        getChildren().add(FxIconUtil.newIconButton(FxIcons.WEBSITE, "Open asset website",
            () -> DesktopService.getDesktop().openWebPage(asset.getDownloadInfoPage())));
      }
    }
  }

  private void cancelCurrentTask(final ObjectProperty<FileDownloadTask> taskProperty) {
    FileDownloadTask task = taskProperty.get();
    if (task != null) {
      task.cancel();
      taskProperty.set(null);
    }
  }

  /**
   * Action called once download is finished
   */
  public void setOnDownloadFinished(final Consumer<List<File>> onDownloadFinished) {
    this.onDownloadFinished = onDownloadFinished;
  }

  private ButtonBase addDownloadLinksButton(final List<DownloadAsset> assets) {
    FxIconButtonBuilder<?> buttonBuilder;
    if (assets.size() == 1) {
      DownloadAsset asset = assets.getFirst();
      buttonBuilder = new FxIconButtonBuilder<>(new Button(), FxIcons.DOWNLOAD);
      buttonBuilder.onAction(
          () -> download(assets.getFirst(), isDownloading, onDownloadFinished, progress, task));
      buttonBuilder.tooltip(asset.getDownloadDescription());
    } else {
      // opens drop down menu with selection of items
      MenuButton menuButton = new MenuButton();

      List<CustomMenuItem> menuItems = assets.stream().map(this::createMenuItem).toList();

      menuButton.getItems().setAll(FXCollections.observableList(menuItems));
      buttonBuilder = new FxIconButtonBuilder<>(menuButton, FxIcons.DOWNLOAD);
    }
    var downloadButton = buttonBuilder.build();
    downloadButton.disableProperty().bind(isDownloading);
    return downloadButton;
  }

  private CustomMenuItem createMenuItem(final DownloadAsset asset) {
    ObjectProperty<FileDownloadTask> taskProperty = new SimpleObjectProperty<>();
    BooleanProperty isDownloading = new SimpleBooleanProperty(false);
    var main = FxLayout.newBorderPane(new Insets(0, 3, 0, 3),
        FxLabels.newBoldLabel(asset.getLabel(false)));
    var progressBar = new ProgressBar();
    progressBar.setMaxWidth(Double.MAX_VALUE);
    progressBar.setOpaqueInsets(new Insets(5, 1, 1, 1));
//    progressBar.prefWidthProperty().bind(main.widthProperty().subtract(15));
    var progressPane = new HBox(4, progressBar,
        FxIconUtil.newIconButton(FxIcons.X_CIRCLE, "Cancel download", EventHandling.CONSUME_EVENTS,
            () -> cancelCurrentTask(taskProperty)));
    HBox.setHgrow(progressBar, Priority.ALWAYS);
    progressPane.setAlignment(Pos.CENTER);
    progressPane.visibleProperty().bind(isDownloading);
    progressPane.managedProperty().bind(isDownloading);
    main.setTop(progressPane);
    Runnable downloadAction = () -> {
      if (!isDownloading.get()) {
        download(asset, isDownloading, files -> onDownloadFinished.accept(files),
            progressBar.progressProperty(), taskProperty);
      }
    };
    main.setRight(new HBox(FxLayout.DEFAULT_ICON_SPACE,
        FxIconUtil.newIconButton(FxIcons.DOWNLOAD, "Download", EventHandling.CONSUME_EVENTS,
            downloadAction),
        FxIconUtil.newIconButton(FxIcons.WEBSITE, "Open website", EventHandling.CONSUME_EVENTS,
            () -> DesktopService.getDesktop()
                .openWebPage(asset.extAsset().getDownloadInfoPage()))));

    var menu = new CustomMenuItem(main);
    menu.setOnAction(_ -> downloadAction.run());
    return menu;
  }

  /**
   * Starts a download task and blocks any further download attempts
   */
  private static void download(final DownloadAsset asset, final BooleanProperty isDownloading,
      final Consumer<List<File>> onDownloadFinished, final DoubleProperty progress,
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
      var result = DownloadUtils.showUseExistingFilesDialog(existing);
      skipExisting = result == Result.USE_EXISTING;
      if (!skipExisting) {
        // download all again
        existing = List.of();
      }
    }
    if (skipExisting) {
      if (existing.size() == finalFile.size()) {
        // all are existing
        onDownloadFinished.accept(finalFile);
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
        onDownloadFinished.accept(files);
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
