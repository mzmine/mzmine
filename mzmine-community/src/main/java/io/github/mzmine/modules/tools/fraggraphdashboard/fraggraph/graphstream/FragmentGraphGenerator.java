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

package io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream;

import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.SignalWithFormulae;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FragmentGraphGenerator {

  private static final Logger logger = Logger.getLogger(FragmentGraphGenerator.class.getName());
  private static final int MAX_NODE_SIZE = 50;
  private static final int MIN_NODE_SIZE = 10;
  private final List<SignalWithFormulae> signalsWithFormulae;
  private final Map<SignalWithFormulae, SignalFormulaeModel> nodeModelMap = new HashMap<>();
  private final SignalWithFormulae root;
  private final MultiGraph graph;
  private final NumberFormat nodeNameFormatter = new DecimalFormat("0.00000");
  private List<SubFormulaEdge> edges;

  /**
   * @param graphId             The id for the built graph.
   * @param signalsWithFormulae A list of all signals in the ms2 spectrum with their corresponding
   *                            formulae.
   * @param root                The root signal = precursor. If none of the
   *                            {@param signalsWithFormulae} contain the (first) precursor formula
   *                            (which may happen if the precursor is completely fragmented in the
   *                            ms2), this will be added and used as a root node.
   */
  public FragmentGraphGenerator(String graphId, List<SignalWithFormulae> signalsWithFormulae,
      SignalWithFormulae root) {
    this.signalsWithFormulae = new ArrayList<>(
        signalsWithFormulae.stream().filter(pf -> !pf.formulae().isEmpty()).toList());
    this.root = root;
    if (!this.signalsWithFormulae.contains(root)) {
      this.signalsWithFormulae.add(root);
    }

    var nodeSizeMap = calculateNormalizedNodeSizes();

    System.setProperty("org.graphstream.ui", "javafx");
    graph = new MultiGraph(graphId);

    generateNodes(nodeSizeMap);
    addEdges();

//    graph.nodes().forEach(node -> {
//      final SignalFormulaeModel model = nodeModelMap.computeIfAbsent(peakWithFormulae,
//          pwf -> new SignalFormulaeModel(newNode, peakWithFormulae));
//    });
  }

  private Map<SignalWithFormulae, Double> calculateNormalizedNodeSizes() {
    Map<SignalWithFormulae, Double> nodeSizes = new HashMap<>();
    final double minMz = signalsWithFormulae.stream().mapToDouble(swf -> swf.peak().getMZ()).min()
        .orElse(0);
    final double maxMz =
        signalsWithFormulae.stream().mapToDouble(swf -> swf.peak().getMZ()).max().orElse(100)
            - minMz;

    for (SignalWithFormulae swf : signalsWithFormulae) {
      final double size = MIN_NODE_SIZE + MAX_NODE_SIZE * (swf.peak().getMZ() - minMz) / maxMz;
      nodeSizes.put(swf, size);
    }
    return nodeSizes;
  }

  private void generateNodes(Map<SignalWithFormulae, Double> nodeSizeMap) {
    logger.finest(
        () -> "Generating nodes fragment graph of precursor %s".formatted(root.toString()));

    boolean rootFound = false;
    for (SignalWithFormulae signalWithFormulae : signalsWithFormulae) {
      final Node node = getOrCreateNode(signalWithFormulae);
      if (!rootFound && signalWithFormulae.formulae().contains(root.formulae().getFirst())) {
        node.setAttribute("ui.class", "root_fragment");
        rootFound = true;
      }
      node.setAttribute("ui.size", nodeSizeMap.get(signalWithFormulae));
    }
  }

  private void addEdges() {
    logger.finest(() -> "Generating edges for fragment graph of %s".formatted(root.toString()));

    final SubFormulaEdgeGenerator edgeGenerator = new SubFormulaEdgeGenerator(
        nodeModelMap.values().stream().toList(), nodeNameFormatter);
    edges = edgeGenerator.getEdges();

    for (SubFormulaEdge edge : edges) {
      final Edge graphEdge = graph.addEdge(edge.getId(), getNode(edge.smaller()),
          getNode(edge.larger()), true);
      edge.addGraph(graph, true);
    }
  }

  @Nullable
  private Node getNode(SignalWithFormulae signalWithFormulae) {
    return graph.getNode(toNodeName(signalWithFormulae));
  }

  @Nullable
  private Node getNode(SignalFormulaeModel model) {
    return graph.getNode(toNodeName(model.getPeakWithFormulae()));
  }

  @NotNull
  private Node getOrCreateNode(SignalWithFormulae signalWithFormulae) {
    final Node node = getNode(signalWithFormulae);
    if (node != null) {
      return node;
    }

    final Node newNode = graph.addNode(toNodeName(signalWithFormulae));
    final SignalFormulaeModel model = new SignalFormulaeModel(newNode, signalWithFormulae);
    nodeModelMap.put(signalWithFormulae, model);

    // todo: some magic to select a good formula
    return newNode;
  }

  /**
   * Maps the peak to a node name. Uses the mz rather than the index so peaks can be deleted from
   * the network.
   *
   * @return The node name -> mz formatted to 5 decimal digits
   */
  public String toNodeName(SignalWithFormulae peak) {
    return nodeNameFormatter.format(peak.peak().getMZ());
  }

  public String toEdgeName(SignalWithFormulae a, SignalWithFormulae b) {
    SignalWithFormulae smaller = a.peak().getMZ() < b.peak().getMZ() ? a : b;
    SignalWithFormulae larger = a.peak().getMZ() > b.peak().getMZ() ? a : b;

    return "%s-%s".formatted(nodeNameFormatter.format(smaller.peak().getMZ()),
        nodeNameFormatter.format(larger.peak().getMZ()));
  }

  public MultiGraph getGraph() {
    return graph;
  }

  public Map<SignalWithFormulae, SignalFormulaeModel> getNodeModelMap() {
    return nodeModelMap;
  }

  public List<SubFormulaEdge> getEdges() {
    return edges;
  }
}
