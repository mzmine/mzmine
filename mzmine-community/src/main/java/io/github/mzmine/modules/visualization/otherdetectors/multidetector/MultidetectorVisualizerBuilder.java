/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MultidetectorVisualizerBuilder extends FxViewBuilder<MultidetectorVisualizerModel> {

  private static final Logger logger = Logger.getLogger(
      MultidetectorVisualizerBuilder.class.getName());

  private BorderPane main;
  private VBox content;

  protected MultidetectorVisualizerBuilder(MultidetectorVisualizerModel model) {
    super(model);
  }

  @Override
  public Region build() {
    content = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true);

    final Button addButton = FxButtons.createButton(null, FxIcons.ADD, "Add another trace",
        this::addNewDetector);
    final HBox addWrapper = FxLayout.newHBox(Pos.TOP_RIGHT, addButton);
    final VBox contentWrapper = content = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true,
        content, addWrapper);

    main = new BorderPane(contentWrapper);
    var scroll = new ScrollPane(main);
    scroll.setFitToWidth(true);
    scroll.setFitToHeight(true);
    scroll.setVbarPolicy(ScrollBarPolicy.ALWAYS);

    addNewDetector();

//    main.addEventFilter(KeyEvent.KEY_PRESSED, event -> logger.finest("main " + event.toString()));
//    contentWrapper.addEventFilter(KeyEvent.KEY_PRESSED, event -> logger.finest("wrapper " + event.toString()));
//    content.addEventFilter(KeyEvent.KEY_PRESSED, event -> logger.finest("content " + event.toString()));
    return scroll;
  }

  private void addNewDetector() {
    final Optional<RawDataFile> file = ProjectService.getProject().getCurrentRawDataFiles().stream()
        .filter(f -> f.getOtherDataFiles().stream().anyMatch(o -> o.hasTimeSeries())).findFirst();
    if (!file.isPresent()) {
      MZmineCore.getDesktop().displayErrorMessage("No raw file contains other detector traces.");
      return;
    }

    final IntegrationPane pane = new IntegrationPane(file.get());
    pane.minHeightProperty().set(250);
    VBox.setVgrow(pane, Priority.SOMETIMES);
//    pane.addEventFilter(KeyEvent.KEY_PRESSED, event -> logger.finest("pane " + event.toString()));
    content.getChildren().add(pane);
  }
}
