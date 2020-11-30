package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javax.annotation.Nonnull;

public class FeatureTableFXTab extends MZmineTab {
  private FeatureTableFX table;
  private FeatureTableFXMLTabAnchorPaneController controller;

  public FeatureTableFXTab(FeatureList flist) {
    super("Feature Table", true, false);

    table = new FeatureTableFX();

    FXMLLoader loader =
        new FXMLLoader((FeatureTableFX.class.getResource("FeatureTableFXMLTabAnchorPane.fxml")));

    AnchorPane root = null;
    try {
      root = loader.load();
      logger.finest("Feature table anchor pane has been successfully loaded from the FXML loader.");
    } catch (IOException e) {
      e.printStackTrace();
    }

    controller = loader.getController();
    controller.setFeatureList(flist);

    setContent(root);
  }

  public FeatureTableFX getTable() {
    return table;
  }

  public FeatureList getFeatureList() {
    return table.getFeatureList();
  }

  @Nonnull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return getFeatureList().getRawDataFiles();
  }

  @Nonnull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return !getFeatureList().isAligned()
        ? Collections.singletonList(getFeatureList())
        : Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return getFeatureList().isAligned()
        ? Collections.singletonList(getFeatureList())
        : Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
    if(featureLists == null || featureLists.isEmpty()) {
      return;
    }

    // Get first selected feature list
    FeatureList featureList = featureLists.iterator().next();

    controller.setFeatureList(featureList);
  }

  @Override
  public void onAlignedFeatureListSelectionChanged(
      Collection<? extends FeatureList> featurelists) {
    onFeatureListSelectionChanged(featurelists);
  }
}
