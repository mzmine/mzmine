/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.chartbasics.javafx;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.interaction.MouseHandlerFX;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.RangeType;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import net.sf.mzmine.chartbasics.gestures.ChartGestureHandler;
import net.sf.mzmine.chartbasics.gestures.interf.GestureHandlerFactory;
import net.sf.mzmine.chartbasics.graphicsexport.GraphicsExportDialog;
import net.sf.mzmine.chartbasics.javafx.menu.MenuExportToClipboard;
import net.sf.mzmine.chartbasics.javafx.menu.MenuExportToExcel;
import net.sf.mzmine.chartbasics.listener.AxesRangeChangedListener;
import net.sf.mzmine.chartbasics.listener.AxisRangeChangedListener;
import net.sf.mzmine.chartbasics.listener.ZoomHistory;
import net.sf.mzmine.chartbasics.swing.ChartGestureMouseAdapter;
import net.sf.mzmine.chartbasics.wrapper.ChartViewWrapper;
import net.sf.mzmine.util.io.XSSFExcelWriterReader;

/**
 * This is an extended version of the ChartViewer (JFreeChartFX). it Adds: ChartGestures (with a set
 * of standard chart gestures), ZoomHistory, AxesRangeChangeListener, data export, graphics export,
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class EChartViewer extends ChartViewer {
  private static final long serialVersionUID = 1L;
  private Logger logger = Logger.getLogger(this.getClass().getName());

  protected ZoomHistory zoomHistory;
  protected List<AxesRangeChangedListener> axesRangeListener;
  protected boolean isMouseZoomable = true;
  protected boolean stickyZeroForRangeAxis = false;
  protected boolean standardGestures = true;
  // only for XYData (not for categoryPlots)
  protected boolean addZoomHistory = true;
  private ChartGestureMouseAdapterFX mouseAdapter;


  /**
   * Enhanced ChartPanel with extra scrolling methods, zoom history, graphics and data export<br>
   * stickyZeroForRangeAxis = false <br>
   * Graphics and data export menu are added
   * 
   * @param chart
   */
  public EChartViewer(JFreeChart chart) {
    this(chart, true, true, true, true, false);
  }

  /**
   * Enhanced ChartPanel with extra scrolling methods, zoom history, graphics and data export<br>
   * stickyZeroForRangeAxis = false
   * 
   * @param chart
   * @param graphicsExportMenu adds graphics export menu
   * @param standardGestures adds the standard ChartGestureHandlers
   * @param dataExportMenu adds data export menu
   */
  public EChartViewer(JFreeChart chart, boolean graphicsExportMenu, boolean dataExportMenu,
      boolean standardGestures) {
    this(chart, graphicsExportMenu, dataExportMenu, standardGestures, false);
  }

  /**
   * Enhanced ChartPanel with extra scrolling methods, zoom history, graphics and data export
   * 
   * @param chart
   * @param graphicsExportMenu adds graphics export menu
   * @param dataExportMenu adds data export menu
   * @param standardGestures adds the standard ChartGestureHandlers
   * @param stickyZeroForRangeAxis
   */
  public EChartViewer(JFreeChart chart, boolean graphicsExportMenu, boolean dataExportMenu,
      boolean standardGestures, boolean stickyZeroForRangeAxis) {
    this(chart, graphicsExportMenu, dataExportMenu, standardGestures, true, stickyZeroForRangeAxis);
  }

  /**
   * Enhanced ChartPanel with extra scrolling methods, zoom history, graphics and data export
   * 
   * @param chart
   * @param graphicsExportMenu adds graphics export menu
   * @param dataExportMenu adds data export menu
   * @param standardGestures adds the standard ChartGestureHandlers
   * @param stickyZeroForRangeAxis
   */
  public EChartViewer(JFreeChart chart, boolean graphicsExportMenu, boolean dataExportMenu,
      boolean standardGestures, boolean addZoomHistory, boolean stickyZeroForRangeAxis) {
    super(null);
    this.stickyZeroForRangeAxis = stickyZeroForRangeAxis;
    this.standardGestures = standardGestures;
    this.addZoomHistory = addZoomHistory;
    setChart(chart);

    // Add Export to Excel and graphics export menu
    if (graphicsExportMenu || dataExportMenu)
      addExportMenu(graphicsExportMenu, dataExportMenu);
  }

  protected void addMenuItem(Menu parent, String title, EventHandler<ActionEvent> al) {
    MenuItem pngItem = new MenuItem(title);
    pngItem.setOnAction(al);
    parent.getItems().add(pngItem);
  }

  protected void addMenuItem(ContextMenu parent, String title, EventHandler<ActionEvent> al) {
    MenuItem pngItem = new MenuItem(title);
    pngItem.setOnAction(al);
    parent.getItems().add(pngItem);
  }

  protected void addMenu(ContextMenu menu, Menu m) {
    menu.getItems().add(m);
  }

  @Override
  public void setChart(JFreeChart chart) {
    super.setChart(chart);
    if (chart != null) {
      initChartPanel();
    }
  }

  /**
   * Init ChartPanel Mouse Listener For MouseDraggedOverAxis event For scrolling X-Axis und zooming
   * Y-Axis0
   */
  private void initChartPanel() {
    final EChartViewer chartPanel = this;

    // remove old init
    if (mouseAdapter != null) {
      this.getCanvas().removeMouseHandler(mouseAdapter);
    }

    if (chartPanel.getChart().getPlot() instanceof XYPlot) {
      // set sticky zero
      if (stickyZeroForRangeAxis) {
        ValueAxis rangeAxis = chartPanel.getChart().getXYPlot().getRangeAxis();
        if (rangeAxis instanceof NumberAxis) {
          NumberAxis axis = (NumberAxis) rangeAxis;
          axis.setAutoRangeIncludesZero(true);
          axis.setAutoRange(true);
          axis.setAutoRangeStickyZero(true);
          axis.setRangeType(RangeType.POSITIVE);
        }
      }

      if (addZoomHistory) {
        // zoom history
        zoomHistory = new ZoomHistory(this, 20);

        // axis range changed listener for zooming and more
        ValueAxis rangeAxis = this.getChart().getXYPlot().getRangeAxis();
        ValueAxis domainAxis = this.getChart().getXYPlot().getDomainAxis();
        if (rangeAxis != null) {
          rangeAxis.addChangeListener(new AxisRangeChangedListener(new ChartViewWrapper(this)) {
            @Override
            public void axisRangeChanged(ChartViewWrapper chart, ValueAxis axis, Range lastR,
                Range newR) {
              // notify listeners of changed range
              if (axesRangeListener != null)
                for (AxesRangeChangedListener l : axesRangeListener)
                  l.axesRangeChanged(chart, axis, lastR, newR);
            }
          });
        }
        if (domainAxis != null) {
          domainAxis.addChangeListener(new AxisRangeChangedListener(new ChartViewWrapper(this)) {
            @Override
            public void axisRangeChanged(ChartViewWrapper chart, ValueAxis axis, Range lastR,
                Range newR) {
              // notify listeners of changed range
              if (axesRangeListener != null)
                for (AxesRangeChangedListener l : axesRangeListener)
                  l.axesRangeChanged(chart, axis, lastR, newR);
            }
          });
        }
      }

      // mouse adapter for scrolling and zooming
      mouseAdapter = new ChartGestureMouseAdapterFX("gestures", this);
      // mouseAdapter.addDebugHandler();
      addMouseHandler(mouseAdapter);

      // add gestures
      if (standardGestures) {
        addStandardGestures();
      }
    }
  }


  public void addMouseHandler(MouseHandlerFX handler) {
    this.getCanvas().addAuxiliaryMouseHandler(handler);
  }

  /**
   * Adds all standard gestures defined in {@link ChartGestureHandler#getStandardGestures()}
   */
  public void addStandardGestures() {
    // add ChartGestureHandlers
    ChartGestureMouseAdapterFX m = getGestureAdapter();
    if (m != null) {
      m.clearHandlers();
      for (GestureHandlerFactory f : ChartGestureHandler.getStandardGestures())
        m.addGestureHandler(f.createHandler());

      logger.log(Level.INFO, "Added standard gestures: " + m.getGestureHandlers().size());
    }
  }

  /**
   * Adds the GraphicsExportDialog menu and the data export menu
   */
  protected void addExportMenu(boolean graphics, boolean data) {
    if (graphics) {
      // Graphics Export
      addMenuItem(getContextMenu(), "Export graphics...",
          e -> GraphicsExportDialog.openDialog(getChart()));
    }
    if (data) {
      // General data export
      Menu export = new Menu("Export data ...");
      // Excel XY
      MenuExportToExcel exportXY =
          new MenuExportToExcel(new XSSFExcelWriterReader(), "to Excel", this);
      export.getItems().add(exportXY);
      // clip board
      MenuExportToClipboard exportXYClipboard = new MenuExportToClipboard("to Clipboard", this);
      export.getItems().add(exportXYClipboard);
      // add to panel
      addMenu(getContextMenu(), export);
    }
  }

  /**
   * Default tries to extract all series from an XYDataset or XYZDataset<br>
   * series 1 | Series 2 <br>
   * x y x y x y z x y z
   * 
   * @return Data array[columns][rows]
   */
  public Object[][] getDataArrayForExport() {
    if (getChart().getPlot() instanceof XYPlot && getChart().getXYPlot() != null
        && getChart().getXYPlot().getDataset() != null) {
      try {
        List<Object[]> modelList = new ArrayList<>();

        for (int d = 0; d < getChart().getXYPlot().getDatasetCount(); d++) {
          XYDataset data = getChart().getXYPlot().getDataset(d);
          if (data instanceof XYZDataset) {
            XYZDataset xyz = (XYZDataset) data;
            int series = data.getSeriesCount();
            Object[][] model = new Object[series * 3][];
            for (int s = 0; s < series; s++) {
              int size = 2 + xyz.getItemCount(s);
              Object[] x = new Object[size];
              Object[] y = new Object[size];
              Object[] z = new Object[size];
              // create new Array model[row][col]
              // Write header
              Comparable title = data.getSeriesKey(series);
              x[0] = title;
              y[0] = "";
              z[0] = "";
              x[1] = getChart().getXYPlot().getDomainAxis().getLabel();
              y[1] = getChart().getXYPlot().getRangeAxis().getLabel();
              z[1] = "z-axis";
              // write data
              for (int i = 0; i < xyz.getItemCount(s); i++) {
                x[i + 2] = xyz.getX(s, i);
                y[i + 2] = xyz.getY(s, i);
                z[i + 2] = xyz.getZ(s, i);
              }
              model[s * 3] = x;
              model[s * 3 + 1] = y;
              model[s * 3 + 2] = z;
            }

            for (Object[] o : model)
              modelList.add(o);
          } else {
            int series = data.getSeriesCount();
            Object[][] model = new Object[series * 2][];
            for (int s = 0; s < series; s++) {
              int size = 2 + data.getItemCount(s);
              Object[] x = new Object[size];
              Object[] y = new Object[size];
              // create new Array model[row][col]
              // Write header
              Comparable title = data.getSeriesKey(s);
              x[0] = title;
              y[0] = "";
              x[1] = getChart().getXYPlot().getDomainAxis().getLabel();
              y[1] = getChart().getXYPlot().getRangeAxis().getLabel();
              // write data
              for (int i = 0; i < data.getItemCount(s); i++) {
                x[i + 2] = data.getX(s, i);
                y[i + 2] = data.getY(s, i);
              }
              model[s * 2] = x;
              model[s * 2 + 1] = y;
            }

            for (Object[] o : model)
              modelList.add(o);
          }
        }

        return modelList.toArray(new Object[modelList.size()][]);
      } catch (Exception ex) {
        logger.log(Level.WARNING, "Cannot retrieve data for export", ex);
        return null;
      }
    }
    return null;
  }

  public void addAxesRangeChangedListener(AxesRangeChangedListener l) {
    if (axesRangeListener == null)
      axesRangeListener = new ArrayList<AxesRangeChangedListener>(1);
    axesRangeListener.add(l);
  }

  public void removeAxesRangeChangedListener(AxesRangeChangedListener l) {
    if (axesRangeListener != null)
      axesRangeListener.remove(l);
  }

  public void clearAxesRangeChangedListeners() {
    if (axesRangeListener != null)
      axesRangeListener.clear();
  }

  public void setMouseZoomable(boolean flag) {
    setDomainZoomable(flag);
    setRangeZoomable(flag);
    isMouseZoomable = flag;
  }


  public void setRangeZoomable(boolean flag) {
    getCanvas().setRangeZoomable(flag);
  }

  public void setDomainZoomable(boolean flag) {
    getCanvas().setDomainZoomable(flag);
  }

  public boolean isMouseZoomable() {
    return isMouseZoomable;
  }

  public boolean isDomainZoomable() {
    return getCanvas().isDomainZoomable();
  }

  public boolean isRangeZoomable() {
    return getCanvas().isRangeZoomable();
  }

  public ZoomHistory getZoomHistory() {
    return zoomHistory;
  }

  public void setZoomHistory(ZoomHistory h) {
    zoomHistory = h;
  }

  /**
   * Returns the {@link ChartGestureMouseAdapter} alternatively for other ChartPanel classes use:
   * 
   * <pre>
   * for(MouseListener l : getMouseListeners())
   *    if(ChartGestureMouseAdapter.class.isInstance(l)){
   *        ChartGestureMouseAdapter m = (ChartGestureMouseAdapter) l;
   * </pre>
   * 
   * @return
   */
  public ChartGestureMouseAdapterFX getGestureAdapter() {
    return mouseAdapter;
  }

  public void setGestureAdapter(ChartGestureMouseAdapterFX mouseAdapter) {
    this.mouseAdapter = mouseAdapter;
  }
}
