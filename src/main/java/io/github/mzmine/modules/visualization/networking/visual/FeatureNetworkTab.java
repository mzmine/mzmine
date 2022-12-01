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
