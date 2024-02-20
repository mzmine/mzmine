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

package io.github.mzmine.modules.dataanalysis.volcanoplot;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.util.Collection;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;

public class VolcanoPlotModel {

  private final ObjectProperty<FeatureList> selectedFlist = new SimpleObjectProperty<>();
  private final ObjectProperty<ObservableList<FeatureList>> flists = new SimpleObjectProperty<>();
  private final ObjectProperty<ObservableList<MetadataColumn<?>>> metadataColumns = new SimpleObjectProperty<>();
  private final ObjectProperty<MetadataColumn<?>> selectedMetadataColumn = new SimpleObjectProperty<>();
  private final ObjectProperty<AbundanceMeasure> abundanceMeasure = new SimpleObjectProperty<>();
  private final ObjectProperty<Collection<PlotXYZDataProvider>> datasets = new SimpleObjectProperty<>();

  public MetadataColumn<?> getSelectedMetadataColumn() {
    return selectedMetadataColumn.get();
  }

  /**
   * Changed by the {@link VolcanoPlotViewBuilder} and reflects the selected metadata column.
   */
  public ObjectProperty<MetadataColumn<?>> selectedMetadataColumnProperty() {
    return selectedMetadataColumn;
  }

  public FeatureList getSelectedFlist() {
    return selectedFlist.get();
  }

  public ObjectProperty<FeatureList> selectedFlistProperty() {
    return selectedFlist;
  }

  public void setSelectedFlist(FeatureList selectedFlist) {
    this.selectedFlist.set(selectedFlist);
  }

  public ObservableList<FeatureList> getFlists() {
    return flists.get();
  }

  public ObjectProperty<ObservableList<FeatureList>> flistsProperty() {
    return flists;
  }

  public void setFlists(ObservableList<FeatureList> flists) {
    this.flists.set(flists);
  }

  public ObservableList<MetadataColumn<?>> getMetadataColumns() {
    return metadataColumns.get();
  }

  public ObjectProperty<ObservableList<MetadataColumn<?>>> metadataColumnsProperty() {
    return metadataColumns;
  }

  public void setMetadataColumns(ObservableList<MetadataColumn<?>> metadataColumns) {
    this.metadataColumns.set(metadataColumns);
  }

  public AbundanceMeasure getAbundanceMeasure() {
    return abundanceMeasure.get();
  }

  public ObjectProperty<AbundanceMeasure> abundanceMeasureProperty() {
    return abundanceMeasure;
  }

  public void setAbundanceMeasure(AbundanceMeasure abundanceMeasure) {
    this.abundanceMeasure.set(abundanceMeasure);
  }

  public Collection<PlotXYZDataProvider> getDatasets() {
    return datasets.get();
  }

  public ObjectProperty<Collection<PlotXYZDataProvider>> datasetsProperty() {
    return datasets;
  }

  public void setDatasets(Collection<PlotXYZDataProvider> datasets) {
    this.datasets.set(datasets);
  }
}

