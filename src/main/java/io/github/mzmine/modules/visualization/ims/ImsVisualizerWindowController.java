package io.github.mzmine.modules.visualization.ims;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

public class ImsVisualizerWindowController {

  @FXML public BorderPane topPlot;
  @FXML public BorderPane bottomPlot;



  BorderPane getTopPlot()
  {
    return topPlot;
  }
  BorderPane getbottomPlot(){return  bottomPlot;}
}
