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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.gui.chartbasics.simplechart.RegionSelectionWrapper;
import static io.github.mzmine.gui.chartbasics.simplechart.RegionSelectionWrapper.REGION_FILE_EXTENSION;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javax.validation.constraints.NotNull;

public class RegionsComponent extends FlowPane {

  private final Label label;
  private ObservableList<List<Point2D>> value = FXCollections.observableArrayList();


  public RegionsComponent() {
//    tvPoints = new TreeView<>();
//    TreeItem<String> regions = new TreeItem<>("Regions");
//    tvPoints.setRoot(tvPoints);
    setAlignment(Pos.TOP_LEFT);
    setHgap(FxLayout.DEFAULT_SPACE);

    label = new Label();
    label.textProperty().bind(Bindings.createStringBinding(
        () -> (value != null && !value.isEmpty() ? value.size() : 0) + " region(s) selected",
        value));

    final Button loadButton = FxButtons.createLoadButton(() -> {
      FxThread.runLater(() -> {
        final FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters()
            .add(new ExtensionFilter("mzmine regions file", REGION_FILE_EXTENSION));
        final File file = chooser.showOpenDialog(MZmineCore.getDesktop().getMainWindow());
        value.setAll(RegionSelectionWrapper.loadRegionsFromFile(file));
      });
    });
    getChildren().addAll(loadButton, label);
  }

  public List<List<Point2D>> getValue() {
    return new ArrayList<>(value);
  }

  public void setValue(@NotNull List<List<Point2D>> value) {
    this.value.setAll(value);
  }
}
