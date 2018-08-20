package net.sf.mzmine.chartbasics.javafx.mouse;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.fx.ChartCanvas;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.interaction.AbstractMouseHandlerFX;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.Zoomable;
import org.jfree.chart.util.ShapeUtils;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import net.sf.mzmine.chartbasics.gestures.ChartGesture;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Button;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Entity;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Event;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Key;
import net.sf.mzmine.chartbasics.gestures.ChartGestureDragDiffHandler.Orientation;
import net.sf.mzmine.chartbasics.gestures.ChartGestureEvent;
import net.sf.mzmine.chartbasics.gestures.ChartGestureHandler;
import net.sf.mzmine.chartbasics.gestures.ChartGestureHandler.DragHandler;
import net.sf.mzmine.chartbasics.gestures.ChartGestureHandler.Handler;

/**
 * Handles drag zooming of charts on a {@link ChartCanvas}. This handler should be configured with
 * the required modifier keys and installed as a live handler (not an auxiliary handler). This
 * handler only works for a <b>ChartCanvas</b> that is embedded in a {@link ChartViewer}, since it
 * relies on the <b>ChartViewer</b> for drawing the zoom rectangle.
 */
public class EMouseAdapterFX extends AbstractMouseHandlerFX {

  /** The viewer is used to overlay the zoom rectangle. */
  private ChartViewer viewer;

  /** The starting point for the zoom. */
  private Point2D startPoint;

  /**
   * Creates a new instance with no modifier keys required.
   * 
   * @param id the handler ID ({@code null} not permitted).
   * @param parent the chart viewer.
   */
  public EMouseAdapterFX(String id, ChartViewer parent) {
    this(id, parent, false, false, false, false);
  }

  /**
   * Creates a new instance that will be activated using the specified combination of modifier keys.
   * 
   * @param id the handler ID ({@code null} not permitted).
   * @param parent the chart viewer.
   * @param altKey require ALT key?
   * @param ctrlKey require CTRL key?
   * @param metaKey require META key?
   * @param shiftKey require SHIFT key?
   */
  public EMouseAdapterFX(String id, ChartViewer parent, boolean altKey, boolean ctrlKey,
      boolean metaKey, boolean shiftKey) {
    super(id, altKey, ctrlKey, metaKey, shiftKey);
    this.viewer = parent;
  }


  /**
   * Handles a mouse moved event. This implementation does nothing, override the method if required.
   * 
   * @param canvas the canvas ({@code null} not permitted).
   * @param e the event ({@code null} not permitted).
   */
  @Override
  public void handleMouseMoved(ChartCanvas canvas, MouseEvent e) {
    // does nothing unless overridden
  }

  /**
   * Handles a mouse clicked event. This implementation does nothing, override the method if
   * required.
   * 
   * @param canvas the canvas ({@code null} not permitted).
   * @param e the event ({@code null} not permitted).
   */
  @Override
  public void handleMouseClicked(ChartCanvas chartPanel, MouseEvent eOrig) {
    if (gestureHandlers == null || gestureHandlers.isEmpty()
        || !(listensFor(Event.CLICK) || listensFor(Event.DOUBLE_CLICK)))
      return;

    MouseEventWrapper e = new MouseEventWrapper(eOrig);
    ChartEntity entity = findChartEntity(chartPanel, e);
    ChartGesture.Entity gestureEntity = ChartGesture.getGestureEntity(entity);
    Button button = Button.getButton(e.getButton());

    if (!e.isConsumed()) {
      // double clicked
      if (e.getClickCount() == 2) {
        // reset click count to handle double clicks quickly
        e.consume();
        // handle event
        handleEvent(new ChartGestureEvent(chartPanel, e, entity,
            new ChartGesture(gestureEntity, Event.DOUBLE_CLICK, button)));
      } else if (e.getClickCount() == 1) {
        // handle event
        handleEvent(new ChartGestureEvent(chartPanel, e, entity,
            new ChartGesture(gestureEntity, Event.CLICK, button)));
      }
    }
  }

