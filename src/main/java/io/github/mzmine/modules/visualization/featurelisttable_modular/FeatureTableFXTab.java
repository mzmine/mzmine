package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import java.io.IOException;
import java.util.Collection;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javax.annotation.Nonnull;

public class FeatureTableFXTab extends MZmineTab {
  private FeatureTableFX table;

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

    FeatureTableFXMLTabAnchorPaneController controller = loader.getController();
    controller.setFeatureList(flist);

    setContent(root);
  }

  public FeatureTableFX getTable() {
    return table;
  }

  public FeatureList getFeatureList() {
    return table.getFeatureList();
  }

  // TODO: implement methods inherited from MZmineTab

  @Nonnull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return null;
  }

  @Nonnull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return null;
  }

  @Nonnull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return null;
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(
      Collection<? extends FeatureList> featurelists) {

  }
}
