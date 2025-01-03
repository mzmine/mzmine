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

package io.github.mzmine.modules.visualization.networking.visual;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.R2RNetworkingMaps;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.types.annotations.GNPSSpectralLibraryMatchesType;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.datamodel.identities.iontype.networks.IonNetworkRelation;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch.ATT;
import io.github.mzmine.modules.visualization.networking.visual.enums.EdgeAtt;
import io.github.mzmine.modules.visualization.networking.visual.enums.EdgeType;
import io.github.mzmine.modules.visualization.networking.visual.enums.ElementType;
import io.github.mzmine.modules.visualization.networking.visual.enums.NodeAtt;
import io.github.mzmine.modules.visualization.networking.visual.enums.NodeType;
import io.github.mzmine.util.GraphStreamUtils;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.jetbrains.annotations.NotNull;

public class FeatureNetworkGenerator {

  private static final Logger logger = Logger.getLogger(FeatureNetworkGenerator.class.getName());
  private final NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
  private final NumberFormat rtForm = MZmineCore.getConfiguration().getRTFormat();
  private final NumberFormat scoreForm = MZmineCore.getConfiguration().getScoreFormat();
  private final NumberFormat intensityForm = MZmineCore.getConfiguration().getIntensityFormat();

  private Object2IntMap<ElementType> elementsOfType = new Object2IntOpenHashMap<>();
  private MultiGraph graph;
  private Node neutralNode;
  private boolean ms1FeatureShapeEdges;


  public MultiGraph createNewGraph(FeatureList flist, boolean useIonIdentity,
      boolean onlyBestIonIdentityNet, boolean ms1FeatureShapeEdges) {
    return createNewGraph(flist.getName(), flist, useIonIdentity, onlyBestIonIdentityNet,
        ms1FeatureShapeEdges);
  }

  public MultiGraph createNewGraph(String graphName, FeatureList flist, boolean useIonIdentity,
      boolean onlyBestIonIdentityNet, boolean ms1FeatureShapeEdges) {
    return createNewGraph(graphName, flist.getRows(), useIonIdentity, onlyBestIonIdentityNet,
        flist.getRowMaps(), ms1FeatureShapeEdges);
  }

  public MultiGraph createNewGraph(List<FeatureListRow> rows, boolean useIonIdentity,
      boolean onlyBestIonIdentityNet, @NotNull R2RNetworkingMaps relationsMaps,
      boolean ms1FeatureShapeEdges) {
    return createNewGraph("molnet", rows, useIonIdentity, onlyBestIonIdentityNet, relationsMaps,
        ms1FeatureShapeEdges);
  }

  public MultiGraph createNewGraph(String graphName, List<FeatureListRow> rows,
      boolean useIonIdentity, boolean onlyBestIonIdentityNet,
      @NotNull R2RNetworkingMaps relationsMaps, boolean ms1FeatureShapeEdges) {
    this.graph = new MultiGraph(graphName);
    this.ms1FeatureShapeEdges = ms1FeatureShapeEdges;
    logger.info("Adding all annotations to a network");
    if (rows != null) {
      AtomicInteger added = new AtomicInteger(0);

      if (useIonIdentity) {
        // ion identity networks are currently not covered in the relations maps
        // add all IIN
        IonNetwork[] nets = IonNetworkLogic.getAllNetworks(rows, onlyBestIonIdentityNet);
        for (IonNetwork net : nets) {
          addIonNetwork(net, added);
        }

        // add relations
        addNetworkRelationsEdges(nets);
      }

      // add all types of row 2 row relation ships:
      // cosine similarity etc
      addRelationshipEdges(relationsMaps);

      if (useIonIdentity) {
        // connect representative edges to neutral molecule nodes from IINs
        addConsensusEdgesToMoleculeNodes();
      }

      // add gnps library matches to nodes
      addGNPSLibraryMatchesToNodes(rows);

      // add missing row nodes
      for (final FeatureListRow row : rows) {
        if (row.hasMs2Fragmentation()) {
          getRowNode(row, true);
        }
      }

      // add id name
      for (Node node : graph) {
        GraphStreamUtils.getUiClass(node).ifPresent(uiClass -> {
          node.setAttribute("ui.class", uiClass);
        });

        String l = (String) node.getAttribute(NodeAtt.LABEL.toString());
        if (l != null) {
          node.setAttribute("ui.label", l);
        }
      }
      logger.info("Added " + added.get() + " connections");
    }
    return graph;
  }