  /**
   * Handles a mouse pressed event. This implementation does nothing, override the method if
   * required.
   * 
   * @param canvas the canvas ({@code null} not permitted).
   * @param e the event ({@code null} not permitted).
   */
  @Override
  public void handleMousePressed(ChartCanvas canvas, MouseEvent e) {
    // does nothing unless overridden
  }

  /**
   * Handles a mouse dragged event. This implementation does nothing, override the method if
   * required.
   * 
   * @param canvas the canvas ({@code null} not permitted).
   * @param e the event ({@code null} not permitted).
   */
  @Override
  public void handleMouseDragged(ChartCanvas canvas, MouseEvent e) {
    // does nothing unless overridden
  }

  /**
   * Handles a mouse released event. This implementation does nothing, override the method if
   * required.
   * 
   * @param canvas the canvas ({@code null} not permitted).
   * @param e the event ({@code null} not permitted).
   */
  @Override
  public void handleMouseReleased(ChartCanvas canvas, MouseEvent e) {
    // does nothing unless overridden
  }

  /**
   * Handles a scroll event. This implementation does nothing, override the method if required.
   * 
   * @param canvas the canvas ({@code null} not permitted).
   * @param e the event ({@code null} not permitted).
   */
  @Override
  public void handleScroll(ChartCanvas canvas, ScrollEvent e) {
    // does nothing unless overridden
  }

  /**
   * Handles a mouse pressed event by recording the initial mouse pointer location.
   * 
   * @param canvas the JavaFX canvas ({@code null} not permitted).
   * @param e the mouse event ({@code null} not permitted).
   */
  @Override
  public void handleMousePressed(ChartCanvas canvas, MouseEvent e) {
    Point2D pt = new Point2D.Double(e.getX(), e.getY());
    Rectangle2D dataArea = canvas.findDataArea(pt);
    if (dataArea != null) {
      this.startPoint = ShapeUtils.getPointInRectangle(e.getX(), e.getY(), dataArea);
    } else {
      this.startPoint = null;
      canvas.clearLiveHandler();
    }
  }

  /**
   * Handles a mouse dragged event by updating the zoom rectangle displayed in the ChartViewer.
   * 
   * @param canvas the JavaFX canvas ({@code null} not permitted).
   * @param e the mouse event ({@code null} not permitted).
   */
  @Override
  public void handleMouseDragged(ChartCanvas canvas, MouseEvent e) {
    if (this.startPoint == null) {
      // no initial zoom rectangle exists but the handler is set
      // as life handler unregister
      canvas.clearLiveHandler();
      return;
    }

    boolean hZoom, vZoom;
    Plot p = canvas.getChart().getPlot();
    if (!(p instanceof Zoomable)) {
      return;
    }
    Zoomable z = (Zoomable) p;
    if (z.getOrientation().isHorizontal()) {
      hZoom = z.isRangeZoomable();
      vZoom = z.isDomainZoomable();
    } else {
      hZoom = z.isDomainZoomable();
      vZoom = z.isRangeZoomable();
    }
    Rectangle2D dataArea = canvas.findDataArea(this.startPoint);

    double x = this.startPoint.getX();
    double y = this.startPoint.getY();
    double w = 0;
    double h = 0;
    if (hZoom && vZoom) {
      // selected rectangle shouldn't extend outside the data area...
      double xmax = Math.min(e.getX(), dataArea.getMaxX());
      double ymax = Math.min(e.getY(), dataArea.getMaxY());
      w = xmax - this.startPoint.getX();
      h = ymax - this.startPoint.getY();
    } else if (hZoom) {
      double xmax = Math.min(e.getX(), dataArea.getMaxX());
      y = dataArea.getMinY();
      w = xmax - this.startPoint.getX();
      h = dataArea.getHeight();
    } else if (vZoom) {
      double ymax = Math.min(e.getY(), dataArea.getMaxY());
      x = dataArea.getMinX();
      w = dataArea.getWidth();
      h = ymax - this.startPoint.getY();
    }
    this.viewer.showZoomRectangle(x, y, w, h);
  }

