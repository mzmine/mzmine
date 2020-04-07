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

import io.github.mzmine.gui.chartbasics.HistogramChartFactory;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.util.maths.Precision;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.DoubleFunction;
import java.util.stream.DoubleStream;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HistogramPanel extends BorderPane {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final BorderPane contentPanel;
  private BorderPane southwest;
  private TextField txtBinWidth, txtBinShift;
  private CheckBox cbExcludeSmallerNoise, cbThirdSQRT;
  private Label lbStats;
  private TextField txtRangeX;
  private TextField txtRangeY;
  private TextField txtRangeXEnd;
  private TextField txtRangeYEnd;
  private TextField txtGaussianLower;
  private TextField txtGaussianUpper;
  private TextField txtPrecision;
  private CheckBox cbGaussianFit;

  private EChartViewer pnHisto;
  private String xLabel;
  private HistogramData data;
  private VBox boxSettings;
  private HBox secondGaussian, pnHistoSett, xRanges, yRanges;
  private Executor exec;

  /**
   * Create the dialog.
   */
  public HistogramPanel() {
    contentPanel = new BorderPane();
    setCenter(contentPanel);
    {
      Pane panel = new Pane();
      contentPanel.setLeft(panel);
    }
    {
      cbExcludeSmallerNoise = new CheckBox("exclude smallest");
      cbExcludeSmallerNoise.setSelected(true);

      cbThirdSQRT = new CheckBox("cube root(I)");
      cbThirdSQRT.setSelected(false);

      Label lblBinWidth = new Label("bin width");

      txtBinWidth = new TextField();
      txtBinWidth.setText("");

      Label lblBinShift = new Label("shift bins by");

      txtBinShift = new TextField();
      txtBinShift.setText("0");

      pnHistoSett = new HBox(10);
      pnHistoSett.getChildren().addAll(cbExcludeSmallerNoise, cbThirdSQRT, lblBinWidth, txtBinWidth,
          lblBinShift, txtBinShift);
      pnHistoSett.setAlignment(Pos.CENTER);
    }
    {
      ToggleButton btnToggleLegend = new ToggleButton("Toggle legend");
      btnToggleLegend.setOnAction(e -> toggleLegends());
      btnToggleLegend.setTooltip(new Tooltip("Show/hide legend"));

      Button btnUpdateGaussian = new Button("Update");
      btnUpdateGaussian.setOnAction(e -> updateGaussian());
      btnUpdateGaussian.setTooltip(new Tooltip("Update Gaussian fit"));

      Label lblFrom = new Label("from");

      cbGaussianFit = new CheckBox("Gaussian fit");

      txtGaussianLower = new TextField();
      txtGaussianLower
          .setTooltip(new Tooltip("The lower bound (domain axis) for the Gaussian fit"));
      txtGaussianLower.setText("0");

      Label label = new Label("-");

      txtGaussianUpper = new TextField();
      txtGaussianUpper
          .setTooltip(new Tooltip("The upper bound (domain axis, x) for the Gaussian fit"));
      txtGaussianUpper.setText("0");

      Label lblSignificantFigures = new Label("significant figures");

      txtPrecision = new TextField();
      txtPrecision
          .setTooltip(new Tooltip("Change number of significant figures and press update"));
      txtPrecision.setText("6");

      secondGaussian = new HBox(10);
      secondGaussian.getChildren()
          .addAll(btnToggleLegend, btnUpdateGaussian, lblFrom, cbGaussianFit,
              txtGaussianLower, label, txtGaussianUpper, lblSignificantFigures, txtPrecision);
      secondGaussian.setAlignment(Pos.CENTER);
    }
    {
      Label lblRanges = new Label("x-range");

      txtRangeX = new TextField();
      txtRangeX.setTooltip(new Tooltip("Set the x-range for both histograms"));
      txtRangeX.setText("0");

      Label label = new Label("-");

      txtRangeXEnd = new TextField();
      txtRangeXEnd.setTooltip(new Tooltip("Set the x-range for both histograms"));
      txtRangeXEnd.setText("0");

      Button btnApplyX = new Button("Apply");
      btnApplyX.setOnAction(e -> applyXRange());

      xRanges = new HBox(10);
      xRanges.getChildren().addAll(lblRanges, txtRangeX, label, txtRangeXEnd, btnApplyX);
      xRanges.setAlignment(Pos.CENTER);
    }
    {
      Label lblRanges = new Label("y-range");

      txtRangeY = new TextField();
      txtRangeY.setText("0");
      txtRangeY.setTooltip(new Tooltip("Set the y-range for both histograms"));

      Label label = new Label("-");

      txtRangeYEnd = new TextField();
      txtRangeYEnd.setTooltip(new Tooltip("Set the y-range for both histograms"));
      txtRangeYEnd.setText("0");

      Button btnApplyY = new Button("Apply");
      btnApplyY.setOnAction(e -> applyYRange());

      yRanges = new HBox(10);
      yRanges.getChildren().addAll(lblRanges, txtRangeY, label, txtRangeYEnd, btnApplyY);
      yRanges.setAlignment(Pos.CENTER);
    }
    lbStats = new Label("");
    lbStats.setFont(new Font("Tahoma", 14));

    boxSettings = new VBox(10);
    boxSettings.setAlignment(Pos.CENTER);
    boxSettings.setPadding(new Insets(10));
    boxSettings.getChildren().addAll(lbStats, pnHistoSett, secondGaussian, xRanges, yRanges);
    contentPanel.setBottom(boxSettings);

    southwest = new BorderPane();
    contentPanel.setCenter(southwest);

    addListener();
    exec = Executors.newFixedThreadPool(5, runnable -> {
      Thread t = new Thread(runnable);
      t.setDaemon(true);
      return t;
    });

  }

  /**
   * @param data
   * @param binWidth zero (0) for auto detection, -1 to keep last binWidth
   */
  public HistogramPanel(String xLabel, HistogramData data, double binWidth) {
    this();
    setData(data, binWidth);
    this.xLabel = xLabel;
  }

  public void setData(HistogramData data) {
    setData(data, -1);
  }

  /**
   * set data and update histo
   *
   * @param data
   * @param binWidth zero (0) for auto detection, -1 to keep last binWidth
   */
  public void setData(HistogramData data, double binWidth) {
    this.data = data;
    if (data != null) {
      if (binWidth > 0) {
        txtBinWidth.setText(String.valueOf(binWidth));
      } else if (binWidth == 0 || txtBinWidth.getText().isEmpty()) {
        // set bin width
        int bin = (int) Math.sqrt(data.size());
        double l = data.getRange().getLength();
        double bw = l / (double) bin;
        String bws = String.valueOf(bw);
        // round
        try {
          bws = Precision.toString(bw, 4);
        } catch (Exception e) {
        }
        txtBinWidth.setText(bws);
      }

      updateHistograms();

      contentPanel.requestLayout();
    }
  }

  /**
   * Toggles visibility of legends
   */
  private void toggleLegends() {
    if (pnHisto != null) {
      LegendTitle legend = pnHisto.getChart().getLegend();
      if (legend != null) {
        legend.setVisible(!legend.isVisible());
      }
    }
  }

  private void addListener() {

    txtRangeX.setOnKeyTyped(event -> applyXRange());
    txtRangeXEnd.setOnKeyTyped(event -> applyXRange());
    txtRangeY.setOnKeyTyped(event -> applyYRange());
    txtRangeYEnd.setOnKeyTyped(event -> applyYRange());
    txtBinShift.setOnKeyTyped(event -> updateHistograms());
    txtBinWidth.setOnKeyTyped(event -> updateHistograms());
    cbThirdSQRT.setOnAction(e -> updateHistograms());
    cbExcludeSmallerNoise.setOnAction(e -> updateHistograms());
    // add gaussian?
    cbGaussianFit.setOnAction(e -> updateGaussian());
  }

  private void applyXRange() {
    if (!txtRangeX.getText().equals("") && !txtRangeXEnd.getText().equals("")) {
      try {
        double x = Double.parseDouble(txtRangeX.getText());
        double xe = Double.parseDouble(txtRangeXEnd.getText());
        if (x < xe) {
          if (pnHisto != null) {
            pnHisto.getChart().getXYPlot().getDomainAxis().setRange(x, xe);
          }
        }
      } catch (Exception e2) {
        logger.error("", e2);
      }
    }
  }

  private void applyYRange() {
    if (!txtRangeY.getText().equals("") && !txtRangeYEnd.getText().equals("")) {
      try {
        double y = Double.parseDouble(txtRangeY.getText());
        double ye = Double.parseDouble(txtRangeYEnd.getText());
        if (y < ye) {
          if (pnHisto != null) {
            pnHisto.getChart().getXYPlot().getRangeAxis().setRange(y, ye);
          }
        }
      } catch (Exception e2) {
        logger.error("", e2);
      }
    }
  }

  /**
   * Create new histograms
   */
  private void updateHistograms() {
    if (data != null) {
      double binwidth2 = Double.NaN;
      double binShift2 = Double.NaN;
      try {
        binwidth2 = Double.parseDouble(txtBinWidth.getText());
        binShift2 = Double.parseDouble(txtBinShift.getText());
      } catch (Exception e) {
        logger.error("", e);
      }
      if (!Double.isNaN(binShift2)) {
        try {

          lbStats.setText("UPDATING");
          lbStats.setTextFill(Color.RED);

          final double binwidth = binwidth2;
          final double binShift = Math.abs(binShift2);
          try {

            exec.execute(() -> {
              JFreeChart chart = doInBackground(binShift, binwidth);
              Platform.runLater(() -> {
                done(chart);
              });
            });

          } catch (Exception e) {
            logger.error("", e);
          }
        } catch (Exception e1) {
          logger.error("", e1);
        }
      }
    }
  }

  protected JFreeChart doInBackground(double binShift, double binwidth) {
    // create histogram
    double[] dat = data.getData();
    if (cbExcludeSmallerNoise.isSelected()) {
      double noise = data.getRange().getLowerBound();
      // get processed data from original image
      dat = DoubleStream.of(dat).filter(d -> d > noise).toArray();
    }

    Range r = HistogramChartFactory.getBounds(dat);

    DoubleFunction<Double> f =
        cbThirdSQRT.isSelected() ? val -> Math.cbrt(val) : val -> val;

    JFreeChart chart = HistogramChartFactory.createHistogram(dat, xLabel, binwidth,
        r.getLowerBound() - binShift, r.getUpperBound(), f);
    // add gaussian?
    if (cbGaussianFit.isSelected()) {
      addGaussianCurve(chart.getXYPlot());
    }
    return chart;
  }

  protected void done(JFreeChart chart) {
    JFreeChart histo;
    try {
      Range x = null, y = null;
      if (pnHisto != null) {
        x = pnHisto.getChart().getXYPlot().getDomainAxis().getRange();
        y = pnHisto.getChart().getXYPlot().getRangeAxis().getRange();
      }
      histo = chart;

      if (histo != null) {
        if (x != null) {
          histo.getXYPlot().getDomainAxis().setRange(x);
        }
        if (y != null) {
          histo.getXYPlot().getRangeAxis().setRange(y);
        }
        pnHisto = new EChartViewer(histo, true, true, true, true, true);
        histo.getLegend().setVisible(true);

        southwest.setCenter(pnHisto);

        lbStats.setText("DONE");
        lbStats.setTextFill(Color.GREEN);
        southwest.requestLayout();
      } else {
        lbStats.setText("ERROR");
      }
    } catch (Exception e) {
      logger.error("", e);
      lbStats.setText("ERROR");
    }
  }

  protected void updateGaussian() {
    if (cbGaussianFit.isSelected()) {
      addGaussianCurves();
    } else {
      hideGaussianCurves();
    }
  }

  protected void addGaussianCurves() {
    if (pnHisto != null) {
      addGaussianCurve(pnHisto.getChart().getXYPlot());
    }
  }

  /**
   * Add Gaussian curve to the plot
   *
   * @param p
   */
  protected void addGaussianCurve(XYPlot p) {
    try {
      double gMin = Double.parseDouble(txtGaussianLower.getText());
      double gMax = Double.parseDouble(txtGaussianUpper.getText());
      int sigDigits = Integer.parseInt(getTxtPrecision().getText());

      XYDataset data = p.getDataset(0);
      hideGaussianCurve(p);

      HistogramChartFactory.addGaussianFit(p, data, 0, gMin, gMax, sigDigits, true);
    } catch (Exception ex) {
      logger.error("", ex);
    }
  }

  protected void hideGaussianCurves() {
    if (pnHisto != null) {
      hideGaussianCurve(pnHisto.getChart().getXYPlot());
    }
  }

  protected void hideGaussianCurve(XYPlot p) {
    if (p.getDatasetCount() > 1) {
      p.setRenderer(p.getDatasetCount() - 1, null);
      p.setDataset(p.getDatasetCount() - 1, null);
    }
  }

  public EChartViewer getChartPanel() {
    return pnHisto;
  }

  public HistogramData getData() {
    return data;
  }

  public Pane getSouthwest() {
    return southwest;
  }

  public TextField getTxtBinWidth() {
    return txtBinWidth;
  }

  public CheckBox getCbExcludeSmallerNoise() {
    return cbExcludeSmallerNoise;
  }

  public Label getLbStats() {
    return lbStats;
  }

  public TextField getTxtRangeX() {
    return txtRangeX;
  }

  public TextField getTxtRangeY() {
    return txtRangeY;
  }

  public TextField getTxtRangeYEnd() {
    return txtRangeYEnd;
  }

  public TextField getTxtRangeXEnd() {
    return txtRangeXEnd;
  }

  public CheckBox getCbGaussianFit() {
    return cbGaussianFit;
  }

  public TextField getTxtGaussianLower() {
    return txtGaussianLower;
  }

  public TextField getTxtGaussianUpper() {
    return txtGaussianUpper;
  }

  public TextField getTxtPrecision() {
    return txtPrecision;
  }

  public void setBinWidth(double binWidth) {
    txtBinWidth.setText(String.valueOf(binWidth));
  }

  public boolean isGaussianFitEnabled() {
    return cbGaussianFit.isSelected();
  }

  /**
   * set and update gaussian
   *
   * @param lower
   * @param upper
   */
  public void setGaussianFitRange(double lower, double upper) {
    txtGaussianLower.setText(String.valueOf(lower));
    txtGaussianUpper.setText(String.valueOf(upper));
    updateGaussian();
  }

  public Pane getBoxSettings() {
    return boxSettings;
  }
}
