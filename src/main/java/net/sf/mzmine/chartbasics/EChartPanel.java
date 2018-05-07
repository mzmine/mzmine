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

package net.sf.mzmine.chartbasics;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.Range;
import org.jfree.data.RangeType;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import net.sf.mzmine.chartbasics.gestures.ChartGestureHandler;
import net.sf.mzmine.chartbasics.gestures.ChartGestureMouseAdapter;
import net.sf.mzmine.chartbasics.gestures.interf.GestureHandlerFactory;
import net.sf.mzmine.chartbasics.graphicsexport.ChartExportUtil;
import net.sf.mzmine.chartbasics.listener.AspectRatioListener;
import net.sf.mzmine.chartbasics.listener.AxesRangeChangedListener;
import net.sf.mzmine.chartbasics.listener.AxisRangeChangedListener;
import net.sf.mzmine.chartbasics.listener.ZoomHistory;
import net.sf.mzmine.chartbasics.menu.MenuExportToClipboard;
import net.sf.mzmine.chartbasics.menu.MenuExportToExcel;
import net.sf.mzmine.util.io.XSSFExcelWriterReader;

/**
 * Enhanced ChartPanel with extra chart gestures (drag mouse over entities (e.g., axis, titles)
 * ZoomHistory, GraphicsExportDialog, axesRangeListener included
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class EChartPanel extends ChartPanel {
  private static final long serialVersionUID = 1L;

  protected ZoomHistory zoomHistory;
  protected List<AxesRangeChangedListener> axesRangeListener;
  protected AspectRatioListener aspectRatioListener;
  protected boolean isMouseZoomable = true;
  protected ChartGestureMouseAdapter mouseAdapter;

  /**
   * Enhanced ChartPanel with extra scrolling methods, zoom history, graphics and data export<br>
   * stickyZeroForRangeAxis = false <br>
   * Graphics and data export menu are added
   * 
   * @param chart
   */
  public EChartPanel(JFreeChart chart) {
    this(chart, true, true, true, true, false);
  }

  /**
   * Enhanced ChartPanel with extra scrolling methods, zoom history, graphics and data export<br>
   * stickyZeroForRangeAxis = false <br>
   * Graphics and data export menu are added
   * 
   * @param chart
   */
  public EChartPanel(JFreeChart chart, boolean useBuffer) {
    this(chart, useBuffer, true, true, true, false);
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
  public EChartPanel(JFreeChart chart, boolean graphicsExportMenu, boolean dataExportMenu,
      boolean standardGestures) {
    this(chart, graphicsExportMenu, dataExportMenu, standardGestures, false);
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
  public EChartPanel(JFreeChart chart, boolean useBuffer, boolean graphicsExportMenu,
      boolean dataExportMenu, boolean standardGestures) {
    this(chart, useBuffer, graphicsExportMenu, dataExportMenu, standardGestures, false);
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
  public EChartPanel(JFreeChart chart, boolean useBuffer, boolean graphicsExportMenu,
      boolean dataExportMenu, boolean standardGestures, boolean stickyZeroForRangeAxis) {
    super(chart, useBuffer);
    // super(chart, true, false, true, true, true);
    // setDoubleBuffered(useBuffer);
    // setRefreshBuffer(useBuffer);
    initChartPanel(stickyZeroForRangeAxis);
    // Add Export to Excel and graphics export menu
    if (graphicsExportMenu || dataExportMenu)
      addExportMenu(graphicsExportMenu, dataExportMenu);

    // add gestures
    if (standardGestures)
      addStandardGestures();
  }

  /**
   * Adds all standard gestures defined in {@link ChartGestureHandler#getStandardGestures()}
   */
  public void addStandardGestures() {
    // add ChartGestureHandlers
    ChartGestureMouseAdapter m = getGestureAdapter();
    if (m != null) {
      for (GestureHandlerFactory f : ChartGestureHandler.getStandardGestures())
        m.addGestureHandler(f.createHandler());
    }
  }

  /**
   * Init ChartPanel Mouse Listener For MouseDraggedOverAxis event For scrolling X-Axis und zooming
   * Y-Axis0
   */
  private void initChartPanel(boolean stickyZeroForRangeAxis) {
    final EChartPanel chartPanel = this;

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

    // zoom history
    zoomHistory = new ZoomHistory(this, 20);

    // axis range changed listener for zooming and more
    ValueAxis rangeAxis = this.getChart().getXYPlot().getRangeAxis();
    ValueAxis domainAxis = this.getChart().getXYPlot().getDomainAxis();
    if (rangeAxis != null) {
      rangeAxis.addChangeListener(new AxisRangeChangedListener(this) {
        @Override
        public void axisRangeChanged(ChartPanel chart, ValueAxis axis, Range lastR, Range newR) {
          // resize according to aspect ratio of domain to range axis
          if (aspectRatioListener != null)
            aspectRatioListener.resize(chartPanel);
          // notify listeners of changed range
          if (axesRangeListener != null)
            for (AxesRangeChangedListener l : axesRangeListener)
              l.axesRangeChanged(chartPanel, axis, lastR, newR);
        }
      });
    }
    if (domainAxis != null) {
      domainAxis.addChangeListener(new AxisRangeChangedListener(this) {
        @Override
        public void axisRangeChanged(ChartPanel chart, ValueAxis axis, Range lastR, Range newR) {
          // resize according to aspect ratio of domain to range axis
          if (aspectRatioListener != null)
            aspectRatioListener.resize(chartPanel);
          // notify listeners of changed range
          if (axesRangeListener != null)
            for (AxesRangeChangedListener l : axesRangeListener)
              l.axesRangeChanged(chartPanel, axis, lastR, newR);
        }
      });
    }

    // mouse adapter for scrolling and zooming
    mouseAdapter = new ChartGestureMouseAdapter();
    // mouseAdapter.addDebugHandler();
    this.addMouseListener(mouseAdapter);
    this.addMouseMotionListener(mouseAdapter);
    this.addMouseWheelListener(mouseAdapter);
  }

  @Override
  public void setMouseZoomable(boolean flag) {
    super.setMouseZoomable(flag);
    isMouseZoomable = flag;
  }

  /**
   * Default tries to extract all series from an XYDataset or XYZDataset<br>
   * series 1 | Series 2 <br>
   * x y x y x y z x y z
   * 
   * @return Data array[columns][rows]
   */
  public Object[][] getDataArrayForExport() {
    if (getChart().getXYPlot() != null && getChart().getXYPlot().getDataset() != null) {
      try {
        XYDataset data = getChart().getXYPlot().getDataset();
        if (data instanceof XYZDataset) {
          XYZDataset xyz = (XYZDataset) data;
          int series = data.getSeriesCount();
          Object[][] model = new Object[series * 3][];
          for (int s = 0; s < series; s++) {
            int size = 1 + xyz.getItemCount(s);
            Object[] x = new Object[size];
            Object[] y = new Object[size];
            Object[] z = new Object[size];
            // create new Array model[row][col]
            // Write header
            x[0] = getChart().getXYPlot().getDomainAxis().getLabel();
            y[0] = getChart().getXYPlot().getRangeAxis().getLabel();
            z[0] = "z-axis";
            // write data
            for (int i = 0; i < xyz.getItemCount(s); i++) {
              x[i + 1] = xyz.getX(s, i);
              y[i + 1] = xyz.getY(s, i);
              z[i + 1] = xyz.getZ(s, i);
            }
            model[s * 3] = x;
            model[s * 3 + 1] = y;
            model[s * 3 + 2] = z;
          }
          return model;
        } else {
          int series = data.getSeriesCount();
          Object[][] model = new Object[series * 2][];
          for (int s = 0; s < series; s++) {
            int size = 1 + data.getItemCount(s);
            Object[] x = new Object[size];
            Object[] y = new Object[size];
            // create new Array model[row][col]
            // Write header
            x[0] = getChart().getXYPlot().getDomainAxis().getLabel();
            y[0] = getChart().getXYPlot().getRangeAxis().getLabel();
            // write data
            for (int i = 0; i < data.getItemCount(s); i++) {
              x[i + 1] = data.getX(s, i);
              y[i + 1] = data.getY(s, i);
            }
            model[s * 2] = x;
            model[s * 2 + 1] = y;
          }
          return model;
        }
      } catch (Exception ex) {
        return null;
      }
    }
    return null;

  }

  /*
   * ############################################################### Export Graphics
   */
  /**
   * Adds the GraphicsExportDialog menu and the data export menu
   */
  protected void addExportMenu(boolean graphics, boolean data) {
    this.getPopupMenu().addSeparator();
    if (graphics) {
      // Graphics Export
      ChartExportUtil.addExportDialogToMenu(this);
    }
    if (data) {
      // General data export
      JMenu export = new JMenu("Export data ...");
      // Excel XY
      MenuExportToExcel exportXY =
          new MenuExportToExcel(new XSSFExcelWriterReader(), "to Excel", this);
      export.add(exportXY);
      // clip board
      MenuExportToClipboard exportXYClipboard = new MenuExportToClipboard("to Clipboard", this);
      export.add(exportXYClipboard);
      // add to panel
      addPopupMenu(export);
    }
  }

  public void addPopupMenuItem(JMenuItem item) {
    this.getPopupMenu().add(item);
  }

  public void addPopupMenu(JMenu menu) {
    this.getPopupMenu().add(menu);
  }

  /**
   * AspectRatioListener needed if domain and range axes share the same dimension
   * 
   * @param listener
   */
  public void setAspectRatioListener(AspectRatioListener listener) {
    aspectRatioListener = listener;
  }

  public AspectRatioListener getAspectRatioListener() {
    return aspectRatioListener;
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

  public boolean isMouseZoomable() {
    return isMouseZoomable;
  }

  public ZoomHistory getZoomHistory() {
    return zoomHistory;
  }

  /**
   * Returns the {@link ChartGestureMouseAdapter} alternatively for other ChartPanel classes use:
   * 
   * <pre>
   * for(MouseListener l : getMouseListeners())
   * 	if(ChartGestureMouseAdapter.class.isInstance(l)){
   * 		ChartGestureMouseAdapter m = (ChartGestureMouseAdapter) l;
   * </pre>
   * 
   * @return
   */
  public ChartGestureMouseAdapter getGestureAdapter() {
    return mouseAdapter;
  }

  public void setGestureAdapter(ChartGestureMouseAdapter mouseAdapter) {
    this.mouseAdapter = mouseAdapter;
  }
}
