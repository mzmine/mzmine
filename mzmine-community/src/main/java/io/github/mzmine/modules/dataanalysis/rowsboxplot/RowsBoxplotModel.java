/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataanalysis.rowsboxplot;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.Nullable;

public class RowsBoxplotModel {

  private final ObjectProperty<List<FeatureListRow>> selectedRows = new SimpleObjectProperty<>();
  private final ObjectProperty<@Nullable MetadataColumn<?>> groupingColumn = new SimpleObjectProperty<>();
  private final ObjectProperty<@Nullable RowBoxPlotDataset> dataset = new SimpleObjectProperty<>();
  private final ObjectProperty<AbundanceMeasure> abundanceMeasure = new SimpleObjectProperty<>(
      AbundanceMeasure.Height);

  private final BooleanProperty showCategoryAxislabel = new SimpleBooleanProperty(true);
  private final BooleanProperty showTitle = new SimpleBooleanProperty(true);
  private final BooleanProperty showCategoryAxisColumnLabels = new SimpleBooleanProperty(true);

  public List<FeatureListRow> getSelectedRows() {
    return selectedRows.get();
  }

  public void setSelectedRows(List<FeatureListRow> selectedRows) {
    this.selectedRows.set(selectedRows);
  }

  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return selectedRows;
  }

  public @Nullable MetadataColumn<?> getGroupingColumn() {
    return groupingColumn.get();
  }

  public void setGroupingColumn(@Nullable MetadataColumn<?> groupingColumn) {
    this.groupingColumn.set(groupingColumn);
  }

  public ObjectProperty<@Nullable MetadataColumn<?>> groupingColumnProperty() {
    return groupingColumn;
  }

  public @Nullable RowBoxPlotDataset getDataset() {
    return dataset.get();
  }

  public void setDataset(@Nullable RowBoxPlotDataset dataset) {
    this.dataset.set(dataset);
  }

  public ObjectProperty<@Nullable RowBoxPlotDataset> datasetProperty() {
    return dataset;
  }

  public AbundanceMeasure getAbundanceMeasure() {
    return abundanceMeasure.get();
  }

  public void setAbundanceMeasure(AbundanceMeasure abundanceMeasure) {
    this.abundanceMeasure.set(abundanceMeasure);
  }

  public ObjectProperty<AbundanceMeasure> abundanceMeasureProperty() {
    return abundanceMeasure;
  }

  public boolean getShowCategoryAxislabel() {
    return showCategoryAxislabel.get();
  }

  public BooleanProperty showCategoryAxisLabelProperty() {
    return showCategoryAxislabel;
  }

  public boolean isShowTitle() {
    return showTitle.get();
  }

  public BooleanProperty showTitleProperty() {
    return showTitle;
  }

  public boolean getShowCategoryAxisColumnLabels() {
    return showCategoryAxisColumnLabels.get();
  }

  public BooleanProperty showCategoryAxisColumnLabelsProperty() {
    return showCategoryAxisColumnLabels;
  }
}
