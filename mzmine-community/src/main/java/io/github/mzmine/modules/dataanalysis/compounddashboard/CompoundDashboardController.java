package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.framework.fx.FxControllerBinding;
import io.github.mzmine.gui.framework.fx.SelectedCompoundRowBinding;
import io.github.mzmine.gui.framework.fx.SelectedFeatureListsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxInteractor;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.CompoundRowQualityController;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FxFeatureTableController;
import io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot.ChromatogramPlotController;
import io.github.mzmine.modules.visualization.spectra.simplespectrachart.SimpleSpectraChartController;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
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

  private final ChromatogramPlotController eicPlot = new ChromatogramPlotController(true);
  private final SimpleSpectraChartController ms1Chart = new SimpleSpectraChartController();
  private final SimpleSpectraChartController ms2Chart = new SimpleSpectraChartController();
  private final CompoundRowQualityController qualityCtrl = new CompoundRowQualityController();
  private final FxFeatureTableController tableCtrl = new FxFeatureTableController();

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

    // Feature table receives the feature list directly.
    model.featureListProperty().subscribe(flist -> tableCtrl.setFeatureList(flist));

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

    // Heavy debounced recompute of MS1+MS2.
    PropertyUtils.onChangeDelayedSubscription(this::scheduleSpectra, DEBOUNCE,
        model.selectedCompoundRowProperty(), model.selectedAdductRowProperty(),
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

    // Axis labels (set once, the renderers handle the rest).
    eicPlot.setDomainAxisLabel("Retention time (min)");
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
