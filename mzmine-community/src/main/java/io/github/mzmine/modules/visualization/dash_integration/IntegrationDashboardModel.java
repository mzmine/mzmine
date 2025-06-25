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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableTab;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.ProjectService;
import java.util.List;
import java.util.function.Function;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntegrationDashboardModel {

  private final ObjectProperty<@NotNull ModularFeatureList> featureList = new SimpleObjectProperty<>(
      new ModularFeatureList("flist", null, List.of()));
  private final ObjectProperty<@NotNull FeatureTableTab> featureTableTab = new ReadOnlyObjectWrapper<>(
      new FeatureTableTab(featureList.get()));
  private final ObjectProperty<@NotNull FeatureTableFX> featureTableFx = new ReadOnlyObjectWrapper<>(
      featureTableTab.get().getFeatureTable());
  private final ObjectProperty<@Nullable FeatureListRow> row = new SimpleObjectProperty<>();
  private final ListProperty<RawDataFile> sortedFiles = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ObjectProperty<@NotNull MetadataColumn<?>> rawFileSortingColumn = new SimpleObjectProperty<>(
      ProjectService.getMetadata().getSampleTypeColumn());
  private final ObjectProperty<IntegrationTransfer> syncReIntegration = new SimpleObjectProperty<>(
      IntegrationTransfer.NONE);
  private final ObjectProperty<@NotNull MZTolerance> integrationTolerance = new SimpleObjectProperty<>(
      new MZTolerance(0.005, 10));
  private final MapProperty<RawDataFile, FeatureIntegrationData> featureDataEntries = new SimpleMapProperty<>(
      FXCollections.observableHashMap());
  private final ObjectProperty<@NotNull Function<IonTimeSeries, IonTimeSeries>> postProcessingMethod = new SimpleObjectProperty<>(
      t -> t);
  private final BooleanProperty applyPostProcessing = new SimpleBooleanProperty(false);

  private final IntegerProperty gridNumColumns = new SimpleIntegerProperty(3);
  private final IntegerProperty gridNumRows = new SimpleIntegerProperty(2);
  private final IntegerProperty gridPaneFileOffset = new SimpleIntegerProperty(0);
  private final NumberBinding cellsPerPage = gridNumColumns.multiply(gridNumRows);
  private final NumberBinding numPages = featureDataEntries.sizeProperty().divide(cellsPerPage)
      .add(1);

  public int getGridNumColumns() {
    return gridNumColumns.get();
  }

  public void setGridNumColumns(int gridNumColumns) {
    this.gridNumColumns.set(gridNumColumns);
  }

  public IntegerProperty gridNumColumnsProperty() {
    return gridNumColumns;
  }

  public int getGridNumRows() {
    return gridNumRows.get();
  }

  public void setGridNumRows(int gridNumRows) {
    this.gridNumRows.set(gridNumRows);
  }

  public IntegerProperty gridNumRowsProperty() {
    return gridNumRows;
  }

  public int getCellsPerPage() {
    return cellsPerPage.intValue();
  }

  public NumberBinding cellsPerPageProperty() {
    return cellsPerPage;
  }

  public int getNumPages() {
    return (int) Math.ceil(numPages.doubleValue());
  }

  public NumberBinding numPagesProperty() {
    return numPages;
  }

  public @NotNull ModularFeatureList getFeatureList() {
    return featureList.get();
  }

  public void setFeatureList(@NotNull ModularFeatureList flist) {
    this.featureList.set(flist);
  }

  public ObjectProperty<@NotNull ModularFeatureList> featureListProperty() {
    return featureList;
  }

  public @NotNull FeatureTableFX getFeatureTableFx() {
    return featureTableFx.get();
  }

  public void setFeatureTableFx(@NotNull FeatureTableFX featureTableFx) {
    this.featureTableFx.set(featureTableFx);
  }

  public ObjectProperty<@NotNull FeatureTableFX> featureTableFxProperty() {
    return featureTableFx;
  }

  public @Nullable FeatureListRow getRow() {
    return row.get();
  }

  public void setRow(@Nullable FeatureListRow row) {
    this.row.set(row);
  }

  public ObjectProperty<@Nullable FeatureListRow> rowProperty() {
    return row;
  }

  public ObservableList<RawDataFile> getSortedFiles() {
    return sortedFiles.get();
  }

  public void setSortedFiles(List<RawDataFile> sortedFiles) {
    this.sortedFiles.setAll(sortedFiles);
  }

  public ListProperty<RawDataFile> sortedFilesProperty() {
    return sortedFiles;
  }

  public @NotNull MetadataColumn<?> getRawFileSortingColumn() {
    return rawFileSortingColumn.get();
  }

  public void setRawFileSortingColumn(@NotNull MetadataColumn<?> rawFileSortingColumn) {
    this.rawFileSortingColumn.set(rawFileSortingColumn);
  }

  public ObjectProperty<@NotNull MetadataColumn<?>> rawFileSortingColumnProperty() {
    return rawFileSortingColumn;
  }

  public IntegrationTransfer getSyncReIntegration() {
    return syncReIntegration.get();
  }

  public ObjectProperty<IntegrationTransfer> syncReIntegrationProperty() {
    return syncReIntegration;
  }

  public @NotNull MZTolerance getIntegrationTolerance() {
    return integrationTolerance.get();
  }

  public void setIntegrationTolerance(@NotNull MZTolerance integrationTolerance) {
    this.integrationTolerance.set(integrationTolerance);
  }

  public ObjectProperty<@NotNull MZTolerance> integrationToleranceProperty() {
    return integrationTolerance;
  }

  public ObservableMap<RawDataFile, FeatureIntegrationData> getFeatureDataEntries() {
    return featureDataEntries.get();
  }

  public MapProperty<RawDataFile, FeatureIntegrationData> featureDataEntriesProperty() {
    return featureDataEntries;
  }

  public int getGridPaneFileOffset() {
    return gridPaneFileOffset.get();
  }

  public void setGridPaneFileOffset(int gridPaneFileOffset) {
    this.gridPaneFileOffset.set(gridPaneFileOffset);
  }

  public IntegerProperty gridPaneFileOffsetProperty() {
    return gridPaneFileOffset;
  }

  public @NotNull FeatureTableTab getFeatureTableTab() {
    return featureTableTab.get();
  }

  public ObjectProperty<@NotNull FeatureTableTab> featureTableTabProperty() {
    return featureTableTab;
  }

  public @NotNull Function<IonTimeSeries, IonTimeSeries> getPostProcessingMethod() {
    return postProcessingMethod.get();
  }

  public ObjectProperty<@NotNull Function<IonTimeSeries, IonTimeSeries>> postProcessingMethodProperty() {
    return postProcessingMethod;
  }

  public boolean isApplyPostProcessing() {
    return applyPostProcessing.get();
  }

  public BooleanProperty applyPostProcessingProperty() {
    return applyPostProcessing;
  }

  public void setApplyPostProcessing(boolean applyPostProcessing) {
    this.applyPostProcessing.set(applyPostProcessing);
  }
}