  @Override
  public void handleMouseReleased(ChartCanvas canvas, MouseEvent e) {
    Plot p = canvas.getChart().getPlot();
    if (!(p instanceof Zoomable)) {
      return;
    }
    boolean hZoom, vZoom;
    Zoomable z = (Zoomable) p;
    if (z.getOrientation().isHorizontal()) {
      hZoom = z.isRangeZoomable();
      vZoom = z.isDomainZoomable();
    } else {
      hZoom = z.isDomainZoomable();
      vZoom = z.isRangeZoomable();
    }

    boolean zoomTrigger1 = hZoom && Math.abs(e.getX() - this.startPoint.getX()) >= 10;
    boolean zoomTrigger2 = vZoom && Math.abs(e.getY() - this.startPoint.getY()) >= 10;
    if (zoomTrigger1 || zoomTrigger2) {
      Point2D endPoint = new Point2D.Double(e.getX(), e.getY());
      PlotRenderingInfo pri = canvas.getRenderingInfo().getPlotInfo();
      if ((hZoom && (e.getX() < this.startPoint.getX()))
          || (vZoom && (e.getY() < this.startPoint.getY()))) {
        boolean saved = p.isNotify();
        p.setNotify(false);
        z.zoomDomainAxes(0, pri, endPoint);
        z.zoomRangeAxes(0, pri, endPoint);
        p.setNotify(saved);
      } else {
        double x = this.startPoint.getX();
        double y = this.startPoint.getY();
        double w = e.getX() - x;
        double h = e.getY() - y;
        Rectangle2D dataArea = canvas.findDataArea(this.startPoint);
        double maxX = dataArea.getMaxX();
        double maxY = dataArea.getMaxY();
        // for mouseReleased event, (horizontalZoom || verticalZoom)
        // will be true, so we can just test for either being false;
        // otherwise both are true
        if (!vZoom) {
          y = dataArea.getMinY();
          w = Math.min(w, maxX - this.startPoint.getX());
          h = dataArea.getHeight();
        } else if (!hZoom) {
          x = dataArea.getMinX();
          w = dataArea.getWidth();
          h = Math.min(h, maxY - this.startPoint.getY());
        } else {
          w = Math.min(w, maxX - this.startPoint.getX());
          h = Math.min(h, maxY - this.startPoint.getY());
        }
        Rectangle2D zoomArea = new Rectangle2D.Double(x, y, w, h);

        boolean saved = p.isNotify();
        p.setNotify(false);
        double pw0 = percentW(x, dataArea);
        double pw1 = percentW(x + w, dataArea);
        double ph0 = percentH(y, dataArea);
        double ph1 = percentH(y + h, dataArea);
        PlotRenderingInfo info = this.viewer.getRenderingInfo().getPlotInfo();
        if (z.getOrientation().isVertical()) {
          z.zoomDomainAxes(pw0, pw1, info, endPoint);
          z.zoomRangeAxes(1 - ph1, 1 - ph0, info, endPoint);
        } else {
          z.zoomRangeAxes(pw0, pw1, info, endPoint);
          z.zoomDomainAxes(1 - ph1, 1 - ph0, info, endPoint);
        }
        p.setNotify(saved);

      }
    }
    this.viewer.hideZoomRectangle();
    this.startPoint = null;
    canvas.clearLiveHandler();
  }

  private double percentW(double x, Rectangle2D r) {
    return (x - r.getMinX()) / r.getWidth();
  }

  private double percentH(double y, Rectangle2D r) {
    return (y - r.getMinY()) / r.getHeight();
  }

  private int lastEntityX = -1, lastEntityY = -1;
  private ChartEntity lastEntity = null;
  private ChartGestureEvent lastDragEvent = null;

  // handle gestures
  private List<ChartGestureHandler> gestureHandlers;
  // listen for
  private HashMap<Event, Boolean> listensFor = new HashMap<Event, Boolean>(Event.values().length);

