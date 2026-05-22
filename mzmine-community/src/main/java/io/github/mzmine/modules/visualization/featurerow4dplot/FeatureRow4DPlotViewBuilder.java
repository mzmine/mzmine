package io.github.mzmine.modules.visualization.featurerow4dplot;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRowSelection;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import java.util.List;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;

/**
 * Builds the {@link FeatureRow4DPlotController} view: a {@link FeatureRow4DPlotChart} in the centre
 * of a {@link BorderPane} with an "Options" {@link Accordion} pane at the south, wired to the
 * model's dataset / orientation / selection / compound-row-selection properties.
 * <p>
 * Event handling lives here: primary-button clicks on a bubble forward the underlying
 * {@link FeatureListRow} to the controller via the {@code onRowClicked} callback; the context menu
 * gets an extra entry for toggling the plot orientation; the options ComboBox drives the row-subset
 * the dataset is rebuilt from.
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
    // re-apply the currently highlighted rows to keep the overlay after a feature-list swap.
    model.datasetProperty().subscribe(dataset -> {
      if (dataset == null) {
        chart.clearData();
      } else {
        chart.applyData(dataset);
        chart.setSelectedRows(model.getChartHighlightRows());
      }
    });

    model.plotOrientationProperty().subscribe(chart::setOrientation);
    // Subscribe to chartHighlightRows (CompoundRow-expanded view of selectedRows) so an external
    // selection — e.g. the dashboard auto-selecting the first compound row — still draws an overlay.
    model.chartHighlightRowsProperty()
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

    final BorderPane root = new BorderPane(chart);
    root.setBottom(buildOptionsAccordion());
    return root;
  }

  /**
   * Builds the south Accordion holding a single "Options" {@link TitledPane}. The TitledPane hosts
   * a ComboBox that drives {@link FeatureRow4DPlotModel#compoundRowSelectionProperty()}; its
   * available items are re-filtered whenever the selected feature list changes (see
   * {@link #refreshComboItems}).
   */
  private @NotNull Accordion buildOptionsAccordion() {
    // Single observable list set on the ComboBox once so any later setAll keeps existing bindings
    // and listeners intact (per project guideline on observable list usage).
    final ObservableList<CompoundRowSelection> items = FXCollections.observableArrayList();

    final ComboBox<CompoundRowSelection> combo = FxComboBox.createComboBox(
        "Which feature-list rows to plot", items, model.compoundRowSelectionProperty());
    // Make the combo not user-editable as a text-field. The disable state is driven separately by
    // whether the feature list carries a compound list (only ALL_MAJOR_IONS is available then).
    combo.setEditable(false);

    final Label label = FxLabels.newLabel("Rows:");
    final HBox row = FxLayout.newHBox(Pos.CENTER_LEFT, label, combo);

    // Keep items and current selection in sync with the selected feature list. Driven by a single
    // subscriber so the listener block stays inside the View (per MVCI guideline).
    model.selectedFeatureListsProperty().subscribe(lists -> refreshComboItems(items, combo, lists));

    final TitledPane options = FxLayout.newTitledPane("Options", row);
    // expandFirst=false so the panel opens collapsed; the user expands it when they want to change
    // the row subset. This keeps the south strip short by default.
    return FxLayout.newAccordion(false, options);
  }

  /**
   * Rebuild the ComboBox items based on the first selected feature list. {@code ALL_MAJOR_IONS} is
   * always offered. {@code COMPOUNDS} only when the list carries a non-stale {@code CompoundList}.
   * {@code ALL_ISOTOPES} is intentionally never offered here. When no compound list is available,
   * the combo collapses to a single option, snaps the model selection to {@code ALL_MAJOR_IONS},
   * and disables the control so the user can see the only available choice without being able to
   * pick something nonsensical.
   */
  private void refreshComboItems(@NotNull final ObservableList<CompoundRowSelection> items,
      @NotNull final ComboBox<CompoundRowSelection> combo, @NotNull final List<FeatureList> lists) {
    final boolean hasCompounds =
        !lists.isEmpty() && lists.getFirst() != null && lists.getFirst().hasCompoundList();
    if (hasCompounds) {
      // decision: list COMPOUNDS first so it acts as the "headline" option matching the dashboard's
      // initial CompoundRowSelection (set by CompoundDashboardController).
      items.setAll(CompoundRowSelection.COMPOUNDS, CompoundRowSelection.ALL_MAJOR_IONS);
      combo.setDisable(false);
    } else {
      items.setAll(CompoundRowSelection.ALL_MAJOR_IONS);
      // Snap to the only available choice; this also triggers the dataset rebuild via the
      // controller's onChange listener on compoundRowSelectionProperty.
      if (model.getCompoundRowSelection() != CompoundRowSelection.ALL_MAJOR_IONS) {
        model.setCompoundRowSelection(CompoundRowSelection.ALL_MAJOR_IONS);
      }
      combo.setDisable(true);
    }
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
