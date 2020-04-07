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

package io.github.mzmine.modules.visualization.mzhistogram.chart;

import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.javafx.WindowsMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

/**
 * Enhanced version. Use arrows to jump to the next or previous distribution
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class HistogramDialog extends Stage implements ActionListener {

  private CheckBox cbKeepSameXaxis;
  private final Scene mainScene;
  private final BorderPane mainPane;

  protected HistogramPanel histo;

  /**
   * Create the dialog. Auto detect binWidth
   *
   * @wbp.parser.constructor
   */
  public HistogramDialog(String title, String xLabel, HistogramData data) {
    this(title, xLabel, data, 0);
  }

  /**
   * @param title
   * @param data
   * @param binWidth zero (0) for auto detection, -1 to keep last binWidth
   */
  public HistogramDialog(String title, String xLabel, HistogramData data, double binWidth) {
    setTitle(title);

    mainPane = new BorderPane();
    mainScene = new Scene(mainPane);

    // Use main CSS
    mainScene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());

    histo = new HistogramPanel(xLabel, data, binWidth);

    setMinWidth(1050);
    setMinHeight(700);
    setScene(mainScene);

    mainPane.setCenter(histo);

    // Add the Windows menu
    WindowsMenu.addWindowsMenu(mainScene);
    addKeyBindings();
  }

  private void addKeyBindings() {
    FlowPane pnJump = new FlowPane();

    cbKeepSameXaxis = new CheckBox("keep same x-axis length");
    pnJump.getChildren().add(cbKeepSameXaxis);

    Button btnPrevious = new Button("<");
    btnPrevious.setTooltip(new Tooltip("Jump to previous distribution (use left arrow"));
    btnPrevious.setOnAction(e -> jumpToPrevPeak());
    pnJump.getChildren().add(btnPrevious);

    Button btnNext = new Button(">");
    btnNext.setTooltip(new Tooltip("Jump to previous distribution (use right arrow"));
    btnNext.setOnAction(e -> jumpToNextPeak());
    pnJump.getChildren().add(btnNext);
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    final String command = event.getActionCommand();
    if ("PREVIOUS_PEAK".equals(command)) {
      jumpToPrevPeak();
    } else if ("NEXT_PEAK".equals(command)) {
      jumpToNextPeak();
    }
  }

  /**
   * tries to find the next local maximum to jump to the prev peak
   */
  private void jumpToPrevPeak() {
    XYPlot plot = getXYPlot();
    if (plot == null) {
      return;
    }

    XYDataset data = plot.getDataset(0);
    // get center of zoom
    ValueAxis x = plot.getDomainAxis();
    double mid = (x.getUpperBound() + x.getLowerBound()) / 2;

    boolean started = false;

    for (int i = data.getItemCount(0) - 1; i >= 1; i--) {
      double mz = data.getXValue(0, i);
      if (mz < mid) {
        // wait for y to be 0 to start the search for a new peak
        if (!started) {
          if (data.getYValue(0, i) == 0) {
            started = true;
          }
        } else {
          // intensity drops?
          if (data.getYValue(0, i - 1) != 0 && data.getYValue(0, i) >= 100
              && data.getYValue(0, i - 1) < data.getYValue(0, i)) {
            // peak found with max at i
            setZoomAroundPeakAt(i);
            return;
          }
        }
      }
    }
  }

  /**
   * tries to find the next local maximum to jump to the prev peak
   */
  private void jumpToNextPeak() {
    XYPlot plot = getXYPlot();
    if (plot == null) {
      return;
    }

    XYDataset data = plot.getDataset(0);
    // get center of zoom
    ValueAxis x = plot.getDomainAxis();
    // mid of range
    double mid = (x.getUpperBound() + x.getLowerBound()) / 2;

    boolean started = false;

    for (int i = 0; i < data.getItemCount(0) - 1; i++) {
      double mz = data.getXValue(0, i);
      if (mz > mid) {
        // wait for y to be 0 to start the search for a new peak
        if (!started) {
          if (data.getYValue(0, i) == 0) {
            started = true;
          }
        } else {
          // intensity drops?
          if (data.getYValue(0, i + 1) != 0 && data.getYValue(0, i) >= 100
              && data.getYValue(0, i + 1) < data.getYValue(0, i)) {
            // peak found with max at i
            setZoomAroundPeakAt(i);
            return;
          }
        }
      }
    }
  }

  /**
   * Set zoom factor around peak at data point i
   *
   * @param i
   */
  private void setZoomAroundPeakAt(int i) {
    XYPlot plot = getXYPlot();
    if (plot == null) {
      return;
    }

    XYDataset data = plot.getDataset(0);

    // keep same domain axis range length
    boolean keepRange = cbKeepSameXaxis.isSelected();

    // find lower bound (where y=0)
    double lower = data.getXValue(0, i);
    for (int x = i; x >= 0; x--) {
      if (data.getYValue(0, x) == 0) {
        lower = data.getXValue(0, x);
        break;
      }
    }
    // find upper bound /where y=0)
    double upper = data.getXValue(0, i);
    for (int x = i; x < data.getItemCount(0); x++) {
      if (data.getYValue(0, x) == 0) {
        upper = data.getXValue(0, x);
        break;
      }
    }

    if (keepRange) {
      // set constant range zoom
      double length = plot.getDomainAxis().getRange().getLength();
      plot.getDomainAxis().setRangeAboutValue(data.getXValue(0, i), length);
    } else {
      // set range directly around peak
      plot.getDomainAxis().setRange(lower, upper);
    }

    // auto gaussian fit
    if (getHistoPanel().isGaussianFitEnabled()) {
      // find
      getHistoPanel().setGaussianFitRange(lower, upper);
    }

    // auto range y
    ChartLogicsFX.autoRangeAxis(getChartPanel());
  }

  private ChartViewer getChartPanel() {
    return getHistoPanel().getChartPanel();
  }

  private XYPlot getXYPlot() {
    ChartViewer chart = getHistoPanel().getChartPanel();
    if (chart != null) {
      return chart.getChart().getXYPlot();
    } else {
      return null;
    }
  }

  public CheckBox getCbKeepSameXaxis() {
    return cbKeepSameXaxis;
  }

  public HistogramPanel getHistoPanel() {
    return histo;
  }
}