  /**
   * True if Event.ALL or e was added as part of a gesture handler
   * 
   * @param e
   * @return
   */
  private boolean listensFor(Event e) {
    return listensFor.get(Event.ALL) || listensFor.get(e);
  }

  /**
   * Call all handlers for a specific Gesture Also call all parent handlers (AXIS is parent entity
   * of RANGE_AXIS entity ALL is parent of all events/entities)
   * 
   * @param e
   */
  private void handleEvent(final ChartGestureEvent e) {
    if (gestureHandlers != null)
      gestureHandlers.stream().filter(handler -> handler.getGesture().filter(e.getGesture()))
          .forEach(handler -> handler.accept(e));
  }

  /**
   * Add drag handlers for each key (key and handler have to be ordered)
   * 
   * @param g
   * @param handler
   */
  public void addDragGestureHandler(DragHandler[] handler, Key[] key, Entity entity, Button button,
      Orientation orient, Object[] param) {
    ChartGestureHandler h =
        ChartGestureHandler.createDragDiffHandler(handler, key, entity, button, orient, param);
    addGestureHandler(h);
  }

  /**
   * Add a preset handler for specific gestures and ChartMouseGestureEvents
   * 
   * @param g
   * @param handler
   */
  public void addGestureHandler(Handler handler, Entity entity, Event[] event, Button button,
      Key key, Object[] param) {
    ChartGestureHandler h = ChartGestureHandler.createHandler(handler,
        new ChartGesture(entity, event, button, key), param);
    addGestureHandler(h);
  }

  /**
   * Add a handler for specific gestures and ChartMouseGestureEvents
   * 
   * @param g
   * @param handler
   */
  public void addGestureHandler(ChartGestureHandler handler) {
    if (gestureHandlers == null) {
      gestureHandlers = new ArrayList<ChartGestureHandler>();
      for (Event e : Event.values())
        listensFor.put(e, false);
    }

    gestureHandlers.add(handler);
    for (Event e : handler.getGesture().getEvent())
      listensFor.replace(e, true);
  }

  /**
   * Add a handler for specific gestures and ChartMouseGestureEvents
   * 
   * @param g
   * @param handler
   */
  public void removeGestureHandler(ChartGestureHandler handler) {
    if (gestureHandlers == null)
      return;

    if (gestureHandlers.remove(handler)) {
      // reset listenFor
      for (Event e : Event.values())
        listensFor.replace(e, false);

      // set all again
      for (ChartGestureHandler gh : gestureHandlers)
        for (Event e : gh.getGesture().getEvent())
          listensFor.replace(e, true);
    }
  }

  /**
   * Find chartentities like JFreeChartEntity, AxisEntity, PlotEntity, TitleEntity, XY...
   * 
   * @param chartPanel
   * @param x
   * @param y
   * @return
   */
  private ChartEntity findChartEntity(ChartCanvas chartPanel, MouseEventWrapper e) {
    // TODO check if insets were needed
    // coordinates to find chart entities
    int x = (int) ((e.getX()) / chartPanel.getScaleX());
    int y = (int) ((e.getY()) / chartPanel.getScaleY());

    if (lastEntity != null && x == lastEntityX && y == lastEntityY)
      return lastEntity;
    else {
      ChartRenderingInfo info = chartPanel.getRenderingInfo();
      ChartEntity entity = null;
      if (info != null) {
        EntityCollection entities = info.getEntityCollection();
        if (entities != null) {
          entity = entities.getEntity(x, y);
        }
      }
      return entity;
    }
  }

  /**
   * Example how to create a new handler handles all events and prints them
   */
  public void addDebugHandler() {
    addGestureHandler(ChartGestureHandler.createHandler(Handler.DEBUG, // a preset handler
        ChartGesture.ALL, // no gesture filters
        null) // no parameters for this handler
    );
  }

  public void clearHandlers() {
    if (gestureHandlers != null)
      gestureHandlers.clear();
  }

  public List<ChartGestureHandler> getGestureHandlers() {
    return gestureHandlers;
  }
}
