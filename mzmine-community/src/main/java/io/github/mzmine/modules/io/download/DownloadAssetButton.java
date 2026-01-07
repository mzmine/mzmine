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

package io.github.mzmine.modules.io.download;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.components.factories.FxIconButtonBuilder;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DownloadAssetButton extends HBox {

  private static final Logger logger = Logger.getLogger(DownloadAssetButton.class.getName());
  private final BooleanProperty isDownloading = new SimpleBooleanProperty(false);
  private final DoubleProperty progress = new SimpleDoubleProperty(0);
  private final ButtonBase downloadButton;
  private final BooleanProperty isSingleButton = new SimpleBooleanProperty(true);
  private final ObjectProperty<Consumer<List<File>>> onDownloadFinished = new SimpleObjectProperty<>();
  private final ObjectProperty<FileDownloadTask> task = new SimpleObjectProperty<>();
  // may be empty if there is only one download asset
  private final List<DownloadAssetMenuItem> menuItems = new ArrayList<>();


  public DownloadAssetButton(@NotNull final List<DownloadAsset> assets) {
    this(null, assets);
  }

  public DownloadAssetButton(@Nullable final AssetGroup asset,
      @NotNull final List<DownloadAsset> assets) {
    downloadButton = addDownloadLinksButton(assets);

    ProgressBar progressBar = new ProgressBar();
    progressBar.progressProperty().bind(progress);
    var progressPane = new HBox(4, progressBar,
        FxIconUtil.newIconButton(FxIcons.X_CIRCLE, () -> cancelAllTasks()));
    progressPane.setAlignment(Pos.CENTER);

    progressPane.visibleProperty().bind(isDownloading);
    progressPane.managedProperty().bind(isDownloading);

    BorderPane firstPane = new BorderPane();
    firstPane.setCenter(new HBox(downloadButton, progressPane));
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

  private void cancelAllTasks() {
    DownloadUtils.cancelCurrentTask(this.task);
    menuItems.forEach(m -> m.cancelTask());
  }

  /**
   * Action called once download is finished
   */
  public void setOnDownloadFinished(final Consumer<List<File>> onDownloadFinished) {
    this.onDownloadFinished.set(onDownloadFinished);
  }

  private ButtonBase addDownloadLinksButton(final List<DownloadAsset> assets) {
    final ButtonBase downloadButton;
    if (assets.size() == 1) {
      isSingleButton.set(true);
      DownloadAsset asset = assets.getFirst();
      var buttonBuilder = new FxIconButtonBuilder<>(new Button(), FxIcons.DOWNLOAD);
      buttonBuilder.onAction(
          () -> DownloadUtils.download(assets.getFirst(), isDownloading, onDownloadFinished,
              progress, task));
      buttonBuilder.tooltip(asset.getDownloadDescription());
      downloadButton = buttonBuilder.build();

      downloadButton.disableProperty().bind(isDownloading);
      BooleanBinding notDownloading = isDownloading.not();
      downloadButton.visibleProperty().bind(notDownloading);
      downloadButton.managedProperty().bind(notDownloading);
    } else {
      // opens drop down menu with selection of items
      MenuButton menuButton = new MenuButton();
      isSingleButton.set(false);
      menuItems.addAll(
          assets.stream().map(asset -> new DownloadAssetMenuItem(asset, onDownloadFinished))
              .toList());
      var allProgress = menuItems.stream().map(DownloadAssetMenuItem::progressProperty)
          .toArray(DoubleProperty[]::new);
      var allIsDownloading = menuItems.stream().map(DownloadAssetMenuItem::isDownloadingProperty)
          .toArray(BooleanProperty[]::new);
      var allProgressBinding = Bindings.createDoubleBinding(() -> {
        if (allProgress.length == 0) {
          return 0d;
        }
        int n = 0;
        double sum = 0;
        for (int i = 0; i < allProgress.length; i++) {
          if (allIsDownloading[i].get()) {
            n++;
            sum += allProgress[i].get();
          }
        }
        return sum / n;
      }, allProgress);

      progress.bind(allProgressBinding);

      isDownloading.bind(Bindings.createBooleanBinding(() -> {
        // any true (OR)
        for (final BooleanProperty bp : allIsDownloading) {
          if (bp.get()) {
            return true;
          }
        }
        return false;
      }, allIsDownloading));

      menuButton.getItems().setAll(FXCollections.observableList(menuItems));
      var buttonBuilder = new FxIconButtonBuilder<>(menuButton, FxIcons.DOWNLOAD);
      downloadButton = buttonBuilder.build();
    }

    return downloadButton;
  }

}
