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

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import javax.swing.JFrame;
import javax.swing.JTextField;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.ThreadingModel;

public class TestNetworks {
  public static void main(String args[]) {
    // System.setProperty("gs.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
    System.setProperty("org.graphstream.ui", "javafx");
    new TestNetworks();
  }

  SingleGraph graph;
  Viewer viewer;
  double viewPercent = 1;
  FxViewPanel view;
  JTextField txt;

  public TestNetworks() {
//    createNewFrame();
    // createDirectMulti();
    createDirect();
  }

  private void createDirect() {
    Graph graph = new SingleGraph("tutorial 1");

    graph.setAttribute("ui.stylesheet", styleSheet);
    graph.setAutoCreate(true);
    graph.setStrict(false);
    graph.display();

    graph.addEdge("AB", "A", "B");
    graph.addEdge("BC", "B", "C");
    graph.addEdge("CA", "C", "A");
    graph.addEdge("AD", "A", "D");
    graph.addEdge("DE", "D", "E");
    graph.addEdge("DF", "D", "F");
    graph.addEdge("EF", "E", "F");

    for (Node node : graph) {
      node.setAttribute("ui.label", node.getId());
    }


    explore(graph.getNode("A"));
  }

  private void createDirectMulti() {
    Graph graph = new SingleGraph("tutorial 1");

    graph.setAttribute("ui.stylesheet", styleSheet);
    graph.setAutoCreate(true);
    graph.setStrict(false);
    graph.display();

    graph.addEdge("AB", "A", "B");
    graph.addEdge("BC", "B", "C");
    graph.addEdge("CA", "C", "A");
    graph.addEdge("AD", "A", "D");
    graph.addEdge("DE", "D", "E");
    graph.addEdge("DF", "D", "F");
    graph.addEdge("EF", "E", "F");

    graph.addEdge("XY", "X", "Y");
    graph.addEdge("YZ", "Y", "Z");
    graph.addEdge("XZ", "X", "Z");

    for (Node node : graph) {
      node.setAttribute("ui.label", node.getId());
    }

  }

//  public void createNewFrame() {
//    graph = new SingleGraph("tutorial 1");
//
//    graph.setAttribute("ui.stylesheet", styleSheet);
//    graph.setAutoCreate(true);
//    graph.setStrict(false);
//
//    graph.addEdge("AB", "A", "B");
//    graph.addEdge("BC", "B", "C");
//    graph.addEdge("CA", "C", "A");
//    graph.addEdge("AD", "A", "D");
//    graph.addEdge("DE", "D", "E");
//    graph.addEdge("DF", "D", "F");
//    graph.addEdge("EF", "E", "F");
//
//    graph.getEdge("AB").setAttribute("ui.label", "EDGE");
//
//    graph.getNode("A").setAttribute("ui.class", "big, important");
//
//    for (Node node : graph) {
//      node.setAttribute("ui.label", node.getId());
//    }
//
//    viewer = new FxViewer(graph, ThreadingModel.GRAPH_IN_GUI_THREAD);
//    viewer.enableAutoLayout();
//    view = (ViewPanel) viewer.addDefaultView(false); // false indicates "no JFrame".
//
//    JFrame frame = new JFrame("Test");
//    frame.getContentPane().add(view, BorderLayout.CENTER);
//
//    txt = new JTextField();
//    frame.getContentPane().add(txt, BorderLayout.SOUTH);
//    frame.setSize(800, 800);
//    frame.setVisible(true);
//
//
//    graph.addEdge("XY", "X", "Y");
//    graph.addEdge("YZ", "Y", "Z");
//    graph.addEdge("XZ", "X", "Z");
//
//    SpriteManager sman = new SpriteManager(graph);
//    Sprite s = sman.addSprite("XYs");
//    s.setAttribute("ui.label", "text");
//    s.attachToEdge("XY");
//    s.setPosition(0.2);
//
//
//    // listener
//    view.addMouseListener(new MouseAdapter() {
//      private Point last;
//
//      @Override
//      public void mouseClicked(MouseEvent e) {
//        System.out.println("CLICK");
//        // try to find node
//        for (Node node : graph) {
//          if (node.hasAttribute("ui.selected")) {
//            System.out.println(String.format(" %s", node.getId()));
//          }
//          if (node.hasAttribute("ui.clicked")) {
//            System.err.printf("node %s clicked%n", node.getId());
//          }
//        }
//        //
//        // // no node do zoom
//        // if (e.getClickCount() == 2) {
//        // viewPercent = 1;
//        // view.getCamera().resetView();
//        // e.consume();
//        // } else if (e.getClickCount() == 1)
//        // setCenter(e.getX(), e.getY());
//
//      }
//
//      @Override
//      public void mouseReleased(MouseEvent e) {
//        last = null;
//      }
//
//      @Override
//      public void mousePressed(MouseEvent e) {
//        if (last == null)
//          last = e.getPoint();
//      }
//
//      @Override
//      public void mouseDragged(MouseEvent e) {
//        if (last != null) {
//          // translate
//          translate(e.getX() - last.getX(), e.getY() - last.getY());
//        }
//        last = e.getPoint();
//      }
//    });
//    view.addMouseWheelListener(event -> zoom(event.getWheelRotation() < 0));
//  }

  public void explore(Node source) {
    Iterator<? extends Node> k = source.getBreadthFirstIterator();

    while (k.hasNext()) {
      Node next = k.next();
      next.setAttribute("ui.class", "marked");
      sleep();
    }
  }

  protected void sleep() {
    try {
      Thread.sleep(1000);
    } catch (Exception e) {
    }
  }


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

  public void setCenter(int x, int y) {
    Point3 c = view.getCamera().getViewCenter();
    Point3 p = view.getCamera().transformPxToGu(x, y);
    view.getCamera().setViewCenter(p.x, p.y, c.z);
  }


  protected String styleSheet =
      "node {fill-color: black; size: 10px; stroke-mode: plain; stroke-color: black; stroke-width: 1px;} "
          + "node.important{fill-color: red;} " + "node.big {size: 15px;}";
}
