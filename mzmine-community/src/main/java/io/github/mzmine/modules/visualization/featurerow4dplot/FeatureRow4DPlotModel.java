package io.github.mzmine.modules.visualization.featurerow4dplot;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.PlotOrientation;

/**
 * Observable state of the {@link FeatureRow4DPlotController}. Holds the input feature list, the
 * currently computed dataset, the cross-controller selection properties and the plot orientation.
 */
public class FeatureRow4DPlotModel {

  private final ObjectProperty<@Nullable ModularFeatureList> featureList = new SimpleObjectProperty<>();

  // Computed snapshot of the rows of {@link #featureList}. Replaced as a whole when the feature
  // list (or any other input that would change the rendered values) changes.
  private final ObjectProperty<@Nullable FeatureRow4DPlotDataset> dataset = new SimpleObjectProperty<>();

  // Selection state shared with the rest of the dashboard via SelectedRowsBinding /
  // SelectedCompoundRowBinding. Defaults to an empty list rather than null so listeners do not need
  // to null-guard.
  private final ObjectProperty<List<FeatureListRow>> selectedRows = new SimpleObjectProperty<>(
      List.of());
  private final ObjectProperty<@Nullable CompoundRow> selectedCompoundRow = new SimpleObjectProperty<>();

  // VERTICAL = m/z on the x-axis, RT on the y-axis (the request's default). HORIZONTAL swaps them
  // visually without rebuilding the dataset.
  private final ObjectProperty<@NotNull PlotOrientation> plotOrientation = new SimpleObjectProperty<>(
      PlotOrientation.HORIZONTAL);

  public @Nullable ModularFeatureList getFeatureList() {
    return featureList.get();
  }

  public void setFeatureList(@Nullable final ModularFeatureList value) {
    featureList.set(value);
  }

  public ObjectProperty<@Nullable ModularFeatureList> featureListProperty() {
    return featureList;
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

  public @Nullable CompoundRow getSelectedCompoundRow() {
    return selectedCompoundRow.get();
  }

  public void setSelectedCompoundRow(@Nullable final CompoundRow value) {
    selectedCompoundRow.set(value);
  }

  public ObjectProperty<@Nullable CompoundRow> selectedCompoundRowProperty() {
    return selectedCompoundRow;
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
