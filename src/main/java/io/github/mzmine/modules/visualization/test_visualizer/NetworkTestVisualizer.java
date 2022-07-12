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

import java.awt.Color;
import java.util.EnumSet;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseButton;
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
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.ThreadingModel;
import org.graphstream.ui.view.util.InteractiveElement;

public class NetworkTestVisualizer extends Stage {

  private static final Logger logger = Logger.getLogger(NetworkTestVisualizer.class.getName());
  openSetupDialog di = new openSetupDialog();
  protected Graph graph;
  protected Viewer viewer;
  protected FxViewPanel view;
  protected double viewPercent = 1;
  private Point2D last;
  int NV = di.getNodeValue();
  String GA = di.getGeneratingAlgo();

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

  public NetworkTestVisualizer() {
    graph = new MultiGraph(GA);
    graph.setAttribute("ui.stylesheet",
        "edge { fill-color: blue; shape: angle; arrow-shape: none; size: 2px; } node {fill-color: green;}");
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
    for (org.graphstream.graph.Node node : graph) {
      node.setAttribute("ui.label", node.getId());
    }

    try {
      viewer = new FxViewer(graph, ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
      viewer.enableAutoLayout();
      view = (FxViewPanel) viewer.addDefaultView(false);
      double Mxw = view.getMaxWidth(), Mxh = view.getMaxHeight();
      view.enableMouseOptions();
      Pane graphpane = new Pane(view);
      graphpane.setMaxSize(Mxh, Mxw);
      Pane sp = new Pane();
      sp.getChildren().addAll(graphpane);
      view.setOnScroll(event -> zoom(event.getDeltaY() > 0));
      view.setOnKeyPressed(event -> {
        int r = new Random().nextInt(0, 255);
        int g = new Random().nextInt(0, 255);
        int b = new Random().nextInt(0, 255);
        String style_up =
            "edge { size:" + (int) Math.floor(Math.random() * 20) + "px;" + "fill-color: rgb(" + r
                + "," + g + "," + b + "); }";
        String style_down =
            "edge { size:" + (int) Math.floor(Math.random() * 10) + "px;" + "fill-color: rgb(" + r
                + "," + g + "," + b + "); }";
        switch (event.getCode()) {
          case I -> graph.setAttribute("ui.stylesheet", style_up);
          case D -> graph.setAttribute("ui.stylesheet", style_down);
        }

      });
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
        if (e.getButton() == MouseButton.SECONDARY) {
          String nid = view.findGraphicElementAt(EnumSet.of(InteractiveElement.NODE), e.getX(),
              e.getY()).getId();
          Alert alert = new Alert(AlertType.INFORMATION);
          if (nid != null) {
            alert.setTitle("Node Identifier");
            alert.setContentText("You have clicked on Node Number: " + nid);
            alert.showAndWait();
          }
        }
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
      Scene scene = new Scene(sp, Mxh, Mxw);
      setTitle("Test_Visualizer");
      setScene(scene);
      setResizable(false);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
  }
}