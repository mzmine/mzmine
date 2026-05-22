package io.github.mzmine.modules.visualization.featurerow4dplot;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.framework.fx.SelectedRowsBinding;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.PlotOrientation;

/**
 * MVCI controller for the 4D feature plot. Exposes selection bindings so the surrounding dashboard
 * can synchronise the highlighted rows / compound row, and owns the feature-list-driven rebuild of
 * the underlying {@link FeatureRow4DPlotDataset}.
 */
public class FeatureRow4DPlotController extends FxController<FeatureRow4DPlotModel> implements
    SelectedRowsBinding {

  private final FeatureRow4DPlotViewBuilder viewBuilder;

  public FeatureRow4DPlotController() {
    super(new FeatureRow4DPlotModel());
    this.viewBuilder = new FeatureRow4DPlotViewBuilder(model, this::setSelectedRowFromClick);

    // Heavy work — building the dataset — runs on a task thread. The featureList property is the
    // single trigger; any other change (orientation, selection) reuses the cached dataset.
    model.featureListProperty().subscribe(this::scheduleRebuild);
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

  // --- bindings --------------------------------------------------------------

  @Override
  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return model.selectedRowsProperty();
  }

  // --- internals -------------------------------------------------------------

  private void setSelectedRowFromClick(@NotNull final FeatureListRow clicked) {
    final List<FeatureListRow> current = model.getSelectedRows();
    // Avoid bouncing a redundant write through any bidirectional binding listeners.
    if (current.size() == 1 && current.get(0) == clicked) {
      return;
    }
    model.setSelectedRows(List.of(clicked));
  }

  private void scheduleRebuild(@Nullable final ModularFeatureList flist) {
    if (flist == null) {
      onGuiThread(() -> model.setDataset(null));
      return;
    }
    onTaskThread(new RebuildDatasetTask(model, flist));
  }

  private static final class RebuildDatasetTask extends FxUpdateTask<FeatureRow4DPlotModel> {

    private final @NotNull ModularFeatureList sourceFlist;
    private @Nullable FeatureRow4DPlotDataset built;

    private RebuildDatasetTask(@NotNull final FeatureRow4DPlotModel model,
        @NotNull final ModularFeatureList sourceFlist) {
      super("Build 4D feature plot dataset", model);
      this.sourceFlist = sourceFlist;
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
      built = new FeatureRow4DPlotDataset(sourceFlist);
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
