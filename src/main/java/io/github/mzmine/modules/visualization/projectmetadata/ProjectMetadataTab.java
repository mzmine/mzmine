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

import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.main.MZmineCore;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;

/**
 * Tab-wrapper for a ProjectMetadata table.
 */
public class ProjectMetadataTab extends SimpleTab {

  private static final Logger logger = Logger.getLogger(ProjectMetadataTab.class.getName());

  public ProjectMetadataTab() {
    super("Sample metadata", false, false);

    try {
      // try to load the markdown object from the FXML file
      FXMLLoader loader = new FXMLLoader(getClass().getResource("ProjectMetadataPane.fxml"));
      BorderPane borderPane = loader.load();
      ProjectMetadataPaneController controller = loader.getController();
      // set stage for a controller of the loaded object
      controller.setStage(MZmineCore.getDesktop().getMainWindow());

      setContent(borderPane);
    } catch (Exception e) {
      logger.severe("Didn't manage to load the markdown from ProjectMetadataDialog.fxml");
    }
  }

}
