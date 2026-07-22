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
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRowSelection;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.AnnotationSummaryType;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.XYDatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.framework.fx.FxControllerBinding;
import io.github.mzmine.gui.framework.fx.SelectedCompoundRowBinding;
import io.github.mzmine.gui.framework.fx.SelectedFeatureListsBinding;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxInteractor;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.CompoundRowQualityController;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckEvent;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckEvent.AnnotationDetailRequestedEvent;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckEvent.AnnotationStructureSelectedEvent;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckEvent.AnnotationSummaryActivated;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckEvent.FragmentEnergyMethodSelectedEvent;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableOwner;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableController;
import io.github.mzmine.modules.visualization.featurerow4dplot.FeatureRow4DPlotController;
import io.github.mzmine.modules.visualization.network_overview.NetworkOverviewFlavor;
import io.github.mzmine.modules.visualization.network_overview.NetworkOverviewWindow;
import io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot.ChromatogramPlotController;
import io.github.mzmine.modules.visualization.spectra.simplespectrachart.SimpleSpectraChartController;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.data.xy.XYDataset;

/**
 * Top-level controller for the Compound Dashboard. Bundles a {@link ChromatogramPlotController},
 * two {@link SimpleSpectraChartController} (MS1 + MS2), the existing
 * {@link CompoundRowQualityController} and an {@link FxFeatureTableController}, and orchestrates
 * dataset rebuilds when the selected compound, raw file, or adduct change.
 */
