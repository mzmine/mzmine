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

import java.util.Calendar;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import org.graphstream.graph.Edge;
import org.graphstream.ui.fx_viewer.util.FxMouseManager;
import org.graphstream.ui.geom.Point2;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.geom.Vector2;
import org.graphstream.ui.graphicGraph.GraphicEdge;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.graphicGraph.stylesheet.Values;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.camera.Camera;
import org.graphstream.ui.view.util.GraphMetrics;
import org.graphstream.ui.view.util.InteractiveElement;

public class NetworkMouseManager extends FxMouseManager {

  private final ReentrantLock hoverLock = new ReentrantLock();
  private final Timer hoverTimer = new Timer(true);
  /**
   * (copied from the GraphsStream Library) The mouse needs to stay on an element for at least this
   * amount of milliseconds, until the element gets the attribute "ui.mouseOver" assigned. A value
   * smaller or equal to zero indicates, that the attribute is assigned without delay.
   */
  private final long delayHover;
  private GraphicElement hoveredElement;
  private long hoveredElementLastChanged;
  private HoverTimerTask latestHoverTimerTask;
  private EventHandler<MouseEvent> mouseMoved;

  public NetworkMouseManager() {
    super(EnumSet.of(InteractiveElement.NODE, InteractiveElement.EDGE));
    this.delayHover = 100;
  }

  //findGraphicElement could be used but I wanted to implement the edgeContains method myself
  public static GraphicEdge findEdgeAt(View view, GraphicGraph graph, double x, double y) {
    Camera cam = view.getCamera();

    GraphMetrics metrics = cam.getMetrics();

    //transform x and y
    double xT = x + metrics.viewport[0];
    double yT = y + metrics.viewport[0];

    GraphicEdge edgeFound = null;

    Optional<GraphicEdge> edge = graph.edges().map(e -> (GraphicEdge) e).filter(e -> edgeContains(view, e, xT, yT))
        .findFirst();
    if (edge.isPresent()) {
      if (cam.isVisible(edge.get())) {
        edgeFound = edge.get();
      }
    }

    return edgeFound;
  }

  //new edgeContains() method that finds edge at hovering not only when hovered over edge center
  public static boolean edgeContains(View view, GraphicEdge edge, double x, double y) {
    Camera cam = view.getCamera();
    GraphMetrics metrics = cam.getMetrics();

    Values size = edge.getStyle().getSize();
    double deviation = metrics.lengthToPx(size, 0);

    Point3 edgeNode0 = cam.transformGuToPx(edge.from.x, edge.from.y, 0);
    Point3 edgeNode1 = cam.transformGuToPx(edge.to.x, edge.to.y, 0);

    //check of point x,y is between nodes of the edge
    boolean edgeContains = false;
    //check x,y range
    if (x > Math.min(edgeNode0.x, edgeNode1.x) - deviation
        && x < Math.max(edgeNode0.x, edgeNode1.x) + deviation
        && y > Math.min(edgeNode0.y, edgeNode1.y) - deviation
        && y < Math.max(edgeNode0.y, edgeNode1.y) + deviation) {

      //check deviation from edge

      Vector2 vectorNode0To1 = new Vector2(edgeNode0, edgeNode1);
      Point2 point = new Point2(x, y);
      Vector2 vectorNode0ToPoint = new Vector2(edgeNode0, point);
      //cross product of vectorNode0ToPoint and vectorNode0to1
      double crossProduct =
          vectorNode0ToPoint.x() * vectorNode0To1.y() - vectorNode0To1.x() * vectorNode0ToPoint.y();
      //distance of point to the line extending the edge
      double d = Math.abs(crossProduct) / vectorNode0To1.length();
      if (d <= deviation) {
        edgeContains = true;
      }
    }

    return edgeContains;
  }

  @Override
  public void init(GraphicGraph graph, View view) {
    this.graph = graph;
    this.view = view;

    mouseMoved = new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent event) {
        try {
          hoverLock.lockInterruptibly();
          boolean stayedOnElement = false;
          curElement = view.findGraphicElementAt(getManagedTypes(), event.getX(), event.getY());

          //adjusted implementation of search for edges
          if (curElement == null && getManagedTypes().contains(InteractiveElement.EDGE)) {
            curElement = findEdgeAt(view, graph, event.getX(), event.getY());
          }

          if (hoveredElement != null) {
            //check if mouse stayed on the same element to avoid the mouseOverEvent being processed multiple times
            stayedOnElement = curElement != null && curElement.equals(hoveredElement);
            if (!stayedOnElement && hoveredElement.hasAttribute("ui.mouseOver")) {
              mouseLeftElement(hoveredElement);
            }
          }

          if (!stayedOnElement && curElement != null) {
            if (delayHover <= 0) {
              mouseOverElement(curElement);

            } else {
              hoveredElement = curElement;
              hoveredElementLastChanged = Calendar.getInstance().getTimeInMillis();
              if (latestHoverTimerTask != null) {
                latestHoverTimerTask.cancel();
              }
              latestHoverTimerTask = new HoverTimerTask(hoveredElementLastChanged, hoveredElement);
              hoverTimer.schedule(latestHoverTimerTask, delayHover);
            }

          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
          hoverLock.unlock();
        }

      }
    };

    view.addListener(MouseEvent.MOUSE_MOVED, mouseMoved);
  }

  @Override
  public void release() {
    view.removeListener(MouseEvent.MOUSE_MOVED, mouseMoved);
  }

  @Override
  public EnumSet<InteractiveElement> getManagedTypes() {
    return super.getManagedTypes();
  }

  protected void mouseOverElement(GraphicElement element) {
    element.setAttribute("ui.mouseOver", true);
//    element.setAttribute("ui.class",
//        "mouseOver"); //I defined a class/type for edges in the CSS styling sheet that is calles "mouseOver"

    if (element instanceof GraphicEdge) {
      mouseOverEdge((GraphicEdge) element);
    } else if (element instanceof GraphicNode) {
      mouseOverNode((GraphicNode) element);
    }
  }

  private void mouseOverNode(GraphicNode element) {
  }

  protected void mouseOverEdge(GraphicEdge graphicEdge) {
    view.freezeElement(graphicEdge, true);
    Edge edge = graph.getEdge(graphicEdge.getId());
    System.out.println("Mouse over edge " + edge.getId());
  }

  protected void mouseLeftElement(GraphicElement element) {
    this.hoveredElement = null;
    element.removeAttribute("ui.mouseOver");
    element.removeAttribute("ui.class");
  }

  //copied from GraphStream Library
  final class HoverTimerTask extends TimerTask {

    private final long lastChanged;
    private final GraphicElement element;

    public HoverTimerTask(long lastChanged, GraphicElement element) {
      this.lastChanged = lastChanged;
      this.element = element;
    }

    @Override
    public void run() {
      try {
        hoverLock.lock();
        if (hoveredElementLastChanged == lastChanged) {
          mouseOverElement(element);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      } finally {
        hoverLock.unlock();
      }
    }
  }
}