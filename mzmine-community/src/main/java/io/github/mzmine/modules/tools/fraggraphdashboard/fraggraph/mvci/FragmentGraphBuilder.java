/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SignalFormulaeModel;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SubFormulaEdge;
import io.github.mzmine.modules.visualization.networking.visual.NetworkPane;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Node;
import org.jetbrains.annotations.NotNull;

class FragmentGraphBuilder extends FxViewBuilder<FragmentGraphModel> {

  private static final Logger logger = Logger.getLogger(FragmentGraphBuilder.class.getName());

  FragmentGraphBuilder(FragmentGraphModel model) {
    super(model);
  }

  @Override
  public Region build() {
    final BorderPane pane = new BorderPane();
    addGraphListenerForNetworkUpdate(pane);

//    final Accordion settingsAccordion = newAccordion(true, newTitledPane("Precursor settings",
//        newHBox(new Label("Precursor formula: "), formulaField, new Label("m/z:"),
//            formulaMassLabel)));
    return pane;
  }

  private void addGraphListenerForNetworkUpdate(BorderPane pane) {
    final List<ChangeListener<ObservableList<SignalFormulaeModel>>> oldNodeListeners = new ArrayList<>();
    final List<ChangeListener<ObservableList<SubFormulaEdge>>> oldEdgeListeners = new ArrayList<>();

    model.graphProperty().addListener((_, oldGraph, graph) -> {
      pane.setCenter(null);
      removeOldListeners(oldNodeListeners, oldEdgeListeners);

      if (graph == null) {
        return;
      }
      NetworkPane network = new NetworkPane(graph.getId(), false, graph);
      network.showFullGraph();
      pane.setCenter(network);

      model.getAllNodes().forEach(nodeModel -> {
        nodeModel.clearPassThroughGraphs();
        nodeModel.addPassThroughGraph(network.getGraphicGraph());
      });
      model.getAllEdges().forEach(edge -> {
        edge.removeGraph(oldGraph);
        edge.addGraph(network.getGraphicGraph(), true);
      });

      addNetworkToModelNodeListener(network);
      addNetworkToModelEdgeListener(network);

      // listen to changes in model and map to graph
      final ChangeListener<ObservableList<SignalFormulaeModel>> modelToGraphNodeListener = createModelToGraphNodeListener(
          network);
      model.selectedNodesProperty().addListener(modelToGraphNodeListener);
      oldNodeListeners.add(modelToGraphNodeListener);

      final ChangeListener<ObservableList<SubFormulaEdge>> modelToGraphEdgeListener = createModelToGraphEdgeListener(
          network);
      model.selectedEdgesProperty().addListener(modelToGraphEdgeListener);
      oldEdgeListeners.add(modelToGraphEdgeListener);
    });
  }

  private void addNetworkToModelEdgeListener(NetworkPane network) {
    network.getSelectedEdges().addListener((ListChangeListener<? super Edge>) c -> {
      c.next();
      final ObservableList<? extends Edge> selected = c.getList();
      var selectedEdges = selected.stream().map(Element::getId)
          .map(id -> model.getAllEdgesMap().get(id)).filter(Objects::nonNull).toList();
      if (model.getSelectedEdges().equals(selected)) {
        return;
      }
      model.getSelectedEdges().setAll(selectedEdges);
      logger.finest(() -> "Selected edges: " + selectedEdges.toString());
    });
  }

  private void addNetworkToModelNodeListener(NetworkPane network) {
    // listen to changes in network selection and map to model
    network.getSelectedNodes().addListener((ListChangeListener<Node>) c -> {
      c.next();
      final ObservableList<? extends Node> selected = c.getList();
      var selectedNodes = selected.stream().map(Element::getId)
          .map(id -> model.getAllNodesMap().get(id)).filter(Objects::nonNull).toList();
//        model.selectedNodesProperty().clear();
      if (model.getSelectedNodes().equals(selectedNodes)) {
        return;
      }
      model.setSelectedNodes(selectedNodes);
      logger.finest(() -> "Selected nodes: " + selectedNodes.toString());
    });
  }

  private void removeOldListeners(
      List<ChangeListener<ObservableList<SignalFormulaeModel>>> oldNodeListeners,
      List<ChangeListener<ObservableList<SubFormulaEdge>>> oldEdgeListeners) {
    for (ChangeListener<ObservableList<SignalFormulaeModel>> old : oldNodeListeners) {
      model.selectedNodesProperty().removeListener(old);
    }
    for (ChangeListener<ObservableList<SubFormulaEdge>> old : oldEdgeListeners) {
      model.selectedEdgesProperty().removeListener(old);
    }
  }

  @NotNull
  private ChangeListener<ObservableList<SignalFormulaeModel>> createModelToGraphNodeListener(
      NetworkPane network) {
    return (_, _, n) -> {
      if (network.getSelectedNodes().stream().map(Element::getId)
          .map(id -> model.getAllNodesMap().get(id)).filter(Objects::nonNull).toList().equals(n)) {
        return;
      }
      var selectedNodes = n.stream().map(SignalFormulaeModel::getId)
          .map(id -> model.getAllNodesMap().get(id)).filter(Objects::nonNull)
          .map(sfm -> network.getGraphicGraph().getNode(sfm.getId())).toList();
      network.getSelectedNodes().setAll(selectedNodes);
    };
  }

  @NotNull
  private ChangeListener<ObservableList<SubFormulaEdge>> createModelToGraphEdgeListener(
      NetworkPane network) {
    return (_, _, n) -> {
      if (network.getSelectedEdges().stream().map(Element::getId)
          .map(id -> model.getAllEdgesMap().get(id)).filter(Objects::nonNull).toList().equals(n)) {
        return;
      }
      // select the edges
      var selectedEdges = n.stream().map(edge -> network.getGraphicGraph().getEdge(edge.getId()))
          .filter(Objects::nonNull).toList();
      network.getSelectedEdges().setAll(selectedEdges);

      // also select nodes
      final List<Node> nodesToSelect = selectedEdges.stream().<Node>mapMulti((edge, c) -> {
        c.accept(network.getGraphicGraph().getNode(edge.getNode0().getId()));
        c.accept(network.getGraphicGraph().getNode(edge.getNode1().getId()));
      }).filter(Objects::nonNull).toList();
      network.getSelectedNodes().setAll(nodesToSelect);

    };
  }
}
