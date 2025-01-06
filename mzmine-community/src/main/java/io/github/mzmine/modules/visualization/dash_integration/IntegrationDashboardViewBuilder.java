package io.github.mzmine.modules.visualization.dash_integration;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.visualization.otherdetectors.integrationplot.IntegrationPlotController;
import io.github.mzmine.util.FeatureTableFXUtil;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

public class IntegrationDashboardViewBuilder extends FxViewBuilder<IntegrationDashboardModel> {

  protected IntegrationDashboardViewBuilder(IntegrationDashboardModel model) {
    super(model);
  }

  @Override
  public Region build() {

    BorderPane mainBorder = new BorderPane();
    BorderPane ftableControlsPane = new BorderPane();
    BorderPane gridWrapper = new BorderPane();
    final SplitPane mainSplit = new SplitPane(ftableControlsPane, gridWrapper);
    mainBorder.setCenter(mainSplit);
    mainSplit.setOrientation(Orientation.HORIZONTAL);
    mainSplit.setDividerPositions(0.7);

    model.featureTableFxProperty().get().getSelectionModel().selectedItemProperty()
        .addListener((_, _, row) -> model.setRow(row == null ? null : row.getValue()));
    model.rowProperty().addListener((_, _, row) -> {
      if (row != null && model.getFeatureTableFx().getSelectedRow() != model.getRow()) {
        FeatureTableFXUtil.selectAndScrollTo(row, model.getFeatureTableFx());
      }
    });

    return null;
  }

  private GridPane updatePlots() {

    int columnIndex = 0;
    int rowIndex = 0;
    final GridPane grid = new GridPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);

    for (RawDataFile file : model.getSortedFiles()) {
      final BorderPane pane = new BorderPane();
      IntegrationPlotController integrationPlot = new IntegrationPlotController();
      final Region view = integrationPlot.buildView();
      pane.setCenter(view);
      grid.add(pane, columnIndex++, rowIndex);
      if (columnIndex >= model.getGridSizeX()) {
        columnIndex = 0;
        rowIndex++;
      }


    }

  }
}
