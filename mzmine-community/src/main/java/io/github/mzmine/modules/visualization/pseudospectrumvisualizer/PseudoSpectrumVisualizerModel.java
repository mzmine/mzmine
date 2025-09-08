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

package io.github.mzmine.modules.visualization.pseudospectrumvisualizer;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;

public class PseudoSpectrumVisualizerModel {

  private final Property<Color> color = new SimpleObjectProperty<>();

  // comes from interfaces
  private final ObjectProperty<List<FeatureListRow>> selectedRows = new SimpleObjectProperty<>();
  private final ObjectProperty<List<RawDataFile>> selectedFiles = new SimpleObjectProperty<>();
  // defined by selected rows as the first row
  private final ObservableValue<FeatureListRow> selectedRow;
  private final ObservableValue<FeatureList> featureList;
  // defined by selected files as first file
  private final ObservableValue<RawDataFile> selectedFile;
  // defined by feature list
  private final Property<MZTolerance> mzTolerance = new SimpleObjectProperty<>();

  // the final data to be shown
  private final Property<Scan> pseudoSpec = new SimpleObjectProperty<>();
  private final ObjectProperty<List<DatasetAndRenderer>> ticDatasets = new SimpleObjectProperty<>();


  public PseudoSpectrumVisualizerModel() {
    selectedRow = PropertyUtils.firstElementProperty(selectedRows);
    selectedFile = PropertyUtils.firstElementProperty(selectedFiles);
    featureList = selectedRow.map(FeatureListRow::getFeatureList);
  }

  public List<FeatureListRow> getSelectedRows() {
    return selectedRows.get();
  }

  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return selectedRows;
  }

  public FeatureListRow getSelectedRow() {
    return selectedRow.getValue();
  }

  public ObservableValue<FeatureListRow> selectedRowProperty() {
    return selectedRow;
  }

  public MZTolerance getMzTolerance() {
    return mzTolerance.getValue();
  }

  public Property<MZTolerance> mzToleranceProperty() {
    return mzTolerance;
  }

  public void setMzTolerance(MZTolerance mzTol) {
    mzTolerance.setValue(mzTol);
  }

  public Scan getPseudoSpec() {
    return pseudoSpec.getValue();
  }

  public Property<Scan> pseudoSpecProperty() {
    return pseudoSpec;
  }

  public FeatureList getFeatureList() {
    return featureList.getValue();
  }

  public ObservableValue<FeatureList> featureListProperty() {
    return featureList;
  }

  public List<DatasetAndRenderer> getTicDatasets() {
    return ticDatasets.get();
  }

  public ObjectProperty<List<DatasetAndRenderer>> ticDatasetsProperty() {
    return ticDatasets;
  }

  public void setTicDatasets(List<DatasetAndRenderer> datasets) {
    ticDatasets.set(datasets);
  }

  public void setPseudoSpec(Scan scan) {
    pseudoSpec.setValue(scan);
  }

  public RawDataFile getSelectedFile() {
    return selectedFile.getValue();
  }

  public ObservableValue<RawDataFile> selectedFileProperty() {
    return selectedFile;
  }

  public void setColor(Color color) {
    this.color.setValue(color);
  }

  public Color getColor() {
    return color.getValue();
  }

  public Property<Color> colorProperty() {
    return color;
  }

  public List<RawDataFile> getSelectedFiles() {
    return selectedFiles.get();
  }

  public ObjectProperty<List<RawDataFile>> selectedFilesProperty() {
    return selectedFiles;
  }
}