  /**
   * Last step to add consensus edges for each EdgeType to the neutral molecule node of each IIN.
   */
  private void addConsensusEdgesToMoleculeNodes() {
    HashSet<IonNetwork> finalizedNetworks = new HashSet<>();
    List<ConsensusEdge> consensusEdges = new ArrayList<>();

    for (Node node : graph) {
      IonNetwork net = getIonNetwork(node);
      if (net == null || finalizedNetworks.contains(net)) {
        // never do ion networks twice
        continue;
      } else {
        Node mnode = getNeutralMolNode(net, false);
        if (mnode == null) {
          continue;
        }
        consensusEdges.clear();
        //
        for (FeatureListRow row : net.keySet()) {
          Node rowNode = getRowNode(row, false);
          rowNode.setAttribute("FeatureListNode", row);
          rowNode.edges().forEach(edge -> {
            EdgeType edgeType = edge.getAttribute(EdgeAtt.TYPE.toString(), EdgeType.class);
            if (edgeType != null && edgeType != EdgeType.ION_IDENTITY) {
              // find the second node
              final Node secondNode = findSecondNodeConnectedTo(net, edge);
              if (secondNode != null) {
                // compare to best edge of this type
                boolean added = false;
                for (ConsensusEdge consensusEdge : consensusEdges) {
                  if (consensusEdge.matches(mnode, secondNode, edge)) {
                    consensusEdge.add(edge);
                    added = true;
                    break;
                  }
                }
                if (!added) {
                  consensusEdges.add(new ConsensusEdge(mnode, secondNode, edge));
                }
              }
            }
          });
        }
        // Add consensus edges
        for (ConsensusEdge e : consensusEdges) {
          Edge edge = addNewEdge(e.getA(), e.getB(), e.getType(), e.getAnnotation(), false);
          edge.setAttribute(EdgeAtt.SCORE.toString(), scoreForm.format(e.getScore()));
          edge.setAttribute(EdgeAtt.TYPE_STRING.toString(), e.getTypeString());
        }
        // set network as finalized
        finalizedNetworks.add(net);
      }
    }
  }

  /**
   * Finds the representative node connected to this IonNetwork
   *
   * @param net  the base ion network
   * @param edge the edge of an ion identity (feature) of net to another node
   * @return A node or null
   */
  private Node findSecondNodeConnectedTo(IonNetwork net, Edge edge) {
    IonNetwork netA = getIonNetwork(edge.getNode0());
    if (netA == null) {
      // found a node without IIN
      return edge.getNode0();
    } else if (!net.equals(netA)) {
      // found another IIN: only add if net has smallest ID to avoid duplicate edges
      return net.getID() < netA.getID() ? getNeutralMolNode(netA, false) : null;
    }

    netA = getIonNetwork(edge.getNode1());
    if (netA == null) {
      // found a node without IIN
      return edge.getNode1();
    } else if (!net.equals(netA)) {
      // found another IIN: only add if net has smallest ID to avoid duplicate edges
      return net.getID() < netA.getID() ? getNeutralMolNode(netA, false) : null;
    }
    return null;
  }

  private IonNetwork getIonNetwork(Node node) {
    FeatureListRow row = node.getAttribute(NodeAtt.ROW.toString(), FeatureListRow.class);
    if (row != null) {
      IonIdentity ion = row.getBestIonIdentity();
      return ion != null ? ion.getNetwork() : null;
    }
    return null;
  }

  /**
   * Get the attribute value or null
   *
   * @param element      node, edge, or sprite element
   * @param attribute    the attribute
   * @param defaultValue the default value in case of null
   * @return the value or default value in case of null
   */
  <T> T get(Element element, EdgeAtt attribute, T defaultValue) {
    T value = (T) element.getAttribute(attribute.toString(), defaultValue.getClass());
    return value == null ? defaultValue : value;
  }

  /**
   * Delete all ion feature nodes that are represented by a neutral M node
   */
  public void deleteAllCollapsedNodes() {
    for (int i = 0; i < graph.getNodeCount(); ) {
      NodeType type = (NodeType) graph.getNode(i).getAttribute(NodeAtt.TYPE.toString());
      if (type.equals(NodeType.ION_FEATURE)) {
        graph.removeNode(i);
      } else {
        i++;
      }
    }
  }

  private void addGNPSLibraryMatchesToNodes(List<FeatureListRow> rows) {
    int n = 0;
    for (FeatureListRow r : rows) {
      final List<GNPSLibraryMatch> matches = r.get(GNPSSpectralLibraryMatchesType.class);
      if (matches == null || matches.isEmpty()) {
        continue;
      }
      GNPSLibraryMatch identity = matches.get(0);

      if (identity != null) {
        n++;
        Node node = getRowNode(r, true);
        identity.getResults().entrySet().stream().filter(e -> e.getValue() != null).forEach(e -> {
          // row node
          node.setAttribute(e.getKey(), e.getValue());
        });
      }
    }
    logger.info("Added " + n + " GNPS library matches to their respective nodes");
  }

