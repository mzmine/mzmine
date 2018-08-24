package net.sf.mzmine.chartbasics.javafx.charts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.fx.ChartCanvas;
import org.jfree.chart.fx.interaction.ZoomHandlerFX;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.util.ExportUtils;
import org.jfree.data.Range;
import org.jfree.data.RangeType;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import net.sf.mzmine.chartbasics.gestures.ChartGestureHandler;
import net.sf.mzmine.chartbasics.gestures.ChartGestureMouseAdapter;
import net.sf.mzmine.chartbasics.gestures.interf.GestureHandlerFactory;
import net.sf.mzmine.chartbasics.graphicsexport.GraphicsExportDialog;
import net.sf.mzmine.chartbasics.javafx.menu.MenuExportToClipboard;
import net.sf.mzmine.chartbasics.javafx.menu.MenuExportToExcel;
import net.sf.mzmine.chartbasics.javafx.mouse.ChartGestureMouseAdapterFX;
import net.sf.mzmine.chartbasics.javafx.mouse.ChartViewWrapper;
import net.sf.mzmine.chartbasics.listener.AxesRangeChangedListener;
import net.sf.mzmine.chartbasics.listener.AxisRangeChangedListener;
import net.sf.mzmine.chartbasics.listener.ZoomHistory;
import net.sf.mzmine.util.io.XSSFExcelWriterReader;

public class EChartCanvas extends ChartCanvas {
  private static final long serialVersionUID = 1L;
  private Logger logger = Logger.getLogger(this.getClass().getName());

