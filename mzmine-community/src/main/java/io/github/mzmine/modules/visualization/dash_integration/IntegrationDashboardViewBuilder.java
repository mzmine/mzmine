package io.github.mzmine.modules.visualization.dash_integration;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.visualization.otherdetectors.integrationplot.IntegrationPlotController;
import io.github.mzmine.util.FeatureTableFXUtil;
import java.util.HashMap;
import java.util.Map;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
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

  private GridPane buildPlots() {

    int columnIndex = 0;
    int rowIndex = 0;
    final GridPane grid = new GridPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);

    final Map<RawDataFile, IntegrationPlotController> filePlotCache = new HashMap<>();
    final ObservableList<RawDataFile> sortedFiles = model.getSortedFiles();

    for (int i = 0; i < sortedFiles.size(); i++) {
      final RawDataFile file = sortedFiles.get(i);
      final BorderPane pane = new BorderPane();
      final IntegrationPlotController integrationPlot = filePlotCache.computeIfAbsent(file,
          f -> new IntegrationPlotController());
      final Region view = integrationPlot.buildView();
      pane.setCenter(view);
      grid.add(pane, columnIndex++, rowIndex);
      if (columnIndex >= model.getGridSizeX()) {
        columnIndex = 0;
        rowIndex++;
      }

      // auto update on change of the feature data entry
      model.featureDataEntriesProperty()
          .subscribe(map -> integrationPlot.setFeatureDataEntry(map.get(file)));
    }
  }

  private Region buildGridPageControls() {
    final Label lblCols = FxLabels.newLabel("Columns");
    final Label lblRows = FxLabels.newLabel("Rows");

    Spinner<Integer> spCols = new Spinner<>(1, 10, model.getGridSizeX());
    Spinner<Integer> spRows = new Spinner<>(1, 10, model.getGridSizeY());
  }
}
