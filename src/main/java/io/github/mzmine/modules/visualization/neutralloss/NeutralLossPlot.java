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

package io.github.mzmine.modules.visualization.neutralloss;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Shape;
import java.awt.event.ActionEvent;

import java.awt.geom.Ellipse2D;
import java.io.File;
import java.text.NumberFormat;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;

import com.google.common.collect.Range;

import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.GUIUtils;
import io.github.mzmine.util.SaveImage;
import io.github.mzmine.util.SaveImage.FileType;

import static javafx.scene.input.MouseEvent.*;

class NeutralLossPlot extends EChartViewer implements EventHandler<KeyEvent>// implements ChartMouseListenerFX
{

  private static final long serialVersionUID = 1L;

  private JFreeChart chart;

  private XYPlot plot;
  private NeutralLossDataPointRenderer defaultRenderer;

  private boolean showSpectrumRequest;

  private NeutralLossVisualizerWindow visualizer;

  // crosshair (selection) color
  private static final Color crossHairColor = Color.gray;

  // crosshair stroke
  private static final BasicStroke crossHairStroke =
      new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {5, 3}, 0);

  // title font
  private static final Font titleFont = new Font("SansSerif", Font.PLAIN, 11);

  // Item's shape, small circle
  private static final Shape dataPointsShape = new Ellipse2D.Double(-1, -1, 2, 2);
  private static final Shape dataPointsShape2 = new Ellipse2D.Double(-1, -1, 3, 3);

  // Series colors
  private static final Color pointColor = Color.blue;
  private static final Color searchPrecursorColor = Color.green;
  private static final Color searchNeutralLossColor = Color.orange;

  private TextTitle chartTitle;

  private Range<Double> highlightedPrecursorRange = Range.singleton(Double.NEGATIVE_INFINITY);
  private Range<Double> highlightedNeutralLossRange = Range.singleton(Double.NEGATIVE_INFINITY);

  NeutralLossPlot() {
    super(ChartFactory.createXYLineChart("", "","",null,
            PlotOrientation.VERTICAL,true,true,false));

    // this.visualizer = visualizer;
    // setBackground(Color.white);
    // setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

    showSpectrumRequest = false;

    // set the renderer properties
    defaultRenderer = new NeutralLossDataPointRenderer(false, true);
    defaultRenderer.setTransparency(0.4f);
    setSeriesColorRenderer(0, pointColor, dataPointsShape);
    setSeriesColorRenderer(1, searchPrecursorColor, dataPointsShape2);
    setSeriesColorRenderer(2, searchNeutralLossColor, dataPointsShape2);

    // tooltips
    // defaultRenderer.setDefaultToolTipGenerator(dataset);

    // chart properties
    chart = getChart();
    chart.setBackgroundPaint(Color.white);
    // setChart(chart);

    // set the plot properties
    plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.white);
    // plot.setRenderer(defaultRenderer);
    plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);



    // title
    chartTitle = chart.getTitle();
    chartTitle.setMargin(5, 0, 0, 0);
    chartTitle.setFont(titleFont);

    // disable maximum size (we don't want scaling)
    // setMaximumDrawWidth(Integer.MAX_VALUE);
    // setMaximumDrawHeight(Integer.MAX_VALUE);

    // set crosshair (selection) properties
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);
    plot.setDomainCrosshairPaint(crossHairColor);
    plot.setRangeCrosshairPaint(crossHairColor);
    plot.setDomainCrosshairStroke(crossHairStroke);
    plot.setRangeCrosshairStroke(crossHairStroke);

    plot.addRangeMarker(new ValueMarker(0));

    // set focusable state to receive key events
    // setFocusable(true);

    // register key handlers
    // TODO: GUIUtils are still implemented with swing
    // GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke("SPACE"), visualizer, "SHOW_SPECTRUM");

    // add items to popup menu
    final ContextMenu popupMenu = getContextMenu();
    Menu saveAsMenu = new Menu("Save as");
    popupMenu.getItems().add(saveAsMenu);
    MenuItem emfMenuItem = new MenuItem("EMF...");
    emfMenuItem.setOnAction(event -> {
      // TODO: get rid of copy/pasted code: make a method that gets eps/emf as argument
      // TODO: fix: the saving location cannot be changed yet
      FileChooser chooser = new FileChooser();
      chooser.setTitle("Choose file");
      chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("EMF Image", "EMF"));
      File file = chooser.showSaveDialog(visualizer);
      if (file != null) { // file is null when cancel was pressed
        String filename = file.getName();
        if (!filename.toLowerCase().endsWith(".emf"))
          filename += ".emf";

        int width = (int) this.getWidth();
        int height = (int) this.getHeight();

        // Save image
        SaveImage SI = new SaveImage(getChart(), filename, width, height, FileType.EMF);
        new Thread(SI).start();
      }

    });

    saveAsMenu.getItems().add(emfMenuItem);
    MenuItem epsMenuItem = new MenuItem("EPS...");
    epsMenuItem.setOnAction(event -> {

      FileChooser chooser = new FileChooser();
      chooser.setTitle("Choose file");
      chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("EPS Image", "EPS"));
      File file = chooser.showSaveDialog(visualizer);
      if (file != null) { // file is null when cancel was pressed
        String filename = file.getName();
        if (!filename.toLowerCase().endsWith(".eps"))
          filename += ".eps";

        int width = (int) this.getWidth();
        int height = (int) this.getHeight();

        // Save image
        SaveImage SI = new SaveImage(getChart(), filename, width, height, FileType.EPS);
        new Thread(SI).start();

      }
    });
    saveAsMenu.getItems().add(epsMenuItem);
    popupMenu.getItems().add(new SeparatorMenuItem());

    // Add EMF and EPS options to the save as menu
    // JMenuItem saveAsMenu = (JMenuItem) popupMenu.getComponent(3);
    // GUIUtils.addMenuItem(saveAsMenu, "EMF...", this, "SAVE_EMF");
    // GUIUtils.addMenuItem(saveAsMenu, "EPS...", this, "SAVE_EPS");


    /*
    MenuItem highLightPrecursorRange = new MenuItem("Highlight precursor m/z range...");
    highLightPrecursorRange.addActionListener(visualizer);
    highLightPrecursorRange.setActionCommand("HIGHLIGHT_PRECURSOR");
    popupMenu.getItems().add(highLightPrecursorRange);

    MenuItem highLightNeutralLossRange = new MenuItem("Highlight neutral loss m/z range...");
    highLightNeutralLossRange.addActionListener(visualizer);
    highLightNeutralLossRange.setActionCommand("HIGHLIGHT_NEUTRALLOSS");
    popupMenu.getItems().add(highLightNeutralLossRange);
    */
    resetZoomHistory();
    // setMouseZoomable(false);

    this.addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        requestFocus();
        MouseEvent mouseEvent = event.getTrigger();

        if ((mouseEvent.getClickCount() == 2) && (mouseEvent.getButton() == MouseButton.PRIMARY))  {
          //showSpectrum();
          showSpectrumRequest=true;
        }
      }

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {
        return;
      }
    });


    this.setOnKeyTyped(new EventHandler<KeyEvent>() {
      @Override
      public void handle(KeyEvent keyEvent) {
        if(keyEvent.getCharacter().equals(" ")) {
          showSpectrum();
          // showSpectrumRequest=true; // this does not work with the ProgressListener
        };
      }
    });

    chart.addProgressListener(new ChartProgressListener() {
      @Override
      public void chartProgress(ChartProgressEvent event) {
        if (event.getType() == ChartProgressEvent.DRAWING_FINISHED) {

          visualizer.updateTitle();

          if (showSpectrumRequest) {
            showSpectrumRequest = false;
            showSpectrum();
            
          }
        }
      }
    });

  }

  public void showSpectrum () {
    NeutralLossDataSet dataset = (NeutralLossDataSet) plot.getDataset();
    double xValue = plot.getDomainCrosshairValue();
    double yValue = plot.getRangeCrosshairValue();
    NeutralLossDataPoint pos = dataset.getDataPoint(xValue,yValue);
    RawDataFile dataFile = visualizer.getDataFile();
    if (pos != null) {
       	SpectraVisualizerModule.showNewSpectrumWindow(dataFile, pos.getScanNumber());
    }

    resetZoomHistory();
  }

  public void resetZoomHistory() {
    ZoomHistory history = getZoomHistory();
    if (history != null)
      history.clear();
  }

  /*
  @Override
  public void actionPerformed(final ActionEvent event) {

    super.actionPerformed(event);

    final String command = event.getActionCommand();

    if ("SAVE_EMF".equals(command)) {

      JFileChooser chooser = new JFileChooser();
      FileNameExtensionFilter filter = new FileNameExtensionFilter("EMF Image", "EMF");
      chooser.setFileFilter(filter);
      int returnVal = chooser.showSaveDialog(null);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        String file = chooser.getSelectedFile().getPath();
        if (!file.toLowerCase().endsWith(".emf"))
          file += ".emf";

        int width = (int) this.getSize().getWidth();
        int height = (int) this.getSize().getHeight();

        // Save image
        SaveImage SI = new SaveImage(getChart(), file, width, height, FileType.EMF);
        new Thread(SI).start();

      }
    }

    if ("SAVE_EPS".equals(command)) {

      JFileChooser chooser = new JFileChooser();
      FileNameExtensionFilter filter = new FileNameExtensionFilter("EPS Image", "EPS");
      chooser.setFileFilter(filter);
      int returnVal = chooser.showSaveDialog(null);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        String file = chooser.getSelectedFile().getPath();
        if (!file.toLowerCase().endsWith(".eps"))
          file += ".eps";

        int width = (int) this.getSize().getWidth();
        int height = (int) this.getSize().getHeight();

        // Save image
        SaveImage SI = new SaveImage(getChart(), file, width, height, FileType.EPS);
        new Thread(SI).start();

      }

    }
  }
   */

  public void setAxisTypes (Object xAxisType) {

    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

    // set the X axis (retention time) properties
    final NumberAxis xAxis = (NumberAxis) this.plot.getDomainAxis();
    if (xAxisType.equals(NeutralLossParameters.xAxisPrecursor)) {
      xAxis.setLabel("Precursor m/z");
      xAxis.setNumberFormatOverride(mzFormat);
    } else {
      xAxis.setLabel("Retention time");
      xAxis.setNumberFormatOverride(rtFormat);
    }
    xAxis.setUpperMargin(0);
    xAxis.setLowerMargin(0);
    xAxis.setAutoRangeIncludesZero(false);
    // set the Y axis (intensity) properties
    final NumberAxis yAxis = (NumberAxis) this.plot.getRangeAxis();
    yAxis.setLabel("Neutral loss (Da)");
    yAxis.setAutoRangeIncludesZero(false);
    yAxis.setNumberFormatOverride(mzFormat);
    yAxis.setUpperMargin(0);
    yAxis.setLowerMargin(0);
  }

  public void addNeutralLossDataSet (NeutralLossDataSet dataset) {
    plot.setDataset(dataset);
    defaultRenderer.setDefaultToolTipGenerator(dataset);
    plot.setRenderer(defaultRenderer);
  }

  public void setVisualizer (NeutralLossVisualizerWindow visualizer) {
    this.visualizer=visualizer;
  }

  public void setMenuItems () {
    final ContextMenu popupMenu = getContextMenu();

    MenuItem highlightPrecursorMenuItem = new MenuItem("Highlight precursor m/z range...");
    highlightPrecursorMenuItem.setOnAction(event -> {
      NeutralLossSetHighlightDialog dialog = new NeutralLossSetHighlightDialog(visualizer, this, "HIGHLIGHT_PRECURSOR");
      dialog.show();
    });
    popupMenu.getItems().add(highlightPrecursorMenuItem);
    popupMenu.getItems().add(new SeparatorMenuItem());

    MenuItem highlightNLMenuItem = new MenuItem("Highlight neutral loss m/z range...");
    highlightNLMenuItem.setOnAction(event -> {
      NeutralLossSetHighlightDialog dialog = new NeutralLossSetHighlightDialog(visualizer, this, "HIGHLIGHT_NEUTRALLOSS");
      dialog.show();
    });
    popupMenu.getItems().add(highlightNLMenuItem);
    popupMenu.getItems().add(new SeparatorMenuItem());
  }

  private void setSeriesColorRenderer(int series, Color color, Shape shape) {
    defaultRenderer.setSeriesPaint(series, color);
    defaultRenderer.setSeriesFillPaint(series, color);
    defaultRenderer.setSeriesShape(series, shape);
  }

  void setTitle(String title) {
    chartTitle.setText(title);
  }

  /**
   * @return Returns the highlightedPrecursorRange.
   */
  Range<Double> getHighlightedPrecursorRange() {
    return highlightedPrecursorRange;
  }

  /**
   * @param range The highlightedPrecursorRange to set.
   */
  void setHighlightedPrecursorRange(Range<Double> range) {
    this.highlightedPrecursorRange = range;
  }

  /**
   * @return Returns the highlightedNeutralLossRange.
   */
  Range<Double> getHighlightedNeutralLossRange() {
    return highlightedNeutralLossRange;
  }

  /**
   * @param range The highlightedNeutralLossRange to set.
   */
  void setHighlightedNeutralLossRange(Range<Double> range) {
    this.highlightedNeutralLossRange = range;
  }

  /**
   * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
   */
  /*
  public void mouseClicked(MouseEvent event) {

    // let the parent handle the event (selection etc.)
    // super.mouseClicked(event);

    // request focus to receive key events
    requestFocus();

    // if user double-clicked left button, place a request to open a
    // spectrum
    if ((event.getButton() == BUTTON1) && (event.getClickCount() == 2)) {
      showSpectrumRequest = true;
    }

  }
   */

  /**
   * @see org.jfree.chart.event.ChartProgressListener#chartProgress(org.jfree.chart.event.ChartProgressEvent)
   */
  public void chartProgress(ChartProgressEvent event) {

    // super.chartProgress(event);

    if (event.getType() == ChartProgressEvent.DRAWING_FINISHED) {

      visualizer.updateTitle();

      if (showSpectrumRequest) {
        showSpectrumRequest = false;
        visualizer.actionPerformed(
            new ActionEvent(event.getSource(), ActionEvent.ACTION_PERFORMED, "SHOW_SPECTRUM"));
      }
    }

  }

  XYPlot getXYPlot() {
    return plot;
  }

  @Override
  public void handle(KeyEvent keyEvent) {

  }
}
