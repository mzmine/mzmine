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

package io.github.mzmine.modules.visualization.kendrickmassplot;

import static io.github.mzmine.javafx.components.factories.FxLabels.newLabel;
import static io.github.mzmine.javafx.components.factories.FxTexts.boldText;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.FeatureShapeMobilogramType;
import io.github.mzmine.datamodel.features.types.FeatureShapeType;
import io.github.mzmine.datamodel.features.types.ImageType;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.main.ConfigService;
import java.awt.image.BufferedImage;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import org.jetbrains.annotations.Nullable;

public class InChartFeatureInfoPane extends InChartInfoPane {

  private final ObjectProperty<FeatureListRow> row = new SimpleObjectProperty<>();

  public InChartFeatureInfoPane() {
    super();

    row.subscribe(this::extract);
    bindVisible(row.isNotNull());
  }

  private void extract(final FeatureListRow row) {
    getChildren().clear();
    if (row == null) {
      return;
    }

    final Node shapeChart;
    @Nullable Node mobilogramChart = null;
    if (!(row.getFeatureList() instanceof ModularFeatureList flist)) {
      throw new IllegalStateException("Cannot handle non modular feature list in kendrick plot");
    }
    if (flist.hasFeatureType(ImageType.class) && row.getBestFeature() != null) {
      // image is sample specific
      shapeChart = flist.getChartForRow(row, DataTypes.get(ImageType.class),
          row.getBestFeature().getRawDataFile());
    } else {
      // Create the FeatureShapeChart with the row - not sample specific so raw is null
      shapeChart = flist.getChartForRow(row, DataTypes.get(FeatureShapeType.class), null);
    }
    // mobility
    if (flist.hasFeatureType(FeatureShapeMobilogramType.class) && row.getBestFeature() != null) {
      // Create mobilogram with the row - not sample specific so raw is null
      mobilogramChart = flist.getChartForRow(row, DataTypes.get(FeatureShapeMobilogramType.class),
          null);
      // make wider
      this.setSize(DEFAULT_WIDTH * 2, DEFAULT_HEIGHT);
    } else {
      this.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    // remove the size constraints provided by feature table
    FxLayout.useComputedSize(shapeChart, mobilogramChart);

    // add infos
    //"m/z", "Retention Time",
    //              "Intensity", "Bubble Size");
    NumberFormats form = ConfigService.getGuiFormats();
    Object annotation = row.getPreferredAnnotation();
    String bestAnnotation = annotation == null ? "" : annotation.toString();
    var textFlow = FxTextFlows.newTextFlow( //
        boldText(bestAnnotation), // text for automatic breaks
        newLabel("ID: " + row.getID()), // label to avoid breaks
        newLabel("   m/z " + form.mz(row.getAverageMZ())) // label to avoid breaks
    );
    ObservableList<Node> texts = textFlow.getChildren();
    if (row.getAverageRT() != null) {
      texts.add(newLabel("   RT: " + form.rt(row.getAverageRT())));
    }
    if (row.getAverageCCS() != null) {
      texts.add(newLabel("   CCS: " + form.ccs(row.getAverageCCS())));
    }
    if (row.getAverageMobility() != null) {
      texts.add(newLabel("   Mobility: " + form.mobility(row.getAverageMobility())));
    }

    // create split if needed

    var content = new BorderPane(
        mobilogramChart != null ? new SplitPane(shapeChart, mobilogramChart) : shapeChart);
    content.setBottom(textFlow);
    getChildren().setAll(content);
  }


  public static BufferedImage paintImage(Pane pane) {
    WritableImage writableImage = new WritableImage((int) pane.getPrefWidth(),
        (int) pane.getPrefHeight());
    pane.snapshot(null, writableImage);
    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);
    return bufferedImage;
  }


  public FeatureListRow getRow() {
    return row.get();
  }

  public ObjectProperty<FeatureListRow> rowProperty() {
    return row;
  }

  public void setRow(final FeatureListRow row) {
    this.row.set(row);
  }
}
