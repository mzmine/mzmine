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

package io.github.mzmine.modules.visualization.feat_histogram;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberType;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import java.util.Collection;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

public class FeatureHistogramPlotModel {

  private final ObservableList<FeatureList> featureLists = FXCollections.observableArrayList();

  private final ObservableList<NumberType> typeChoices = FXCollections.observableArrayList();

  private final ObjectProperty<NumberType> dataType = new SimpleObjectProperty<>();
  //  private final ObjectProperty<AbundanceMeasure> abundanceMeasure = new SimpleObjectProperty<>(
//      AbundanceMeasure.Height);
  private final ObjectProperty<Collection<DatasetAndRenderer>> datasets = new SimpleObjectProperty<>(
      List.of());
//  private final ObjectProperty<@Nullable RowSignificanceTest> test = new SimpleObjectProperty<>();

//  private final DoubleProperty pValue = new SimpleDoubleProperty(0.05);

  private final ObjectProperty<List<FeatureListRow>> selectedRows = new SimpleObjectProperty<>();

  public FeatureHistogramPlotModel(@NotNull final List<FeatureList> featureLists) {
    // first listen for changes to typeChoices
    this.typeChoices.addListener((ListChangeListener<NumberType>) c -> {
      var selectedType = dataType.get();
      if (selectedType == null) {
        return;
      }
      if (!typeChoices.contains(selectedType)) {
        dataType.set(null); // remove as its not present
      }
    });

    // listen to changes to featureLists
    this.featureLists.addListener((ListChangeListener<? super FeatureList>) change -> {
      List<NumberType> types = this.featureLists.stream().map(FeatureList::getFeatureTypes)
          .flatMap(Collection::stream).filter(NumberType.class::isInstance)
          .map(NumberType.class::cast).toList();
      typeChoices.setAll(types);
    });

    this.featureLists.setAll(featureLists);

    model.getTypeChoices().setAll(
        featureLists.getFeatureTypes().stream().filter(NumberType.class::isInstance)
            .map(NumberType.class::cast).toList());
  }

  public ObservableList<FeatureList> getFeatureLists() {
    return featureLists;
  }

  public void setFeatureLists(List<FeatureList> featureLists) {
    this.featureLists.setAll(featureLists);
  }

  //  public void setTypeChoices(FeatureList flist) {
//    this.typeChoices = flist.getFeatureTypes().stream();
//  }
  public ObservableList<NumberType> getTypeChoices() {
    return this.typeChoices;
  }

//  public AbundanceMeasure getAbundanceMeasure() {
//    return abundanceMeasure.get();
//  }

//  public void set(AbundanceMeasure abundanceMeasure) {
//    this.abundanceMeasure.set(abundanceMeasure);
//  }
//
//  public ObjectProperty<AbundanceMeasure> abundanceMeasureProperty() {
//    return abundanceMeasure;
//  }

  public Collection<DatasetAndRenderer> getDatasets() {
    return datasets.get();
  }

  public void setDatasets(Collection<DatasetAndRenderer> datasets) {
    this.datasets.set(datasets);
  }

  public ObjectProperty<Collection<DatasetAndRenderer>> datasetsProperty() {
    return datasets;
  }

  public NumberType getDataType() {
    return dataType.get();
  }

  //todo rename into selectedDataType
  public ObjectProperty<NumberType> dataTypeProperty() {
    return dataType;
  }

  public void setDataType(NumberType type) {
    this.dataType.set(type);
  }

//  public @Nullable RowSignificanceTest getTest() {
//    return test.get();
//  }
//
//  public void setTest(@Nullable RowSignificanceTest test) {
//    this.test.set(test);
//  }

//  public ObjectProperty<@Nullable RowSignificanceTest> testProperty() {
//    return test;
//  }

//  public double getpValue() {
//    return pValue.get();
//  }
//
//  public DoubleProperty pValueProperty() {
//    return pValue;
//  }
//
//  public void setpValue(double pValue) {
//    this.pValue.set(pValue);
//  }

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

