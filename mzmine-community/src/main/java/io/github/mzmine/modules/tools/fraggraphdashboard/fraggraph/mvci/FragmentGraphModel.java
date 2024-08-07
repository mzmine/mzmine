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

package io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.mvci;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SignalFormulaeModel;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SubFormulaEdge;
import io.github.mzmine.util.javafx.ListToMapListener;
import java.util.ArrayList;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyMapWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.graphstream.graph.implementations.MultiGraph;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

class FragmentGraphModel {

  FragmentGraphModel() {
    allNodes.addListener(new ListToMapListener<>(SignalFormulaeModel::getId, allNodesMap.get()));
    allEdges.addListener(new ListToMapListener<>(SubFormulaEdge::getId, allEdgesMap.get()));
  }

  private final ObjectProperty<IMolecularFormula> precursorFormula = new SimpleObjectProperty<>();
  private final ObjectProperty<@Nullable Double> measuredPrecursorMz = new SimpleObjectProperty<>();
  private final ObjectProperty<MassSpectrum> ms2Spectrum = new SimpleObjectProperty<>();
  private final BooleanProperty precursorFormulaEditable = new SimpleBooleanProperty(false);
  private final ObjectProperty<MultiGraph> graph = new SimpleObjectProperty<>();
  private final ListProperty<SignalFormulaeModel> selectedNodes = new SimpleListProperty<>(
      FXCollections.observableArrayList(new ArrayList<>()));
  private final ListProperty<SubFormulaEdge> selectedEdges = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ListProperty<SignalFormulaeModel> allNodes = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ListProperty<SubFormulaEdge> allEdges = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ReadOnlyMapWrapper<String, SignalFormulaeModel> allNodesMap = new ReadOnlyMapWrapper<>(
      FXCollections.observableHashMap());
  private final ReadOnlyMapWrapper<String, SubFormulaEdge> allEdgesMap = new ReadOnlyMapWrapper<>(
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

  public @Nullable Double getMeasuredPrecursorMz() {
    return measuredPrecursorMz.get();
  }

  public ObjectProperty<@Nullable Double> measuredPrecursorMzProperty() {
    return measuredPrecursorMz;
  }

  public void setMeasuredPrecursorMz(@Nullable Double measuredPrecursorMz) {
    this.measuredPrecursorMz.set(measuredPrecursorMz);
  }

  public MassSpectrum getMs2Spectrum() {
    return ms2Spectrum.get();
  }

  public ObjectProperty<MassSpectrum> ms2SpectrumProperty() {
    return ms2Spectrum;
  }

  public void setMs2Spectrum(MassSpectrum ms2Spectrum) {
    this.ms2Spectrum.set(ms2Spectrum);
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

  public Map<String, SignalFormulaeModel> getAllNodesMap() {
    return allNodesMap.getReadOnlyProperty();
  }

  public ReadOnlyMapProperty<String, SignalFormulaeModel> allNodesMapProperty() {
    return allNodesMap.getReadOnlyProperty();
  }

  public Map<String, SubFormulaEdge> getAllEdgesMap() {
    return allEdgesMap.getReadOnlyProperty();
  }

  public ReadOnlyMapProperty<String, SubFormulaEdge> allEdgesMapProperty() {
    return allEdgesMap.getReadOnlyProperty();
  }

}
