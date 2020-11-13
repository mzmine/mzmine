/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.combinedmodule;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javax.annotation.Nonnull;

public class CombinedModuleVisualizerTab extends MZmineTab {

  private CombinedModuleVisualizerTabController controller;
  //private Scene scene;

  public CombinedModuleVisualizerTab(ParameterSet parameters) {
    super("Combined Module Visualizer", true, false);

    try {
      FXMLLoader root = new FXMLLoader(
          getClass().getResource("CombinedModuleVisualizerTab.fxml"));
      Parent rootPane = root.load();
      controller = root.getController();
      controller.setParameters(MZmineCore.getDesktop().getMainWindow(), parameters);
      //scene = new Scene(rootPane);
      //setScene(scene);
      setContent(rootPane);

    } catch (IOException e) {
      e.printStackTrace();
    }
    //setTitle("Combined Module Plot");
    //scene.getStylesheets()
    //    .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    //WindowsMenu.addWindowsMenu(scene);
  }

  @Nonnull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return new ArrayList<>(Collections.singletonList(controller.getDataFile()));
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
    controller.updateVisualizedFiles(rawDataFiles);
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(
      Collection<? extends FeatureList> featurelists) {

  }
}