  // popup
  protected ContextMenu menu;
  /**
   * The zoom rectangle is used to display the zooming region when doing a drag-zoom with the mouse.
   * Most of the time this rectangle is not visible.
   */
  private Rectangle zoomRectangle;

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
  public EChartCanvas(JFreeChart chart) {
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
  public EChartCanvas(JFreeChart chart, boolean graphicsExportMenu, boolean dataExportMenu,
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
  public EChartCanvas(JFreeChart chart, boolean graphicsExportMenu, boolean dataExportMenu,
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
  public EChartCanvas(JFreeChart chart, boolean graphicsExportMenu, boolean dataExportMenu,
      boolean standardGestures, boolean addZoomHistory, boolean stickyZeroForRangeAxis) {
    super(null);
    this.stickyZeroForRangeAxis = stickyZeroForRangeAxis;
    this.standardGestures = standardGestures;
    this.addZoomHistory = addZoomHistory;
    setChart(chart);

    // create menu and add basic graphics export
    createMenu();
    // Add Export to Excel and graphics export menu
    if (graphicsExportMenu || dataExportMenu)
      addExportMenu(graphicsExportMenu, dataExportMenu);
  }

  private void createMenu() {
    this.menu = new ContextMenu();
    menu.setAutoHide(true);
    Menu export = new Menu("Export graphics as");
    addMenuItem(export, "PNG...", e -> handleExportToPNG());
    addMenuItem(export, "JPEG...", e -> handleExportToJPEG());
    if (ExportUtils.isOrsonPDFAvailable())
      addMenuItem(export, "PDF...", e -> handleExportToPDF());
    if (ExportUtils.isJFreeSVGAvailable())
      addMenuItem(export, "SVG...", e -> handleExportToSVG());
    addMenu(menu, export);
    // finished add items

    // show and hide
    setOnContextMenuRequested((ContextMenuEvent event) -> {
      menu.show(this.getScene().getWindow(), event.getScreenX(), event.getScreenY());
    });
    this.menu.setOnShowing(e -> setTooltipEnabled(false));
    this.menu.setOnHiding(e -> setTooltipEnabled(true));
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
    final EChartCanvas chartPanel = this;

    // remove old init
    if (mouseAdapter != null) {
      this.removeMouseHandler(mouseAdapter);
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
      mouseAdapter = new ChartGestureMouseAdapterFX("gestures");
      // mouseAdapter.addDebugHandler();
      this.addMouseHandler(mouseAdapter);

      // add gestures
      if (standardGestures) {
        addStandardGestures();
      }
    }
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
      addMenuItem(menu, "Export graphics...", e -> GraphicsExportDialog.openDialog(getChart()));
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
      addMenu(menu, export);
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
    super.setDomainZoomable(flag);
    super.setRangeZoomable(flag);
    isMouseZoomable = flag;
  }

  public boolean isMouseZoomable() {
    return isMouseZoomable;
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

  /**
   * Returns the context menu for this component.
   * 
   * @return The context menu for this component.
   */
  public ContextMenu getContextMenu() {
    return this.menu;
  }

  /**
   * A handler for the export to PDF option in the context menu.
   */
  private void handleExportToPDF() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export to PDF");
    FileChooser.ExtensionFilter filter =
        new FileChooser.ExtensionFilter("Portable Document Format (PDF)", "pdf");
    chooser.getExtensionFilters().add(filter);
    File file = chooser.showSaveDialog(getScene().getWindow());
    if (file != null) {
      ExportUtils.writeAsPDF(this.getChart(), (int) getWidth(), (int) getHeight(), file);
    }
  }

  /**
   * A handler for the export to SVG option in the context menu.
   */
  private void handleExportToSVG() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export to SVG");
    FileChooser.ExtensionFilter filter =
        new FileChooser.ExtensionFilter("Scalable Vector Graphics (SVG)", "svg");
    chooser.getExtensionFilters().add(filter);
    File file = chooser.showSaveDialog(getScene().getWindow());
    if (file != null) {
      ExportUtils.writeAsSVG(this.getChart(), (int) getWidth(), (int) getHeight(), file);
    }
  }

  /**
   * A handler for the export to PNG option in the context menu.
   */
  private void handleExportToPNG() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export to PNG");
    FileChooser.ExtensionFilter filter =
        new FileChooser.ExtensionFilter("Portable Network Graphics (PNG)", "png");
    chooser.getExtensionFilters().add(filter);
    File file = chooser.showSaveDialog(getScene().getWindow());
    if (file != null) {
      try {
        ExportUtils.writeAsPNG(this.getChart(), (int) getWidth(), (int) getHeight(), file);
      } catch (IOException ex) {
        // FIXME: show a dialog with the error
        throw new RuntimeException(ex);
      }
    }
  }

  /**
   * A handler for the export to JPEG option in the context menu.
   */
  private void handleExportToJPEG() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export to JPEG");
    FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("JPEG", "jpg");
    chooser.getExtensionFilters().add(filter);
    File file = chooser.showSaveDialog(getScene().getWindow());
    if (file != null) {
      try {
        ExportUtils.writeAsJPEG(this.getChart(), (int) getWidth(), (int) getHeight(), file);
      } catch (IOException ex) {
        // FIXME: show a dialog with the error
        throw new RuntimeException(ex);
      }
    }
  }

  /**
   * Sets the size and location of the zoom rectangle and makes it visible if it wasn't already
   * visible.. This method is provided for the use of the {@link ZoomHandlerFX} class, you won't
   * normally need to call it directly.
   * 
   * @param x the x-location.
   * @param y the y-location.
   * @param w the width.
   * @param h the height.
   */
  public void showZoomRectangle(double x, double y, double w, double h) {
    this.zoomRectangle.setX(x);
    this.zoomRectangle.setY(y);
    this.zoomRectangle.setWidth(w);
    this.zoomRectangle.setHeight(h);
    this.zoomRectangle.setVisible(true);
  }

  /**
   * Hides the zoom rectangle. This method is provided for the use of the {@link ZoomHandlerFX}
   * class, you won't normally need to call it directly.
   */
  public void hideZoomRectangle() {
    this.zoomRectangle.setVisible(false);
  }
}
