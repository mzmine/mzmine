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

package io.github.mzmine.modules.visualization.vankrevelendiagram;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.parameters.ParameterSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import org.jetbrains.annotations.NotNull;

public class VanKrevelenDiagramTab extends SimpleTab {

  private final VanKrevelenDiagramAnchorPaneController controller;

  public VanKrevelenDiagramTab(ParameterSet parameters) {
    super("Van Krevelen Diagram");

    FXMLLoader loader = new FXMLLoader(
        (getClass().getResource("VanKrevelenDiagramAnchorPane.fxml")));

    AnchorPane root = null;
    try {
      root = loader.load();
      logger.finest("Stage has been successfully loaded from the FXML loader.");
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Get controller
    controller = loader.getController();
    controller.initialize(parameters);
    setContent(root);
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return controller.getFeatureList().getRawDataFiles();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return new ArrayList<>(
        Collections.singletonList((ModularFeatureList) controller.getFeatureList()));
  }

}
