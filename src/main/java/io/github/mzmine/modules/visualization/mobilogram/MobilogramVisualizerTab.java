package io.github.mzmine.modules.visualization.mobilogram;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javax.annotation.Nonnull;

public class MobilogramVisualizerTab extends MZmineTab {

  private MobilogramVisualizerController controller;

  public MobilogramVisualizerTab() {
    super("Mobilogram visualizer", true, true);

    controller = null;
    FXMLLoader loader = new FXMLLoader((getClass().getResource("MobilogramVisualizerPane.fxml")));

    try {
      AnchorPane root = loader.load();
      controller = loader.getController();
      controller.initialize();
      setContent(root);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Could not load MobilogramVisualizerPane.fxml", e);
      return;
    }

  }

  @Nonnull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return controller.getRawDataFiles();
  }

  @Nonnull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    controller.setRawDataFiles(new ArrayList<>(rawDataFiles));
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featurelists) {

  }
}
