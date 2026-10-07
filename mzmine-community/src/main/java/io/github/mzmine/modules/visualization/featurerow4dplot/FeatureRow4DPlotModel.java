package io.github.mzmine.modules.visualization.featurerow4dplot;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRowSelection;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.PlotOrientation;

/**
 * Observable state of the {@link FeatureRow4DPlotController}. Holds the input feature list(s), the
 * currently computed dataset, the cross-controller selection properties and the plot orientation.
 */
public class FeatureRow4DPlotModel {

  // Exposed as Property<List<FeatureList>> so the controller can implement
  // SelectedFeatureListsBinding and share with the dashboard. Only the first list is rendered —
  // this plot is single-feature-list by design.
  private final ObjectProperty<List<FeatureList>> selectedFeatureLists = new SimpleObjectProperty<>(
      List.of());

  // Which subset of rows the dataset draws when the feature list carries a CompoundList: the
  // compound rows themselves, or all major-ion member rows. {@code ALL_ISOTOPES} is intentionally
  // not offered by the UI for this plot. Default keeps the previous "all rows" behaviour visible
  // when the dashboard initialises the plot.
  private final ObjectProperty<@NotNull CompoundRowSelection> compoundRowSelection = new SimpleObjectProperty<>(
      CompoundRowSelection.ALL_MAJOR_IONS);

  // Computed snapshot of the rows of {@link #selectedFeatureLists} (filtered by
  // {@link #compoundRowSelection}). Replaced as a whole when the feature list, the row selection,
  // or any other input that would change the rendered values changes.
  private final ObjectProperty<@Nullable FeatureRow4DPlotDataset> dataset = new SimpleObjectProperty<>();

  // Selection state shared with the rest of the dashboard via SelectedRowsBinding /
  // SelectedCompoundRowBinding. Defaults to an empty list rather than null so listeners do not need
  // to null-guard.
  private final ObjectProperty<List<FeatureListRow>> selectedRows = new SimpleObjectProperty<>(
      List.of());

  // Rows the chart should outline: a derived view of {@link #selectedRows} where any CompoundRow
  // has been expanded to its member feature rows, since the chart's dataset only contains regular
  // feature rows (compound rows live in CompoundList and are never indexed by FeatureRow4DPlotDataset).
  // The controller is the sole writer.
  private final ObjectProperty<List<FeatureListRow>> chartHighlightRows = new SimpleObjectProperty<>(
      List.of());

  // VERTICAL = m/z on the x-axis, RT on the y-axis (the request's default). HORIZONTAL swaps them
  // visually without rebuilding the dataset.
  private final ObjectProperty<@NotNull PlotOrientation> plotOrientation = new SimpleObjectProperty<>(
      PlotOrientation.HORIZONTAL);

  public @NotNull List<FeatureList> getSelectedFeatureLists() {
    final List<FeatureList> v = selectedFeatureLists.get();
    return v == null ? List.of() : v;
  }

  public void setSelectedFeatureLists(@NotNull final List<FeatureList> value) {
    selectedFeatureLists.set(value);
  }

  public ObjectProperty<List<FeatureList>> selectedFeatureListsProperty() {
    return selectedFeatureLists;
  }

  /**
   * Convenience accessor: the first selected feature list as a {@link ModularFeatureList}, or
   * {@code null} when no list is selected or the first entry is not a modular list.
   */
  public @Nullable ModularFeatureList getFeatureList() {
    final List<FeatureList> lists = getSelectedFeatureLists();
    if (lists.isEmpty()) {
      return null;
    }
    return lists.getFirst() instanceof ModularFeatureList m ? m : null;
  }

  public void setFeatureList(@Nullable final ModularFeatureList value) {
    setSelectedFeatureLists(value == null ? List.of() : List.of(value));
  }

  public @NotNull CompoundRowSelection getCompoundRowSelection() {
    final CompoundRowSelection v = compoundRowSelection.get();
    return v == null ? CompoundRowSelection.ALL_MAJOR_IONS : v;
  }

  public void setCompoundRowSelection(@NotNull final CompoundRowSelection value) {
    compoundRowSelection.set(value);
  }

  public ObjectProperty<@NotNull CompoundRowSelection> compoundRowSelectionProperty() {
    return compoundRowSelection;
  }

  public @Nullable FeatureRow4DPlotDataset getDataset() {
    return dataset.get();
  }

  public void setDataset(@Nullable final FeatureRow4DPlotDataset value) {
    dataset.set(value);
  }

  public ObjectProperty<@Nullable FeatureRow4DPlotDataset> datasetProperty() {
    return dataset;
  }

  public @NotNull List<FeatureListRow> getSelectedRows() {
    final List<FeatureListRow> v = selectedRows.get();
    return v == null ? List.of() : v;
  }

  public void setSelectedRows(@NotNull final List<FeatureListRow> value) {
    selectedRows.set(value);
  }

  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return selectedRows;
  }

  public @NotNull List<FeatureListRow> getChartHighlightRows() {
    final List<FeatureListRow> v = chartHighlightRows.get();
    return v == null ? List.of() : v;
  }

  public void setChartHighlightRows(@NotNull final List<FeatureListRow> value) {
    chartHighlightRows.set(value);
  }

  public ObjectProperty<List<FeatureListRow>> chartHighlightRowsProperty() {
    return chartHighlightRows;
  }

  public @NotNull PlotOrientation getPlotOrientation() {
    return plotOrientation.get();
  }

  public void setPlotOrientation(@NotNull final PlotOrientation value) {
    plotOrientation.set(value);
  }

  public ObjectProperty<@NotNull PlotOrientation> plotOrientationProperty() {
    return plotOrientation;
  }
}
