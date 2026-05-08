package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the observable state of the compound dashboard. Pure data — no logic. The Controller wires
 * external selection into {@link #selectedCompoundRowProperty()} (via FxControllerBinding) and the
 * Interactor maintains the derived lists ({@link #getAvailableRawDataFiles()},
 * {@link #getAdductRows()}) and the dataset output properties.
 */
public class CompoundDashboardModel {

  // --- master selection ------------------------------------------------------
  private final ObjectProperty<@Nullable CompoundRow> selectedCompoundRow = new SimpleObjectProperty<>();
  private final ObjectProperty<@Nullable ModularFeatureList> featureList = new SimpleObjectProperty<>();
  // selectedFeatureLists is exposed as a Property<List<FeatureList>> backed by a derived view of
  // featureList, so the dashboard can implement SelectedFeatureListsBinding to share with
  // CompoundRowQualityController.
  private final ObjectProperty<List<FeatureList>> selectedFeatureLists = new SimpleObjectProperty<>(
      List.of());
  private final ObjectProperty<@Nullable RawDataFile> currentRawDataFile = new SimpleObjectProperty<>();
  // first-level adduct member rows (drives MS2 selector)
  private final ObjectProperty<@Nullable FeatureListRow> selectedAdductRow = new SimpleObjectProperty<>();

  // --- derived ---------------------------------------------------------------
  private final ObservableList<RawDataFile> availableRawDataFiles = FXCollections.observableArrayList();
  private final ObservableList<FeatureListRow> adductRows = FXCollections.observableArrayList();

  // --- visual state ----------------------------------------------------------
  private final ObjectProperty<@Nullable SimpleColorPalette> colorPalette = new SimpleObjectProperty<>();
  private final BooleanProperty computing = new SimpleBooleanProperty(false);

  // --- dataset outputs (mutated by FxUpdateTask.updateGuiModel on FX thread) -
  // Bare ObservableLists so subscribers get list-change events; the controller pushes the contents
  // onto the sub-controllers in the list listener.
  private final ObservableList<DatasetAndRenderer> eicDatasets = FXCollections.observableArrayList();
  private final ObservableList<DatasetAndRenderer> ms1Datasets = FXCollections.observableArrayList();
  private final ObservableList<DatasetAndRenderer> ms2Datasets = FXCollections.observableArrayList();

  // --- accessors -------------------------------------------------------------

  public @Nullable CompoundRow getSelectedCompoundRow() {
    return selectedCompoundRow.get();
  }

  public void setSelectedCompoundRow(@Nullable CompoundRow row) {
    selectedCompoundRow.set(row);
  }

  public ObjectProperty<@Nullable CompoundRow> selectedCompoundRowProperty() {
    return selectedCompoundRow;
  }

  public @Nullable ModularFeatureList getFeatureList() {
    return featureList.get();
  }

  public void setFeatureList(@Nullable ModularFeatureList flist) {
    featureList.set(flist);
    selectedFeatureLists.set(flist == null ? List.of() : List.of(flist));
  }

  public ObjectProperty<@Nullable ModularFeatureList> featureListProperty() {
    return featureList;
  }

  public Property<List<FeatureList>> selectedFeatureListsProperty() {
    return selectedFeatureLists;
  }

  public @Nullable RawDataFile getCurrentRawDataFile() {
    return currentRawDataFile.get();
  }

  public void setCurrentRawDataFile(@Nullable RawDataFile file) {
    currentRawDataFile.set(file);
  }

  public ObjectProperty<@Nullable RawDataFile> currentRawDataFileProperty() {
    return currentRawDataFile;
  }

  public @Nullable FeatureListRow getSelectedAdductRow() {
    return selectedAdductRow.get();
  }

  public void setSelectedAdductRow(@Nullable FeatureListRow row) {
    selectedAdductRow.set(row);
  }

  public ObjectProperty<@Nullable FeatureListRow> selectedAdductRowProperty() {
    return selectedAdductRow;
  }

  public @NotNull ObservableList<RawDataFile> getAvailableRawDataFiles() {
    return availableRawDataFiles;
  }

  public @NotNull ObservableList<FeatureListRow> getAdductRows() {
    return adductRows;
  }

  public @Nullable SimpleColorPalette getColorPalette() {
    return colorPalette.get();
  }

  public void setColorPalette(@Nullable SimpleColorPalette palette) {
    colorPalette.set(palette);
  }

  public ObjectProperty<@Nullable SimpleColorPalette> colorPaletteProperty() {
    return colorPalette;
  }

  public boolean isComputing() {
    return computing.get();
  }

  public void setComputing(boolean value) {
    computing.set(value);
  }

  public BooleanProperty computingProperty() {
    return computing;
  }

  public @NotNull ObservableList<DatasetAndRenderer> getEicDatasets() {
    return eicDatasets;
  }

  public @NotNull ObservableList<DatasetAndRenderer> getMs1Datasets() {
    return ms1Datasets;
  }

  public @NotNull ObservableList<DatasetAndRenderer> getMs2Datasets() {
    return ms2Datasets;
  }
}
