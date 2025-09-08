/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.gui.chartbasics.gui.swing;

import io.github.mzmine.gui.chartbasics.JFreeChartUtils;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gestures.interf.GestureHandlerFactory;
import io.github.mzmine.gui.chartbasics.graphicsexport.ChartExportUtil;
import io.github.mzmine.gui.chartbasics.gui.swing.menu.JMenuExportToClipboard;
import io.github.mzmine.gui.chartbasics.gui.swing.menu.JMenuExportToExcel;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.listener.AxesRangeChangedListener;
import io.github.mzmine.gui.chartbasics.listener.AxisRangeChangedListener;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.util.io.XSSFExcelWriterReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.CombinedRangeXYPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.RangeType;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

/**
 * Enhanced ChartPanel with extra chart gestures (drag mouse over entities (e.g., axis, titles)
 * ZoomHistory, GraphicsExportDialog, axesRangeListener included
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class EChartPanel extends ChartPanel {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(EChartPanel.class.getName());

  protected ZoomHistory zoomHistory;
  protected List<AxesRangeChangedListener> axesRangeListener;
  protected boolean isMouseZoomable = true;
  protected boolean stickyZeroForRangeAxis = false;
  protected boolean standardGestures = true;
  // only for XYData (not for categoryPlots)
  protected boolean addZoomHistory = true;
  protected ChartGestureMouseAdapter mouseAdapter;

  /**
   * Enhanced ChartPanel with extra scrolling methods, zoom history, graphics and data export<br>
   * stickyZeroForRangeAxis = false <br> Graphics and data export menu are added
   *
   * @param chart
   */
  public EChartPanel(JFreeChart chart) {
    this(chart, true, true, true, true, false);
  }

  /**
   * Enhanced ChartPanel with extra scrolling methods, zoom history, graphics and data export<br>
   * stickyZeroForRangeAxis = false <br> Graphics and data export menu are added
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
   * @param standardGestures   adds the standard ChartGestureHandlers
   * @param dataExportMenu     adds data export menu
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
   * @param standardGestures   adds the standard ChartGestureHandlers
   * @param dataExportMenu     adds data export menu
   */
  public EChartPanel(JFreeChart chart, boolean useBuffer, boolean graphicsExportMenu,
      boolean dataExportMenu, boolean standardGestures) {
    this(chart, useBuffer, graphicsExportMenu, dataExportMenu, standardGestures, false);
  }

  /**
   * Enhanced ChartPanel with extra scrolling methods, zoom history, graphics and data export
   *
   * @param chart
   * @param graphicsExportMenu     adds graphics export menu
   * @param dataExportMenu         adds data export menu
   * @param standardGestures       adds the standard ChartGestureHandlers
   * @param stickyZeroForRangeAxis
   */
  public EChartPanel(JFreeChart chart, boolean useBuffer, boolean graphicsExportMenu,
      boolean dataExportMenu, boolean standardGestures, boolean stickyZeroForRangeAxis) {
    this(chart, useBuffer, graphicsExportMenu, dataExportMenu, standardGestures, true,
        stickyZeroForRangeAxis);
  }

  /**
   * Enhanced ChartPanel with extra scrolling methods, zoom history, graphics and data export
   *
   * @param chart
   * @param graphicsExportMenu     adds graphics export menu
   * @param dataExportMenu         adds data export menu
   * @param standardGestures       adds the standard ChartGestureHandlers
   * @param stickyZeroForRangeAxis
   */
  public EChartPanel(JFreeChart chart, boolean useBuffer, boolean graphicsExportMenu,
      boolean dataExportMenu, boolean standardGestures, boolean addZoomHistory,
      boolean stickyZeroForRangeAxis) {
    super(null, useBuffer);
    this.stickyZeroForRangeAxis = stickyZeroForRangeAxis;
    this.standardGestures = standardGestures;
    this.addZoomHistory = addZoomHistory;
    setChart(chart);
    // setDoubleBuffered(useBuffer);
    // setRefreshBuffer(useBuffer);
    // Add Export to Excel and graphics export menu
    if (graphicsExportMenu || dataExportMenu) {
      addExportMenu(graphicsExportMenu, dataExportMenu);
    }
  }

  /**
   * Adds all standard gestures defined in {@link ChartGestureHandler#getStandardGestures()}
   */
  public void addStandardGestures() {
    // add ChartGestureHandlers
    ChartGestureMouseAdapter m = getGestureAdapter();
    if (m != null) {
      m.clearHandlers();
      for (GestureHandlerFactory f : ChartGestureHandler.getStandardGestures()) {
        m.addGestureHandler(f.createHandler());
      }

      logger.log(Level.FINEST, "Added standard gestures: " + m.getGestureHandlers().size());
    }
  }

  @Override
  public void setChart(JFreeChart chart) {
    super.setChart(chart);
    if (chart != null) {
      initChartPanel(stickyZeroForRangeAxis);
    }
  }

  /**
   * Init ChartPanel Mouse Listener For MouseDraggedOverAxis event For scrolling X-Axis und zooming
   * Y-Axis0
   */
  private void initChartPanel(boolean stickyZeroForRangeAxis) {
    final EChartPanel chartPanel = this;

    // remove old init
    if (mouseAdapter != null) {
      this.removeMouseListener(mouseAdapter);
      this.removeMouseMotionListener(mouseAdapter);
      this.removeMouseWheelListener(mouseAdapter);
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

      Plot p = getChart().getPlot();
      if (addZoomHistory && (p instanceof XYPlot) && !(p instanceof CombinedDomainXYPlot
          || p instanceof CombinedRangeXYPlot)) {
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
              if (axesRangeListener != null) {
                for (AxesRangeChangedListener l : axesRangeListener) {
                  l.axesRangeChanged(chart, axis, lastR, newR);
                }
              }
            }
          });
        }
        if (domainAxis != null) {
          domainAxis.addChangeListener(new AxisRangeChangedListener(new ChartViewWrapper(this)) {
            @Override
            public void axisRangeChanged(ChartViewWrapper chart, ValueAxis axis, Range lastR,
                Range newR) {
              // notify listeners of changed range
              if (axesRangeListener != null) {
                for (AxesRangeChangedListener l : axesRangeListener) {
                  l.axesRangeChanged(chart, axis, lastR, newR);
                }
              }
            }
          });
        }
      }

      // mouse adapter for scrolling and zooming
      mouseAdapter = new ChartGestureMouseAdapter();
      // mouseAdapter.addDebugHandler();
      this.addMouseListener(mouseAdapter);
      this.addMouseMotionListener(mouseAdapter);
      this.addMouseWheelListener(mouseAdapter);

      // add gestures
      if (standardGestures) {
        addStandardGestures();
      }
    }
  }

  @Override
  public void setMouseZoomable(boolean flag) {
    super.setMouseZoomable(flag);
    isMouseZoomable = flag;
  }

  /**
   * Default tries to extract all series from an XYDataset or XYZDataset<br> series 1 | Series 2
   * <br> x y x y x y z x y z
   *
   * @return Data array[columns][rows]
   */
  public Object[][] getDataArrayForExport() {
    if (getChart().getPlot() instanceof XYPlot plot) {
      try {
        List<Object[]> modelList = new ArrayList<>();

        int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
        for (int d = 0; d < numDatasets; d++) {
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

            for (Object[] o : model) {
              modelList.add(o);
            }
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

            for (Object[] o : model) {
              modelList.add(o);
            }
          }
        }

        return modelList.toArray(new Object[modelList.size()][]);
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
      JMenuExportToExcel exportXY = new JMenuExportToExcel(new XSSFExcelWriterReader(), "to Excel",
          this);
      export.add(exportXY);
      // clip board
      JMenuExportToClipboard exportXYClipboard = new JMenuExportToClipboard("to Clipboard", this);
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
   * Opens a file chooser and gives the user an opportunity to save the chart in PNG format.
   *
   * @throws IOException if there is an I/O error.
   */
  @Override
  public void doSaveAs() throws IOException {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setCurrentDirectory(this.getDefaultDirectoryForSaveAs());
    FileNameExtensionFilter filter = new FileNameExtensionFilter(
        localizationResources.getString("PNG_Image_Files"), "png");
    fileChooser.addChoosableFileFilter(filter);
    fileChooser.setFileFilter(filter);

    int option = fileChooser.showSaveDialog(this);
    if (option == JFileChooser.APPROVE_OPTION) {
      String filename = fileChooser.getSelectedFile().getPath();
      if (isEnforceFileExtensions()) {
        if (!filename.endsWith(".png")) {
          filename = filename + ".png";
        }
      }
      ChartUtils.saveChartAsPNG(new File(filename), getChart(), getWidth(), getHeight(),
          getChartRenderingInfo());
    }
  }

  public void addAxesRangeChangedListener(AxesRangeChangedListener l) {
    if (axesRangeListener == null) {
      axesRangeListener = new ArrayList<AxesRangeChangedListener>(1);
    }
    axesRangeListener.add(l);
  }

  public void removeAxesRangeChangedListener(AxesRangeChangedListener l) {
    if (axesRangeListener != null) {
      axesRangeListener.remove(l);
    }
  }

  public void clearAxesRangeChangedListeners() {
    if (axesRangeListener != null) {
      axesRangeListener.clear();
    }
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

  public void setZoomHistory(ZoomHistory h) {
    zoomHistory = h;
  }
}
