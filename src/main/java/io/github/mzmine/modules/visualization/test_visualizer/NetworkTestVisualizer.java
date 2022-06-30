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
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.graphstream.algorithm.generator.BarabasiAlbertGenerator;
import org.graphstream.algorithm.generator.DorogovtsevMendesGenerator;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.algorithm.generator.GridGenerator;
import org.graphstream.algorithm.generator.RandomEuclideanGenerator;
import org.graphstream.algorithm.generator.RandomGenerator;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.ThreadingModel;

public class NetworkTestVisualizer extends Stage {

  private static final Logger logger = Logger.getLogger(NetworkTestVisualizer.class.getName());
  protected Graph graph;
  protected Viewer viewer;
  protected FxViewPanel view;
  protected Node node;
  protected Edge edge;
  static int NodeValue=0;
  static String GenaratingAlgo="";
  public static String DisplayDialog() {
    String[] GA = {"Random generator", "Barabasi-Albert generator", "Square-Grid generator",
        "Random Euclidean generator", "Dorogovtsev-Mendes generator"};
    Stage s = new Stage();
    s.initModality(Modality.APPLICATION_MODAL);
    TextField tf = new TextField();
    ComboBox cb = new ComboBox(FXCollections.observableArrayList(GA));
    Button btn = new Button("Generate Graph");
    btn.setOnAction(e -> {
      NodeValue = Integer.parseInt(tf.getText());
      GenaratingAlgo = cb.getValue() + "";
      s.close();
    });
    Label lb1 = new Label("Enter the minimum no. of nodes:");
    Label lb2 = new Label("Generating algorithm:");
    GridPane layout = new GridPane();
    layout.setPadding(new Insets(10, 10, 10, 10));
    layout.setVgap(5);
    layout.setHgap(5);
    layout.add(tf, 1, 1);
    layout.add(cb, 1, 2);
    layout.add(btn, 1, 3);
    layout.add(lb1, 0, 1);
    layout.add(lb2, 0, 2);
    Scene scene = new Scene(layout, 450, 150);
    s.setTitle("Genarate a Graph");
    s.setScene(scene);
    s.setResizable(false);
    s.showAndWait();
    return GenaratingAlgo + "#" + NodeValue;
  }
  public NetworkTestVisualizer() {
    String[] Values=DisplayDialog().split("#");
    graph=new SingleGraph(Values[0]);
    graph.setAttribute("ui.stylesheet", "edge { fill-color: blue;shape: angle; arrow-shape: none; size: 3px; } node {fill-color: green;} node:clicked{fill-color: yellow;}");
    if (Values[1].equals("Random generator")) {
      Generator gen = new RandomGenerator(2);
      gen.addSink(graph);
      gen.begin();
      for (int i = 0; i < Integer.parseInt(Values[1]); i++) {
        gen.nextEvents();
      }
      gen.end();
    } else if (Values[0].equals("Barabasi-Albert generator")) {
      Generator gen = new BarabasiAlbertGenerator(3);
      gen.addSink(graph);
      gen.begin();
      for (int i = 0; i < Integer.parseInt(Values[1]); i++) {
        gen.nextEvents();
      }
      gen.end();
    } else if (Values[0].equals("Square-Grid generator")) {
      Generator gen = new GridGenerator();
      gen.addSink(graph);
      gen.begin();
      for (int i = 0; i < Integer.parseInt(Values[1]); i++) {
        gen.nextEvents();
      }
      gen.end();
    } else if (Values[0].equals("Random Euclidean generator")) {
      Generator gen = new RandomEuclideanGenerator();
      gen.addSink(graph);
      gen.begin();
      for (int i = 0; i < Integer.parseInt(Values[1]); i++) {
        gen.nextEvents();
      }
      gen.end();
    } else if (Values[0].equals("Dorogovtsev-Mendes generator")) {
      Generator gen = new DorogovtsevMendesGenerator();
      gen.addSink(graph);
      gen.begin();
      for (int i = 0; i < Integer.parseInt(Values[1]); i++) {
        gen.nextEvents();
      }
      gen.end();
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
      Scene scene = new Scene(sp, Mxh, Mxw);
      setTitle("Test_Visualizer");
      setScene(scene);
      setResizable(false);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
  }
}