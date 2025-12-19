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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.gui.chartbasics.gui.javafx;

import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.gui.chartbasics.JFreeChartUtils;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gestures.interf.GestureHandlerFactory;
import io.github.mzmine.gui.chartbasics.graphicsexport.GraphicsExportModule;
import io.github.mzmine.gui.chartbasics.graphicsexport.GraphicsExportParameters;
import io.github.mzmine.gui.chartbasics.gui.javafx.menu.MenuExportToClipboard;
import io.github.mzmine.gui.chartbasics.gui.javafx.menu.MenuExportToExcel;
import io.github.mzmine.gui.chartbasics.gui.javafx.model.FxEChartViewerModel;
import io.github.mzmine.gui.chartbasics.gui.swing.ChartGestureMouseAdapter;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.listener.AxesRangeChangedListener;
import io.github.mzmine.gui.chartbasics.listener.AxisRangeChangedListener;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.dialogs.AxesSetupDialog;
import io.github.mzmine.util.io.XSSFExcelWriterReader;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.print.PrinterJob;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.interaction.MouseHandlerFX;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.CombinedRangeXYPlot;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.data.Range;
import org.jfree.data.RangeType;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

/// This is an extended version of the ChartViewer (JFreeChartFX). it Adds:
///
/// - [DelayedChartDrawAdapter] is attached in the constructor and accumulates change events to
/// reduce redundant draw call
/// - [ChartGesture] (with a set of standard chart gestures)
/// - [ZoomHistory]
/// - [AxesRangeChangedListener]
/// - data export
/// - graphics export
///
/// @author Robin Schmid (robinschmid@uni-muenster.de)
public class EChartViewer extends ChartViewer implements DatasetChangeListener {

  private static final Logger logger = Logger.getLogger(EChartViewer.class.getName());
  // one history for each plot/subplot
  protected ZoomHistory zoomHistory;
  protected List<AxesRangeChangedListener> axesRangeListener;
  protected boolean isMouseZoomable = true;
  protected boolean stickyZeroForRangeAxis = true;
  protected boolean standardGestures = true;
  // only for XYData (not for categoryPlots)
  protected boolean addZoomHistory = true;
  private ChartGestureMouseAdapterFX mouseAdapter;
  private final FxEChartViewerModel model = new FxEChartViewerModel(this);

  /**
   * Enhanced ChartPanel with extra scrolling methods, zoom history, graphics and data export<br>
   * stickyZeroForRangeAxis = false <br> Graphics and data export menu are added
   */
  public EChartViewer() {
    this(null, true, true, true, true, true);
  }

