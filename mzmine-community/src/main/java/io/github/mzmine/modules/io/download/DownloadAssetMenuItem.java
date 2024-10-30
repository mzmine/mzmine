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
import io.github.mzmine.javafx.components.factories.FxIconButtonBuilder.EventHandling;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class DownloadAssetMenuItem extends CustomMenuItem {

  private final ProgressBar progressBar;
  private final BooleanProperty isDownloading = new SimpleBooleanProperty(false);

  public DownloadAssetMenuItem(final DownloadAsset asset,
      final ObjectProperty<Consumer<List<File>>> onDownloadFinished) {
    ObjectProperty<FileDownloadTask> taskProperty = new SimpleObjectProperty<>();
    var main = FxLayout.newBorderPane(new Insets(0, 3, 0, 3),
        FxLabels.newBoldLabel(asset.getLabel(false)));
    progressBar = new ProgressBar();
    progressBar.setMaxWidth(Double.MAX_VALUE);
    progressBar.setOpaqueInsets(new Insets(5, 1, 1, 1));

    var progressPane = new HBox(4, progressBar,
        FxIconUtil.newIconButton(FxIcons.X_CIRCLE, "Cancel download", EventHandling.CONSUME_EVENTS,
            () -> DownloadUtils.cancelCurrentTask(taskProperty)));
    HBox.setHgrow(progressBar, Priority.ALWAYS);
    progressPane.setAlignment(Pos.CENTER);
    progressPane.visibleProperty().bind(isDownloading);
    progressPane.managedProperty().bind(isDownloading);
    main.setTop(progressPane);
    Runnable downloadAction = () -> {
      if (!isDownloading.get()) {
        DownloadUtils.download(asset, isDownloading, onDownloadFinished,
            progressBar.progressProperty(), taskProperty);
      }
    };
    main.setRight(new HBox(FxLayout.DEFAULT_ICON_SPACE,
        FxIconUtil.newIconButton(FxIcons.DOWNLOAD, "Download", EventHandling.CONSUME_EVENTS,
            downloadAction),
        FxIconUtil.newIconButton(FxIcons.WEBSITE, "Open website", EventHandling.CONSUME_EVENTS,
            () -> DesktopService.getDesktop()
                .openWebPage(asset.extAsset().getDownloadInfoPage()))));

    setOnAction(_ -> downloadAction.run());
    setContent(main);
  }

  public DoubleProperty progressProperty() {
    return progressBar.progressProperty();
  }

  public BooleanProperty isDownloadingProperty() {
    return isDownloading;
  }
}
