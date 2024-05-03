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

package io.github.mzmine.modules.tools.fraggraphdashboard;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.tools.id_fraggraph.graphstream.SignalFormulaeModel;
import io.github.mzmine.modules.tools.id_fraggraph.graphstream.SubFormulaEdge;
import io.github.mzmine.util.FormulaWithExactMz;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class FragDashboardModel {

  private final ObjectProperty<MassSpectrum> spectrum = new SimpleObjectProperty<>();
  private final ObjectProperty<MassSpectrum> isotopePattern = new SimpleObjectProperty<>();
  private final DoubleProperty precursorMz = new SimpleDoubleProperty();
  private final ObjectProperty<IonType> ionType = new SimpleObjectProperty<>(
      new IonType(IonModification.H));
  private final ListProperty<IonType> ionTypes = new SimpleListProperty<>(
      FXCollections.observableArrayList(new IonType(IonModification.H),
          new IonType(IonModification.H_NEG)));
  private final ListProperty<ResultFormula> precursorFormulae = new SimpleListProperty<>();
  private final ObjectProperty<IMolecularFormula> precursorFormula = new SimpleObjectProperty<>();
  private final ListProperty<SignalFormulaeModel> selectedNodes = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ListProperty<SubFormulaEdge> selectedEdges = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ListProperty<SignalFormulaeModel> allNodes = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ListProperty<SubFormulaEdge> allEdges = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final MapProperty<String, SignalFormulaeModel> allNodesMap = new SimpleMapProperty<>(
      FXCollections.observableHashMap());
  private final MapProperty<String, SubFormulaEdge> allEdgesMap = new SimpleMapProperty<>(
      FXCollections.observableHashMap());

  public FragDashboardModel() {
  }

  public MassSpectrum getIsotopePattern() {
    return isotopePattern.get();
  }

  public ObjectProperty<MassSpectrum> isotopePatternProperty() {
    return isotopePattern;
  }

  public void setIsotopePattern(MassSpectrum isotopePattern) {
    this.isotopePattern.set(isotopePattern);
  }

  public double getPrecursorMz() {
    return precursorMz.get();
  }

  public DoubleProperty precursorMzProperty() {
    return precursorMz;
  }

  public void setPrecursorMz(double precursorMz) {
    this.precursorMz.set(precursorMz);
  }

  public IonType getIonType() {
    return ionType.get();
  }

  public ObjectProperty<IonType> ionTypeProperty() {
    return ionType;
  }

  public void setIonType(IonType ionType) {
    this.ionType.set(ionType);
  }

  public ObservableList<IonType> getIonTypes() {
    return ionTypes.get();
  }

  public ListProperty<IonType> ionTypesProperty() {
    return ionTypes;
  }

  public void setIonTypes(ObservableList<IonType> ionTypes) {
    this.ionTypes.set(ionTypes);
  }

  public ObservableList<ResultFormula> getPrecursorFormulae() {
    return precursorFormulae.get();
  }

  public ListProperty<ResultFormula> precursorFormulaeProperty() {
    return precursorFormulae;
  }

  public void setPrecursorFormulae(ObservableList<ResultFormula> precursorFormulae) {
    this.precursorFormulae.set(precursorFormulae);
  }

  public IMolecularFormula getPrecursorFormula() {
    return precursorFormula.get();
  }

  public ObjectProperty<IMolecularFormula> precursorFormulaProperty() {
    return precursorFormula;
  }

  public void setPrecursorFormula(IMolecularFormula precursorFormula) {
    this.precursorFormula.set(precursorFormula);
  }

  public MassSpectrum getSpectrum() {
    return spectrum.get();
  }

  public ObjectProperty<MassSpectrum> spectrumProperty() {
    return spectrum;
  }

  public void setSpectrum(MassSpectrum spectrum) {
    this.spectrum.set(spectrum);
  }

  public ObservableList<SignalFormulaeModel> getSelectedNodes() {
    return selectedNodes.get();
  }

  public ListProperty<SignalFormulaeModel> selectedNodesProperty() {
    return selectedNodes;
  }

  public void setSelectedNodes(ObservableList<SignalFormulaeModel> selectedNodes) {
    this.selectedNodes.set(selectedNodes);
  }

  public ObservableList<SignalFormulaeModel> getAllNodes() {
    return allNodes.get();
  }

  public ListProperty<SignalFormulaeModel> allNodesProperty() {
    return allNodes;
  }

  public void setAllNodes(ObservableList<SignalFormulaeModel> allNodes) {
    this.allNodes.set(allNodes);
  }

  public ObservableList<SubFormulaEdge> getSelectedEdges() {
    return selectedEdges.get();
  }

  public ListProperty<SubFormulaEdge> selectedEdgesProperty() {
    return selectedEdges;
  }

  public void setSelectedEdges(ObservableList<SubFormulaEdge> selectedEdges) {
    this.selectedEdges.set(selectedEdges);
  }

  public ObservableList<SubFormulaEdge> getAllEdges() {
    return allEdges.get();
  }

  public ListProperty<SubFormulaEdge> allEdgesProperty() {
    return allEdges;
  }

  public void setAllEdges(ObservableList<SubFormulaEdge> allEdges) {
    this.allEdges.set(allEdges);
  }

  public ReadOnlyMapProperty<String, SignalFormulaeModel> getAllNodesMap() {
    return new ReadOnlyMapWrapper<>(allNodesMap);
  }

  public ReadOnlyMapProperty<String, SignalFormulaeModel> allNodesMapProperty() {
    return new ReadOnlyMapWrapper<>(allNodesMap);
  }

  public ReadOnlyMapProperty<String, SubFormulaEdge> getAllEdgesMap() {
    return new ReadOnlyMapWrapper<>(allEdgesMap);
  }

  public ReadOnlyMapProperty<String, SubFormulaEdge> allEdgesMapProperty() {
    return new ReadOnlyMapWrapper<>(allEdgesMap);
  }

  MapProperty<String, SubFormulaEdge> allEdgesMapPropertyModifiable() {
    return allEdgesMap;
  }
}
