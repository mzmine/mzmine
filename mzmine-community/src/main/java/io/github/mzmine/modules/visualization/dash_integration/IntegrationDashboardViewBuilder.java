package io.github.mzmine.modules.visualization.dash_integration;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.modules.visualization.otherdetectors.integrationplot.FeatureIntegratedListener.EventType;
import io.github.mzmine.modules.visualization.otherdetectors.integrationplot.IntegrationPlotController;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.parametertypes.ComboComponent;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.FeatureTableFXUtil;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;

public class IntegrationDashboardViewBuilder extends FxViewBuilder<IntegrationDashboardModel> {

  private static final Logger logger = Logger.getLogger(
      IntegrationDashboardViewBuilder.class.getName());

  protected IntegrationDashboardViewBuilder(IntegrationDashboardModel model) {
    super(model);
  }

  @Override
  public Region build() {

    BorderPane mainBorder = new BorderPane();
    BorderPane ftableControlsPane = new BorderPane();
    BorderPane gridWrapper = new BorderPane();
    final SplitPane mainSplit = new SplitPane(gridWrapper, ftableControlsPane);
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

    ftableControlsPane.setCenter(model.getFeatureTableFx());
    ftableControlsPane.setBottom(
        FxLayout.newVBox(buildIntegrationTransfer(), buildMetadataColSelectionForSorting()));

    final HBox pageControls = FxLayout.newHBox(buildGridPageControls());
    gridWrapper.setTop(pageControls);
    BorderPane.setAlignment(pageControls, Pos.CENTER);
    gridWrapper.setCenter(buildPlots());

    return mainBorder;
  }

  private GridPane buildPlots() {

    final GridPane grid = new GridPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);
    final Map<RawDataFile, Region> filePlotCache = new HashMap<>();

    updatePlotLayout(grid, filePlotCache);

    // update layout on change to the number of cols/rows. Update on change to data is covered in updatePlotLayout
    PropertyUtils.onChange(() -> updatePlotLayout(grid, filePlotCache),
        model.gridNumColumnsProperty(), model.gridNumRowsProperty(),
        model.gridPaneFileOffsetProperty(), model.sortedFilesProperty());

    model.gridNumRowsProperty().addListener((_, _, row) -> {
      logger.finest("echo");
      updatePlotLayout(grid, filePlotCache);
    });
    model.gridNumColumnsProperty().addListener((_, _, row) -> {
      logger.finest("echo");
      updatePlotLayout(grid, filePlotCache);
    });

