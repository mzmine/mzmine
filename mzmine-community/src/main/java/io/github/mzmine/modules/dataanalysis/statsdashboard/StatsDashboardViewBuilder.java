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

package io.github.mzmine.modules.dataanalysis.statsdashboard;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.dataanalysis.pca_new.PCAController;
import io.github.mzmine.modules.dataanalysis.rowsboxplot.RowsBoxplotController;
import io.github.mzmine.modules.dataanalysis.volcanoplot.VolcanoPlotController;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.util.FeatureTableFXUtil;
import java.util.List;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

public class StatsDashboardViewBuilder extends FxViewBuilder<StatsDashboardModel> {

  private final FeatureTableFX table;
  private final PCAController pcaController;
  private final VolcanoPlotController volcanoPlotController;
  private final RowsBoxplotController boxplotController;

  public StatsDashboardViewBuilder(StatsDashboardModel model, FeatureTableFX table,
      PCAController pcaController, VolcanoPlotController controller,
      RowsBoxplotController boxplotController) {
    super(model);
    this.table = table;
    this.pcaController = pcaController;
    this.volcanoPlotController = controller;
    this.boxplotController = boxplotController;
  }

  @Override
  public Region build() {
    final SplitPane main = new SplitPane();
    main.setOrientation(Orientation.VERTICAL);
    final SplitPane stats = buildStatsPane();
    main.getItems().addAll(stats, table);

    initFeatureListListeners();
    return main;
  }

  private void initFeatureListListeners() {
    model.flistsProperty().addListener((_, _, flists) -> table.setFeatureList(
        flists.isEmpty() ? null : (ModularFeatureList) flists.getFirst()));

    // select correct row in table
    model.selectedRowsProperty().addListener((_, _, rows) -> {
      if (rows.isEmpty()) {
        return;
      }
      final TreeItem<ModularFeatureListRow> rowItem = table.getRoot().getChildren().stream()
          .filter(item -> item.getValue().equals(rows.getFirst())).findFirst().orElse(null);
      if (rowItem == null) {
        return;
      }
      FeatureTableFXUtil.selectAndScrollTo(rowItem, table);
    });

    // listen to changes in the selected row, this updates the controllers via a binding in their
    // view builders.
    table.getSelectionModel().selectedItemProperty().addListener((_, old, row) -> {
      if (old == null || (row.getValue() != null && !old.equals(row))) {
        model.selectedRowsProperty().set(List.of(row.getValue()));
      }
    });
  }

  @NotNull
  private SplitPane buildStatsPane() {
    final SplitPane stats = new SplitPane();
    stats.setOrientation(Orientation.HORIZONTAL);
    final var analysisTab = new TabPane();
    analysisTab.getTabs().addAll(new Tab("PCA", pcaController.buildView()),
        new Tab("Volcano Plot", volcanoPlotController.buildView()));
    stats.getItems().add(analysisTab);
    stats.getItems().add(boxplotController.buildView());
    stats.setDividerPosition(0, 0.9d);
    stats.setMinHeight(Region.USE_COMPUTED_SIZE);
    return stats;
  }
}
