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
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.networking.visual.FeatureNetworkController;
import io.github.mzmine.modules.visualization.networking.visual.NetworkGraphData;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MVCI controller for the network overview window. It offloads the heavy graph data generation
 * ({@link FeatureNetworkController#createGraphData}) to a task thread so the window becomes
 * responsive immediately. The remaining FX-bound setup runs on the GUI thread once the graph is
 * ready.
 */
public class NetworkOverviewMvciController extends FxController<NetworkOverviewModel> {

  private static final Logger logger = Logger.getLogger(
      NetworkOverviewMvciController.class.getName());

  private final NetworkOverviewController networkOverviewController;
  private final BorderPane fxmlPane;

  public NetworkOverviewMvciController(@NotNull ModularFeatureList featureList,
      @Nullable FeatureTableFX externalTable, @Nullable List<? extends FeatureListRow> focussedRows,
      @NotNull NetworkOverviewFlavor flavor) throws IOException {
    super(new NetworkOverviewModel(featureList, externalTable, focussedRows, flavor));

    // Load the overview pane FXML – fast (layout + controller injection only, no graph generation)
    final FXMLLoader loader = new FXMLLoader(
        NetworkOverviewController.class.getResource("NetworkOverviewPane.fxml"));
    fxmlPane = loader.load();
    networkOverviewController = loader.getController();

    // Show a loading indicator in the network area while the graph is built on the task thread
    final ProgressIndicator spinner = new ProgressIndicator(-1);
    spinner.setMaxSize(80, 80);
    final VBox loadingBox = new VBox(10, spinner, new Label("Loading network…"));
    loadingBox.setAlignment(Pos.CENTER);
    networkOverviewController.pnNetwork.setCenter(loadingBox);
  }

  /**
   * Starts network setup. Graph data generation runs on a task thread; the FX-bound setup completes
   * on the GUI thread afterwards.
   */
  public void initNetwork() {
    onTaskThread(new NetworkSetupTask());
  }

  @Override
  protected @NotNull FxViewBuilder<NetworkOverviewModel> getViewBuilder() {
    return new NetworkOverviewViewBuilder(model, fxmlPane);
  }

  @Override
  public void close() {
    super.close();
    networkOverviewController.close();
  }

  public @NotNull NetworkOverviewController getNetworkOverviewController() {
    return networkOverviewController;
  }


  /**
   * Splits the heavy graph-data generation (task thread) from the FX-bound setup (GUI thread).
   */
  private final class NetworkSetupTask extends FxUpdateTask<NetworkOverviewModel> {

    // computed on task thread, consumed in updateGuiModel
    private NetworkGraphData graphData;

    private NetworkSetupTask() {
      super("Create feature network graph", NetworkOverviewMvciController.this.model);
    }

    @Override
    protected void process() {
      // decision: only createNewGraph runs on the task thread; all FX setup stays on GUI thread
      graphData = FeatureNetworkController.createGraphData(model.getFeatureList());
    }

    @Override
    protected void updateGuiModel() {
      try {
        final FeatureNetworkController featureNetworkController = FeatureNetworkController.createWithGraphData(
            model.getFeatureList(), networkOverviewController.getFocussedRows(), graphData,
            model.getFlavor());
        networkOverviewController.setUpWithNetworkController(featureNetworkController,
            model.getFeatureList(), model.getExternalTable(), model.getFocussedRows());
      } catch (IOException ex) {
        logger.log(Level.WARNING, "Could not set up network overview: " + ex.getMessage(), ex);
      }
    }

    @Override
    public String getTaskDescription() {
      return "Generating network graph";
    }

    @Override
    public double getFinishedPercentage() {
      return 0;
    }
  }
}
