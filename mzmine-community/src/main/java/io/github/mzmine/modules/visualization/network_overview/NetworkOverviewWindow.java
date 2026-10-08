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

package io.github.mzmine.modules.visualization.network_overview;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetworkOverviewWindow extends Stage {

  private static final Logger logger = Logger.getLogger(NetworkOverviewWindow.class.getName());
  private NetworkOverviewMvciController mvciController;

  public NetworkOverviewWindow(@NotNull ModularFeatureList featureList,
      @Nullable FeatureTableFX externalTable, @Nullable List<? extends FeatureListRow> selectedRows,
      final NetworkOverviewFlavor flavor) {
    setTitle("Network overview: " + featureList.getName());
    try {
      mvciController = new NetworkOverviewMvciController(featureList, externalTable, selectedRows,
          flavor);
      setOnCloseRequest(_ -> mvciController.close());

      final Scene mainScene = new Scene(mvciController.buildView());
      mainScene.getStylesheets()
          .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
      setScene(mainScene);

      // Start graph generation on a task thread after the window is ready to show
      mvciController.initNetwork();
    } catch (Exception ex) {
      final Scene mainScene = new Scene(
          new BorderPane(new Label("Could not load pane, see log for more information")));
      mainScene.getStylesheets()
          .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
      setScene(mainScene);
      logger.log(Level.WARNING, "Could not load network overview pane " + ex.getMessage(), ex);
    }
  }

  public NetworkOverviewController getController() {
    return mvciController != null ? mvciController.getNetworkOverviewController() : null;
  }
}
