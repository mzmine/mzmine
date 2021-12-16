/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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

  public FeatureList getFeatureList() {
    return controller.getFeatureList();
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
