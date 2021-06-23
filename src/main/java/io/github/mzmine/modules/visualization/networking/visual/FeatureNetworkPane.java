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


import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch;
import java.util.Map;
import java.util.logging.Logger;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import org.graphstream.graph.Node;

public class FeatureNetworkPane extends NetworkPane {

  private static final Logger logger = Logger.getLogger(FeatureNetworkPane.class.getName());

  private FeatureNetworkGenerator generator = new FeatureNetworkGenerator();

  // data
  private FeatureList featureList;
  private FeatureListRow[] rows;

  private boolean onlyBest;
  private boolean showNetRelationsEdges;
  private boolean collapse = true;
  private boolean showIonEdges = true;
  private boolean showMs2SimEdges;

  private R2RMap<RowsRelationship> ms2SimMap;
  private Map<Type, R2RMap<RowsRelationship>> relationMaps;


  /**
   * Create the panel.
   */
  public FeatureNetworkPane() {
    this(false);
  }

  public FeatureNetworkPane(boolean showTitle) {
    super("Ion identity networks (IINs)", showTitle);
    addMenu();
  }

  private void addMenu() {
    Pane menu = getPnSettings();
    menu.setVisible(true);
    menu.getChildren().add(new Label("Menu"));

    showEdgeLabels = false;
    showNodeLabels = true;
    collapse = true;

    ToggleButton toggleCollapseIons = new ToggleButton("Collapse ions");
    toggleCollapseIons.setSelected(collapse);
    menu.getChildren().add(toggleCollapseIons);
    toggleCollapseIons.selectedProperty()
        .addListener((o, old, value) -> collapseIonNodes(toggleCollapseIons.isSelected()));

    ToggleButton toggleShowMS2SimEdges = new ToggleButton("Show MS2 sim");
    toggleShowMS2SimEdges.setSelected(true);
    menu.getChildren().add(toggleShowMS2SimEdges);
    toggleShowMS2SimEdges.selectedProperty()
        .addListener((o, old, value) -> setShowMs2SimEdges(toggleShowMS2SimEdges.isSelected()));

    ToggleButton toggleShowRelations = new ToggleButton("Show relational edges");
    toggleShowRelations.setSelected(true);
    menu.getChildren().add(toggleShowRelations);
    toggleShowRelations.selectedProperty()
        .addListener((o, old, value) -> setConnectByNetRelations(toggleShowRelations.isSelected()));

    ToggleButton toggleShowIonIdentityEdges = new ToggleButton("Show ion edges");
    toggleShowIonIdentityEdges.setSelected(true);
    menu.getChildren().add(toggleShowIonIdentityEdges);
    toggleShowIonIdentityEdges.selectedProperty().addListener(
        (o, old, value) -> showIonIdentityEdges(toggleShowIonIdentityEdges.isSelected()));

    ToggleButton toggleShowEdgeLabel = new ToggleButton("Show edge label");
    toggleShowEdgeLabel.setSelected(showEdgeLabels);
    menu.getChildren().add(toggleShowEdgeLabel);
    toggleShowEdgeLabel.selectedProperty()
        .addListener((o, old, value) -> showEdgeLabels(toggleShowEdgeLabel.isSelected()));

    ToggleButton toggleShowNodeLabel = new ToggleButton("Show node label");
    toggleShowNodeLabel.setSelected(showNodeLabels);
    menu.getChildren().add(toggleShowNodeLabel);
    toggleShowNodeLabel.selectedProperty()
        .addListener((o, old, value) -> showNodeLabels(toggleShowNodeLabel.isSelected()));

    Button showGNPSMatches = new Button("GNPS matches");
    menu.getChildren().add(showGNPSMatches);
    showGNPSMatches.onMouseClickedProperty().addListener((o, old, value) -> showGNPSMatches());
  }

