package io.github.mzmine.modules.visualization.featurerow4dplot;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRowSelection;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.gui.framework.fx.SelectedFeatureListsBinding;
import io.github.mzmine.gui.framework.fx.SelectedRowsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.properties.PropertyUtils;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.PlotOrientation;

/**
 * MVCI controller for the 4D feature plot. Exposes selection bindings so the surrounding dashboard
 * can synchronise the highlighted rows / selected feature list, and owns the (feature-list +
 * compound-row-selection)-driven rebuild of the underlying {@link FeatureRow4DPlotDataset}.
 */
public class FeatureRow4DPlotController extends FxController<FeatureRow4DPlotModel> implements
    SelectedRowsBinding, SelectedFeatureListsBinding {

  private final FeatureRow4DPlotViewBuilder viewBuilder;

  public FeatureRow4DPlotController() {
    super(new FeatureRow4DPlotModel());
    this.viewBuilder = new FeatureRow4DPlotViewBuilder(model, this::setSelectedRowFromClick);

    // Heavy work — building the dataset — runs on a task thread. Either input change reuses the
    // existing dataset until the rebuild task finishes.
    PropertyUtils.onChange(this::scheduleRebuild, model.selectedFeatureListsProperty(),
        model.compoundRowSelectionProperty());

    // Translate the externally-bound selection into the row list the chart's dataset can match by
    // identity. The translation flips with the row-subset mode (CompoundRow ↔ member rows), so we
    // re-run on every input that could change the dataset's row identities — not just selectedRows.
    PropertyUtils.onChange(this::refreshChartHighlight, model.selectedRowsProperty(),
        model.compoundRowSelectionProperty(), model.selectedFeatureListsProperty());
  }

  @Override
  protected @NotNull FxViewBuilder<FeatureRow4DPlotModel> getViewBuilder() {
    return viewBuilder;
  }

  // --- public API ------------------------------------------------------------

  public void setFeatureList(@Nullable final ModularFeatureList featureList) {
    onGuiThread(() -> model.setFeatureList(featureList));
  }

  public void setPlotOrientation(@NotNull final PlotOrientation orientation) {
    onGuiThread(() -> model.setPlotOrientation(orientation));
  }

  public void setCompoundRowSelection(@NotNull final CompoundRowSelection selection) {
    onGuiThread(() -> model.setCompoundRowSelection(selection));
  }

  // --- bindings --------------------------------------------------------------

  @Override
  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return model.selectedRowsProperty();
  }

  @Override
  public Property<List<FeatureList>> selectedFeatureListsProperty() {
    return model.selectedFeatureListsProperty();
  }

  // --- internals -------------------------------------------------------------

  private void setSelectedRowFromClick(@NotNull final FeatureListRow clicked) {
    // In ALL_MAJOR_IONS mode the bubble is a member row, but the dashboard table is compound-
    // centric: route the selection to the parent CompoundRow so the table scrolls to and highlights
    // the parent. Other modes already click the row the dashboard wants to select.
    final FeatureListRow target = toDashboardSelectionTarget(clicked);
    final List<FeatureListRow> current = model.getSelectedRows();
    // Avoid bouncing a redundant write through any bidirectional binding listeners.
    if (current.size() == 1 && current.get(0) == target) {
      return;
    }
    model.setSelectedRows(List.of(target));
  }

  /**
   * In ALL_MAJOR_IONS mode, replace a clicked member row by its first parent {@link CompoundRow} so
   * the dashboard table jumps to the compound rather than the member. In every other case the
   * clicked row already matches what the dashboard wants to select.
   */
  private @NotNull FeatureListRow toDashboardSelectionTarget(
      @NotNull final FeatureListRow clicked) {
    if (model.getCompoundRowSelection() != CompoundRowSelection.ALL_MAJOR_IONS
        || clicked instanceof CompoundRow) {
      return clicked;
    }
    final ModularFeatureList flist = model.getFeatureList();
    if (flist == null) {
      return clicked;
    }
    final CompoundList cl = flist.getCompoundList();
    if (cl == null || cl.isStale()) {
      return clicked;
    }
    final List<ModularCompoundRow> parents = cl.findCompoundsOf(clicked);
    return parents.isEmpty() ? clicked : parents.getFirst();
  }

  private void refreshChartHighlight() {
    model.setChartHighlightRows(translateToDatasetRows(model.getSelectedRows()));
  }

  /**
   * Translate {@code rows} so each entry matches an item the chart's dataset actually contains:
   * <ul>
   *   <li>{@code COMPOUNDS} (dataset holds compound rows): contract member rows to their first
   *       parent {@link CompoundRow}, keep existing compound rows as-is.</li>
   *   <li>{@code ALL_MAJOR_IONS} (dataset holds member rows): expand each {@link CompoundRow} to
   *       its member feature rows, keep regular rows as-is.</li>
   *   <li>No compound list: pass through (the dataset is built from {@code featureList.getRows()}
   *       directly).</li>
   * </ul>
   * Duplicates from the contraction step are dropped using an identity set so the overlay does not
   * render the same bubble twice.
   */
  private @NotNull List<FeatureListRow> translateToDatasetRows(
      @NotNull final List<FeatureListRow> rows) {
    if (rows.isEmpty()) {
      return List.of();
    }
    final ModularFeatureList flist = model.getFeatureList();
    final CompoundList cl = flist == null ? null : flist.getCompoundList();
    if (cl == null || cl.isStale()) {
      // Dataset mirrors featureList.getRows(); any CompoundRow in the input cannot match.
      return rows;
    }
    return switch (model.getCompoundRowSelection()) {
      case COMPOUNDS -> contractToCompoundRows(rows, cl);
      case ALL_MAJOR_IONS, ALL_ISOTOPES -> expandCompoundRows(rows);
    };
  }

  /**
   * Expand any {@link CompoundRow} in {@code rows} to its member feature rows. Non-compound rows
   * pass through unchanged. Used in modes where the dataset stores member rows, not compounds.
   */
  private static @NotNull List<FeatureListRow> expandCompoundRows(
      @NotNull final List<FeatureListRow> rows) {
    boolean anyCompound = false;
    for (final FeatureListRow row : rows) {
      if (row instanceof CompoundRow) {
        anyCompound = true;
        break;
      }
    }
    if (!anyCompound) {
      return rows;
    }
    final List<FeatureListRow> expanded = new ArrayList<>(rows.size());
    for (final FeatureListRow row : rows) {
      if (row instanceof CompoundRow compound) {
        expanded.addAll(compound.getMemberRows());
      } else {
        expanded.add(row);
      }
    }
    return List.copyOf(expanded);
  }

  /**
   * Contract each member row in {@code rows} to its first parent {@link CompoundRow}; rows already
   * being compound rows pass through. Members without a parent (or rows outside the compound list)
   * are dropped because no matching bubble exists in COMPOUNDS-mode datasets.
   */
  private static @NotNull List<FeatureListRow> contractToCompoundRows(
      @NotNull final List<FeatureListRow> rows, @NotNull final CompoundList cl) {
    final List<FeatureListRow> result = new ArrayList<>(rows.size());
    // Identity-based dedup so two member rows of the same compound don't add it twice.
    final IdentityHashMap<FeatureListRow, Boolean> seen = new IdentityHashMap<>();
    for (final FeatureListRow row : rows) {
      final FeatureListRow target;
      if (row instanceof CompoundRow) {
        target = row;
      } else {
        final List<ModularCompoundRow> parents = cl.findCompoundsOf(row);
        target = parents.isEmpty() ? null : parents.getFirst();
      }
      if (target != null && seen.put(target, Boolean.TRUE) == null) {
        result.add(target);
      }
    }
    return List.copyOf(result);
  }

  private void scheduleRebuild() {
    final ModularFeatureList flist = model.getFeatureList();
    if (flist == null) {
      onGuiThread(() -> model.setDataset(null));
      return;
    }
    onTaskThread(new RebuildDatasetTask(model, flist, model.getCompoundRowSelection()));
  }

  private static final class RebuildDatasetTask extends FxUpdateTask<FeatureRow4DPlotModel> {

    private final @NotNull ModularFeatureList sourceFlist;
    private final @NotNull CompoundRowSelection selection;
    private @Nullable FeatureRow4DPlotDataset built;

    private RebuildDatasetTask(@NotNull final FeatureRow4DPlotModel model,
        @NotNull final ModularFeatureList sourceFlist,
        @NotNull final CompoundRowSelection selection) {
      super("Build 4D feature plot dataset", model);
      this.sourceFlist = sourceFlist;
      this.selection = selection;
    }

    @Override
    public String getTaskDescription() {
      return "Computing 4D feature plot dataset";
    }

    @Override
    public double getFinishedPercentage() {
      return built == null ? 0.0 : 1.0;
    }

    @Override
    protected void process() {
      // decision: when the feature list carries a CompoundList, honour the selection (compounds vs.
      // all major ions); otherwise fall back to the flat row list. ALL_ISOTOPES is reachable in
      // principle but the view restricts the ComboBox so users cannot pick it.
      final CompoundList cl = sourceFlist.getCompoundList();
      final List<? extends FeatureListRow> rows =
          (cl != null && !cl.isStale()) ? cl.getRowsCopy(selection) : sourceFlist.getRows();
      built = new FeatureRow4DPlotDataset(sourceFlist, rows);
    }

    @Override
    protected void updateGuiModel() {
      // Stale-task guard: only apply our result when the feature list still matches what the model
      // currently points at. A newer featureList swap would already have scheduled its own task.
      if (model.getFeatureList() != sourceFlist) {
        return;
      }
      model.setDataset(built);
    }
  }
}
