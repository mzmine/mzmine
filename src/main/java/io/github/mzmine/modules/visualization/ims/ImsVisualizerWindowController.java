package io.github.mzmine.modules.visualization.ims;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

public class ImsVisualizerWindowController {

  @FXML public FlowPane flowPane;

  @FXML public BorderPane plotePaneMI;

  @FXML public BorderPane plotePaneMMZ;
  @FXML public BorderPane plotePaneIRT;
  @FXML public BorderPane plotePane3;

  BorderPane getPlotPaneMI() {
    return plotePaneMI;
  }

  BorderPane getPlotPaneMMZ() {
    return plotePaneMMZ;
  }

  BorderPane getPlotePaneIRT() {
    return plotePaneIRT;
  }
}
