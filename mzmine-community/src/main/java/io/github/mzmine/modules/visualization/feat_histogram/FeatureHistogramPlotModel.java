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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberType;
import io.github.mzmine.modules.visualization.scan_histogram.chart.HistogramData;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

public class FeatureHistogramPlotModel {

  private final ObjectProperty<List<FeatureList>> featureLists = new SimpleObjectProperty<>(
      List.of());

  private final ObservableList<NumberType> typeChoices = FXCollections.observableArrayList();

  private final ObjectProperty<NumberType> selectedType = new SimpleObjectProperty<>();

  private final ObjectProperty<HistogramData> dataset = new SimpleObjectProperty<>();


  @NotNull
  public List<FeatureList> getFeatureLists() {
    return requireNonNullElse(featureLists.get(), List.of());
  }

  public void setFeatureLists(List<FeatureList> featureLists) {
    this.featureLists.set(featureLists);
  }

  public ObjectProperty<List<FeatureList>> featureListsProperty() {
    return featureLists;
  }

  public ObservableList<NumberType> getTypeChoices() {
    return this.typeChoices;
  }

  public HistogramData getDataset() {
    return dataset.get();
  }

  public void setDataset(HistogramData dataset) {
    this.dataset.set(dataset);
  }

  public ObjectProperty<HistogramData> datasetProperty() {
    return dataset;
  }

  public NumberType getSelectedType() {
    return selectedType.get();
  }

  public ObjectProperty<NumberType> selectedTypeProperty() {
    return selectedType;
  }

  public void setSelectedType(NumberType type) {
    this.selectedType.set(type);
  }

}

