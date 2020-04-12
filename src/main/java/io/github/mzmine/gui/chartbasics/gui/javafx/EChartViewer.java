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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.gui.chartbasics.gui.javafx;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.interaction.MouseHandlerFX;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.CombinedRangeXYPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.RangeType;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gestures.interf.GestureHandlerFactory;
import io.github.mzmine.gui.chartbasics.graphicsexport.GraphicsExportModule;
import io.github.mzmine.gui.chartbasics.graphicsexport.GraphicsExportParameters;
import io.github.mzmine.gui.chartbasics.gui.javafx.menu.MenuExportToClipboard;
import io.github.mzmine.gui.chartbasics.gui.javafx.menu.MenuExportToExcel;
import io.github.mzmine.gui.chartbasics.gui.swing.ChartGestureMouseAdapter;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.listener.AxesRangeChangedListener;
import io.github.mzmine.gui.chartbasics.listener.AxisRangeChangedListener;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.SaveImage;
import io.github.mzmine.util.SaveImage.FileType;
import io.github.mzmine.util.dialogs.AxesSetupDialog;
import io.github.mzmine.util.io.XSSFExcelWriterReader;
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
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

/**
 * This is an extended version of the ChartViewer (JFreeChartFX). it Adds: ChartGestures (with a set
 * of standard chart gestures), ZoomHistory, AxesRangeChangeListener, data export, graphics export,
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class EChartViewer extends ChartViewer {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  // one history for each plot/subplot
  protected ZoomHistory zoomHistory;
  protected List<AxesRangeChangedListener> axesRangeListener;
  protected boolean isMouseZoomable = true;
  protected boolean stickyZeroForRangeAxis = false;
  protected boolean standardGestures = true;
  // only for XYData (not for categoryPlots)
  protected boolean addZoomHistory = true;
  private ChartGestureMouseAdapterFX mouseAdapter;
  private Menu exportMenu;

  /**
   * Enhanced ChartPanel with extra scrolling methods, zoom history, graphics and data export<br>
   * stickyZeroForRangeAxis = false <br>
   * Graphics and data export menu are added
   *
   * @param chart
   */
  public EChartViewer() {
    this(null, true, true, true, true, false);
  }

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

    // Add chart and configure
    if (chart != null)
      setChart(chart);

    exportMenu = (Menu) getContextMenu().getItems().get(0);

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
      AxesSetupDialog dialog =
          new AxesSetupDialog((Stage) this.getScene().getWindow(), chart.getXYPlot());
      dialog.show();
    });

    addMenuItem(exportMenu, "EPS..", event -> handleSave("EMF Image", "EMF", ".emf", FileType.EMF));

    addMenuItem(exportMenu, "EMF..", event -> handleSave("EPS Image", "EPS", ".eps", FileType.EPS));

    addMenuItem(getContextMenu(), "Copy chart to clipboard", event -> {
      BufferedImage bufferedImage =
          getChart().createBufferedImage((int) this.getWidth(), (int) this.getHeight());
      Image image = SwingFXUtils.toFXImage(bufferedImage, null);
      ClipboardContent content = new ClipboardContent();
      content.putImage(image);
      Clipboard.getSystemClipboard().setContent(content);
    });

    addMenuItem(getContextMenu(), "Print", event -> {
      BufferedImage bufferedImage =
          getChart().createBufferedImage((int) this.getWidth(), (int) this.getHeight());
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

  private void handleSave(String description, String extensions, String extension,
      FileType filetype) {
    FileChooser chooser = new FileChooser();
    chooser.getExtensionFilters().add(new ExtensionFilter(description, extensions));
    File file = chooser.showSaveDialog(null);

    if (file != null) {
      String filepath = file.getPath();
      if (!filepath.toLowerCase().endsWith(extension)) {
        filepath += extension;
      }

      int width = (int) this.getWidth();
      int height = (int) this.getHeight();

      // Save image
      SaveImage SI = new SaveImage(getChart(), filepath, width, height, filetype);
      new Thread(SI).start();
    }
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
    super.setChart(chart);

    // If no chart, end here
    if (chart == null)
      return;

    final EChartViewer chartPanel = this;

    // apply the theme here, let's see how that works
    MZmineCore.getConfiguration().getDefaultChartTheme().apply(chart);

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

      Plot p = getChart().getPlot();
      if (addZoomHistory && p instanceof XYPlot
          && !(p instanceof CombinedDomainXYPlot || p instanceof CombinedRangeXYPlot)) {
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
      mouseAdapter = new ChartGestureMouseAdapterFX("gestures", this);
      addMouseHandler(mouseAdapter);

      // add gestures
      if (standardGestures) {
        addStandardGestures();
      }
      // mouseAdapter.addDebugHandler();
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

      logger.log(Level.FINEST, "Added standard gestures: " + m.getGestureHandlers().size());
    }
  }

  /**
   * Adds the GraphicsExportDialog menu and the data export menu
   */
  protected void addExportMenu(boolean graphics, boolean data) {
    if (graphics) {
      // Graphics Export
      addMenuItem(getContextMenu(), "Export graphics...", e -> {

        GraphicsExportParameters parameters = (GraphicsExportParameters) MZmineCore
            .getConfiguration().getModuleParameters(GraphicsExportModule.class);

        MZmineCore.getModuleInstance(GraphicsExportModule.class).openDialog(getChart(), parameters);
      });
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
      getContextMenu().getItems().add(export);
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
        logger.log(Level.WARNING, "Cannot retrieve data for export", ex);
        return null;
      }
    }
    return null;
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
}
