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

package io.github.mzmine.modules.dataanalysis.pca_new;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class PCAModel {

  private final ObservableList<Integer> availablePCs = FXCollections.observableArrayList(1, 2, 3,
      4);
  private final Property<Integer> domainPc = new SimpleIntegerProperty(1).asObject();
  private final Property<Integer> rangePc = new SimpleIntegerProperty(2).asObject();
  private final ObjectProperty<List<FeatureList>> flists = new SimpleObjectProperty<>();
  private final ObjectProperty<List<FeatureListRow>> selectedRows = new SimpleObjectProperty<>();
  private final ObjectProperty<AbundanceMeasure> abundance = new SimpleObjectProperty<>(
      AbundanceMeasure.Height);
  private final StringProperty metadataColumn = new SimpleStringProperty();
  private final ObjectProperty<List<DatasetAndRenderer>> scoresDatasets = new SimpleObjectProperty<>(
      List.of());
  private final ObjectProperty<List<DatasetAndRenderer>> loadingsDatasets = new SimpleObjectProperty<>(
      List.of());

  private final ObjectProperty<PCARowsResult> pcaResult = new SimpleObjectProperty<>();

  public ObservableList<Integer> getAvailablePCs() {
    return availablePCs;
  }

  public Integer getDomainPc() {
    return domainPc.getValue();
  }

  public void setDomainPc(Integer domainPc) {
    this.domainPc.setValue(domainPc);
  }

  public Property<Integer> domainPcProperty() {
    return domainPc;
  }

  public Integer getRangePc() {
    return rangePc.getValue();
  }

  public void setRangePc(Integer rangePc) {
    this.rangePc.setValue(rangePc);
  }

  public Property<Integer> rangePcProperty() {
    return rangePc;
  }

  public String getMetadataColumn() {
    return metadataColumn.get();
  }

  public void setMetadataColumn(String metadataColumn) {
    this.metadataColumn.set(metadataColumn);
  }

  public StringProperty metadataColumnProperty() {
    return metadataColumn;
  }

  public List<FeatureList> getFlists() {
    return flists.get();
  }

  public void setFlists(List<FeatureList> flists) {
    this.flists.set(flists);
  }

  public ObjectProperty<List<FeatureList>> flistsProperty() {
    return flists;
  }

  public List<FeatureListRow> getSelectedRows() {
    return selectedRows.get();
  }

  public void setSelectedRows(List<FeatureListRow> selectedRows) {
    this.selectedRows.set(selectedRows);
  }

  public ObjectProperty<List<FeatureListRow>> selectedRowsProperty() {
    return selectedRows;
  }

  public AbundanceMeasure getAbundance() {
    return abundance.get();
  }

  public void setAbundance(AbundanceMeasure abundance) {
    this.abundance.set(abundance);
  }

  public ObjectProperty<AbundanceMeasure> abundanceProperty() {
    return abundance;
  }

  public List<DatasetAndRenderer> getScoresDatasets() {
    return scoresDatasets.get();
  }

  public void setScoresDatasets(List<DatasetAndRenderer> scoresDatasets) {
    this.scoresDatasets.set(scoresDatasets);
  }

  public ObjectProperty<List<DatasetAndRenderer>> scoresDatasetsProperty() {
    return scoresDatasets;
  }

  public List<DatasetAndRenderer> getLoadingsDatasets() {
    return loadingsDatasets.get();
  }

  public void setLoadingsDatasets(List<DatasetAndRenderer> loadingsDatasets) {
    this.loadingsDatasets.set(loadingsDatasets);
  }

  public ObjectProperty<List<DatasetAndRenderer>> loadingsDatasetsProperty() {
    return loadingsDatasets;
  }

  public PCARowsResult getPcaResult() {
    return pcaResult.get();
  }

  public void setPcaResult(PCARowsResult pcaResult) {
    this.pcaResult.set(pcaResult);
  }

  public ObjectProperty<PCARowsResult> pcaResultProperty() {
    return pcaResult;
  }
}
