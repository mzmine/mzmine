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

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.test;

//import org.graphstream.algorithm.generator.DorogovtsevMendesGenerator;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.javafx.FxGraphRenderer;

/**
 * Copied from https://github.com/graphstream/gs-ui-javafx/blob/2.0-alpha/src-test/org/graphstream/ui/viewer_fx/test/AllFxTest.java
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class TestNetworkFX extends Application {

  // TODO delete as soon as visualizer is integrated
  public static void main(String[] args) {
    Application.launch(TestNetworkFX.class, args);
  }

  protected String styleSheet = "graph {padding: 60px;}";

  public void start(Stage primaryStage) throws Exception {
    MultiGraph g = new MultiGraph("mg");
    FxViewer v = new FxViewer(g, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
//    DorogovtsevMendesGenerator gen = new DorogovtsevMendesGenerator();
//
//    g.setAttribute("ui.antialias");
//    g.setAttribute("ui.quality");
//    g.setAttribute("ui.stylesheet", styleSheet);
//
//    v.enableAutoLayout();
//    FxViewPanel panel = (FxViewPanel) v.addDefaultView(false, new FxGraphRenderer());
//
//    gen.addSink(g);
//    gen.begin();
//    for (int i = 0; i < 100; i++) {
//      gen.nextEvents();
//    }
//    gen.end();
//    gen.removeSink(g);

//    Scene scene = new Scene(panel, 800, 600);
//    primaryStage.setScene(scene);
//
//    primaryStage.show();
  }
}