public class CompoundDashboardController extends FxController<CompoundDashboardModel> implements
    SelectedCompoundRowBinding, SelectedFeatureListsBinding {

  // Coalesces rapid selection changes (arrow-key navigation) into a single recompute.
  private static final Duration DEBOUNCE = Duration.millis(150);

  // Selected-row highlight: wider MS1 sticks for the renderer owned by the currently selected
  // adduct row. EIC line-width highlighting now lives in ChromatogramPlotBuilder, driven by the
  // chromatogram plot model's selectedDataset property.
  private static final double MS1_DEFAULT_BAR_MULTIPLIER = 1.0;
  private static final double MS1_SELECTED_BAR_MULTIPLIER = 2.0;

  private final ChromatogramPlotController eicPlot = new ChromatogramPlotController(true);
  private final ChromatogramPlotController mobilogramPlot = new ChromatogramPlotController(true);
  private final SimpleSpectraChartController ms1Chart = new SimpleSpectraChartController();
  private final SimpleSpectraChartController ms2Chart = new SimpleSpectraChartController();
  // Shown in place of the isotope mirror when the selected row has no detected isotope pattern:
  // a plain full MS1 spectrum of the row's representative scan.
  private final SimpleSpectraChartController isotopeSpectrumChart = new SimpleSpectraChartController();
  private final CompoundRowQualityController qualityCtrl = new CompoundRowQualityController();
  private final FxFeatureTableController tableCtrl = new FxFeatureTableController(
      FeatureTableOwner.COMPOUND_DASHBOARD);
  private final FeatureRow4DPlotController featurePlot4D = new FeatureRow4DPlotController();

  // Guards both directions of the selectedAdductRow <-> plot-selection bridge (selectedDataset and
  // cursor-position dataset) so we don't bounce events back and forth when one side updates the
  // other.
  private boolean syncingSelection = false;

  private final CompoundDashboardInteractor interactor;
  private final CompoundDashboardViewBuilder builder;

  public CompoundDashboardController() {
    super(new CompoundDashboardModel());
    this.interactor = new CompoundDashboardInteractor(model);
    this.builder = new CompoundDashboardViewBuilder(model, this, eicPlot, mobilogramPlot, ms1Chart,
        ms2Chart, isotopeSpectrumChart, qualityCtrl, tableCtrl, featurePlot4D);

    model.setColorPalette(CompoundDashboardInteractor.snapshotPalette());

    // Quality pane: bind selectedCompoundRow + selectedFeatureLists in both directions.
    FxControllerBinding.bindExposedProperties(this, qualityCtrl);
    // Mirror the dashboard color palette into the quality pane so per-member chips (e.g. in the
    // IMS fragmentation check) match the EIC / mobilogram / MS1 colors. Bidirectional because the
    // dashboard owns the source of truth and may snapshot a fresh palette per compound change.
    qualityCtrl.colorPaletteProperty().bind(model.colorPaletteProperty());
    // Bidirectional binding: the quality pane's selected member row IS the dashboard's selected
    // adduct row. Clicking a chip in any check writes through to the dashboard; programmatic
    // selection from the dashboard (e.g. legend click, table click) reflects back into the
    // chips' bold-label state. Same end-effect as the previous one-way onMemberRowClick callback
    // but now also drives the chip selection styling.
    qualityCtrl.selectedMemberRowProperty().bindBidirectional(model.selectedAdductRowProperty());
    // Sealed {@link QualityCheckEvent} bus from the quality pane. A
    // {@link FragmentEnergyMethodSelectedEvent} fires after a fragment-scan chip click; by then the
    // accompanying onMemberRowClick has already updated selectedAdductRow (which in turn populated
    // availableMs2Scans). Pick the row's first scan with matching energy + method and assign it as
    // selectedMs2Scan so the MS2 chart switches to it.
    qualityCtrl.onQualityCheckEventProperty().set(this::handleQualityCheckEvent);
    // 4D feature plot: share the selected compound row + the selected feature list with the
    // dashboard, and the selected rows with the feature table so a click in the bubble plot drives
    // the table selection (and vice versa). The SelectedFeatureListsBinding is what propagates the
    // dashboard's current list into the plot, so no explicit setFeatureList call is needed below.
    FxControllerBinding.bindExposedProperties(this, featurePlot4D);
    FxControllerBinding.bindExposedProperties(tableCtrl, featurePlot4D);

    // The 4D plot owns its own row-subset selector (see FeatureRow4DPlotViewBuilder Options pane),
    // so we only seed the initial value once. COMPOUNDS shows one bubble per compound and matches
    // the dashboard's compound-centric view; the plot falls back to ALL_MAJOR_IONS automatically
    // when the active feature list has no CompoundList.
    featurePlot4D.setCompoundRowSelection(CompoundRowSelection.COMPOUNDS);

    // Feature table receives the feature list directly. After a new list is set, auto-select the
    // first available row so the dashboard already shows charts.
    model.featureListProperty().subscribe(flist -> {
      tableCtrl.setFeatureList(flist);
      if (flist != null) {
        selectFirstRowWhenAvailable();
      }
    });

    // Table -> model: resolve the clicked row to its parent CompoundRow (mirrors
    // StatsDashboardController.resolveCompoundRow). Mute echoes by checking equality first.
    // When the clicked row is a child (member) of a compound rather than the compound itself,
    // also set it as the selectedAdductRow so the EIC/MS1 highlight follows the user's pick. The
    // adduct ComboBox / selectedMs2Row are derived from selectedAdductRow by a separate listener
    // below, so we only need to write the single master property here.
    tableCtrl.getFeatureTable().getSelectionModel().selectedItemProperty()
        .addListener((_, _, item) -> {
          final FeatureListRow clicked = item == null ? null : item.getValue();
          final CompoundRow resolved = resolveCompoundRow(clicked);
          if (resolved != model.getSelectedCompoundRow()) {
            model.setSelectedCompoundRow(resolved);
          }
          if (clicked != null && !(clicked instanceof CompoundRow)
              && model.getSelectedAdductRow() != clicked) {
            model.setSelectedAdductRow(clicked);
          }
        });

    // Cheap, FX-thread refresh of derived state when compound or feature list changes.
    PropertyUtils.onChange(interactor::onSelectionChanged, model.selectedCompoundRowProperty(),
        model.featureListProperty());

    // Heavy debounced recompute of MS1+MS2. The MS2 source is `selectedMs2Scan` (the user's pick in
    // the scan ComboBox). Triggering on selectedMs2Row would be redundant because every row change
    // is followed by a recomputeAvailableMs2Scans -> setSelectedMs2Scan call below, which fires the
    // scan change here. Triggering on selectedAdductRow would also be redundant because every
    // selectedAdductRow change propagates into selectedMs2Row first.
    PropertyUtils.onChangeDelayedSubscription(this::scheduleSpectra, DEBOUNCE,
        model.selectedCompoundRowProperty(), model.selectedMs2ScanProperty(),
        model.colorPaletteProperty(), model.currentRawDataFileProperty());
    // When the selected MS2 row changes, rebuild the list of available MS2 scans (merged +
    // individual fragment scans) and reset selectedMs2Scan to the merged scan. The scan change in
    // turn triggers the spectra task above.
    model.selectedMs2RowProperty().subscribe(_ -> interactor.recomputeAvailableMs2Scans());
    // Isotope mirror: recompute the charge-state list + representative MS1 scan whenever the ion
    // selection or the current raw file changes. Cheap (patterns are already in memory), so no
    // background task is needed. Runs on the FX thread.
    PropertyUtils.onChange(interactor::recomputeIsotopePattern, model.selectedAdductRowProperty(),
        model.currentRawDataFileProperty());
    // Heavy debounced recompute of EICs.
    PropertyUtils.onChangeDelayedSubscription(this::scheduleEic, DEBOUNCE,
        model.selectedCompoundRowProperty(), model.currentRawDataFileProperty(),
        model.colorPaletteProperty());
    // Heavy debounced recompute of mobilograms. Shares triggers with the EIC task.
    PropertyUtils.onChangeDelayedSubscription(this::scheduleMobilogram, DEBOUNCE,
        model.selectedCompoundRowProperty(), model.currentRawDataFileProperty(),
        model.colorPaletteProperty());

    // Apply prebuilt datasets to the sub-controllers when the model lists change. The lists are
    // mutated by FxUpdateTask.updateGuiModel which already runs on the FX thread.
    PropertyUtils.onChangeList(() -> applyDatasets(eicPlot, model.getEicDatasets()),
        model.getEicDatasets());
    PropertyUtils.onChangeList(() -> applyDatasets(mobilogramPlot, model.getMobilogramDatasets()),
        model.getMobilogramDatasets());
    PropertyUtils.onChangeList(() -> applyDatasets(ms1Chart, model.getMs1Datasets()),
        model.getMs1Datasets());
    PropertyUtils.onChangeList(() -> applyDatasets(ms2Chart, model.getMs2Datasets()),
        model.getMs2Datasets());

    // Mobilogram domain axis label tracks the mobility type of the current IMS file.
    model.mobilogramDomainAxisLabelProperty().subscribe(label -> {
      if (label != null) {
        mobilogramPlot.setDomainAxisLabel(label);
      }
    });

    // Selection model: selectedAdductRow is the user's pick (any member row, MS2 or not).
    // selectedMs2Row is a derived view used by the adduct ComboBox and the MS2 spectra task:
    //   - selectedAdductRow change -> selectedMs2Row = (in adductRows ? adduct : null)
    //   - selectedMs2Row change (ComboBox picked or programmatic non-null) -> mirror back into
    //     selectedAdductRow so the legend bold + EIC/MS1 highlight follow the ComboBox too.
    // The non-null guard on the inverse direction means clicking a row without MS2 (which sets
    // selectedMs2Row to null) does NOT clobber selectedAdductRow.
    PropertyUtils.onChange(this::updateSelectedMs2Row, model.selectedAdductRowProperty());
    PropertyUtils.onChange(this::updateSelectedAdductRowFromMs2, model.selectedMs2RowProperty());

    // Re-apply the selected-row highlight whenever the selection changes or fresh datasets land.
    // The renderer maps are updated in the task's updateGuiModel before the dataset list setAll,
    // so by the time these listeners fire the maps already reflect the new datasets.
    model.selectedAdductRowProperty().subscribe(_ -> applySelectionHighlight());
    model.getEicDatasets()
        .addListener((ListChangeListener<XYDatasetAndRenderer>) _ -> applySelectionHighlight());
    model.getMobilogramDatasets()
        .addListener((ListChangeListener<XYDatasetAndRenderer>) _ -> applySelectionHighlight());
    model.getMs1Datasets()
        .addListener((ListChangeListener<XYDatasetAndRenderer>) _ -> applySelectionHighlight());

    // Bridge plot selection -> selectedAdductRow. Legend clicks change selectedDataset directly;
    // data-point clicks update the FxXYPlot cursor position, which carries the selected dataset.
    // The forward direction (selectedAdductRow -> selectedDataset) is handled by
    // applySelectionHighlight. A reentrancy guard prevents bounce-back when one side updates the
    // other.
    eicPlot.selectedDatasetProperty()
        .subscribe(ds -> bridgeDatasetToRow(ds, model.getEicDatasetsByRow()));
    mobilogramPlot.selectedDatasetProperty()
        .subscribe(ds -> bridgeDatasetToRow(ds, model.getMobilogramDatasetsByRow()));
    eicPlot.getXYPlot().getCursorConfigModel().cursorPositionProperty()
        .subscribe(pos -> bridgeCursorPositionToRow(pos, model.getEicDatasetsByRow()));
    mobilogramPlot.getXYPlot().getCursorConfigModel().cursorPositionProperty()
        .subscribe(pos -> bridgeCursorPositionToRow(pos, model.getMobilogramDatasetsByRow()));

    // Chart titles are computed by the spectra task per scan and pushed via model properties.
    ms1Chart.titleProperty().bind(model.ms1TitleProperty());
    ms2Chart.titleProperty().bind(model.ms2TitleProperty());

    // Axis labels (set once, the renderers handle the rest).
    final NumberFormats fmt = ConfigService.getGuiFormats();
    // decision: use the configured unit format (round-bracket / square-bracket / divide) so the
    // axis label respects the user's preferences instead of hardcoding "(min)".
    eicPlot.setDomainAxisLabel(fmt.unit("Retention time", "min"));
    eicPlot.setRangeAxisLabel("Intensity");
    // Show the short ion label on the apex of each EIC trace; the long label is the tooltip.
    eicPlot.setShowSeriesLabel(true);
    // Mobilogram axis defaults; the domain label is overridden per-IMS-file by the task to honour
    // the actual mobility type (TIMS vs DTIMS etc.).
    mobilogramPlot.setDomainAxisLabel("Mobility");
    mobilogramPlot.setRangeAxisLabel("Intensity");
    mobilogramPlot.setShowSeriesLabel(true);
    ms1Chart.rangeAxisLabelProperty().set("Intensity (MS1)");
    ms2Chart.rangeAxisLabelProperty().set("Intensity (MS2)");
    // Fallback full-MS1 spectrum shown when the selected row has no isotope pattern.
    isotopeSpectrumChart.rangeAxisLabelProperty().set("Intensity (MS1)");
    isotopeSpectrumChart.domainAxisLabelProperty().set("m/z");
    isotopeSpectrumChart.setLegendItemsVisible(false);
  }

  // --- FxController overrides -----------------------------------------------

  @Override
  protected @NotNull FxViewBuilder<CompoundDashboardModel> getViewBuilder() {
    return builder;
  }

  @Override
  protected @Nullable FxInteractor<CompoundDashboardModel> getInteractor() {
    return interactor;
  }

  @Override
  public void close() {
    super.close();
    qualityCtrl.close();
    tableCtrl.close();
    featurePlot4D.close();
    isotopeSpectrumChart.close();
  }

  // --- public API ------------------------------------------------------------

  public void setSelectedCompoundRow(@Nullable final CompoundRow row) {
    onGuiThread(() -> model.setSelectedCompoundRow(row));
  }

  public void setFeatureList(@Nullable final ModularFeatureList flist) {
    onGuiThread(() -> model.setFeatureList(flist));
  }

  public @Nullable ModularFeatureList getFeatureList() {
    return model.getFeatureList();
  }

  /**
   * Cycle to the next available raw data file.
   */
  public void nextRawDataFile() {
    cycleRawDataFile(+1);
  }

  /**
   * Cycle to the previous available raw data file.
   */
  public void previousRawDataFile() {
    cycleRawDataFile(-1);
  }

  /**
   * Cycle to the next MS2 scan in {@link CompoundDashboardModel#getAvailableMs2Scans()}.
   */
  public void nextMs2Scan() {
    cycleMs2Scan(+1);
  }

  /**
   * Cycle to the previous MS2 scan in {@link CompoundDashboardModel#getAvailableMs2Scans()}.
   */
  public void previousMs2Scan() {
    cycleMs2Scan(-1);
  }

  /**
   * Cycle to the next charge-state pattern in
   * {@link CompoundDashboardModel#getIsotopeChargeStates()}.
   */
  public void nextChargeState() {
    cycleChargeState(+1);
  }

  /**
   * Cycle to the previous charge-state pattern in
   * {@link CompoundDashboardModel#getIsotopeChargeStates()}.
   */
  public void previousChargeState() {
    cycleChargeState(-1);
  }

  // --- bindings --------------------------------------------------------------

  @Override
  public ObjectProperty<@Nullable CompoundRow> selectedCompoundRowProperty() {
    return model.selectedCompoundRowProperty();
  }

  @Override
  public Property<List<FeatureList>> selectedFeatureListsProperty() {
    return model.selectedFeatureListsProperty();
  }

  // --- internals -------------------------------------------------------------

  private void cycleRawDataFile(final int delta) {
    final ObservableList<RawDataFile> files = model.getAvailableRawDataFiles();
    if (files.isEmpty()) {
      return;
    }
    final RawDataFile current = model.getCurrentRawDataFile();
    final int idx = current == null ? -1 : files.indexOf(current);
    final int next = ((idx + delta) % files.size() + files.size()) % files.size();
    model.setCurrentRawDataFile(files.get(next));
  }

  /**
   * Dispatch a {@link QualityCheckEvent} from the quality pane. Exhaustive switch over the sealed
   * permits so adding a new event type forces a compile error here.
   */
  private void handleQualityCheckEvent(@NotNull final QualityCheckEvent event) {
    switch (event) {
      case FragmentEnergyMethodSelectedEvent e ->
          selectMatchingFragmentScan(e.row(), e.energy(), e.method());
      case AnnotationDetailRequestedEvent e -> openIimnNetworkFor(e.row());
      // Promote the structure's source member row to the dashboard's selected adduct row so the
      // EIC / MS1 / structure preview all retarget; the bidirectional binding to the quality
      // pane's selectedMemberRow keeps every chip / row highlight in sync.
      case AnnotationStructureSelectedEvent e -> model.setSelectedAdductRow(e.row());
      // Route AnnotationSummaryChart double-clicks to the same detail-tab logic the feature table
      // uses for its summary column. The annotation's DataType drives which tab opens (spectral
      // library / compound DB / lipid QC).
      case AnnotationSummaryActivated e ->
          activateAnnotationSummary(e.row(), e.annotationSummary());
    }
  }

  /// Open the detail tab for the supplied {@link AnnotationSummary} by delegating to
  /// {@link AnnotationSummaryType#getDoubleClickAction}. No-op when the row is not modular, the
  /// summary has no inner annotation, or the type returns no action for this annotation type.
  private void activateAnnotationSummary(@NotNull final FeatureListRow row,
      @NotNull final AnnotationSummary summary) {
    if (!(row instanceof ModularFeatureListRow modRow)) {
      return;
    }
    final FeatureAnnotation ann = summary.annotation();
    if (ann == null) {
      return;
    }
    final AnnotationSummaryType type = DataTypes.get(AnnotationSummaryType.class);
    final DataType<?> superType = DataTypes.get(ann.getDataType());
    final Runnable action = type.getDoubleClickAction(tableCtrl.getFeatureTable(), modRow,
        List.of(), superType, summary);
    if (action != null) {
      action.run();
    }
  }

  /**
   * Open the IIMN {@link NetworkOverviewWindow} for the current feature list, centered on
   * {@code row}. No-op when the current feature list is absent — the request would have no graph to
   * render.
   */
  private void openIimnNetworkFor(@NotNull final FeatureListRow row) {
    final ModularFeatureList flist = model.getFeatureList();
    if (flist == null) {
      return;
    }
    final FeatureTableFX table = tableCtrl.getFeatureTable();
    final NetworkOverviewWindow networks = new NetworkOverviewWindow(flist, table, List.of(row),
        NetworkOverviewFlavor.IIMN);
    networks.show();
  }

  /**
   * Pick the scan on {@code row} whose activation method matches and whose energy is closest to
   * {@code energy} (the chip the user clicked). The chip energy is the average of an entire cluster
   * of near-identical scan energies, so an exact equality match is rarely available — we pick the
   * nearest one within the matching method. When the chip's energy is {@code null} (scans without a
   * reported energy), we match only on method and prefer scans that also have {@code null} energy.
   * <p>
   * The {@code selectedAdductRow} update sent by the accompanying {@code onMemberRowClick} has
   * already populated {@code availableMs2Scans} (synchronously on the FX thread), so by the time we
   * run here the candidate scans are reachable via the row itself.
   */
  private void selectMatchingFragmentScan(@NotNull final FeatureListRow row,
      @Nullable final Float energy, @NotNull final ActivationMethod method) {
    Scan best = null;
    double bestDelta = Double.POSITIVE_INFINITY;
    for (final Scan scan : row.getAllFragmentScans()) {
      if (extractActivationMethod(scan) != method) {
        continue;
      }
      final Float scanEnergy = ScanUtils.extractCollisionEnergy(scan);
      final double delta;
      if (energy == null) {
        // Chip carries no energy; prefer scans that also have no energy, otherwise treat them all
        // as equal candidates (first-method-match wins).
        delta = scanEnergy == null ? 0d : Double.POSITIVE_INFINITY;
      } else {
        // Chip has an energy; null-energy scans on this method are unsuitable matches.
        delta = scanEnergy == null ? Double.POSITIVE_INFINITY : Math.abs(scanEnergy - energy);
      }
      if (delta < bestDelta) {
        best = scan;
        bestDelta = delta;
        if (delta == 0d) {
          break; // can't get closer than exact match
        }
      }
    }
    if (best != null) {
      model.setSelectedMs2Scan(best);
    }
  }

  /// Same fallback-to-UNKNOWN semantics as {@code Ms2AvailableCheck.extractActivationMethod}, kept
  /// in sync so a chip emitted there resolves to the same scan here.
  private static @NotNull ActivationMethod extractActivationMethod(@NotNull final Scan scan) {
    final MsMsInfo info = scan.getMsMsInfo();
    if (info == null) {
      return ActivationMethod.UNKNOWN;
    }
    return Objects.requireNonNullElse(info.getActivationMethod(), ActivationMethod.UNKNOWN);
  }

  private void cycleMs2Scan(final int delta) {
    final ObservableList<Scan> scans = model.getAvailableMs2Scans();
    if (scans.isEmpty()) {
      return;
    }
    final Scan current = model.getSelectedMs2Scan();
    final int idx = current == null ? -1 : scans.indexOf(current);
    // for both directions
    final int next = ((idx + delta) % scans.size() + scans.size()) % scans.size();
    model.setSelectedMs2Scan(scans.get(next));
  }

  private void cycleChargeState(final int delta) {
    final ObservableList<IsotopePattern> patterns = model.getIsotopeChargeStates();
    if (patterns.isEmpty()) {
      return;
    }
    final IsotopePattern current = model.getSelectedIsotopePattern();
    final int idx = current == null ? -1 : patterns.indexOf(current);
    // wrap in both directions
    final int next = ((idx + delta) % patterns.size() + patterns.size()) % patterns.size();
    model.setSelectedIsotopePattern(patterns.get(next));
  }

  private void scheduleSpectra() {
    final FxUpdateTask<CompoundDashboardModel> task = interactor.buildSpectraTask();
    if (task == null) {
      return;
    }
    model.setComputing(true);
    onTaskThread(task);
  }

  private void scheduleEic() {
    final FxUpdateTask<CompoundDashboardModel> task = interactor.buildEicTask();
    if (task == null) {
      return;
    }
    onTaskThread(task);
  }

  private void scheduleMobilogram() {
    final FxUpdateTask<CompoundDashboardModel> task = interactor.buildMobilogramTask();
    if (task == null) {
      return;
    }
    onTaskThread(task);
  }

  private static void applyDatasets(@NotNull final ChromatogramPlotController c,
      @NotNull final ObservableList<XYDatasetAndRenderer> list) {
    c.applyWithNotifyChanges(false, () -> {
      c.setDatasets(list);
      ChartLogicsFX.autoAxes(c.getChart());
    });
  }

  /**
   * Mirror {@code selectedAdductRow} into {@code selectedMs2Row} (the adduct ComboBox value and
   * MS2 spectra task source). The MS2 chart can only render members that carry an MS2 scan
   * (== {@link CompoundDashboardModel#getAdductRows()}); for any other row we surface {@code null}
   * so the ComboBox blanks out and the MS2 chart shows the "no MS2" placeholder.
   */
  private void updateSelectedMs2Row() {
    final FeatureListRow adduct = model.getSelectedAdductRow();
    final FeatureListRow target =
        (adduct != null && model.getAdductRows().contains(adduct)) ? adduct : null;
    if (model.getSelectedMs2Row() != target) {
      model.setSelectedMs2Row(target);
    }
  }

  /**
   * Inverse of {@link #updateSelectedMs2Row()}: when {@code selectedMs2Row} is set to a non-null
   * row (typically via the adduct ComboBox), mirror it back into {@code selectedAdductRow} so the
   * legend bold + EIC/MS1 highlight follow. When {@code selectedMs2Row} is null we deliberately do
   * nothing — that null is the derived consequence of a non-MS2 selection and clobbering
   * selectedAdductRow would undo the user's pick.
   */
  private void updateSelectedAdductRowFromMs2() {
    final FeatureListRow ms2 = model.getSelectedMs2Row();
    if (ms2 != null && model.getSelectedAdductRow() != ms2) {
      model.setSelectedAdductRow(ms2);
    }
  }

  /**
   * Reflect {@link CompoundDashboardModel#selectedAdductRowProperty()} into the chromatogram plots
   * (via their {@code selectedDataset}) and into the MS1 bar widths. The EIC line-width
   * highlighting is performed by the chromatogram plot itself once {@code selectedDataset} is set;
   * the MS1 bar handling stays here because no equivalent abstraction exists for the spectra
   * chart.
   */
  private void applySelectionHighlight() {
    final FeatureListRow selected = model.getSelectedAdductRow();
    syncingSelection = true;
    try {
      final XYDataset eicDs = selected == null ? null : model.getEicDatasetsByRow().get(selected);
      eicPlot.setSelectedDataset(eicDs);
      final XYDataset mobDs =
          selected == null ? null : model.getMobilogramDatasetsByRow().get(selected);
      mobilogramPlot.setSelectedDataset(mobDs);
    } finally {
      syncingSelection = false;
    }
    for (final Map.Entry<FeatureListRow, ColoredXYBarRenderer> e : model.getMs1RenderersByRow()
        .entrySet()) {
      e.getValue().setBarWidthMultiplier(
          e.getKey() == selected ? MS1_SELECTED_BAR_MULTIPLIER : MS1_DEFAULT_BAR_MULTIPLIER);
    }
  }

  /**
   * Inverse of {@link #applySelectionHighlight()}: when a plot's selectedDataset changes (legend
   * click), find the owning row and set it as the selected adduct row. Skips when the change was
   * just initiated by us to avoid a feedback loop.
   */
  private void bridgeDatasetToRow(@Nullable final XYDataset dataset,
      @NotNull final Map<FeatureListRow, XYDataset> datasetsByRow) {
    if (syncingSelection) {
      return;
    }
    if (dataset == null) {
      // Legend deselect -> clear adduct row only when there is one and the user really meant to
      // deselect; clearing it would lose context, so prefer to leave selectedAdductRow alone.
      return;
    }
    for (final Map.Entry<FeatureListRow, XYDataset> e : datasetsByRow.entrySet()) {
      if (e.getValue() == dataset) {
        final FeatureListRow row = e.getKey();
        if (model.getSelectedAdductRow() != row) {
          model.setSelectedAdductRow(row);
        }
        return;
      }
    }
  }

  /**
   * Resolve the dataset carried by the plot cursor position to its owning row. The cursor position
   * is updated by the plot's built-in click handling, so this bridges chart clicks on dataset
   * elements to dashboard adduct-row selection.
   */
  private void bridgeCursorPositionToRow(@Nullable final PlotCursorPosition position,
      @NotNull final Map<FeatureListRow, XYDataset> datasetsByRow) {
    if (position == null) {
      return;
    }
    bridgeDatasetToRow(position.getDataset(), datasetsByRow);
  }

  private static void applyDatasets(@NotNull final SimpleSpectraChartController c,
      @NotNull final ObservableList<XYDatasetAndRenderer> list) {
    c.setDatasets(list);
    ChartLogicsFX.autoAxes(c.getChart());
  }

  /**
   * Select the first available row in the feature table once it is populated. FeatureTableFX
   * populates rows asynchronously via {@code FxThread.runLater}, so when the filtered list is empty
   * at the moment of the call, a one-shot listener finishes the work as soon as rows arrive. Skips
   * when the user already selected a compound, so subsequent feature-list reloads don't override
   * the user's choice.
   */
  private void selectFirstRowWhenAvailable() {
    final FeatureTableFX table = tableCtrl.getFeatureTable();
    final ObservableList<TreeItem<ModularFeatureListRow>> rows = table.getFilteredRowItems();
    if (!rows.isEmpty()) {
      maybeSelectFirstRow();
      return;
    }
    rows.addListener(new ListChangeListener<TreeItem<ModularFeatureListRow>>() {
      @Override
      public void onChanged(final Change<? extends TreeItem<ModularFeatureListRow>> c) {
        if (!rows.isEmpty()) {
          rows.removeListener(this);
          maybeSelectFirstRow();
        }
      }
    });
  }

  private void maybeSelectFirstRow() {
    if (model.getSelectedCompoundRow() != null) {
      return;
    }
    tableCtrl.getFeatureTable().getSelectionModel().select(0);
  }

  /**
   * Resolve a clicked feature-list row to its parent {@link CompoundRow}. Mirrors the helper in
   * {@code StatsDashboardController}: prefers the parent compound when the row is a member; returns
   * the row itself if it is already a compound; otherwise null.
   */
  private static @Nullable CompoundRow resolveCompoundRow(@Nullable final FeatureListRow row) {
    if (row == null) {
      return null;
    }
    final CompoundList compoundList =
        row.getFeatureList() == null ? null : row.getFeatureList().getCompoundList();
    if (compoundList != null) {
      final List<ModularCompoundRow> owners = compoundList.findCompoundsOf(row);
      if (!owners.isEmpty()) {
        return owners.getFirst();
      }
    }
    return row instanceof CompoundRow cr ? cr : null;
  }

  // --- exposed sub-controllers (for the ViewBuilder; package-private) -------

  @SuppressWarnings("unused")
  ChromatogramPlotController getEicPlot() {
    return eicPlot;
  }

  @SuppressWarnings("unused")
  ChromatogramPlotController getMobilogramPlot() {
    return mobilogramPlot;
  }

  @SuppressWarnings("unused")
  SimpleSpectraChartController getMs1Chart() {
    return ms1Chart;
  }

  @SuppressWarnings("unused")
  SimpleSpectraChartController getMs2Chart() {
    return ms2Chart;
  }

  @SuppressWarnings("unused")
  CompoundRowQualityController getQualityController() {
    return qualityCtrl;
  }

  @SuppressWarnings("unused")
  FxFeatureTableController getTableController() {
    return tableCtrl;
  }

  /**
   * Used by the ViewBuilder to listen for TreeItem selection. Tree items wrap
   * {@link FeatureListRow} (which {@link ModularCompoundRow} implements).
   */
  @SuppressWarnings("unused")
  public TreeItem<? extends FeatureListRow> getSelectedTableItem() {
    return tableCtrl.getFeatureTable().getSelectionModel().getSelectedItem();
  }
}