  /**
   * Add all row-2-row relationship edges. (e.g., MS2 cosine similarity edges)
   */
  private void addRelationshipEdges(R2RNetworkingMaps relationsMaps) {
    if (relationsMaps == null || relationsMaps.isEmpty()) {
      return;
    }
    for (Entry<String, R2RMap<RowsRelationship>> entry : relationsMaps.getRowsMaps().entrySet()) {
      R2RMap<RowsRelationship> r2rMap = entry.getValue();
      // do not add MS1 correlation
      if (r2rMap == null) {
        continue;
      }
      for (RowsRelationship rel : r2rMap.values()) {
        if (rel != null) {
          addMS2SimEdges(rel.getRowA(), rel.getRowB(), rel);
        }
      }
    }
  }

  private void addMS2SimEdges(FeatureListRow ra, FeatureListRow rb, RowsRelationship sim) {
    Node a = getRowNode(ra, true);
    Node b = getRowNode(rb, true);
    addMS2SimEdges(a, b, sim, deltaMZ(ra, rb));
  }

  private void addMS2SimEdges(Node a, Node b, RowsRelationship sim, double dmz) {
    EdgeType type = EdgeType.of(sim.getType());
    double score = sim.getScore();
    Edge edge = addNewEdge(a, b, type, sim.getAnnotation(), false, dmz);
    edge.setAttribute(EdgeAtt.LABEL.toString(), sim.getAnnotation());
    edge.setAttribute(EdgeAtt.SCORE.toString(), scoreForm.format(score));
    // replace type here with the string to also capture external types
    edge.setAttribute(EdgeAtt.TYPE_STRING.toString(), sim.getType());
    // weight for layout
    setEdgeWeightQuadraticScore(edge, score);

    switch (type) {
      case MS2_MODIFIED_COSINE, GNPS_MODIFIED_COSINE, MS2Deepscore, DREAMS ->
          edge.setAttribute("ui.size", (float) Math.max(1, Math.min(5, 5 * score * score)));
      case FEATURE_SHAPE_CORRELATION ->
          edge.setAttribute("ui.size", (float) Math.max(1, Math.min(5, 5 * score * score)));
    }
  }

  private double deltaMZ(FeatureListRow a, FeatureListRow b) {
    return Math.abs(a.getAverageMZ() - b.getAverageMZ());
  }

  /**
   * Adds all relational edges between networks
   */
  private void addNetworkRelationsEdges(IonNetwork[] nets) {
    for (IonNetwork net : nets) {
      if (net.getRelations() != null) {

        net.getRelations().values().stream()
            // only do it once
            .filter(rel -> rel.isLowestIDNetwork(net)).forEach(this::addRelationEdges);
      }
    }
  }

  /**
   * Adds all the edges of an relation between the networks
   */
  private void addRelationEdges(IonNetworkRelation rel) {
    IonNetwork[] nets = rel.getAllNetworks();
    for (int i = 0; i < nets.length - 1; i++) {
      IonNetwork netA = nets[i];
      for (int j = i + 1; j < nets.length; j++) {
        IonNetwork netB = nets[j];
        Node a = getNeutralMolNode(netA, false);
        Node b = getNeutralMolNode(netB, false);
        if (a != null && b != null) {
          // b has higher mass
          if (netA.getNeutralMass() > netB.getNeutralMass()) {
            Node tmp = a;
            a = b;
            b = tmp;
          }
          double dmz = Math.abs(netA.getNeutralMass() - netB.getNeutralMass());
          String edgeLabel = rel.getDescription();
          addNewEdge(a, b, EdgeType.NETWORK_RELATIONS, edgeLabel, true, dmz);
        }
      }
    }
  }

