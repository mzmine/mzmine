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

package io.github.mzmine.modules.visualization.projectmetadata;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import java.util.Collection;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

/**
 * Tab-wrapper for a ProjectMetadata table.
 */
public class ProjectMetadataTab extends MZmineTab {

  private static final Logger logger = Logger.getLogger(ProjectMetadataTab.class.getName());

  public ProjectMetadataTab() {
    super("Sample metadata", false, false);

    try {
      // try to load the markdown object from the FXML file
      FXMLLoader loader = new FXMLLoader(getClass().getResource("ProjectParametersSetupPane.fxml"));
      BorderPane borderPane = loader.load();
      ProjectParametersSetupPaneController controller = loader.getController();
      // set stage for a controller of the loaded object
      controller.setStage(MZmineCore.getDesktop().getMainWindow());

      setContent(borderPane);
    } catch (Exception e) {
      logger.severe("Didn't manage to load the markdown from ProjectMetadataDialog.fxml");
    }
  }

  public ProjectMetadataTab(String title) {
    super(title);
  }

  @Override
  public @NotNull Collection<? extends RawDataFile> getRawDataFiles() {
    return null;
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getFeatureLists() {
    return null;
  }

  @Override
  public @NotNull Collection<? extends FeatureList> getAlignedFeatureLists() {
    return null;
  }

  // probably it will be necessary to implement an update on each new RawDataFile import
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
