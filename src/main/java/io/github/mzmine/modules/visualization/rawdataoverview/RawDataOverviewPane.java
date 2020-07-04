package io.github.mzmine.modules.visualization.rawdataoverview;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;

public class RawDataOverviewPane extends MZmineTab {

  public static final Logger logger = Logger.getLogger(RawDataOverviewPane.class.getName());

  private RawDataOverviewWindowController controller;

  public RawDataOverviewPane() {
    super("Raw data overview");

    controller = null;
    FXMLLoader loader = new FXMLLoader((getClass().getResource("RawDataOverviewWindow.fxml")));

    try {
      BorderPane root = loader.load();
      controller = loader.getController();
      controller.initialize();
      setContent(root);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not load RawDataOverview.fxml", e);
      return;
    }
  }

  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return controller.getRawDataFiles();
  }

  @Override
  public Collection<? extends ModularFeatureList> getFeatureLists() {
    return null;
  }

  @Override
  public Collection<? extends ModularFeatureList> getAlignedFeatureLists() {
    return null;
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    controller.setRawDataFiles((Collection<RawDataFile>) rawDataFiles);
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends ModularFeatureList> featureLists) {
    return;
  }

  @Override
  public void onAlignedFeatureListSelectionChanged(
      Collection<? extends ModularFeatureList> featurelists) {
    return;
  }
}