  /**
   * Enhanced ChartPanel with extra scrolling methods, zoom history, graphics and data export<br>
   * stickyZeroForRangeAxis = false <br> Graphics and data export menu are added
   *
   * @param chart
   */
  public EChartViewer(JFreeChart chart) {
    this(chart, true, true, true, true, true);
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
  public EChartViewer(JFreeChart chart, boolean graphicsExportMenu, boolean dataExportMenu,
      boolean standardGestures) {
    this(chart, graphicsExportMenu, dataExportMenu, standardGestures, true);
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
  public EChartViewer(JFreeChart chart, boolean graphicsExportMenu, boolean dataExportMenu,
      boolean standardGestures, boolean stickyZeroForRangeAxis) {
    this(chart, graphicsExportMenu, dataExportMenu, standardGestures, true, stickyZeroForRangeAxis);
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
  public EChartViewer(JFreeChart chart, boolean graphicsExportMenu, boolean dataExportMenu,
      boolean standardGestures, boolean addZoomHistory, boolean stickyZeroForRangeAxis) {
    super(null);
    // attach to accumulate chart change events and reduce draw calls
    DelayedChartDrawAdapter.attach(this);

    this.stickyZeroForRangeAxis = stickyZeroForRangeAxis;
    this.standardGestures = standardGestures;
    this.addZoomHistory = addZoomHistory;

    // Add chart and configure
    if (chart != null) {
      setChart(chart);
      ChartLogicsFX.setAxesMargins(getChart(), 0.05);
    }

    // remove the standard export
    getContextMenu().getItems().remove(0);

    // Add Export to Excel and graphics export menu
    if (graphicsExportMenu || dataExportMenu) {
      addExportMenu(graphicsExportMenu, dataExportMenu);
    }

    addMenuItem(getContextMenu(), "Reset Zoom", event -> {
      ValueAxis xAxis = getChart().getXYPlot().getDomainAxis();
      ValueAxis yAxis = getChart().getXYPlot().getDomainAxis();
      xAxis.setAutoRange(true);
      yAxis.setAutoRange(true);
    });

    addMenuItem(getContextMenu(), "Set Range on Axis", event -> {
      AxesSetupDialog dialog = new AxesSetupDialog(this.getScene().getWindow(), chart.getXYPlot());
      dialog.show();
    });

    addMenuItem(getContextMenu(), "Copy chart to clipboard", event -> {
      BufferedImage bufferedImage = getChart().createBufferedImage((int) this.getWidth(),
          (int) this.getHeight());
      Image image = SwingFXUtils.toFXImage(bufferedImage, null);
      ClipboardContent content = new ClipboardContent();
      content.putImage(image);
      Clipboard.getSystemClipboard().setContent(content);
    });

    addMenuItem(getContextMenu(), "Print", event -> {
      BufferedImage bufferedImage = getChart().createBufferedImage((int) this.getWidth(),
          (int) this.getHeight());
      Image image = SwingFXUtils.toFXImage(bufferedImage, null);
      ImageView imageView = new ImageView(image);
      PrinterJob job = PrinterJob.createPrinterJob();
      if (job != null) {
        boolean doPrint = job.showPrintDialog(this.getScene().getWindow());
        if (doPrint) {
          job.printPage(imageView);
          job.endJob();
        }
      } else {
        MZmineCore.getDesktop().displayErrorMessage("No Printing Service Found");
      }
    });

  }

  public FxEChartViewerModel getModel() {
    return model;
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


  @Override
  public void setChart(JFreeChart chart) {

    final JFreeChart oldChart = getChart();
    if (oldChart == chart) {
      // setting the same chart again makes no sense and will lead to issues with the DelayedChartDrawAdapter
      // because the model chart property will call no update as its the same instance
      return;
    }

    if (oldChart != null) {
      // may need to remove some listeners here
    }

    super.setChart(chart);
    // requires model set chart after internal set
    // first the chart view should add itself to the chart
    // then {@link DelayedChartDrawAdapter} will exchange the listener
    model.setChart(chart);

    // TODO remove
//    addChartDrawDebugListener(() -> {
//      String id = JFreeChartUtils.createChartLogIdentifier(this, getChart());
//    });

    // If no chart, end here
    if (chart == null) {
      return;
    }

    final EChartViewer chartPanel = this;

    // apply the theme here, let's see how that works
    MZmineCore.getConfiguration().getDefaultChartTheme().apply(chart);

    // remove old init
    if (mouseAdapter != null) {
      this.getCanvas().removeMouseHandler(mouseAdapter);
    }

    if (chartPanel.getChart().getPlot() instanceof XYPlot) {
      // set sticky zero
      setStickyZeroRangeAxis(this.stickyZeroForRangeAxis);

      Plot p = getChart().getPlot();
      if (addZoomHistory && p instanceof XYPlot && !(p instanceof CombinedDomainXYPlot
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
      if (mouseAdapter == null) {
        mouseAdapter = new ChartGestureMouseAdapterFX("gestures", this);
        addMouseHandler(mouseAdapter);
        // add gestures
        if (standardGestures) {
          addStandardGestures();
        }
      }
      //      mouseAdapter.addDebugHandler();
    }
  }

  public void setStickyZeroRangeAxis(boolean stickyZeroForRangeAxis) {
    ValueAxis rangeAxis = this.getChart().getXYPlot().getRangeAxis();
    if (rangeAxis instanceof NumberAxis axis) {
      axis.setAutoRangeIncludesZero(stickyZeroForRangeAxis);
      axis.setAutoRangeStickyZero(stickyZeroForRangeAxis);
      axis.setRangeType(stickyZeroForRangeAxis ? RangeType.POSITIVE : RangeType.FULL);
      axis.setAutoRange(true);
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
      for (GestureHandlerFactory f : ChartGestureHandler.getStandardGestures()) {
        m.addGestureHandler(f.createHandler());
      }
    }
  }

  /**
   * The mouse adapter to handle various gestures
   */
  public ChartGestureMouseAdapterFX getMouseAdapter() {
    return mouseAdapter;
  }

  /**
   * Adds the GraphicsExportDialog menu and the data export menu
   */
  protected void addExportMenu(boolean graphics, boolean data) {
    if (graphics) {
      // Graphics Export
      addMenuItem(getContextMenu(), "Export graphics...", e -> {

        GraphicsExportParameters parameters = (GraphicsExportParameters) MZmineCore.getConfiguration()
            .getModuleParameters(GraphicsExportModule.class);

        MZmineCore.getModuleInstance(GraphicsExportModule.class).openDialog(getChart(), parameters);
      });
    }
    if (data) {
      // General data export
      Menu export = new Menu("Export data ...");
      // Excel XY
      MenuExportToExcel exportXY = new MenuExportToExcel(new XSSFExcelWriterReader(), "to Excel",
          this);
      export.getItems().add(exportXY);
      // clip board
      MenuExportToClipboard exportXYClipboard = new MenuExportToClipboard("to Clipboard", this);
      export.getItems().add(exportXYClipboard);
      // add to panel
      getContextMenu().getItems().add(export);
    }
  }

  /**
   * Default tries to extract all series from an XYDataset or XYZDataset<br> series 1 | Series 2
   * <br> x y x y x y z x y z
   *
   * @return Data array[columns][rows]
   */
  public Object[][] getDataArrayForExport() {
    if (!(getChart().getPlot() instanceof XYPlot plot)) {
      return null;
    }
    // getDataset() may be null if the
    // first dataset was removed, but the plot may still hold other datasets
    try {
      List<Object[]> modelList = new ArrayList<>();

      int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
      for (int d = 0; d < numDatasets; d++) {
        XYDataset data = plot.getDataset(d);
        if (data == null) {
          continue;
        } else if (data instanceof XYZDataset xyz) {
          int series = data.getSeriesCount();
          final XYItemRenderer r = this.getChart().getXYPlot().getRendererForDataset(xyz);
          Object[][] model = new Object[series * 4][];
          for (int s = 0; s < series; s++) {
            final XYToolTipGenerator toolTipGen = r.getSeriesToolTipGenerator(s);
            int size = 2 + xyz.getItemCount(s);
            Object[] x = new Object[size];
            Object[] y = new Object[size];
            Object[] z = new Object[size];
            Object[] tooltip = new Object[size];
            // create new Array model[row][col]
            // Write header
            Comparable title = data.getSeriesKey(series);
            x[0] = title;
            y[0] = "";
            z[0] = "";
            tooltip[0] = "";
            x[1] = plot.getDomainAxis().getLabel();
            y[1] = plot.getRangeAxis().getLabel();
            z[1] = "z-axis";
            tooltip[1] = "tooltip";
            // write data
            for (int i = 0; i < xyz.getItemCount(s); i++) {
              x[i + 2] = xyz.getX(s, i);
              y[i + 2] = xyz.getY(s, i);
              z[i + 2] = xyz.getZ(s, i);
              if (toolTipGen != null) {
                tooltip[i + 2] = StringUtils.inQuotes(
                    Objects.requireNonNullElse(toolTipGen.generateToolTip(xyz, s, i), "")
                        .replace('\n', ' '));
              } else if (xyz instanceof ColoredXYZDataset cxyz) {
                tooltip[i + 2] = StringUtils.inQuotes(
                    Objects.requireNonNullElse(cxyz.getToolTipText(i), "").replace('\n', ' '));
              } else {
                tooltip[i + 2] = "";
              }
            }
            model[s * 3] = x;
            model[s * 3 + 1] = y;
            model[s * 3 + 2] = z;
            model[s * 3 + 3] = tooltip;
          }

          Collections.addAll(modelList, model);
        } else if (data != null) {
          int series = data.getSeriesCount();
          Object[][] model = new Object[series * 3][];
          for (int s = 0; s < series; s++) {
            final XYItemRenderer r = getChart().getXYPlot().getRendererForDataset(data);
            final XYToolTipGenerator toolTipGenerator = r.getSeriesToolTipGenerator(s);
            int size = 2 + data.getItemCount(s);
            Object[] x = new Object[size];
            Object[] y = new Object[size];
            Object[] tooltip = new Object[size];
            // create new Array model[row][col]
            // Write header
            Comparable title = data.getSeriesKey(s);
            x[0] = title;
            y[0] = "";
            tooltip[0] = "";
            x[1] = plot.getDomainAxis().getLabel();
            y[1] = plot.getRangeAxis().getLabel();
            tooltip[1] = "tooltip";
            // write data
            for (int i = 0; i < data.getItemCount(s); i++) {
              x[i + 2] = data.getX(s, i);
              y[i + 2] = data.getY(s, i);
              if (toolTipGenerator != null) {
                tooltip[i + 2] = StringUtils.inQuotes(
                    Objects.requireNonNullElse(toolTipGenerator.generateToolTip(data, s, i), "")
                        .replace('\n', ' '));
              } else if (data instanceof ColoredXYDataset cxy) {
                tooltip[i + 2] = StringUtils.inQuotes(
                    Objects.requireNonNullElse(cxy.getToolTipText(i), "").replace('\n', ' '));
              } else {
                tooltip[i + 2] = "";
              }
            }
            model[s * 2] = x;
            model[s * 2 + 1] = y;
            model[s * 2 + 2] = tooltip;
          }

          Collections.addAll(modelList, model);
        }
      }

      return modelList.toArray(new Object[modelList.size()][]);
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Cannot retrieve data for export", ex);
      return null;
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

  public void setMouseZoomable(boolean flag) {
    setDomainZoomable(flag);
    setRangeZoomable(flag);
    isMouseZoomable = flag;
    // TODO find better solution
    // clear handler to stop zoom rectangle (hacky solution)
    getCanvas().clearLiveHandler();
  }

  public boolean isDomainZoomable() {
    return getCanvas().isDomainZoomable();
  }

  public void setDomainZoomable(boolean flag) {
    getCanvas().setDomainZoomable(flag);
  }

  public boolean isRangeZoomable() {
    return getCanvas().isRangeZoomable();
  }

  public void setRangeZoomable(boolean flag) {
    getCanvas().setRangeZoomable(flag);
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
   * this.getCanvas().addAuxiliaryMouseHandler(handler);
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

  @Override
  public void datasetChanged(DatasetChangeEvent event) {
    // may be overridden by extending classes
  }

  /**
   * Notifies about chart changes and updates the chart on any change
   */
  public boolean isNotifyChange() {
    return getChart() == null || getChart().isNotify();
  }

  /**
   * Disable/enable chart change events that trigger updating the chart. Setting to true will always
   * trigger a chart update.
   */
  public void setNotifyChange(boolean notifyChange) {
    if (getChart() == null) {
      return;
    }
    // setting to true will already fire a chart change event automatically
    getChart().setNotify(notifyChange);
//    final Plot plot = getChart().getPlot();
//    if(plot!=null) {
//      plot.setNotify(notifyChange);
//    }
  }

  /**
   * Fires a chart change event
   */
  public void fireChangeEvent() {
    if (getChart() != null) {
      getChart().fireChartChanged();
    }
  }

  /**
   * Will set the chart.notify to tempState, perform logic that changes the chart, and reset to the
   * old notify state. If the old notify was true, a chart change event is fired. The old notify
   * will be false if this call is one of many boxed calls within methods.
   *
   * @param tempState usually false to avoid updating of a chart at every change event
   * @param logic     the logic that updates the chart
   */
  public void applyWithNotifyChanges(boolean tempState, Runnable logic) {
    applyWithNotifyChanges(tempState, isNotifyChange(), logic);
  }

  /**
   * Will set the chart.notify to tempState, perform logic that changes the chart, and reset to the
   * old notify state. If the old notify was true, a chart change event is fired. The old notify
   * will be false if this call is one of many boxed calls within methods.
   *
   * @param tempState     usually false to avoid updating of a chart at every change event
   * @param logic         the logic that updates the chart
   * @param afterRunState the new state after running logic. If true, the chart is updated.
   */
  public void applyWithNotifyChanges(boolean tempState, boolean afterRunState, Runnable logic) {
    setNotifyChange(tempState);
    try {
      // perform changes that t
      logic.run();
    } finally {
      // reset to old state and run changes if true
      // setting to true will automatically trigger a draw event
      setNotifyChange(afterRunState);
    }
  }

  public Marker addDomainMarker(com.google.common.collect.Range<Double> valueRange, Color color,
      float alpha) {
    return addDomainMarker(valueRange.lowerEndpoint(), valueRange.upperEndpoint(), color, alpha);
  }

  public Marker addDomainMarker(double value, Color color, float alpha) {
    final ValueMarker marker = new ValueMarker(value);
    marker.setStroke(
        new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{7}, 0f));
    marker.setPaint(color);
    marker.setAlpha(alpha);
    getChart().getXYPlot().addDomainMarker(marker, Layer.BACKGROUND);
    return marker;
  }

  public Marker addRangeMarker(double value, Color color, float alpha) {
    final ValueMarker marker = new ValueMarker(value);
    marker.setStroke(
        new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{7}, 0f));
    marker.setPaint(color);
    marker.setAlpha(alpha);
    getChart().getXYPlot().addRangeMarker(marker, Layer.BACKGROUND);
    return marker;
  }

  public Marker addDomainMarker(double lowerValue, double upperValue, Color color, float alpha) {
    final IntervalMarker marker = new IntervalMarker(lowerValue, upperValue);
    marker.setStroke(
        new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{7}, 0f));
    marker.setPaint(color);
    marker.setAlpha(alpha);
    getChart().getXYPlot().addDomainMarker(marker, Layer.BACKGROUND);
    return marker;
  }

  /**
   * Debugging of draw events. Only runs when {@link JFreeChart#draw(Graphics2D, Rectangle2D)} is
   * started
   */
  public void addChartDrawDebugListener(Runnable eventListener) {
    if (getChart() == null) {
      return;
    }
    getChart().addProgressListener(event -> {
      if (event.getType() == ChartProgressEvent.DRAWING_STARTED) {
        eventListener.run();
      }
    });
  }
}
