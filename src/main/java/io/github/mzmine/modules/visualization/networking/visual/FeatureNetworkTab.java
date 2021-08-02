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

package io.github.mzmine.modules.visualization.networking.visual;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import java.util.Collection;
import java.util.List;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

public class FeatureNetworkTab extends MZmineTab {

  private FeatureNetworkPane contentPane;
  private final CheckMenuItem toggleCollapseIons;
  private final Desktop desktop;
  private final BorderPane mainPane;

  /**
   * Create the frame.
   */
  private FeatureNetworkTab() {
    super("IIMN", false, false);

    desktop = MZmineCore.getDesktop();

    mainPane = new BorderPane();
    setContent(mainPane);
    // Use main CSS
//    mainScene.getStylesheets()
//        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
//    setScene(mainScene);

    mainPane.setMinWidth(400.0);
    mainPane.setMinHeight(300.0);

    MenuBar menuBar = new MenuBar();
    Menu menu = new Menu("");

    toggleCollapseIons = new CheckMenuItem("Collapse ions");
    toggleCollapseIons.selectedProperty().addListener(
        (object, old, value) -> contentPane.collapseIonNodes(toggleCollapseIons.isSelected()));

    menu.getItems().add(toggleCollapseIons);
    menuBar.getMenus().add(menu);

    // create a VBox
    contentPane = new FeatureNetworkPane();
    VBox vbox = new VBox(menuBar, contentPane);
    mainPane.setCenter(vbox);

    // create a scene
//    Scene scene = new Scene(mainPane, 800, 800);
//    stage.setScene(scene);
//    stage.show();
  }


  public FeatureNetworkTab(FeatureList featureList) {
    this();
    // last, as it recreates the graph
    contentPane.setFeatureList(featureList);
  }

  public FeatureNetworkTab(FeatureList featureList, boolean collapseNodes,
      boolean connectByNetRelations,
      boolean onlyBest, boolean ms2SimEdges, boolean ms1FeatureShapeEdges) {
    this();
    toggleCollapseIons.setSelected(collapseNodes);
    contentPane.collapseIonNodes(collapseNodes);
    contentPane.setConnectByNetRelations(connectByNetRelations);
    contentPane.setOnlyBest(onlyBest);
    contentPane.setShowMs2SimEdges(ms2SimEdges);
    contentPane.setUseMs1FeatureShapeEdges(ms1FeatureShapeEdges);
    // last, as it recreates the graph
    contentPane.setFeatureList(featureList);
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return List.of();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return List.of(contentPane.getFeatureList());
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return List.of(contentPane.getFeatureList());
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {

  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }
}
