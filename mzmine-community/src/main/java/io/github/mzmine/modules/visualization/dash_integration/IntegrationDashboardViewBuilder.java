/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.dash_integration;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxCheckBox;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxSpinners;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.modules.visualization.otherdetectors.integrationplot.FeatureIntegratedListener;
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
import org.jetbrains.annotations.NotNull;

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

    final BorderPane ftable = model.getFeatureTableTab().getMainPane();
    ftable.setRight(null);
    ftableControlsPane.setCenter(ftable);
    ftableControlsPane.setBottom(
        FxLayout.newVBox(buildIntegrationTransfer(), buildMetadataColSelectionForSorting(),
            FxCheckBox.newCheckBox("Apply latest smoothing step to chromatograms",
                model.applyPostProcessingProperty())));

    final HBox pageControls = FxLayout.newHBox(Pos.CENTER, buildGridPageControls());
    gridWrapper.setTop(pageControls);
    pageControls.setMinHeight(Region.USE_PREF_SIZE);
    BorderPane.setAlignment(pageControls, Pos.CENTER);
    gridWrapper.setCenter(buildPlots());

    return mainBorder;
  }

  private GridPane buildPlots() {

    final GridPane grid = new GridPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);
    final Map<RawDataFile, RegionController> filePlotCache = new HashMap<>();
    updatePlotLayout(grid, filePlotCache);

    // pregenerate all plots once, so all are taken into account when integrating
    model.featureListProperty().subscribe(newFlist -> {
      filePlotCache.clear();
      if (newFlist != null) {
        // remove old plots, add new ones in case the feature list changes
        newFlist.getRawDataFiles().forEach(f -> getPlotForFile(filePlotCache, f));
      }
    });

    // update layout on change to the number of cols/rows.
    // Update on change to data is covered in updatePlotLayout
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

  private void updatePlotLayout(GridPane grid, Map<RawDataFile, RegionController> filePlotCache) {
    // make sure all our plots are not visible before updating
    grid.getChildren().clear();
    int columnIndex = 0;
    int rowIndex = 0;

    final ObservableList<RawDataFile> sortedFiles = model.getSortedFiles();
    final int gridNumColumns = model.getGridNumColumns();
    final int gridNumRows = model.getGridNumRows();
    final int pageSize = gridNumColumns * gridNumRows;
    final int startOffset = model.getGridPaneFileOffset();

    grid.getRowConstraints().setAll(FxLayout.newFillHeightRows(gridNumRows));
    grid.getColumnConstraints().setAll(FxLayout.newFillWidthColumns(gridNumColumns));
//    final ChartGroup chartGroup = new ChartGroup(true, true, true, false);

    for (int i = startOffset; i < startOffset + pageSize && i < sortedFiles.size(); i++) {
      final Region integrationPlot = getPlotForFile(filePlotCache, sortedFiles.get(i));

      grid.add(integrationPlot, columnIndex++, rowIndex);
      if (columnIndex >= gridNumColumns) {
        columnIndex = 0;
        rowIndex++;
      }
    }
  }

  private @NotNull Region getPlotForFile(final Map<RawDataFile, RegionController> filePlotCache,
      RawDataFile file) {
    return filePlotCache.computeIfAbsent(file, _ -> {
      final IntegrationPlotController plot = new IntegrationPlotController();
      plot.setTextLessButtons(true);
      plot.setMaxIntegratedFeatures(1);
      if (file instanceof IMSRawDataFile ims) {
        plot.setBinningMobilogramDataAccess(new BinningMobilogramDataAccess(ims,
            BinningMobilogramDataAccess.getPreviousBinningWith(model.getFeatureList(),
                ims.getMobilityType())));
      }
//        plot.setChartGroup(chartGroup);
      // auto update on change of the feature data entry. must be subscribed in here because we don't want to subscribe multiple times
      model.featureDataEntriesProperty()
          .addListener((MapChangeListener<RawDataFile, FeatureIntegrationData>) change -> {
            if (change.getKey() == file) {
              plot.setTitle(file.getName());
              plot.setFeatureDataEntry(change.getValueAdded());
            }
          });

      final Region region = plot.buildView();
      // need to set manually, using a subscription does not work for some reason.
      region.visibleProperty().subscribe(_ -> {
        // setting once at the beginning leads to the change not being applied properly
        plot.setRangeAxisStickyZero(true);
        plot.setFeatureDataEntry(model.getFeatureDataEntries().get(file));
      });

      plot.addIntegrationListener(newIntegrationListener(plot, filePlotCache, file));
      return new RegionController(plot, region);
    }).region();
  }

  private @NotNull FeatureIntegratedListener newIntegrationListener(
      final IntegrationPlotController plot, Map<RawDataFile, RegionController> controllerMap,
      RawDataFile file) {
    return (eventType, newFeatureTimeSeries, newIntegrationRange) -> {
      final IntegrationTransfer syncSetting = model.getSyncReIntegration();
      final FeatureListRow row = model.getRow();
      if (row == null) {
        return;
      }

      // pass integration to other plots if applicable
      if (eventType == EventType.INTERNAL_CHANGE && syncSetting != IntegrationTransfer.NONE) {
        for (RawDataFile otherFile : model.getSortedFiles()) {
          final RegionController rc = controllerMap.get(otherFile);
          if (rc != null && rc.controller() != null && plot != rc.controller()
              && syncSetting.appliesTo(row, otherFile, rc.controller())) {
            logger.finest(
                () -> "Transferring manual integration from file %s to file %s. (%s)".formatted(
                    file.getName(), otherFile.getName(), syncSetting.toString()));
            rc.controller().integrateExternally(newIntegrationRange);
          }
        }
      }

      // reflect integration in row of this plot.
      if (newFeatureTimeSeries instanceof IonTimeSeries<?> its) {
        final ModularFeature currentFeature = (ModularFeature) row.getFeature(file);
        if (currentFeature == null) {
          row.addFeature(file,
              new ModularFeature(model.getFeatureList(), file, its, FeatureStatus.MANUAL));
        } else {
          currentFeature.set(FeatureDataType.class, its);
          FeatureDataUtils.recalculateIonSeriesDependingTypes(currentFeature);
        }
      } else if (newFeatureTimeSeries == null) {
        row.removeFeature(file);
      }
      // reflect change in gui
      final FeatureIntegrationData oldEntry = model.featureDataEntriesProperty().get(file);
      if (oldEntry != null) { // should always be the case
        model.featureDataEntriesProperty().put(file,
            new FeatureIntegrationData(file, newFeatureTimeSeries, oldEntry.chromatogram(),
                oldEntry.additionalData()));
      }
    };
  }

  private Region buildGridPageControls() {
    final Label lblCols = FxLabels.newLabel("Columns");
    final Label lblRows = FxLabels.newLabel("Rows");

    final Spinner<Integer> spCols = FxSpinners.newSpinner(1, 10, model.gridNumColumnsProperty());
    final Spinner<Integer> spRows = FxSpinners.newSpinner(1, 10, model.gridNumRowsProperty());

    final Button previousPage = FxButtons.createButton(null, FxIcons.ARROW_LEFT, "Previous page",
        () -> model.setGridPaneFileOffset(
            Math.max(0, model.getGridPaneFileOffset() - model.getCellsPerPage())));
    final Button nextPage = FxButtons.createButton(null, FxIcons.ARROW_RIGHT, "Next page", () -> {
      final int nextOffset = model.getGridPaneFileOffset() + model.getCellsPerPage();
      if (nextOffset > model.getSortedFiles().size()) {
        return;
      }
      model.setGridPaneFileOffset(nextOffset);
    });

    final Label lblPage = FxLabels.newLabel("Page 1/1");
    PropertyUtils.onChangeSubscription(() -> lblPage.setText("Page %d/%d".formatted(
            // current page
            (int) ((model.getGridPaneFileOffset() + 1) / (double) (model.getCellsPerPage()) + 1),
            // total pages
            model.getNumPages())), model.sortedFilesProperty(), model.gridNumRowsProperty(),
        model.gridNumColumnsProperty(), model.gridPaneFileOffsetProperty());

    final Label lblEntries = FxLabels.newLabel("Entries 0 - 0");
    PropertyUtils.onChangeSubscription(() -> {
          lblEntries.setText("Entries %d - %d".formatted(model.getGridPaneFileOffset() + 1,
              Math.min(model.getGridPaneFileOffset() + model.getCellsPerPage(),
                  model.getSortedFiles().size())));
        }, model.sortedFilesProperty(), model.gridNumRowsProperty(), model.gridNumColumnsProperty(),
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
