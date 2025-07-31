/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.statistics.FeaturesDataTable;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunctions;
import io.github.mzmine.parameters.parametertypes.statistics.UnivariateRowSignificanceTestConfig;
import java.util.Collection;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.Nullable;

public class VolcanoPlotModel {

  private final ObjectProperty<List<FeatureList>> flists = new SimpleObjectProperty<>();
  private final ObjectProperty<AbundanceMeasure> abundanceMeasure = new SimpleObjectProperty<>(
      AbundanceMeasure.Height);
  private final ObjectProperty<ImputationFunctions> missingValueImputation = new SimpleObjectProperty<>(
      ImputationFunctions.GLOBAL_LIMIT_OF_DETECTION);

  // after missing value imputation etc
  private final ObjectProperty<FeaturesDataTable> featureDataTable = new SimpleObjectProperty<>();

  private final ObjectProperty<Collection<DatasetAndRenderer>> datasets = new SimpleObjectProperty<>(
      List.of());
  private final ObjectProperty<@Nullable UnivariateRowSignificanceTestConfig> test = new SimpleObjectProperty<>();

  private final DoubleProperty pValue = new SimpleDoubleProperty(0.05);

  private final ObjectProperty<List<FeatureListRow>> selectedRows = new SimpleObjectProperty<>();

  public List<FeatureList> getFlists() {
    return flists.get();
  }

  public ObjectProperty<List<FeatureList>> flistsProperty() {
    return flists;
  }

  public void setFlists(List<FeatureList> flists) {
    this.flists.set(flists);
  }

  public FeaturesDataTable getFeatureDataTable() {
    return featureDataTable.get();
  }

  public ObjectProperty<FeaturesDataTable> featureDataTableProperty() {
    return featureDataTable;
  }

  public void setFeatureDataTable(FeaturesDataTable featureDataTable) {
    this.featureDataTable.set(featureDataTable);
  }

  public void setMissingValueImputation(ImputationFunctions missingValueImputation) {
    this.missingValueImputation.set(missingValueImputation);
  }

  public ImputationFunctions getMissingValueImputation() {
    return missingValueImputation.get();
  }

  public ObjectProperty<ImputationFunctions> missingValueImputationProperty() {
    return missingValueImputation;
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

  public Collection<DatasetAndRenderer> getDatasets() {
    return datasets.get();
  }

  public void setDatasets(Collection<DatasetAndRenderer> datasets) {
    this.datasets.set(datasets);
  }

  public ObjectProperty<Collection<DatasetAndRenderer>> datasetsProperty() {
    return datasets;
  }

  public @Nullable UnivariateRowSignificanceTestConfig getTest() {
    return test.get();
  }

  public void setTest(@Nullable UnivariateRowSignificanceTestConfig test) {
    this.test.set(test);
  }

  public ObjectProperty<@Nullable UnivariateRowSignificanceTestConfig> testProperty() {
    return test;
  }

  public double getpValue() {
    return pValue.get();
  }

  public DoubleProperty pValueProperty() {
    return pValue;
  }

  public void setpValue(double pValue) {
    this.pValue.set(pValue);
  }

  public List<FeatureListRow> getSelectedRows() {
    return selectedRows.get();
  }

  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return selectedRows;
  }

  public void setSelectedRows(List<FeatureListRow> selectedRows) {
    this.selectedRows.set(selectedRows);
  }
}

