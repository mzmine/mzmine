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
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTest;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import java.util.Collection;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;

public class VolcanoPlotModel {

  private final ObjectProperty<FeatureList> selectedFlist = new SimpleObjectProperty<>();
  private final ObjectProperty<ObservableList<FeatureList>> flists = new SimpleObjectProperty<>();
  private final ObjectProperty<ObservableList<MetadataColumn<?>>> metadataColumns = new SimpleObjectProperty<>();
  private final ObjectProperty<AbundanceMeasure> abundanceMeasure = new SimpleObjectProperty<>();
  private final ObjectProperty<Collection<PlotXYDataProvider>> datasets = new SimpleObjectProperty<>();
  private final ObjectProperty<@Nullable RowSignificanceTest> test = new SimpleObjectProperty<>();

  public FeatureList getSelectedFlist() {
    return selectedFlist.get();
  }

  public void setSelectedFlist(FeatureList selectedFlist) {
    this.selectedFlist.set(selectedFlist);
  }

  public ObjectProperty<FeatureList> selectedFlistProperty() {
    return selectedFlist;
  }

  public ObservableList<FeatureList> getFlists() {
    return flists.get();
  }

  public void setFlists(ObservableList<FeatureList> flists) {
    this.flists.set(flists);
  }

  public ObjectProperty<ObservableList<FeatureList>> flistsProperty() {
    return flists;
  }

  public ObservableList<MetadataColumn<?>> getMetadataColumns() {
    return metadataColumns.get();
  }

  public void setMetadataColumns(ObservableList<MetadataColumn<?>> metadataColumns) {
    this.metadataColumns.set(metadataColumns);
  }

  public ObjectProperty<ObservableList<MetadataColumn<?>>> metadataColumnsProperty() {
    return metadataColumns;
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

  public Collection<PlotXYDataProvider> getDatasets() {
    return datasets.get();
  }

  public void setDatasets(Collection<PlotXYDataProvider> datasets) {
    this.datasets.set(datasets);
  }

  public ObjectProperty<Collection<PlotXYDataProvider>> datasetsProperty() {
    return datasets;
  }

  public @Nullable RowSignificanceTest getTest() {
    return test.get();
  }

  public void setTest(@Nullable RowSignificanceTest test) {
    this.test.set(test);
  }

  public ObjectProperty<@Nullable RowSignificanceTest> testProperty() {
    return test;
  }
}

