package io.github.mzmine.modules.visualization.dash_integration;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.ProjectService;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
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

  private final IntegerProperty gridSizeX = new SimpleIntegerProperty(3);
  private final IntegerProperty gridSizeY = new SimpleIntegerProperty(2);
  private final ObjectProperty<@NotNull ModularFeatureList> featureList = new SimpleObjectProperty<>(
      new ModularFeatureList("flist", null, List.of()));
  private final ObjectProperty<@NotNull FeatureTableFX> featureTableFx = new SimpleObjectProperty<>(
      new FeatureTableFX());
  private final ObjectProperty<@Nullable FeatureListRow> row = new SimpleObjectProperty<>();
  private final ListProperty<RawDataFile> sortedFiles = new SimpleListProperty<>();
  private final ObjectProperty<@NotNull MetadataColumn<?>> rawFileSortingColumn = new SimpleObjectProperty<>(
      ProjectService.getMetadata().getSampleTypeColumn());
  private final BooleanProperty syncReIntegration = new SimpleBooleanProperty(false);
  private final ObjectProperty<@NotNull MZTolerance> integrationTolerance = new SimpleObjectProperty<>(
      new MZTolerance(0.005, 10));
  private final MapProperty<RawDataFile, FeatureDataEntry> featureDataEntries = new SimpleMapProperty<>(
      FXCollections.observableHashMap());

  public int getGridSizeX() {
    return gridSizeX.get();
  }

  public void setGridSizeX(int gridSizeX) {
    this.gridSizeX.set(gridSizeX);
  }

  public IntegerProperty gridSizeXProperty() {
    return gridSizeX;
  }

  public int getGridSizeY() {
    return gridSizeY.get();
  }

  public void setGridSizeY(int gridSizeY) {
    this.gridSizeY.set(gridSizeY);
  }

  public IntegerProperty gridSizeYProperty() {
    return gridSizeY;
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

  public boolean isSyncReIntegration() {
    return syncReIntegration.get();
  }

  public void setSyncReIntegration(boolean syncReIntegration) {
    this.syncReIntegration.set(syncReIntegration);
  }

  public BooleanProperty syncReIntegrationProperty() {
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

  public ObservableMap<RawDataFile, FeatureDataEntry> getFeatureDataEntries() {
    return featureDataEntries.get();
  }

  public MapProperty<RawDataFile, FeatureDataEntry> featureDataEntriesProperty() {
    return featureDataEntries;
  }
}
