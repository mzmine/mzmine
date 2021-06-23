/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.visualization.networking.visual;


import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.datamodel.identities.iontype.networks.IonNetworkRelation;
import io.github.mzmine.main.MZmineCore;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

public class FeatureNetworkGenerator {

  private static final Logger logger = Logger.getLogger(FeatureNetworkGenerator.class.getName());
  private NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();

  private Graph graph;
  private R2RMap<RowsRelationship> ms2SimMap;

  public void createNewGraph(FeatureListRow[] rows, Graph graph, boolean onlyBestNetworks,
      R2RMap<RowsRelationship> ms2SimMap) {
    this.ms2SimMap = ms2SimMap;
    this.graph = graph;
    logger.info("Adding all annotations to a network");
    if (rows != null) {
      IonNetwork[] nets = IonNetworkLogic.getAllNetworks(Arrays.asList(rows), onlyBestNetworks);

      AtomicInteger added = new AtomicInteger(0);
      for (IonNetwork net : nets) {
        addNetworkToGraph(rows, net, added);
      }

      // add relations
      addNetworkRelationsEdges(nets);

      // add ms2 similarity edges
      addMS2SimEdges(rows);

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
//    for (FeatureListRow r : rows) {
//      GNPSLibraryMatch identity = r.get
//          Arrays.stream(r.getPeakIdentities()).filter(GNPSResultsIdentity.class::isInstance)
//              .map(GNPSResultsIdentity.class::cast).findFirst().orElse(null);
//
//      if (identity != null) {
//        n++;
//        Node node = getRowNode(r, true);
//        identity.getResults().entrySet().stream().filter(e -> e.getValue() != null).forEach(e -> {
//          // row node
//          node.setAttribute(e.getKey(), e.getValue());
//          // M nodes
//          streamNeutralMolNodes(r).forEach(mnode -> mnode.setAttribute(e.getKey(), e.getValue()));
//        });
//      }
//    }
    logger.info("Added " + n + " GNPS library matches to their respective nodes");
  }

  private void addMS2SimEdges(FeatureListRow[] rows) {
    if (ms2SimMap != null) {
      for (int i = 0; i < rows.length - 1; i++) {
        for (int j = i + 1; j < rows.length; j++) {
          FeatureListRow a = rows[i];
          FeatureListRow b = rows[j];
          RowsRelationship sim = ms2SimMap.get(a, b);

          if (sim != null) {
            addMS2SimEdges(a, b, sim);
          }
        }
      }
    }
  }

  private void addMS2SimEdges(FeatureListRow ra, FeatureListRow rb, RowsRelationship sim) {
    Node a = getRowNode(ra, true);
    Node b = getRowNode(rb, true);
    addMS2SimEdges(a, b, sim);
  }

  private void addMS2SimEdges(Node a, Node b, RowsRelationship sim) {
    EdgeType type = EdgeType.of(sim.getType());
    String edgeName = addNewEdge(a, b, type.toString(), sim.getAnnotation(), false);
    Edge edge = graph.getEdge(edgeName);
    edge.setAttribute(EdgeAtt.TYPE.toString(), type);
    edge.setAttribute(EdgeAtt.LABEL.toString(), sim.getAnnotation());
    edge.setAttribute(EdgeAtt.SCORE.toString(), sim.getScore());
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
          String edgeLabel = rel.getDescription();
          String edgeName = addNewEdge(a, b, "relations", edgeLabel, true);
          Edge edge = graph.getEdge(edgeName);
          edge.setAttribute("ui.class", "medium");
          edge.setAttribute(EdgeAtt.TYPE.toString(), EdgeType.NETWORK_RELATIONS);
          edge.setAttribute(EdgeAtt.LABEL.toString(), edgeLabel);
        }
      }
    }
  }

  private void addNetworkToGraph(FeatureListRow[] rows, IonNetwork net, AtomicInteger added) {
    Node mnode = !net.isUndefined() ? getNeutralMolNode(net, true) : null;
    // bundle all neutral losses together
    Node neutralNode = getNeutralLossNode();

    // add center neutral M
    net.entrySet().stream().forEach(e -> {
      Node node = getRowNode(e.getKey(), e.getValue());

      if (e.getValue().getIonType().isModifiedUndefinedAdduct()) {
        // neutral
        addNewEdge(neutralNode, node, "ions", "", false);
      } else if (!e.getValue().getIonType().isUndefinedAdduct() && mnode != null) {
        addNewDeltaMZEdge(node, mnode, Math.abs(net.getNeutralMass() - e.getKey().getAverageMZ()));
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
          addNewDeltaMZEdge(node1, node2,
              Math.abs(e.getKey().getAverageMZ() - prow.getAverageMZ()));
          added.incrementAndGet();
        }
      });
    });

  }

  private Node getNeutralLossNode() {
    Node neutralNode = graph.getNode("NEUTRAL LOSSES");
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
      node.setAttribute(NodeAtt.RT.toString(), net.getAvgRT());
      node.setAttribute(NodeAtt.NEUTRAL_MASS.toString(), net.getNeutralMass());
      node.setAttribute(NodeAtt.INTENSITY.toString(), net.getHeightSum());

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
      node.setAttribute("ui.label", label);
      node.setAttribute(NodeAtt.TYPE.toString(),
          esi != null ? NodeType.ION_FEATURE : NodeType.SINGLE_FEATURE);
      node.setAttribute(NodeAtt.ID.toString(), row.getID());
      node.setAttribute(NodeAtt.RT.toString(), row.getAverageRT());
      node.setAttribute(NodeAtt.MZ.toString(), row.getAverageMZ());
      node.setAttribute(NodeAtt.INTENSITY.toString(), row.getBestFeature().getHeight());
      node.setAttribute(NodeAtt.CHARGE.toString(), row.getRowCharge());
      node.setAttribute(NodeAtt.GROUP_ID.toString(), row.getGroupID());
      if (esi != null) {
        // undefined is not represented by a neutral M node
        if (esi.getIonType().isUndefinedAdduct()) {
          node.setAttribute(NodeAtt.TYPE.toString(), NodeType.SINGLE_FEATURE);
        }

        node.setAttribute(NodeAtt.ION_TYPE.toString(), esi.getIonType().toString(false));
        node.setAttribute(NodeAtt.NEUTRAL_MASS.toString(),
            esi.getIonType().getMass(row.getAverageMZ()));
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

  private void addNewDeltaMZEdge(Node node1, Node node2, double dmz) {
    String edgeName = addNewEdge(node1, node2, "ions", "\u0394 " + mzForm.format(dmz), true);
    graph.getEdge(edgeName).setAttribute(EdgeAtt.TYPE.toString(), EdgeType.ION_IDENTITY);
  }

  public String addNewEdge(Node node1, Node node2, String edgeNameSuffix, Object edgeLabel,
      boolean directed) {
    String edge = node1.getId() + node2.getId() + edgeNameSuffix;
    Edge e = graph.getEdge(edge);
    if (e == null) {
      e = graph.addEdge(edge, node1, node2, directed);
    }
    e.setAttribute("ui.label", edgeLabel);
    e.setAttribute("LABEL", edgeLabel);
    return edge;
  }
}
