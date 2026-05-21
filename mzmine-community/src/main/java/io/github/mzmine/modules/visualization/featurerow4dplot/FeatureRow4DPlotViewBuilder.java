package io.github.mzmine.modules.visualization.featurerow4dplot;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import java.util.function.Consumer;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;

/**
 * Builds the {@link FeatureRow4DPlotController} view: a single {@link FeatureRow4DPlotChart} inside
 * a {@link BorderPane}, wired to the model's dataset / orientation / selection properties.
 * <p>
 * Event handling lives here: primary-button clicks on a bubble forward the underlying
 * {@link FeatureListRow} to the controller via the {@code onRowClicked} callback; the context menu
 * gets an extra entry for toggling the plot orientation.
 */
public class FeatureRow4DPlotViewBuilder extends FxViewBuilder<FeatureRow4DPlotModel> {

  private final @NotNull Consumer<@NotNull FeatureListRow> onRowClicked;

  protected FeatureRow4DPlotViewBuilder(@NotNull final FeatureRow4DPlotModel model,
      @NotNull final Consumer<@NotNull FeatureListRow> onRowClicked) {
    super(model);
    this.onRowClicked = onRowClicked;
  }

  @Override
  public @NotNull Region build() {
    final FeatureRow4DPlotChart chart = new FeatureRow4DPlotChart();
    chart.setMinWidth(150);
    chart.setMinHeight(150);

    // Dataset binding: model -> chart. Selection overlay is reset by applyData itself, so we then
    // re-apply the currently selected rows to keep the highlight after a feature-list swap.
    model.datasetProperty().subscribe(dataset -> {
      if (dataset == null) {
        chart.clearData();
      } else {
        chart.applyData(dataset);
        chart.setSelectedRows(model.getSelectedRows());
      }
    });

    model.plotOrientationProperty().subscribe(chart::setOrientation);
    model.selectedRowsProperty()
        .subscribe(rows -> chart.setSelectedRows(rows == null ? java.util.List.of() : rows));

    // Click handling: resolve to the source FeatureListRow via XYItemObjectProvider and forward.
    chart.addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(final ChartMouseEventFX event) {
        if (event.getTrigger() == null || event.getTrigger().getButton() != MouseButton.PRIMARY
            || !event.getTrigger().isStillSincePress()) {
          return;
        }
        if (!(event.getEntity() instanceof XYItemEntity entity)) {
          return;
        }
        final FeatureListRow row = resolveRow(entity.getDataset(), entity.getItem());
        if (row != null) {
          onRowClicked.accept(row);
        }
      }

      @Override
      public void chartMouseMoved(final ChartMouseEventFX event) {
      }
    });

    addOrientationMenu(chart);

    return new BorderPane(chart);
  }

  private void addOrientationMenu(@NotNull final FeatureRow4DPlotChart chart) {
    final MenuItem toggle = new MenuItem();
    toggle.setOnAction(_ -> {
      final PlotOrientation next =
          model.getPlotOrientation() == PlotOrientation.VERTICAL ? PlotOrientation.HORIZONTAL
              : PlotOrientation.VERTICAL;
      model.setPlotOrientation(next);
    });
    // Update label whenever the menu pops up so it reflects the next action, not the current state.
    chart.getContextMenu().setOnShowing(_ -> toggle.setText(
        model.getPlotOrientation() == PlotOrientation.VERTICAL ? "Rotate plot (RT ↔ m/z)"
            : "Rotate plot back (RT ↔ m/z)"));

    chart.getContextMenu().getItems().add(new SeparatorMenuItem());
    chart.getContextMenu().getItems().add(toggle);
  }

  private static FeatureListRow resolveRow(@NotNull final XYDataset dataset, final int item) {
    if (dataset instanceof FeatureRow4DPlotDataset d) {
      return d.getItemObject(item);
    }
    return null;
  }
}
