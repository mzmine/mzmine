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

package io.github.mzmine.modules.dataanalysis.pca_new;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.javafx.components.factories.FxComponentFactory;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingComponent;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public class PCAViewBuilder extends FxViewBuilder<PCAModel> {

  private static final int space = 5;

  private final SimpleXYChart<?> scoresPlot = new SimpleXYChart<>("Scores plot", "PC1", "PC2");
  private final SimpleXYChart<?> loadingsPlot = new SimpleXYChart<>("Loadings plot", "PC1", "PC2");

  public PCAViewBuilder(PCAModel model) {
    super(model);
  }

  @Override
  public Region build() {

    final BorderPane pane = new BorderPane();
    final HBox domain = FxComponentFactory.createLabelledComboBox("Domain PC", model.getAvailablePCs(),
        model.domainPcProperty());
    final HBox range = FxComponentFactory.createLabelledComboBox("Range PC", model.getAvailablePCs(),
        model.rangePcProperty());
    final HBox coloring = createMetadataBox();
    final HBox abundance = FxComponentFactory.createLabelledComboBox("Abundance",
        FXCollections.observableArrayList(AbundanceMeasure.values()), model.abundanceProperty());

    pane.setBottom(new FlowPane(space, space, domain, range, coloring, abundance));

    scoresPlot.setMinSize(500, 500);
    loadingsPlot.setMinSize(500, 500);

    pane.setCenter(new HBox(new BorderPane(scoresPlot), new BorderPane(loadingsPlot)));

    initDatasetListeners();
    return pane;
  }

  private HBox createMetadataBox() {
    final Label coloringLabel = new Label("Coloring:");
    final MetadataGroupingComponent coloringSelection = new MetadataGroupingComponent();
    coloringSelection.valueProperty().bindBidirectional(model.metadataColumnProperty());
    final HBox coloring = new HBox(space, coloringLabel, coloringSelection);
    return coloring;
  }

  private void initDatasetListeners() {
    model.loadingsDatasetsProperty().addListener(((_, _, newValue) -> {
      loadingsPlot.applyWithNotifyChanges(false, () -> {
        loadingsPlot.removeAllDatasets();
        if (newValue == null || newValue.isEmpty()) {
          return;
        }
        newValue.forEach(d -> loadingsPlot.addDataset(d.dataset(), d.renderer()));

        loadingsPlot.setDomainAxisLabel(STR."PC\{model.getDomainPc()}");
        loadingsPlot.setRangeAxisLabel(STR."PC\{model.getRangePc()}");
      });
    }));

    model.scoresDatasetsProperty().addListener(((_, _, newValue) -> {
      scoresPlot.applyWithNotifyChanges(false, () -> {
        scoresPlot.removeAllDatasets();
        if (newValue == null || newValue.isEmpty()) {
          return;
        }
        newValue.forEach(d -> scoresPlot.addDataset(d.dataset(), d.renderer()));
        scoresPlot.setDomainAxisLabel(STR."PC\{model.getDomainPc()}");
        scoresPlot.setRangeAxisLabel(STR."PC\{model.getRangePc()}");
      });
    }));

  }
}
