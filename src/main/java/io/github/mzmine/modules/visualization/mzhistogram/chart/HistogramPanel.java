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
//import io.github.mzmine.gui.framework.listener.DelayedDocumentListener;
import io.github.mzmine.util.maths.Precision;
import java.util.function.DoubleFunction;
import java.util.stream.DoubleStream;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
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

  /**
   * Create the dialog.
   */
  public HistogramPanel() {
    setLayoutX(100);
    setLayoutY(100);
    setPrefSize(903, 952);
    setMinSize(600, 300);
    contentPanel = new BorderPane();
    setCenter(contentPanel);
    {
      Pane panel = new Pane();
      contentPanel.setLeft(panel);
    }
    {
      BorderPane center1 = new BorderPane();
      contentPanel.setCenter(center1);
      {
        boxSettings = new VBox();
        center1.setBottom(boxSettings);
        {
          Pane pnstats = new Pane();
          boxSettings.getChildren().add(pnstats);
          {
            lbStats = new Label("");
            lbStats.setFont(new Font("Tahoma", 14));
            pnstats.getChildren().add(lbStats);
          }
        }
        {

          Pane pnHistoSett = new Pane();
          boxSettings.getChildren().add(pnHistoSett);
          {
            cbExcludeSmallerNoise = new CheckBox("exclude smallest");
            cbExcludeSmallerNoise.setSelected(true);
            pnHistoSett.getChildren().add(cbExcludeSmallerNoise);
          }
          {
            cbThirdSQRT = new CheckBox("cube root(I)");
            cbThirdSQRT.setSelected(false);
            pnHistoSett.getChildren().add(cbThirdSQRT);
          }
          {
//    Component horizontalStrut = Box.createHorizontalStrut(20);
//    pnHistoSett.getChildren().add(horizontalStrut);
          }
          {
            Label lblBinWidth = new Label("bin width");
            pnHistoSett.getChildren().add(lblBinWidth);
          }
          {
            txtBinWidth = new TextField();
            txtBinWidth.setText("");
            pnHistoSett.getChildren().add(txtBinWidth);
          }
          {
//    horizontalStrut = Box.createHorizontalStrut(20);
//    pnHistoSett.getChildren().add(horizontalStrut);
          }
          {
            Label lblBinWidth = new Label("shift bins by");
            pnHistoSett.getChildren().add(lblBinWidth);
          }
          {
            txtBinShift = new TextField();
            txtBinShift.setText("0");
            pnHistoSett.getChildren().add(txtBinShift);
          }
        }
        {
          Pane secondGaussian = new Pane();
          boxSettings.getChildren().add(secondGaussian);
          {
            Button btnToggleLegend = new Button("Toggle legend");
            btnToggleLegend.setOnAction(e -> toggleLegends());
            btnToggleLegend.setTooltip(new Tooltip("Show/hide legend"));
            secondGaussian.getChildren().add(btnToggleLegend);
          }
          {
            Button btnUpdateGaussian = new Button("Update");
            btnUpdateGaussian.setOnAction(e -> updateGaussian());
            btnUpdateGaussian.setTooltip(new Tooltip("Update Gaussian fit"));
            secondGaussian.getChildren().add(btnUpdateGaussian);
          }
          {
            cbGaussianFit = new CheckBox("Gaussian fit");
            secondGaussian.getChildren().add(cbGaussianFit);
          }
          {
            Label lblFrom = new Label("from");
            secondGaussian.getChildren().add(lblFrom);
          }
          {
            txtGaussianLower = new TextField();
            txtGaussianLower
                .setTooltip(new Tooltip("The lower bound (domain axis) for the Gaussian fit"));
            txtGaussianLower.setText("0");
            secondGaussian.getChildren().add(txtGaussianLower);
          }
          {
            Label label = new Label("-");
            secondGaussian.getChildren().add(label);
          }
          {
            txtGaussianUpper = new TextField();
            txtGaussianUpper
                .setTooltip(new Tooltip("The upper bound (domain axis, x) for the Gaussian fit"));
            txtGaussianUpper.setText("0");
            secondGaussian.getChildren().add(txtGaussianUpper);
          }
          {
//    horizontalStrut = Box.createHorizontalStrut(20);
//    secondGaussian.getChildren().add(horizontalStrut);
          }
          {
            Label lblSignificantFigures = new Label("significant figures");
            secondGaussian.getChildren().add(lblSignificantFigures);
          }
          {
            txtPrecision = new TextField();
            txtPrecision
                .setTooltip(new Tooltip("Change number of significant figures and press update"));
            txtPrecision.setText("6");
            secondGaussian.getChildren().add(txtPrecision);
          }
        }
        {
          Pane third = new Pane();
          boxSettings.getChildren().add(third);

          {
            Label lblRanges = new Label("x-range");
            third.getChildren().add(lblRanges);
          }
          {
            txtRangeX = new TextField();
            third.getChildren().add(txtRangeX);
            txtRangeX.setTooltip(new Tooltip("Set the x-range for both histograms"));
            txtRangeX.setText("0");
          }

          {
            Label label = new Label("-");
            third.getChildren().add(label);
          }
          {
            txtRangeXEnd = new TextField();
            txtRangeXEnd.setTooltip(new Tooltip("Set the x-range for both histograms"));
            txtRangeXEnd.setText("0");
            third.getChildren().add(txtRangeXEnd);
          }
          {
            Button btnApplyX = new Button("Apply");
            btnApplyX.setOnAction(e -> applyXRange());
            third.getChildren().add(btnApplyX);
          }
          {
            Pane panel = new Pane();
            boxSettings.getChildren().add(panel);

            {
              Label label = new Label("y-range");
              panel.getChildren().add(label);
            }
            {
              txtRangeY = new TextField();
              panel.getChildren().add(txtRangeY);
              txtRangeY.setText("0");
              txtRangeY.setTooltip(new Tooltip("Set the y-range for both histograms"));
            }
            {
              Label label = new Label("-");
              panel.getChildren().add(label);
            }
            {
              txtRangeYEnd = new TextField();
              txtRangeYEnd.setTooltip(new Tooltip("Set the y-range for both histograms"));
              txtRangeYEnd.setText("0");
              panel.getChildren().add(txtRangeYEnd);
            }
            {
              Button btnApplyY = new Button("Apply");
              btnApplyY.setOnAction(e -> applyYRange());
              panel.getChildren().add(btnApplyY);
            }
          }
        }
      }

      {
        southwest = new BorderPane();
        center1.setCenter(southwest);
      }

    }

    addListener();

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
          logger.error("", e);
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
//    ddlRepaint = new DelayedDocumentListener(e -> requestLayout())

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

  private void applyYRange() {
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

  /**
   * Create new histograms
   *
   *
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
              JFreeChart chart = doInBackground(binShift, binwidth);
              done(chart);
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

        southwest = new BorderPane();
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