  /**
   * Add ion identity networks. Creates a neutral molecule node as the center and connects all ions
   * with it.
   *
   * @param net   the ion identity network
   * @param added counts the added nodes
   */
  private void addIonNetwork(IonNetwork net, AtomicInteger added) {
    // create a neutral molecule node if the neutral mass is known
    Node mnode = !net.isUndefined() ? getNeutralMolNode(net, true) : null;
    // bundle all neutral losses together (ion identities where the exact adduct is unknown,e.g.,
    // if delta mz=18 between two nodes annotates one as [M-H2O+?] and the other as [M+?]
    Node neutralNode = getNeutralLossNode();

    // add center neutral M
    net.forEach((key, value) -> {
      Node node = getRowNode(key);

      if (value.getIonType().isModifiedUndefinedAdduct()) {
        // neutral
        addNewEdge(neutralNode, node, EdgeType.ION_IDENTITY, "", false, 0);
      } else if (!value.getIonType().isUndefinedAdduct() && mnode != null) {
        addNewDeltaMZEdge(node, mnode, EdgeType.ION_IDENTITY,
            Math.abs(net.getNeutralMass() - key.getAverageMZ()));
      }
      added.incrementAndGet();
    });
    // add all edges between ions
    List<FeatureListRow> rows = new ArrayList<>(net.keySet());
    for (int i = 0; i < rows.size() - 1; i++) {
      FeatureListRow row = rows.get(i);
      Node rowNode = getRowNode(row);
      for (int j = i + 1; j < rows.size(); j++) {
        FeatureListRow prow = rows.get(j);

        Node node1 = rowNode;
        Node node2 = getRowNode(prow);
        // node2 has to have higher mass (directed edge)
        if (row.getAverageMZ() > prow.getAverageMZ()) {
          Node tmp = node1;
          node1 = node2;
          node2 = tmp;
        }
        // add directed edge
        addNewDeltaMZEdge(node1, node2, EdgeType.ION_IDENTITY,
            Math.abs(row.getAverageMZ() - prow.getAverageMZ()));
        added.incrementAndGet();
      }
    }
  }

  private Node getNeutralLossNode() {
    if (neutralNode == null) {
      neutralNode = graph.addNode("NEUTRAL LOSSES");
      var type = NodeType.NEUTRAL_LOSS_CENTER;
      neutralNode.setAttribute("ui.class", type.getUiClass().orElse(""));
      neutralNode.setAttribute(NodeAtt.TYPE.toString(), type);
      neutralNode.setAttribute(NodeAtt.ID.toString(), "NEUTRAL_LOSS_NODE");
    }
    return neutralNode;
  }

  private Node createNode(NodeType type) {
    return createNode(type, type.toString() + getNextIndex(type));
  }

  private Node createNode(NodeType type, String id) {
    Node node = graph.addNode(id);
    node.setAttribute(NodeAtt.TYPE.toString(), type);
    String uiClass = type.getUiClass().orElse(null);
    if (uiClass != null) {
      node.setAttribute("ui.class", uiClass);
    }
    return node;
  }

  private int getNextIndex(final NodeType type) {
    return elementsOfType.computeInt(type, (t, count) -> count == null ? 0 : count + 1);
  }

  /**
   * Creates or gets the neutral mol node of this net
   */
  private Node getNeutralMolNode(IonNetwork net, boolean createNew) {
    if (net == null) {
      return null;
    }

    String name = MessageFormat.format("M (m={0} Da) Net{1} corrID={2}",
        mzForm.format(net.getNeutralMass()), net.getID(), net.getCorrID());

    String nodeId = "Net" + net.getID();
    Node node = graph.getNode(nodeId);
    if (node == null && createNew) {
      node = graph.addNode(nodeId);
      node.setAttribute(NodeAtt.ID.toString(), nodeId);
      node.setAttribute(NodeAtt.TYPE.toString(), NodeType.NEUTRAL_M);
      node.setAttribute(NodeAtt.LABEL.toString(), name);
      node.setAttribute("ui.label", name);
      node.setAttribute(NodeAtt.IIN_ID.toString(), net.getID());
      node.setAttribute(NodeAtt.RT.toString(), rtForm.format(net.getAvgRT()));
      node.setAttribute(NodeAtt.NEUTRAL_MASS.toString(), mzForm.format(net.getNeutralMass()));
      node.setAttribute(NodeAtt.MAX_INTENSITY.toString(), intensityForm.format(net.getHeightSum()));

      final SpectralDBAnnotation bestMatch = net.keySet().stream()
          .map(FeatureListRow::getSpectralLibraryMatches).flatMap(List::stream)
          .max(Comparator.comparingDouble(a -> a.getSimilarity().getScore())).orElse(null);
      if (bestMatch != null) {
        double score = bestMatch.getSimilarity().getScore();
        node.setAttribute(NodeAtt.LIB_MATCH.toString(), bestMatch.getCompoundName());
        node.setAttribute(NodeAtt.COMPOUND_NAME.toString(),
            bestMatch.getEntry().getOrElse(DBEntryField.NAME, ""));
        node.setAttribute(NodeAtt.ANNOTATION_SCORE.toString(), scoreForm.format(score));
        node.setAttribute(NodeAtt.EXPLAINED_INTENSITY.toString(),
            scoreForm.format(bestMatch.getSimilarity().getExplainedLibraryIntensity()));
      }

      // add best GNPS match to node
      final GNPSLibraryMatch bestGNPS = net.keySet().stream()
          .map(row -> row.get(GNPSSpectralLibraryMatchesType.class)).filter(Objects::nonNull)
          .flatMap(List::stream)
          .max(Comparator.comparingDouble(a -> a.getResultOr(ATT.LIBRARY_MATCH_SCORE, 0d)))
          .orElse(null);
      if (bestGNPS != null) {
        final Node thisNode = node;
        bestGNPS.getResults().entrySet().stream().filter(e -> e.getValue() != null).forEach(e -> {
          // row node
          thisNode.setAttribute(e.getKey(), e.getValue());
        });
      }

      // all intensitites of all iontypes
      for (Entry<FeatureListRow, IonIdentity> e : net.entrySet()) {
        IonIdentity ion = e.getValue();
        node.setAttribute("Intensity(" + ion.getIonType().toString(false) + ")",
            e.getKey().getBestFeature().getHeight());
      }

      MolecularFormulaIdentity formula = net.getBestMolFormula();
      if (formula != null) {
        node.setAttribute(NodeAtt.FORMULA.toString(), formula.getFormulaAsString());
      }
    }

    return node;
  }


