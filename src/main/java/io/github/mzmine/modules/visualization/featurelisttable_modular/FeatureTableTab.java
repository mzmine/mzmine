/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.featurelisttable_modular;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

public class FeatureTableTab extends MZmineTab {

  private static final Logger logger = Logger.getLogger(FeatureTableTab.class.getName());
  private final BorderPane mainPane;
  private final ToolBar toolBar;

  private final FeatureTableFXMLTabAnchorPaneController controller;

  public FeatureTableTab(FeatureList flist) {
    super("Feature Table", true, false);
    mainPane = new BorderPane();
    toolBar = new ToolBar();

    // Setup feature table
    FXMLLoader loader = new FXMLLoader(
        (FeatureTableFX.class.getResource("FeatureTableFXMLTabAnchorPane.fxml")));

    AnchorPane root = null;
    try {
      root = loader.load();
      logger.finest("Feature table anchor pane has been successfully loaded from the FXML loader.");
    } catch (IOException e) {
      logger.log(Level.WARNING, "Error during feature list loading from fxml", e);
    }

    controller = loader.getController();
    controller.setFeatureList(flist);

    // TODO: if there would be only selectColumnsButton in the toolbar, then remove toolbar and
    //  improve "+" button behaviour of the feature table header
    // Setup tool bar
    toolBar.setOrientation(Orientation.VERTICAL);

    Image SELECTION_ICON = FxIconUtil.loadImageFromResources("icons/propertiesicon.png");
    Button selectColumnsButton = new Button(null, new ImageView(SELECTION_ICON));
    selectColumnsButton.setTooltip(new Tooltip("Select columns to show/hide"));
    selectColumnsButton.setOnAction(e -> controller.miParametersOnAction(null));

    toolBar.getItems().addAll(selectColumnsButton);

    // Setup main pane
    mainPane.setCenter(root);
    mainPane.setRight(toolBar);

    setContent(mainPane);

    setOnClosed(e -> {
      controller.close();
      setOnClosed(null);
    });
  }

  public FeatureTableFXMLTabAnchorPaneController getController() {
    return controller;
  }

  public BorderPane getMainPane() {
    return mainPane;
  }

  public FeatureList getFeatureList() {
    return controller.getFeatureList();
  }
  public FeatureTableFX getFeatureTable() {
    return controller.getFeatureTable();
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return getFeatureList() == null ? Collections.emptyList() : getFeatureList().getRawDataFiles();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return !getFeatureList().isAligned() ? Collections.singletonList(getFeatureList())
        : Collections.emptyList();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return getFeatureList().isAligned() ? Collections.singletonList(getFeatureList())
        : Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
    if (featureLists == null || featureLists.isEmpty()) {
      return;
    }

    // Get first selected feature list
    FeatureList featureList = featureLists.iterator().next();

    controller.setFeatureList(featureList);
  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {
    onFeatureListSelectionChanged(featureLists);
  }
}
