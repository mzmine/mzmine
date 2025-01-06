package io.github.mzmine.modules.visualization.dash_integration;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.project.ProjectService;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
  private ListProperty<FeatureDataEntry> featureDataEntries = new SimpleListProperty<>(
      FXCollections.observableArrayList());

  public int getGridSizeX() {
    return gridSizeX.get();
  }

  public IntegerProperty gridSizeXProperty() {
    return gridSizeX;
  }

  public void setGridSizeX(int gridSizeX) {
    this.gridSizeX.set(gridSizeX);
  }

  public int getGridSizeY() {
    return gridSizeY.get();
  }

  public IntegerProperty gridSizeYProperty() {
    return gridSizeY;
  }

  public void setGridSizeY(int gridSizeY) {
    this.gridSizeY.set(gridSizeY);
  }

  public @NotNull ModularFeatureList getFeatureList() {
    return featureList.get();
  }

  public ObjectProperty<@NotNull ModularFeatureList> featureListProperty() {
    return featureList;
  }

  public void setFeatureList(@NotNull ModularFeatureList flist) {
    this.featureList.set(flist);
  }

  public @NotNull FeatureTableFX getFeatureTableFx() {
    return featureTableFx.get();
  }

  public ObjectProperty<@NotNull FeatureTableFX> featureTableFxProperty() {
    return featureTableFx;
  }

  public void setFeatureTableFx(@NotNull FeatureTableFX featureTableFx) {
    this.featureTableFx.set(featureTableFx);
  }

  public @Nullable FeatureListRow getRow() {
    return row.get();
  }

  public ObjectProperty<@Nullable FeatureListRow> rowProperty() {
    return row;
  }

  public void setRow(@Nullable FeatureListRow row) {
    this.row.set(row);
  }

  public ObservableList<RawDataFile> getSortedFiles() {
    return sortedFiles.get();
  }

  public ListProperty<RawDataFile> sortedFilesProperty() {
    return sortedFiles;
  }

  public void setSortedFiles(List<RawDataFile> sortedFiles) {
    this.sortedFiles.setAll(sortedFiles);
  }

  public @NotNull MetadataColumn<?> getRawFileSortingColumn() {
    return rawFileSortingColumn.get();
  }

  public ObjectProperty<@NotNull MetadataColumn<?>> rawFileSortingColumnProperty() {
    return rawFileSortingColumn;
  }

  public void setRawFileSortingColumn(@NotNull MetadataColumn<?> rawFileSortingColumn) {
    this.rawFileSortingColumn.set(rawFileSortingColumn);
  }

  public boolean isSyncReIntegration() {
    return syncReIntegration.get();
  }

  public BooleanProperty syncReIntegrationProperty() {
    return syncReIntegration;
  }

  public void setSyncReIntegration(boolean syncReIntegration) {
    this.syncReIntegration.set(syncReIntegration);
  }

  public ObservableList<FeatureDataEntry> getFeatureDataEntries() {
    return featureDataEntries.get();
  }

  public ListProperty<FeatureDataEntry> featureDataEntriesProperty() {
    return featureDataEntries;
  }

  public void setFeatureDataEntries(List<FeatureDataEntry> featureDataEntries) {
    this.featureDataEntries.setAll(featureDataEntries);
  }
}