    return grid;
  }

  private void updatePlotLayout(GridPane grid, Map<RawDataFile, Region> filePlotCache) {
    // make sure all our plots are not visible before updating
    grid.getChildren().clear();
    int columnIndex = 0;
    int rowIndex = 0;

    final ObservableList<RawDataFile> sortedFiles = model.getSortedFiles();
    final int pageSize = model.getGridNumColumns() * model.getGridNumRows();
    final int startOffset = model.getGridPaneFileOffset();

    final Map<RawDataFile, IntegrationPlotController> controllerMap = new HashMap<>();

    for (int i = startOffset; i < startOffset + pageSize && i < sortedFiles.size(); i++) {
      final RawDataFile file = sortedFiles.get(i);
      final Region integrationPlot = filePlotCache.computeIfAbsent(file, f -> {
        final IntegrationPlotController plot = controllerMap.computeIfAbsent(f,
            f -> new IntegrationPlotController());
        // auto update on change of the feature data entry. must be subscribed in here because we don't want to subscribe multiple times
        model.featureDataEntriesProperty()
            .addListener((MapChangeListener<RawDataFile, FeatureDataEntry>) change -> {
              if (change.getKey() == file) {
                plot.setFeatureDataEntry(change.getValueAdded());
              }
            });
        // need to set manually, using a subscription does not work for some reason.
        final Region region = plot.buildView();
        region.visibleProperty()
            .subscribe(_ -> plot.setFeatureDataEntry(model.getFeatureDataEntries().get(file)));

        plot.addIntegrationListener((eventType, newFeature, newIntegrationRange) -> {
          if (eventType == EventType.EXTERNAL_CHANGE
              && model.getSyncReIntegration() != IntegrationTransfer.NONE) {
            final IntegrationPlotController otherPlot = controllerMap.get(file);
            if (otherPlot != null) {
              otherPlot.integrateExternally(newIntegrationRange);
            }
          }

          final FeatureListRow r = model.getRow();
          if (r == null) {
            return;
          }
          if (newFeature instanceof IonTimeSeries<?> its) {
            final ModularFeature currentFeature = (ModularFeature) r.getFeature(file);
            if (currentFeature == null) {
              r.addFeature(file,
                  new ModularFeature(model.getFeatureList(), file, its, FeatureStatus.MANUAL));
            } else {
              currentFeature.set(FeatureDataType.class, its);
              FeatureDataUtils.recalculateIonSeriesDependingTypes(currentFeature);
            }
          }
        });
        return region;
      });

      grid.add(integrationPlot, columnIndex++, rowIndex);
      if (columnIndex >= model.getGridNumColumns()) {
        columnIndex = 0;
        rowIndex++;
      }
    }
  }

  private Region buildGridPageControls() {
    final Label lblCols = FxLabels.newLabel("Columns");
    final Label lblRows = FxLabels.newLabel("Rows");

    final Spinner<Integer> spCols = new Spinner<>(1, 10, model.getGridNumColumns());
    final Spinner<Integer> spRows = new Spinner<>(1, 10, model.getGridNumRows());

    model.gridNumColumnsProperty().asObject()
        .bindBidirectional(spCols.getValueFactory().valueProperty());

    model.gridNumRowsProperty().asObject()
        .bindBidirectional(spRows.getValueFactory().valueProperty());

    spCols.getValueFactory().valueProperty().addListener((_, _, c) -> {
      logger.finest(c.toString());
    });
    spRows.getValueFactory().valueProperty().addListener((_, _, r) -> {
      logger.finest(r.toString());
    });

    final Button previousPage = FxButtons.createButton(null, FxIcons.ARROW_LEFT, "Previous page",
        () -> {
          final int currentOffset = model.getGridPaneFileOffset();
          model.setGridPaneFileOffset(
              Math.max(0, currentOffset - (model.getGridNumRows() * model.getGridNumColumns())));
        });
    final Button nextPage = FxButtons.createButton(null, FxIcons.ARROW_RIGHT, "Next page", () -> {
      final int currentOffset = model.getGridPaneFileOffset();
      model.setGridPaneFileOffset(Math.min(Math.max(model.sortedFilesProperty().size() - 1, 0),
          currentOffset + (model.getGridNumRows() * model.getGridNumColumns())));
    });

    final Label lblPage = FxLabels.newLabel("Page 1/1");
    PropertyUtils.onChange(() -> lblPage.setText("Page %d/%d".formatted(
            // current page
            model.getGridPaneFileOffset() / (model.getGridNumRows() * model.getGridNumColumns()) + 1,
            // total pages
            model.getSortedFiles().size() / (model.getGridNumRows() * model.getGridNumColumns()) + 1)),
        model.getSortedFiles(), model.gridNumRowsProperty(), model.gridNumColumnsProperty(),
        model.gridPaneFileOffsetProperty());

    final Label lblEntries = FxLabels.newLabel("Entries 0 - 0");
    PropertyUtils.onChange(() -> {
          lblEntries.setText("Entries %d - %d".formatted(model.getGridPaneFileOffset() + 1, Math.max(
              model.getGridPaneFileOffset() + model.getGridNumRows() * model.getGridNumColumns() + 1,
              model.getSortedFiles().size())));
        }, model.getSortedFiles(), model.gridNumRowsProperty(), model.gridNumColumnsProperty(),
        model.gridPaneFileOffsetProperty());

    return FxLayout.newHBox(Pos.CENTER, lblCols, spCols, lblRows, spRows,
        new Separator(Orientation.VERTICAL), previousPage, lblEntries, lblPage, nextPage);
  }

  private Region buildMetadataColSelectionForSorting() {
    final List<MetadataColumn<?>> columns = ProjectService.getMetadata().getColumns().stream()
        .sorted(Comparator.comparing(MetadataColumn::getTitle)).toList();

    final Label lblSortBy = FxLabels.newLabel("Sort files by:");
    ComboBox<MetadataColumn<?>> cmbMetadataCol = new ComboBox<>(
        FXCollections.observableArrayList(columns));
    cmbMetadataCol.setConverter(new StringConverter<>() {
      @Override
      public String toString(MetadataColumn<?> object) {
        return object != null ? object.getTitle() : "";
      }

      @Override
      public MetadataColumn<?> fromString(String string) {
        return null;
      }
    });
    cmbMetadataCol.valueProperty().bindBidirectional(model.rawFileSortingColumnProperty());

    return FxLayout.newHBox(lblSortBy, cmbMetadataCol);
  }

  private Region buildIntegrationTransfer() {
    final ComboComponent<IntegrationTransfer> component = new ComboComponent<>(
        FXCollections.observableArrayList(IntegrationTransfer.values()));

    component.setValue(model.getSyncReIntegration());
    component.valueProperty().bindBidirectional(model.syncReIntegrationProperty());
    final Label lblSync = FxLabels.newLabel("Synchronize integration");
    final Tooltip tooltip = new Tooltip(
        Arrays.stream(IntegrationTransfer.values()).map(IntegrationTransfer::getToolTip)
            .collect(Collectors.joining("\n")));
    lblSync.setTooltip(tooltip);
    return FxLayout.newHBox(lblSync, component);
  }
}
