/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.XYDatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.data.xy.XYDataset;

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
  // The row whose MS2 the MS2 chart draws. Bound bidirectionally to the adduct ComboBox.
  // Can be null (no MS2 plot) when the compound has no MS2-capable member.
  private final ObjectProperty<@Nullable FeatureListRow> selectedMs2Row = new SimpleObjectProperty<>();
  // Highlight target for EIC + MS1 (thicker line / wider sticks). Derived in the controller:
  // mirrors selectedMs2Row when non-null, otherwise falls back to the preferred row of the
  // selected compound. This way the highlight always has a target even when no MS2 exists, and
  // the EIC/MS1 plots and the MS2 selector stay decoupled.
  private final ObjectProperty<@Nullable FeatureListRow> selectedAdductRow = new SimpleObjectProperty<>();

  // The MS2 scan rendered by the MS2 chart. First item is the merged scan; navigated by the
  // prev/next icon buttons next to the adduct ComboBox.
  private final ObjectProperty<@Nullable Scan> selectedMs2Scan = new SimpleObjectProperty<>();

  // The isotope pattern rendered on top of the isotope mirror plot (the detected pattern for the
  // currently selected charge state). Navigated by the prev/next icon buttons and the charge
  // ComboBox above the mirror plot. Can be null when the source ion has no detected pattern.
  private final ObjectProperty<@Nullable IsotopePattern> selectedIsotopePattern = new SimpleObjectProperty<>();
  // The representative MS1 scan rendered on the bottom of the isotope mirror plot. Same picking
  // rule as the MS1 background scan, but resolved for the ion that supplies the isotope pattern.
  private final ObjectProperty<@Nullable Scan> isotopeRepresentativeScan = new SimpleObjectProperty<>();

  // --- derived ---------------------------------------------------------------
  private final ObservableList<RawDataFile> availableRawDataFiles = FXCollections.observableArrayList();
  private final ObservableList<FeatureListRow> adductRows = FXCollections.observableArrayList();
  // All MS2 scans for the currently selected MS2 row: first the merged scan (REPRESENTATIVE across
  // samples), then the row's individual fragment scans. Drives the scan ComboBox.
  private final ObservableList<Scan> availableMs2Scans = FXCollections.observableArrayList();
  // All detected isotope patterns (one per charge-state hypothesis) for the ion that supplies the
  // isotope mirror plot. Drives the charge-state ComboBox above the mirror. Empty when the ion has
  // no detected isotope pattern.
  private final ObservableList<IsotopePattern> isotopeChargeStates = FXCollections.observableArrayList();
  // All member rows of the currently selected compound (flattened) paired with the color the EIC /
  // mobilogram / MS1 plots use for that row. Maintained by the Interactor so the legend FlowPane
  // mirrors what the user sees on the plots.
  private final ObservableList<CompoundDashboardLegendEntry> legendEntries = FXCollections.observableArrayList();

  // --- visual state ----------------------------------------------------------
  private final ObjectProperty<@Nullable SimpleColorPalette> colorPalette = new SimpleObjectProperty<>();
  private final BooleanProperty computing = new SimpleBooleanProperty(false);

  // --- dataset outputs (mutated by FxUpdateTask.updateGuiModel on FX thread) -
  // Bare ObservableLists so subscribers get list-change events; the controller pushes the contents
  // onto the sub-controllers in the list listener.
  private final ObservableList<XYDatasetAndRenderer> eicDatasets = FXCollections.observableArrayList();
  private final ObservableList<XYDatasetAndRenderer> mobilogramDatasets = FXCollections.observableArrayList();
  private final ObservableList<XYDatasetAndRenderer> ms1Datasets = FXCollections.observableArrayList();
  private final ObservableList<XYDatasetAndRenderer> ms2Datasets = FXCollections.observableArrayList();

  // Reverse maps row -> renderer so the controller can change stroke/bar width on the renderer
  // belonging to the selected adduct row without rebuilding the datasets. Populated by the spectra
  // and EIC tasks in updateGuiModel (before the dataset lists are setAll'd) and consumed by the
  // controller's selectedAdductRow subscriber. IdentityHashMap because rows compare by reference.
  private final Map<FeatureListRow, ColoredXYLineRenderer> eicRenderersByRow = new IdentityHashMap<>();
  private final Map<FeatureListRow, ColoredXYBarRenderer> ms1RenderersByRow = new IdentityHashMap<>();

  // Row -> dataset reverse maps for the EIC and mobilogram plots. Used to bridge selectedAdductRow
  // <-> ChromatogramPlotModel#selectedDataset so a row selection can highlight the right line and a
  // legend click on the plot can resolve back to a row.
  private final Map<FeatureListRow, XYDataset> eicDatasetsByRow = new IdentityHashMap<>();
  private final Map<FeatureListRow, XYDataset> mobilogramDatasetsByRow = new IdentityHashMap<>();

  // Chart titles for MS1 and MS2; computed by the spectra task so they always match the scans
  // actually rendered.
  private final StringProperty ms1Title = new SimpleStringProperty("");
  private final StringProperty ms2Title = new SimpleStringProperty("");

  // Domain axis label for the mobilogram plot; the mobilogram task overrides this per IMS file so
  // the unit follows the actual mobility type (e.g. "1/K0", "drift time (ms)").
  private final StringProperty mobilogramDomainAxisLabel = new SimpleStringProperty("Mobility");

  // Visibility of the optional 4D feature plot pane. Defaults to true so users can immediately see
  // the bubble plot when opening the dashboard; the toggle button in the EIC toolbar collapses it.
  private final BooleanProperty featurePlot4DVisible = new SimpleBooleanProperty(true);

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

  public @Nullable FeatureListRow getSelectedMs2Row() {
    return selectedMs2Row.get();
  }

  public void setSelectedMs2Row(@Nullable FeatureListRow row) {
    selectedMs2Row.set(row);
  }

  public ObjectProperty<@Nullable FeatureListRow> selectedMs2RowProperty() {
    return selectedMs2Row;
  }

  public @NotNull ObservableList<RawDataFile> getAvailableRawDataFiles() {
    return availableRawDataFiles;
  }

  public @NotNull ObservableList<FeatureListRow> getAdductRows() {
    return adductRows;
  }

  public @NotNull ObservableList<Scan> getAvailableMs2Scans() {
    return availableMs2Scans;
  }

  public @Nullable Scan getSelectedMs2Scan() {
    return selectedMs2Scan.get();
  }

  public void setSelectedMs2Scan(@Nullable Scan scan) {
    selectedMs2Scan.set(scan);
  }

  public ObjectProperty<@Nullable Scan> selectedMs2ScanProperty() {
    return selectedMs2Scan;
  }

  public @NotNull ObservableList<IsotopePattern> getIsotopeChargeStates() {
    return isotopeChargeStates;
  }

  public @Nullable IsotopePattern getSelectedIsotopePattern() {
    return selectedIsotopePattern.get();
  }

  public void setSelectedIsotopePattern(@Nullable IsotopePattern pattern) {
    selectedIsotopePattern.set(pattern);
  }

  public ObjectProperty<@Nullable IsotopePattern> selectedIsotopePatternProperty() {
    return selectedIsotopePattern;
  }

  public @Nullable Scan getIsotopeRepresentativeScan() {
    return isotopeRepresentativeScan.get();
  }

  public void setIsotopeRepresentativeScan(@Nullable Scan scan) {
    isotopeRepresentativeScan.set(scan);
  }

  public ObjectProperty<@Nullable Scan> isotopeRepresentativeScanProperty() {
    return isotopeRepresentativeScan;
  }

  public @NotNull ObservableList<CompoundDashboardLegendEntry> getLegendEntries() {
    return legendEntries;
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

  public @NotNull ObservableList<XYDatasetAndRenderer> getEicDatasets() {
    return eicDatasets;
  }

  public @NotNull ObservableList<XYDatasetAndRenderer> getMobilogramDatasets() {
    return mobilogramDatasets;
  }

  public @NotNull ObservableList<XYDatasetAndRenderer> getMs1Datasets() {
    return ms1Datasets;
  }

  public @NotNull ObservableList<XYDatasetAndRenderer> getMs2Datasets() {
    return ms2Datasets;
  }

  public @NotNull Map<FeatureListRow, ColoredXYLineRenderer> getEicRenderersByRow() {
    return eicRenderersByRow;
  }

  public @NotNull Map<FeatureListRow, ColoredXYBarRenderer> getMs1RenderersByRow() {
    return ms1RenderersByRow;
  }

  public @NotNull Map<FeatureListRow, XYDataset> getEicDatasetsByRow() {
    return eicDatasetsByRow;
  }

  public @NotNull Map<FeatureListRow, XYDataset> getMobilogramDatasetsByRow() {
    return mobilogramDatasetsByRow;
  }

  public @Nullable String getMs1Title() {
    return ms1Title.get();
  }

  public void setMs1Title(@Nullable String title) {
    ms1Title.set(title);
  }

  public StringProperty ms1TitleProperty() {
    return ms1Title;
  }

  public @Nullable String getMs2Title() {
    return ms2Title.get();
  }

  public void setMs2Title(@Nullable String title) {
    ms2Title.set(title);
  }

  public StringProperty ms2TitleProperty() {
    return ms2Title;
  }

  public @Nullable String getMobilogramDomainAxisLabel() {
    return mobilogramDomainAxisLabel.get();
  }

  public void setMobilogramDomainAxisLabel(@Nullable String label) {
    mobilogramDomainAxisLabel.set(label);
  }

  public StringProperty mobilogramDomainAxisLabelProperty() {
    return mobilogramDomainAxisLabel;
  }

  public boolean isFeaturePlot4DVisible() {
    return featurePlot4DVisible.get();
  }

  public BooleanProperty featurePlot4DVisibleProperty() {
    return featurePlot4DVisible;
  }
}