  /**
   * Show GNPS library match
   */
  private void showGNPSMatches() {
    int n = 0;
    for (Node node : graph) {
      String name = (String) node.getAttribute(GNPSLibraryMatch.ATT.COMPOUND_NAME.getKey());
      if (name != null) {
        node.setAttribute("ui.label", name);
        n++;
      }
    }
    logger.info("Show " + n + " GNPS library matches");
  }

  private void showIonIdentityEdges(boolean selected) {
    showIonEdges = selected;
    collapseIonNodes(collapse);
  }

  public void collapseIonNodes(boolean collapse) {
    this.collapse = collapse;
    for (Node node : graph) {
      NodeType type = (NodeType) node.getAttribute(NodeAtt.TYPE.toString());
      if (type != null) {
        switch (type) {
          case NEUTRAL_LOSS_CENTER:
          case ION_FEATURE:
            setVisible(node, !collapse);
            break;
          case NEUTRAL_M:
            break;
          case SINGLE_FEATURE:
            break;
          default:
            break;
        }
      }
    }

    graph.edges().forEach(edge -> {
      EdgeType type = (EdgeType) edge.getAttribute(EdgeAtt.TYPE.toString());
      if (type != null) {
        switch (type) {
          case ION_IDENTITY:
            setVisible(edge, !collapse && showIonEdges);
            break;
          case MS2_SIMILARITY_NEUTRAL_M_TO_FEATURE:
          case MS2_SIMILARITY_NEUTRAL_M:
          case MS2_SIMILARITY:
            setVisible(edge, showMs2SimEdges);
            break;
          case NETWORK_RELATIONS:
            setVisible(edge, showNetRelationsEdges);
            break;
          default:
            break;
        }
      }
      // only if both nodes are visible
      if (!isVisible(edge.getSourceNode()) || !isVisible(edge.getTargetNode())) {
        setVisible(edge, false);
      }
    });
  }

  /**
   * All the peaklist
   *
   * @param featureList
   */
  public void setFeatureList(FeatureList featureList) {
    this.featureList = featureList;
    if (featureList != null) {
      relationMaps = featureList.getRowMaps();
      this.ms2SimMap = featureList.getMs2SimilarityMap();
      createNewGraph(featureList.getRows().toArray(FeatureListRow[]::new));
    } else {
      clear();
    }
  }

  /**
   * Array of rows
   *
   * @param rows
   */
  public void setFeatureListRows(FeatureListRow[] rows, R2RMap<RowsRelationship> ms2SimMap) {
    featureList = null;
    this.rows = rows;
    this.ms2SimMap = ms2SimMap;
    if (rows != null) {
      createNewGraph(rows);
    } else {
      clear();
    }
  }

  public void createNewGraph(FeatureListRow[] rows) {
    clear();
    generator.createNewGraph(rows, graph, onlyBest, ms2SimMap);
    clearSelections();
    showEdgeLabels(showEdgeLabels);
    showNodeLabels(showNodeLabels);

    // last state
    collapseIonNodes(collapse);
  }


  public void setSelectedRow(FeatureListRow row) {
    String node = generator.toNodeName(row);
    // set selected
    Node n = graph.getNode(node);
    setSelectedNode(n);
  }

  private FeatureListRow findRowByID(int id, FeatureListRow[] rows) {
    if (rows == null) {
      return null;
    } else {
      for (FeatureListRow r : rows) {
        if (r.getID() == id) {
          return r;
        }
      }

      return null;
    }
  }


  public void setConnectByNetRelations(boolean connectByNetRelations) {
    this.showNetRelationsEdges = connectByNetRelations;
    collapseIonNodes(collapse);
  }

  public void setOnlyBest(boolean onlyBest) {
    this.onlyBest = onlyBest;
  }

  public void dispose() {
    graph.clear();
  }

  public void setShowMs2SimEdges(boolean ms2SimEdges) {
    this.showMs2SimEdges = ms2SimEdges;
    collapseIonNodes(collapse);
  }

  public FeatureList getFeatureList() {
    return featureList;
  }
}
