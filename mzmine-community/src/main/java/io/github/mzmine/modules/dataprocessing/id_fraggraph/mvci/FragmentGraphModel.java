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
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.graphstream.graph.implementations.MultiGraph;
import org.openscience.cdk.interfaces.IMolecularFormula;

class FragmentGraphModel {

  FragmentGraphModel() {
  }

  private final ObjectProperty<IMolecularFormula> precursorFormula = new SimpleObjectProperty<>();
  private final BooleanProperty precursorFormulaEditable = new SimpleBooleanProperty(false);
  private final ObjectProperty<MassSpectrum> spectrum = new SimpleObjectProperty<>();
  private final ObjectProperty<MultiGraph> graph = new SimpleObjectProperty<>();
  private final MapProperty<String, PeakFormulaeModel> selectedNodes = new SimpleMapProperty<>(
      FXCollections.observableHashMap());
  private final MapProperty<String, PeakFormulaeModel> allNodes = new SimpleMapProperty<>(
      FXCollections.observableHashMap());
  private final MapProperty<String, SubFormulaEdge> selectedEdges = new SimpleMapProperty<>(
      FXCollections.observableHashMap());
  private final MapProperty<String, SubFormulaEdge> allEdges = new SimpleMapProperty<>(
      FXCollections.observableHashMap());


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

  public boolean isPrecursorFormulaEditable() {
    return precursorFormulaEditable.get();
  }

  public BooleanProperty precursorFormulaEditableProperty() {
    return precursorFormulaEditable;
  }

  public void setPrecursorFormulaEditable(boolean precursorFormulaEditable) {
    this.precursorFormulaEditable.set(precursorFormulaEditable);
  }

  public ObservableMap<String, PeakFormulaeModel> getSelectedNodes() {
    return selectedNodes.get();
  }

  public MapProperty<String, PeakFormulaeModel> selectedNodesProperty() {
    return selectedNodes;
  }

  public void setSelectedNodes(ObservableMap<String, PeakFormulaeModel> selectedNodes) {
    this.selectedNodes.set(selectedNodes);
  }

  public ObservableMap<String, PeakFormulaeModel> getAllNodes() {
    return allNodes.get();
  }

  public MapProperty<String, PeakFormulaeModel> allNodesProperty() {
    return allNodes;
  }

  public void setAllNodes(ObservableMap<String, PeakFormulaeModel> allNodes) {
    this.allNodes.set(allNodes);
  }

  public ObservableMap<String, SubFormulaEdge> getSelectedEdges() {
    return selectedEdges.get();
  }

  public MapProperty<String, SubFormulaEdge> selectedEdgesProperty() {
    return selectedEdges;
  }

  public void setSelectedEdges(ObservableMap<String, SubFormulaEdge> selectedEdges) {
    this.selectedEdges.set(selectedEdges);
  }

  public ObservableMap<String, SubFormulaEdge> getAllEdges() {
    return allEdges.get();
  }

  public MapProperty<String, SubFormulaEdge> allEdgesProperty() {
    return allEdges;
  }

  public void setAllEdges(ObservableMap<String, SubFormulaEdge> allEdges) {
    this.allEdges.set(allEdges);
  }
}
