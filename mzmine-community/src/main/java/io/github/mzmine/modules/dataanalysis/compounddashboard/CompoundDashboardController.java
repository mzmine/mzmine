package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
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
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableOwner;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableController;
import io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot.ChromatogramPlotController;
import io.github.mzmine.modules.visualization.spectra.simplespectrachart.SimpleSpectraChartController;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.util.List;
import java.util.Map;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Top-level controller for the Compound Dashboard. Bundles a {@link ChromatogramPlotController}, two
 * {@link SimpleSpectraChartController} (MS1 + MS2), the existing {@link CompoundRowQualityController}
 * and an {@link FxFeatureTableController}, and orchestrates dataset rebuilds when the selected
 * compound, raw file, or adduct change.
 */
public class CompoundDashboardController extends FxController<CompoundDashboardModel>
    implements SelectedCompoundRowBinding, SelectedFeatureListsBinding {

  // Coalesces rapid selection changes (arrow-key navigation) into a single recompute.
  private static final Duration DEBOUNCE = Duration.millis(150);

  // Selected-row highlight: thicker EIC line + wider MS1 sticks for the renderer owned by the
  // currently selected adduct row. Strokes are reused (not allocated per repaint).
  private static final Stroke EIC_DEFAULT_STROKE = new BasicStroke(1.0f);
  private static final Stroke EIC_SELECTED_STROKE = new BasicStroke(2.5f);
  private static final double MS1_DEFAULT_BAR_MULTIPLIER = 1.0;
  private static final double MS1_SELECTED_BAR_MULTIPLIER = 2.0;

  private final ChromatogramPlotController eicPlot = new ChromatogramPlotController(true);
  private final SimpleSpectraChartController ms1Chart = new SimpleSpectraChartController();
  private final SimpleSpectraChartController ms2Chart = new SimpleSpectraChartController();
  private final CompoundRowQualityController qualityCtrl = new CompoundRowQualityController();
  private final FxFeatureTableController tableCtrl = new FxFeatureTableController(FeatureTableOwner.COMPOUND_DASHBOARD);

  private final CompoundDashboardInteractor interactor;
  private final CompoundDashboardViewBuilder builder;

  public CompoundDashboardController() {
    super(new CompoundDashboardModel());
    this.interactor = new CompoundDashboardInteractor(model);
    this.builder = new CompoundDashboardViewBuilder(model, this, eicPlot, ms1Chart, ms2Chart,
        qualityCtrl, tableCtrl);

    model.setColorPalette(CompoundDashboardInteractor.snapshotPalette());

    // Quality pane: bind selectedCompoundRow + selectedFeatureLists in both directions.
    FxControllerBinding.bindExposedProperties(this, qualityCtrl);

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
    tableCtrl.getFeatureTable().getSelectionModel().selectedItemProperty()
        .addListener((_, _, item) -> {
          final CompoundRow resolved = resolveCompoundRow(
              item == null ? null : item.getValue());
          if (resolved != model.getSelectedCompoundRow()) {
            model.setSelectedCompoundRow(resolved);
          }
        });

    // Cheap, FX-thread refresh of derived state when compound or feature list changes.
    PropertyUtils.onChange(interactor::onSelectionChanged, model.selectedCompoundRowProperty(),
        model.featureListProperty());

    // Heavy debounced recompute of MS1+MS2. The MS2 source is `selectedMs2Row` (driven by the
    // adduct ComboBox); `selectedAdductRow` is derived state and would just echo this trigger.
    PropertyUtils.onChangeDelayedSubscription(this::scheduleSpectra, DEBOUNCE,
        model.selectedCompoundRowProperty(), model.selectedMs2RowProperty(),
        model.colorPaletteProperty(), model.currentRawDataFileProperty());
    // Heavy debounced recompute of EICs.
    PropertyUtils.onChangeDelayedSubscription(this::scheduleEic, DEBOUNCE,
        model.selectedCompoundRowProperty(), model.currentRawDataFileProperty(),
        model.colorPaletteProperty());

    // Apply prebuilt datasets to the sub-controllers when the model lists change. The lists are
    // mutated by FxUpdateTask.updateGuiModel which already runs on the FX thread.
    PropertyUtils.onChangeList(() -> applyDatasets(eicPlot, model.getEicDatasets()),
        model.getEicDatasets());
    PropertyUtils.onChangeList(() -> applyDatasets(ms1Chart, model.getMs1Datasets()),
        model.getMs1Datasets());
    PropertyUtils.onChangeList(() -> applyDatasets(ms2Chart, model.getMs2Datasets()),
        model.getMs2Datasets());

    // Derive selectedAdductRow from the MS2 ComboBox selection: when the user has picked an MS2
    // row, highlight it; when no MS2 is selected (e.g. compound has no MS2-bearing member), fall
    // back to the preferred row so the EIC/MS1 highlight always has a target.
    PropertyUtils.onChange(this::updateSelectedAdductRow, model.selectedMs2RowProperty(),
        model.selectedCompoundRowProperty());

    // Re-apply the selected-row highlight whenever the selection changes or fresh datasets land.
    // The renderer maps are updated in the task's updateGuiModel before the dataset list setAll,
    // so by the time these listeners fire the maps already reflect the new datasets.
    model.selectedAdductRowProperty().subscribe(_ -> applySelectionHighlight());
    model.getEicDatasets().addListener(
        (ListChangeListener<DatasetAndRenderer>) _ -> applySelectionHighlight());
    model.getMs1Datasets().addListener(
        (ListChangeListener<DatasetAndRenderer>) _ -> applySelectionHighlight());

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
    ms1Chart.rangeAxisLabelProperty().set("Intensity (MS1)");
    ms2Chart.rangeAxisLabelProperty().set("Intensity (MS2)");
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

  /** Cycle to the next available raw data file. */
  public void nextRawDataFile() {
    cycleRawDataFile(+1);
  }

  /** Cycle to the previous available raw data file. */
  public void previousRawDataFile() {
    cycleRawDataFile(-1);
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

  private static void applyDatasets(@NotNull final ChromatogramPlotController c,
      @NotNull final ObservableList<DatasetAndRenderer> list) {
    c.applyWithNotifyChanges(false, () -> {
      c.clearDatasets();
      for (final DatasetAndRenderer dr : list) {
        c.addDataset(dr.dataset(), dr.renderer());
      }
    });
  }

  /**
   * Recompute {@code selectedAdductRow}: mirror {@code selectedMs2Row} when set, otherwise fall
   * back to the selected compound's preferred row (and null when no compound is selected).
   */
  private void updateSelectedAdductRow() {
    final FeatureListRow ms2 = model.getSelectedMs2Row();
    if (ms2 != null) {
      model.setSelectedAdductRow(ms2);
      return;
    }
    final CompoundRow compound = model.getSelectedCompoundRow();
    model.setSelectedAdductRow(compound == null ? null : compound.getPreferredRow());
  }

  /**
   * Thicken the EIC line and widen the MS1 bars belonging to the currently selected adduct row;
   * reset all other renderers to defaults. The maps are populated by the EIC and spectra tasks
   * in their {@code updateGuiModel} step.
   */
  private void applySelectionHighlight() {
    final FeatureListRow selected = model.getSelectedAdductRow();
    for (final Map.Entry<FeatureListRow, ColoredXYLineRenderer> e :
        model.getEicRenderersByRow().entrySet()) {
      e.getValue().setDefaultStroke(
          e.getKey() == selected ? EIC_SELECTED_STROKE : EIC_DEFAULT_STROKE);
    }
    for (final Map.Entry<FeatureListRow, ColoredXYBarRenderer> e :
        model.getMs1RenderersByRow().entrySet()) {
      e.getValue().setBarWidthMultiplier(
          e.getKey() == selected ? MS1_SELECTED_BAR_MULTIPLIER : MS1_DEFAULT_BAR_MULTIPLIER);
    }
  }

  private static void applyDatasets(@NotNull final SimpleSpectraChartController c,
      @NotNull final ObservableList<DatasetAndRenderer> list) {
    // SimpleSpectraChartController internally batches both calls in applyWithNotifyChanges.
    c.clearDatasets();
    if (list.isEmpty()) {
      return;
    }
    final java.util.LinkedHashMap<org.jfree.data.xy.XYDataset, org.jfree.chart.renderer.xy.XYItemRenderer> map = new java.util.LinkedHashMap<>();
    for (final DatasetAndRenderer dr : list) {
      map.put(dr.dataset(), dr.renderer());
    }
    c.addDatasets(map);
  }

  /**
   * Select the first available row in the feature table once it is populated. FeatureTableFX
   * populates rows asynchronously via {@code FxThread.runLater}, so when the filtered list is
   * empty at the moment of the call, a one-shot listener finishes the work as soon as rows arrive.
   * Skips when the user already selected a compound, so subsequent feature-list reloads don't
   * override the user's choice.
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
   * {@code StatsDashboardController}: prefers the parent compound when the row is a member;
   * returns the row itself if it is already a compound; otherwise null.
   */
  private static @Nullable CompoundRow resolveCompoundRow(@Nullable final FeatureListRow row) {
    if (row == null) {
      return null;
    }
    final CompoundList compoundList = row.getFeatureList() == null ? null
        : row.getFeatureList().getCompoundList();
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
