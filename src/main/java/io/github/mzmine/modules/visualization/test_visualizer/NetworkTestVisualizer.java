/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.test_visualizer;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.graphstream.algorithm.generator.BarabasiAlbertGenerator;
import org.graphstream.algorithm.generator.DorogovtsevMendesGenerator;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.algorithm.generator.GridGenerator;
import org.graphstream.algorithm.generator.RandomEuclideanGenerator;
import org.graphstream.algorithm.generator.RandomGenerator;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.ThreadingModel;

public class NetworkTestVisualizer extends Stage {

  private static final Logger logger = Logger.getLogger(NetworkTestVisualizer.class.getName());
  openSetupDialog di= new openSetupDialog();
  protected Graph graph;
  protected Viewer viewer;
  protected FxViewPanel view;
  protected Node node;
  protected Edge edge;
  int NV=di.getNodeValue();
  String GA=di.getGeneratingAlgo();
  public NetworkTestVisualizer() {
    graph=new MultiGraph(GA);
    graph.setAttribute("ui.stylesheet", "edge { fill-color: blue;shape: angle; arrow-shape: none; size: 3px; } node {fill-color: green;} node:clicked{fill-color: yellow;}");
    Generator generator = switch (GA) {
      case "RandomGenerator" -> new RandomGenerator(2);
      case "BarabasiAlbertGenerator" -> new BarabasiAlbertGenerator(3);
      case "SquareGridGenerator" -> new GridGenerator();
      case "RandomEuclideanGenerator" -> new RandomEuclideanGenerator();
      case "DorogovtsevMendesGenerator" -> new DorogovtsevMendesGenerator();
      default -> throw new IllegalStateException("Unexpected value: " + GA);
    };
      generator.addSink(graph);
      generator.begin();
      for (int i = 0; i < NV; i++) {
        generator.nextEvents();
      }
      generator.end();
    try {
      viewer = new FxViewer(graph, ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
      viewer.disableAutoLayout();
      view = (FxViewPanel) viewer.addDefaultView(false);
      double Mxw = view.getMaxWidth(), Mxh = view.getMaxHeight();
      view.enableMouseOptions();
      Pane graphpane = new Pane(view);
      graphpane.setMaxSize(Mxh, Mxw);
      Pane sp = new Pane();
      sp.getChildren().addAll(graphpane);
      Scene scene = new Scene(sp, Mxh, Mxw);
      setTitle("Test_Visualizer");
      setScene(scene);
      setResizable(false);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
  }
}