  public String toNodeName(FeatureListRow row) {
    return String.valueOf(row.getID());
  }

  public Node getRowNode(FeatureListRow row, boolean addMissing) {
    Node node = graph.getNode(toNodeName(row));
    if (addMissing && node == null) {
      node = getRowNode(row);
    }
    return node;
  }

  /**
   * @param row feature list row
   * @return the old or new node
   */
  private Node getRowNode(FeatureListRow row) {
    Node node = graph.getNode(toNodeName(row));
    if (node != null) {
      return node;
    }
    node = graph.addNode(toNodeName(row));

    for (final NodeAtt att : NodeAtt.values()) {
      Object value = att.getFormattedValue(row, false);
      if (value != null) {
        node.setAttribute(att.toString(), value);
      }
    }
    node.setAttribute("ui.label", node.getAttribute(NodeAtt.LABEL.toString()));
    return node;
  }

  private Edge addNewDeltaMZEdge(Node node1, Node node2, EdgeType type, double dmz) {
    return addNewEdge(node1, node2, type, "Δ" + mzForm.format(dmz), true, dmz);
  }

  public Edge addNewEdge(Node node1, Node node2, EdgeType type, Object label, boolean directed,
      double dmz) {
    String uiClass = type.getUiClass().orElse(null);
    Edge e = addNewEdge(node1, node2, type.toString(), label, directed, uiClass);
    e.setAttribute(EdgeAtt.TYPE.toString(), type);
    e.setAttribute(EdgeAtt.TYPE_STRING.toString(), type.toString());
    e.setAttribute(EdgeAtt.DELTA_MZ.toString(), mzForm.format(dmz));
    if (type == EdgeType.ION_IDENTITY) {
      setEdgeWeight(e, 0.25);
    }
    return e;
  }

  private void setEdgeWeightQuadraticScore(final Edge edge, final double score) {
    double weight = 0.2 + Math.pow(1d - score, 2) * 6d;
    setEdgeWeight(edge, weight);
  }

  private void setEdgeWeight(final Edge edge, final double weight) {
    edge.setAttribute("layout.weight", weight);
  }

  public Edge addNewEdge(Node node1, Node node2, EdgeType type, Object label, boolean directed) {
    String uiClass = type.getUiClass().orElse(null);
    Edge e = addNewEdge(node1, node2, type.toString(), label, directed, uiClass);
    e.setAttribute(EdgeAtt.TYPE.toString(), type);
    e.setAttribute(EdgeAtt.TYPE_STRING.toString(), type.toString());
    return e;
  }

  public Edge addNewEdge(Node node1, Node node2, String edgeNameSuffix, Object label,
      boolean directed, String uiClass) {
    String edge = node1.getId() + node2.getId() + edgeNameSuffix;
    Edge e = graph.getEdge(edge);
    if (e == null) {
      e = graph.addEdge(edge, node1, node2, directed);
      e.setAttribute(EdgeAtt.ID1.toString(), node1.getAttribute(NodeAtt.ID.toString()));
      e.setAttribute(EdgeAtt.ID2.toString(), node2.getAttribute(NodeAtt.ID.toString()));
    }
    e.setAttribute("ui.label", label);
    e.setAttribute(EdgeAtt.LABEL.toString(), label);
    if (uiClass != null && !uiClass.isBlank()) {
      e.setAttribute("ui.class", uiClass);
    }
    return e;
  }
}
