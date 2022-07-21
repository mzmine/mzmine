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

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import org.graphstream.algorithm.generator.BarabasiAlbertGenerator;
import org.graphstream.algorithm.generator.DorogovtsevMendesGenerator;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.algorithm.generator.GridGenerator;
import org.graphstream.algorithm.generator.RandomEuclideanGenerator;
import org.graphstream.algorithm.generator.RandomGenerator;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.ThreadingModel;

public class NetworkTestVisualizer extends Stage {

  private static final Logger logger = Logger.getLogger(NetworkTestVisualizer.class.getName());
  protected Graph graph;
  protected Viewer viewer;
  protected FxViewPanel view;
  protected SpriteManager sprites;
  protected double viewPercent = 1;
  private Point2D last;
  int numberOfEdges;
  NetworkGenerators generator;

  public void zoom(boolean zoomOut) {
    viewPercent += viewPercent * 0.1 * (zoomOut ? -1 : 1);
    view.getCamera().setViewPercent(viewPercent);
  }

  public void translate(double dx, double dy) {
    Point3 c = view.getCamera().getViewCenter();
    Point3 p0 = view.getCamera().transformPxToGu(0, 0);
    Point3 p = view.getCamera().transformPxToGu(dx, dy);

    view.getCamera().setViewCenter(c.x + p.x - p0.x, c.y + p.y + p0.y, c.z);
  }

  public void setCenter(double x, double y) {
    Point3 c = view.getCamera().getViewCenter();
    Point3 p = view.getCamera().transformPxToGu(x, y);
    view.getCamera().setViewCenter(p.x, p.y, c.z);
  }

  public void resetZoom() {
    viewPercent = 1;
    view.getCamera().resetView();
  }

  public Node randomNode(Graph graph) {
    int min = 0;
    int max = (int) graph.nodes().count();
    int rand = (int) (min + (Math.random() * (max - min)));
    return graph.getNode(rand);
  }

  public NetworkTestVisualizer(TestNetworkParameters params) {
    numberOfEdges = params.getValue(TestNetworkParameters.edges);
    generator = params.getValue(TestNetworkParameters.generator);

    graph = new MultiGraph(generator.toString());
    graph.setAttribute("ui.stylesheet",
        "edge { fill-mode: dyn-plain;fill-color: red,yellow,green,blue,pink,orange,purple,brown,black,violet; shape: angle; arrow-shape: none; size-mode:dyn-size; size: 2px; } node {fill-color: green;}sprite { shape: pie-chart; fill-color: #FC0, #F00, #03F, #A0F; size: 20px; }");
    Generator generator = switch (this.generator) {
      case RandomGenerator -> new RandomGenerator(2);
      case BarabasiAlbertGenerator -> new BarabasiAlbertGenerator(3);
      case SquareGridGenerator -> new GridGenerator();
      case RandomEuclideanGenerator -> new RandomEuclideanGenerator();
      case DorogovtsevMendesGenerator -> new DorogovtsevMendesGenerator();
    };
    generator.addSink(graph);
    generator.begin();
    for (int i = 0; i < numberOfEdges; i++) {
      generator.nextEvents();
    }
    generator.end();
    graph.edges().forEach(edge -> {
      edge.setAttribute("ui.label", new Random().nextFloat(0, 1));
      edge.setAttribute("ui.color", new Random().nextFloat(0, 1));
      edge.setAttribute("ui.size", new Random().nextInt(1, 20));
    });
    int NODE_COUNT = (int) graph.nodes().count();
    sprites = new SpriteManager(graph);
    for (int i = 0; i < NODE_COUNT; i++) {
      sprites.addSprite(i + "");
    }
    float[] values = new float[4];
    values[0] = new Random().nextFloat(0, 0.25f);
    values[1] = new Random().nextFloat(0, 0.25f);
    values[2] = new Random().nextFloat(0, 0.25f);
    values[3] = new Random().nextFloat(0, 0.25f);
    sprites.forEach(s -> s.attachToNode(randomNode(graph).getId()));
    sprites.forEach(s -> s.setAttribute("ui.pie-values", values));

    try {
      viewer = new FxViewer(graph, ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
      viewer.enableAutoLayout();
      view = (FxViewPanel) viewer.addDefaultView(false);
      double Mxw = view.getMaxWidth(), Mxh = view.getMaxHeight();
      view.enableMouseOptions();

      Label clickLabel = new Label("");
      FlowPane topPane = new FlowPane(clickLabel);
      BorderPane mainPane = new BorderPane(view);
      mainPane.setTop(topPane);

//      graphpane.setMaxSize(Mxh, Mxw);
      view.setOnScroll(event -> zoom(event.getDeltaY() > 0));
      view.setOnMouseClicked(e -> {
        if (e.getButton() == MouseButton.PRIMARY) {
          if (e.getClickCount() == 2) {
            resetZoom();
            e.consume();
          } else if (e.getClickCount() == 1) {
            setCenter(e.getX(), e.getY());
          }
        }
      });
      view.setOnMousePressed(e -> {
        if (last == null) {
          last = new Point2D(e.getX(), e.getY());
        }
        // show clicked edge in label
        Edge edge = NetworkMouseManager.findEdgeAt(view, view.getViewer().getGraphicGraph(),
            e.getX(), e.getY());
        clickLabel.setText("You have clicked on Edge: " + (edge == null ? "NONE" : edge.getId()));
      });
      view.setOnMouseReleased(e -> {
        last = null;
      });
      view.setOnMouseDragged(e -> {
        if (last != null) {
          // translate
          translate(e.getX() - last.getX(), e.getY() - last.getY());
        }
        last = new Point2D(e.getX(), e.getY());
      });

      Scene scene = new Scene(mainPane, Mxh, Mxw);
      setTitle("Test_Visualizer");
      setScene(scene);
      setResizable(false);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
  }
}