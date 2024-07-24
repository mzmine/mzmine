/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.otherdetectors.multidetector;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.otherdetectors.integrationplot.IntegrationPane;
import io.github.mzmine.project.ProjectService;
import java.util.Optional;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MultidetectorVisualizerBuilder extends FxViewBuilder<MultidetectorVisualizerModel> {

  private VBox content;
  private ScrollPane scrollPane;

  protected MultidetectorVisualizerBuilder(MultidetectorVisualizerModel model) {
    super(model);
  }

  @Override
  public Region build() {
    scrollPane = new ScrollPane();
    content = FxLayout.newVBox();
    scrollPane.setFitToWidth(true);
    scrollPane.setFitToHeight(true);

    final Button addButton = FxButtons.createButton(null, FxIcons.ADD, "Add another trace",
        this::addNewDetector);
    final HBox addWrapper = FxLayout.newHBox(Pos.TOP_RIGHT, addButton);
    final VBox contentWrapper = FxLayout.newVBox(content, addWrapper);
    scrollPane.setContent(contentWrapper);
    return new BorderPane(scrollPane);
  }

  private void addNewDetector() {
    final Optional<RawDataFile> file = ProjectService.getProject().getCurrentRawDataFiles().stream()
        .filter(f -> f.getOtherDataFiles().stream().anyMatch(o -> o.hasTimeSeries())).findFirst();
    if (!file.isPresent()) {
      MZmineCore.getDesktop().displayErrorMessage("No raw file contains other detector traces.");
      return;
    }

    final IntegrationPane pane = new IntegrationPane(file.get());
    pane.minHeightProperty().bind(scrollPane.heightProperty().subtract(10).divide(4));
    content.getChildren().add(pane);
  }
}
