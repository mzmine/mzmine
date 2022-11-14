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
