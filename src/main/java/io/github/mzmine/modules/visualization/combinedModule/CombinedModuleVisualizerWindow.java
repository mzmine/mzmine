package io.github.mzmine.modules.visualization.combinedModule;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.neutralloss.NeutralLossParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.javafx.FxIconUtil;
import io.github.mzmine.util.javafx.WindowsMenu;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class CombinedModuleVisualizerWindow extends Stage {

  private static final Image PRECURSOR_MASS_ICON =
      FxIconUtil.loadImageFromResources("icons/datapointsicon.png");

  private ToolBar toolBar;
  private BorderPane borderPane;
  private Scene scene;
  private RawDataFile dataFile;

  public CombinedModuleVisualizerWindow(RawDataFile dataFile, ParameterSet parameters) {
    this.dataFile = dataFile;
    borderPane = new BorderPane();
    scene = new Scene(borderPane);
    scene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    setScene(scene);
    toolBar = new ToolBar();
    toolBar.setOrientation(Orientation.VERTICAL);
    Button highlightPrecursorBtn = new Button(null, new ImageView(PRECURSOR_MASS_ICON));
    toolBar.getItems().add(highlightPrecursorBtn);
    borderPane.setRight(toolBar);
    Range<Double> rtRange =
        parameters.getParameter(CombinedModuleParameters.retentionTimeRange).getValue();
    Range<Double> mzRange = parameters.getParameter(CombinedModuleParameters.mzRange).getValue();
    Object xAxisType = parameters.getParameter(NeutralLossParameters.xAxisType).getValue();
    WindowsMenu.addWindowsMenu(getScene());


  }

}
