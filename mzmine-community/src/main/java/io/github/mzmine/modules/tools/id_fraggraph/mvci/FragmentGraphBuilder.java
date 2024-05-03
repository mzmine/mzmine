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

package io.github.mzmine.modules.tools.id_fraggraph.mvci;

import static io.github.mzmine.javafx.components.util.FxLayout.newAccordion;
import static io.github.mzmine.javafx.components.util.FxLayout.newHBox;
import static io.github.mzmine.javafx.components.util.FxLayout.newTitledPane;

import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.tools.id_fraggraph.graphstream.SubFormulaEdge;
import io.github.mzmine.modules.visualization.networking.visual.NetworkPane;
import io.github.mzmine.util.FormulaUtils;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Node;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

class FragmentGraphBuilder extends FxViewBuilder<FragmentGraphModel> {

  private static final Logger logger = Logger.getLogger(FragmentGraphBuilder.class.getName());

  FragmentGraphBuilder(FragmentGraphModel model) {
    super(model);
  }

  @Override
  public Region build() {
    final BorderPane pane = new BorderPane();
    addGrapListenerForNetworkUpdate(pane);

    final TextField formulaField = createBoundFormulaTextField();
    final Label formulaMassLabel = createBoundFormulaMassLabel();

    final Accordion settingsAccordion = newAccordion(true, newTitledPane("Precursor settings",
        newHBox(new Label("Precursor formula: "), formulaField, new Label("m/z:"),
            formulaMassLabel)));

    pane.setTop(settingsAccordion);

    return pane;
  }

  @NotNull
  private Label createBoundFormulaMassLabel() {
    final Label formulaMassLabel = new Label();
    formulaMassLabel.textProperty().bind(Bindings.createStringBinding(() -> {
      if (model.getPrecursorFormula() == null) {
        return "Cannot parse formula";
      }
      final double mz = FormulaUtils.calculateMzRatio(model.getPrecursorFormula());
      return ConfigService.getGuiFormats().mz(mz);
    }, model.precursorFormulaProperty()));
    return formulaMassLabel;
  }

  @NotNull
  private TextField createBoundFormulaTextField() {
    final TextField formulaField = new TextField();
    formulaField.editableProperty().bind(model.precursorFormulaEditableProperty());

    Bindings.bindBidirectional(formulaField.textProperty(), model.precursorFormulaProperty(),
        new StringConverter<>() {
          @Override
          public String toString(IMolecularFormula object) {
            if (object == null) {
              return "";
            }
            return MolecularFormulaManipulator.getString(object);
          }

          @Override
          public IMolecularFormula fromString(String string) {
            final IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula(string);
            return formula;
          }
        });
    return formulaField;
  }

  private void addGrapListenerForNetworkUpdate(BorderPane pane) {
    model.graphProperty().addListener((_, _, graph) -> {
      pane.setCenter(null);
      if (graph == null) {
        return;
      }
      NetworkPane network = new NetworkPane(graph.getId(), false, graph);
      network.showFullGraph();

      model.getAllNodes().forEach(nodeModel -> {
        nodeModel.clearPassThroughGraphs();
        nodeModel.addPassThroughGraph(network.getGraphicGraph());
      });
      // update mappings to filtered nodes
//      network.getGraph().getEdgeFilteredGraph().nodes().forEach(node -> {
//        final SignalFormulaeModel nodeModel = model.getAllNodes().get(node.getId());
//        if (nodeModel != null) {
//          nodeModel.setFilteredNode(node);
//        }
//      });

      pane.setCenter(network);

      network.getSelectedNodes().addListener((ListChangeListener<Node>) c -> {
        c.next();
        final ObservableList<? extends Node> selected = c.getList();
        var selectedNodes = selected.stream().map(Element::getId)
            .map(id -> model.getAllNodesMap().get(id)).filter(Objects::nonNull).toList();
        model.selectedNodesProperty().setAll(selectedNodes);
        logger.finest(() -> STR."Selected nodes: \{selectedNodes.toString()}");
      });

      network.getSelectedEdges().addListener((ListChangeListener<? super Edge>) c -> {
        c.next();
        final ObservableList<? extends Edge> selected = c.getList();
        var selectedEdges = selected.stream().map(Element::getId)
            .map(id -> model.getAllEdgesMap().get(id)).filter(Objects::nonNull).toList();
        model.getSelectedEdges().setAll(selectedEdges);
        logger.finest(() -> STR."Selected edges: \{selectedEdges.toString()}");
      });
    });
  }
}
