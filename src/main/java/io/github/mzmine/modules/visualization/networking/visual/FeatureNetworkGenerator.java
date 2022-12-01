/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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


import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.types.annotations.GNPSSpectralLibraryMatchesType;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.datamodel.identities.iontype.networks.IonNetworkRelation;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch.ATT;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

public class FeatureNetworkGenerator {

  private static final Logger logger = Logger.getLogger(FeatureNetworkGenerator.class.getName());
  private final NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
  private final NumberFormat rtForm = MZmineCore.getConfiguration().getRTFormat();
  private final NumberFormat scoreForm = MZmineCore.getConfiguration().getScoreFormat();
  private final NumberFormat intensityForm = MZmineCore.getConfiguration().getIntensityFormat();

  private Graph graph;
  private Map<Type, R2RMap<RowsRelationship>> relationsMaps;
  private Node neutralNode;
  private boolean ms1FeatureShapeEdges;


  public void createNewGraph(FeatureListRow[] rows, Graph graph, boolean onlyBestNetworks,
      Map<Type, R2RMap<RowsRelationship>> relationsMaps, boolean ms1FeatureShapeEdges) {
    this.relationsMaps = relationsMaps;
    this.graph = graph;
    this.ms1FeatureShapeEdges = ms1FeatureShapeEdges;
    logger.info("Adding all annotations to a network");
    if (rows != null) {
      // ion identity networks are currently not covered in the relations maps
      // add all IIN
      IonNetwork[] nets = IonNetworkLogic.getAllNetworks(Arrays.asList(rows), onlyBestNetworks);

      AtomicInteger added = new AtomicInteger(0);
      for (IonNetwork net : nets) {
        addIonNetwork(net, added);
      }

      // add relations
      addNetworkRelationsEdges(nets);

      // add all types of row 2 row relation ships:
      // cosine similarity etc
      addRelationshipEdges(relationsMaps);

      // connect representative edges to neutral molecule nodes from IINs
      addConsensusEdgesToMoleculeNodes(relationsMaps);

      // add gnps library matches to nodes
      addGNPSLibraryMatchesToNodes(rows);

      // add id name
      for (Node node : graph) {
        if (node.getId().startsWith("Net")) {
          node.setAttribute("ui.class", "MOL");
        }
        if (node.getId().equals("NEUTRAL LOSSES")) {
          node.setAttribute("ui.class", "NEUTRAL");
        }

        String l = (String) node.getAttribute(NodeAtt.LABEL.toString());
        if (l != null) {
          node.setAttribute("ui.label", l);
        }
      }
      logger.info("Added " + added.get() + " connections");
    }
  }

