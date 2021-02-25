package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javax.annotation.Nonnull;

public class FeatureTableTab extends MZmineTab {
  private final Image SELECTION_ICON =
      FxIconUtil.loadImageFromResources("icons/propertiesicon.png");

  private final BorderPane mainPane;
  private final ToolBar toolBar;

  private FeatureTableFXMLTabAnchorPaneController controller;

  public FeatureTableTab(FeatureList flist) {
    super("Feature Table", true, false);
    mainPane = new BorderPane();
    toolBar = new ToolBar();

    // Setup feature table
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

    // TODO: if there would be only selectColumnsButton in the toolbar, then remove toolbar and
    //  improve "+" button behaviour of the feature table header
    // Setup tool bar
    toolBar.setOrientation(Orientation.VERTICAL);

    Button selectColumnsButton = new Button(null, new ImageView(SELECTION_ICON));
    selectColumnsButton.setTooltip(new Tooltip("Select columns to show/hide"));
    selectColumnsButton.setOnAction(e -> {
      controller.miParametersOnAction(null);
    });

    toolBar.getItems().addAll(selectColumnsButton);

    // Setup main pane
    mainPane.setCenter(root);
    mainPane.setRight(toolBar);

    setContent(mainPane);
  }

  public FeatureList getFeatureList() {
    return controller.getFeatureList();
  }

  @Nonnull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return getFeatureList()==null? Collections.emptyList() : getFeatureList().getRawDataFiles();
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
