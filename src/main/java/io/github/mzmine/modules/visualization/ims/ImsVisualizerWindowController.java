package io.github.mzmine.modules.visualization.ims;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

public class ImsVisualizerWindowController {

  @FXML public FlowPane flowPane;

  @FXML public BorderPane plotePaneMI;

  @FXML public BorderPane plotePaneMF;
  @FXML public BorderPane plotePaneIRT;
  @FXML public BorderPane plotePaneHeatMap;

  BorderPane getPlotPaneMI() {
    return plotePaneMI;
  }

  BorderPane getPlotPaneMF() {
    return plotePaneMF;
  }

  BorderPane getPlotePaneIRT() {
    return plotePaneIRT;
  }

  BorderPane getPlotePaneHeatMap()
  {
    return plotePaneHeatMap;
  }
}
