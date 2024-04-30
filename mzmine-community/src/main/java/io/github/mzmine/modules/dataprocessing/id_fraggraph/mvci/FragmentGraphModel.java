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

package io.github.mzmine.modules.dataprocessing.id_fraggraph.mvci;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.modules.dataprocessing.id_fraggraph.graphstream.PeakFormulaeModel;
import io.github.mzmine.modules.dataprocessing.id_fraggraph.graphstream.SubFormulaEdge;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.graphstream.graph.implementations.MultiGraph;
import org.openscience.cdk.interfaces.IMolecularFormula;

class FragmentGraphModel {

  FragmentGraphModel() {
  }

  private final ObjectProperty<IMolecularFormula> precursorFormula = new SimpleObjectProperty<>();
  private final BooleanProperty precursorFormulaEditable = new SimpleBooleanProperty(false);
  private final ObjectProperty<MassSpectrum> spectrum = new SimpleObjectProperty<>();
  private final ObjectProperty<MultiGraph> graph = new SimpleObjectProperty<>();
  private final ListProperty<PeakFormulaeModel> selectedNodes = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ListProperty<PeakFormulaeModel> allNodes = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ListProperty<SubFormulaEdge> selectedEdges = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ListProperty<SubFormulaEdge> allEdges = new SimpleListProperty<>(
      FXCollections.observableArrayList());

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

  public MultiGraph getGraph() {
    return graph.get();
  }

  public ObjectProperty<MultiGraph> graphProperty() {
    return graph;
  }

  public void setGraph(MultiGraph graph) {
    this.graph.set(graph);
  }

  public ObservableList<PeakFormulaeModel> getSelectedNodes() {
    return selectedNodes.get();
  }

  public ListProperty<PeakFormulaeModel> selectedNodesProperty() {
    return selectedNodes;
  }

  public void setSelectedNodes(ObservableList<PeakFormulaeModel> selectedNodes) {
    this.selectedNodes.set(selectedNodes);
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

  public ObservableList<PeakFormulaeModel> getAllNodes() {
    return allNodes.get();
  }

  public ListProperty<PeakFormulaeModel> allNodesProperty() {
    return allNodes;
  }

  public void setAllNodes(ObservableList<PeakFormulaeModel> allNodes) {
    this.allNodes.set(allNodes);
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

  public boolean isPrecursorFormulaEditable() {
    return precursorFormulaEditable.get();
  }

  public BooleanProperty precursorFormulaEditableProperty() {
    return precursorFormulaEditable;
  }

  public void setPrecursorFormulaEditable(boolean precursorFormulaEditable) {
    this.precursorFormulaEditable.set(precursorFormulaEditable);
  }
}