  /**
   * Last step to add consensus edges for each EdgeType to the neutral molecule node of each IIN.
   *
   * @param relationsMaps
   */
  private void addConsensusEdgesToMoleculeNodes(Map<Type, R2RMap<RowsRelationship>> relationsMaps) {
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
          edge.setAttribute(EdgeAtt.NUMBER_OF_COLLAPSED_EDGES.toString(), e.getNumberOfEdges());
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

  private void addGNPSLibraryMatchesToNodes(FeatureListRow[] rows) {
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
   *
   * @param relationsMaps
   */
  private void addRelationshipEdges(Map<Type, R2RMap<RowsRelationship>> relationsMaps) {
    if (relationsMaps == null || relationsMaps.isEmpty()) {
      return;
    }
    for (Entry<Type, R2RMap<RowsRelationship>> entry : relationsMaps.entrySet()) {
      R2RMap<RowsRelationship> r2rMap = entry.getValue();
      // do not add MS1 correlation
      if (r2rMap != null && (ms1FeatureShapeEdges || !entry.getKey()
          .equals(Type.MS1_FEATURE_CORR))) {
        for (RowsRelationship rel : r2rMap.values()) {
          if (rel != null) {
            addMS2SimEdges(rel.getRowA(), rel.getRowB(), rel);
          }
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
    switch (type) {
      case MS2_SIMILARITY, MS2_GNPS_COSINE_SIM -> edge
          .setAttribute("ui.size", (float) Math.max(1, Math.min(5, 5 * score * score)));
      case FEATURE_CORRELATION -> edge
          .setAttribute("ui.size", (float) Math.max(1, Math.min(5, 5 * score * score)));
    }
  }

  private double deltaMZ(FeatureListRow a, FeatureListRow b) {
    return Math.abs(a.getAverageMZ() - b.getAverageMZ());
  }

  private String getUIClass(EdgeType type) {
    return switch (type) {
      case ION_IDENTITY -> "IIN";
      case MS2_SIMILARITY,
          MS2_SIMILARITY_NEUTRAL_M,
          MS2_SIMILARITY_NEUTRAL_M_TO_FEATURE -> "COSINE";
      case MS2_GNPS_COSINE_SIM -> "GNPS";
      case FEATURE_CORRELATION -> "FEATURECORR";
      case NETWORK_RELATIONS -> "IINREL";
    };
  }

  /**
   * Adds all relational edges between networks
   *
   * @param nets
   */
  private void addNetworkRelationsEdges(IonNetwork[] nets) {
    for (IonNetwork net : nets) {
      if (net.getRelations() != null) {

        net.getRelations().entrySet().stream().map(Entry::getValue)
            // only do it once
            .filter(rel -> rel.isLowestIDNetwork(net)).forEach(rel -> addRelationEdges(rel));
      }
    }
  }

  /**
   * Adds all the edges of an relation between the networks
   *
   * @param rel
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
    net.entrySet().stream().forEach(e -> {
      Node node = getRowNode(e.getKey(), e.getValue());

      if (e.getValue().getIonType().isModifiedUndefinedAdduct()) {
        // neutral
        addNewEdge(neutralNode, node, EdgeType.ION_IDENTITY, "", false, 0);
      } else if (!e.getValue().getIonType().isUndefinedAdduct() && mnode != null) {
        addNewDeltaMZEdge(node, mnode, EdgeType.ION_IDENTITY,
            Math.abs(net.getNeutralMass() - e.getKey().getAverageMZ()));
      }
      added.incrementAndGet();
    });
    // add all edges between ions
    net.entrySet().stream().forEach(e -> {
      FeatureListRow row = e.getKey();
      Node rowNode = getRowNode(row, e.getValue());

      e.getValue().getPartner().entrySet().stream().filter(Objects::nonNull).forEach(partner -> {
        FeatureListRow prow = partner.getKey();
        IonIdentity link = partner.getValue();
        // do only once (for row with smaller index)
        if (prow != null && link != null && row.getID() < prow.getID()) {
          Node node1 = rowNode;
          Node node2 = getRowNode(prow, link);
          // node2 has to have higher mass (directed edge)
          if (row.getAverageMZ() > prow.getAverageMZ()) {
            Node tmp = node1;
            node1 = node2;
            node2 = tmp;
          }
          // add directed edge
          addNewDeltaMZEdge(node1, node2, EdgeType.ION_IDENTITY,
              Math.abs(e.getKey().getAverageMZ() - prow.getAverageMZ()));
          added.incrementAndGet();
        }
      });
    });

  }

  private Node getNeutralLossNode() {
    if (neutralNode == null) {
      neutralNode = graph.addNode("NEUTRAL LOSSES");
      neutralNode.setAttribute("ui.class", "NEUTRAL");
      neutralNode.setAttribute(NodeAtt.TYPE.toString(), NodeType.NEUTRAL_LOSS_CENTER);
    }
    return neutralNode;
  }

  /**
   * Creates or gets the neutral mol node of this net
   *
   * @param net
   * @return
   */
  private Node getNeutralMolNode(IonNetwork net, boolean createNew) {
    if (net == null) {
      return null;
    }

    String name = MessageFormat.format("M (m={0} Da) Net{1} corrID={2}",
        mzForm.format(net.getNeutralMass()), net.getID(), net.getCorrID());

    Node node = graph.getNode("Net" + net.getID());
    if (node == null && createNew) {
      node = graph.addNode("Net" + net.getID());
      node.setAttribute(NodeAtt.TYPE.toString(), NodeType.NEUTRAL_M);
      node.setAttribute(NodeAtt.LABEL.toString(), name);
      node.setAttribute("ui.label", name);
      node.setAttribute(NodeAtt.NET_ID.toString(), net.getID());
      node.setAttribute(NodeAtt.RT.toString(), rtForm.format(net.getAvgRT()));
      node.setAttribute(NodeAtt.NEUTRAL_MASS.toString(), mzForm.format(net.getNeutralMass()));
      node.setAttribute(NodeAtt.MAX_INTENSITY.toString(), intensityForm.format(net.getHeightSum()));

      final SpectralDBAnnotation bestMatch = net.keySet().stream()
          .map(FeatureListRow::getSpectralLibraryMatches).flatMap(List::stream).max(
              Comparator.comparingDouble(a -> a.getSimilarity().getScore())).orElse(null);
      if (bestMatch != null) {
        double score = bestMatch.getSimilarity().getScore();
        node.setAttribute(NodeAtt.SPECTRAL_LIB_MATCH_SUMMARY.toString(), bestMatch.getCompoundName());
        node.setAttribute(NodeAtt.SPECTRAL_LIB_MATCH.toString(), bestMatch.getEntry().getOrElse(
            DBEntryField.NAME, ""));
        node.setAttribute(NodeAtt.SPECTRAL_LIB_SCORE.toString(), scoreForm.format(score));
        node.setAttribute(NodeAtt.SPECTRAL_LIB_EXPLAINED_INTENSITY.toString(),
            scoreForm.format(bestMatch.getSimilarity().getExplainedLibraryIntensity()));
      }

      // add best GNPS match to node
      final GNPSLibraryMatch bestGNPS = net.keySet().stream()
          .map(row -> row.get(GNPSSpectralLibraryMatchesType.class)).filter(Objects::nonNull)
          .flatMap(List::stream).max(Comparator.comparingDouble(a -> a.getResultOr(
              ATT.LIBRARY_MATCH_SCORE, 0d))).orElse(null);
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
    return "Row" + row.getID();
  }

  private Node getRowNode(FeatureListRow row, boolean addMissing) {
    Node node = graph.getNode(toNodeName(row));
    if (addMissing && node == null) {
      node = getRowNode(row, null);
    }
    return node;
  }

  /**
   * @param row
   * @param esi only adds ion type info if given as parameter
   * @return
   */
  private Node getRowNode(FeatureListRow row, IonIdentity esi) {
    Node node = graph.getNode(toNodeName(row));
    if (node != null) {
      return node;
    } else {
      String id = "";
      if (esi != null) {
        id = esi.getAdduct();

        // id += " by n=" + esi.getPartnerRowsID().length;
        //
        // if (esi.getNetID() != -1)
        // id += " (Net" + esi.getNetIDString() + ")";
      }
      String label = MessageFormat.format("{0} (mz={1}) {2}", row.getID(),
          mzForm.format(row.getAverageMZ()), id);

      node = graph.addNode(toNodeName(row));
      node.setAttribute(NodeAtt.LABEL.toString(), label);
      node.setAttribute(NodeAtt.ROW.toString(), row);
      node.setAttribute("ui.label", label);
      node.setAttribute(NodeAtt.TYPE.toString(),
          esi != null ? NodeType.ION_FEATURE : NodeType.SINGLE_FEATURE);
      node.setAttribute(NodeAtt.ID.toString(), row.getID());
      if (row.getAverageRT() != null) {
        node.setAttribute(NodeAtt.RT.toString(), rtForm.format(row.getAverageRT()));
      }
      if (row.getAverageMZ() != null) {
        node.setAttribute(NodeAtt.MZ.toString(), mzForm.format(row.getAverageMZ()));
      }
      node.setAttribute(NodeAtt.MAX_INTENSITY.toString(),
          intensityForm.format(row.getBestFeature().getHeight()));
      final double sumIntensity = row.getSumIntensity();
      node.setAttribute(NodeAtt.SUM_INTENSITY.toString(), intensityForm.format(sumIntensity));
      node.setAttribute(NodeAtt.LOG10_SUM_INTENSITY.toString(), Math.log10(sumIntensity));
      node.setAttribute(NodeAtt.CHARGE.toString(), row.getRowCharge());
      node.setAttribute(NodeAtt.GROUP_ID.toString(), row.getGroupID());

      final SpectralDBAnnotation bestMatch = row.getSpectralLibraryMatches().stream().max(
          Comparator.comparingDouble(a -> a.getSimilarity().getScore())).orElse(null);
      if (bestMatch != null) {
        double score = bestMatch.getSimilarity().getScore();
        node.setAttribute(NodeAtt.SPECTRAL_LIB_MATCH_SUMMARY.toString(), bestMatch.getCompoundName());
        node.setAttribute(NodeAtt.SPECTRAL_LIB_MATCH.toString(), bestMatch.getEntry().getOrElse(
            DBEntryField.NAME, ""));
        node.setAttribute(NodeAtt.SPECTRAL_LIB_SCORE.toString(), scoreForm.format(score));
        node.setAttribute(NodeAtt.SPECTRAL_LIB_EXPLAINED_INTENSITY.toString(),
            scoreForm.format(bestMatch.getSimilarity().getExplainedLibraryIntensity()));
      }

      if (esi != null) {
        // undefined is not represented by a neutral M node
        if (esi.getIonType().isUndefinedAdduct()) {
          node.setAttribute(NodeAtt.TYPE.toString(), NodeType.SINGLE_FEATURE);
        }

        node.setAttribute(NodeAtt.ION_TYPE.toString(), esi.getIonType().toString(false));
        node.setAttribute(NodeAtt.NEUTRAL_MASS.toString(),
            mzForm.format(esi.getIonType().getMass(row.getAverageMZ())));
        node.setAttribute(NodeAtt.NET_ID.toString(), esi.getNetID());
        String ms2Veri = (esi.getMSMSMultimerCount() > 0 ? "xmer_verified" : "")
                         + (esi.getMSMSModVerify() > 0 ? " modification_verified" : "");
        node.setAttribute(NodeAtt.MS2_VERIFICATION.toString(), ms2Veri);

        MolecularFormulaIdentity formula = esi.getBestMolFormula();
        if (esi.getNetwork() != null) {
          formula = esi.getNetwork().getBestMolFormula();
        }
        if (formula != null) {
          node.setAttribute(NodeAtt.FORMULA.toString(), formula.getFormulaAsString());
        }
      }
    }

    return node;
  }

  private Edge addNewDeltaMZEdge(Node node1, Node node2, EdgeType type, double dmz) {
    return addNewEdge(node1, node2, type, "\u0394 " + mzForm.format(dmz), true, dmz);
  }

  public Edge addNewEdge(Node node1, Node node2, EdgeType type, Object label, boolean directed,
      double dmz) {
    String uiClass = getUIClass(type);
    Edge e = addNewEdge(node1, node2, type.toString(), label, directed, uiClass);
    e.setAttribute(EdgeAtt.TYPE.toString(), type);
    e.setAttribute(EdgeAtt.DELTA_MZ.toString(), mzForm.format(dmz));
    return e;
  }

  public Edge addNewEdge(Node node1, Node node2, EdgeType type, Object label,
      boolean directed) {
    String uiClass = getUIClass(type);
    Edge e = addNewEdge(node1, node2, type.toString(), label, directed, uiClass);
    e.setAttribute(EdgeAtt.TYPE.toString(), type);
    return e;
  }

  public Edge addNewEdge(Node node1, Node node2, String edgeNameSuffix, Object label,
      boolean directed, String uiClass) {
    String edge = node1.getId() + node2.getId() + edgeNameSuffix;
    Edge e = graph.getEdge(edge);
    if (e == null) {
      e = graph.addEdge(edge, node1, node2, directed);
    }
    e.setAttribute("ui.label", label);
    e.setAttribute(EdgeAtt.LABEL.toString(), label);
    if (uiClass != null && !uiClass.isEmpty()) {
      e.setAttribute("ui.class", uiClass);
    }
    return e;
  }
}
