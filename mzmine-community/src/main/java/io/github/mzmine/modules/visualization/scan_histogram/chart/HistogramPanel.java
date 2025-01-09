/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.scan_histogram.chart;

import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.gui.chartbasics.HistogramChartFactory;
import io.github.mzmine.gui.chartbasics.JFreeChartUtils;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.util.maths.Precision;
import java.awt.event.ActionEvent;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;


public class HistogramPanel extends BorderPane {

  private final Logger logger = Logger.getLogger(getClass().getName());
  private final BorderPane contentPanel;
  private final AtomicLong currentUpdateID = new AtomicLong(0);
  private final BorderPane southwest;
  private final TextField txtBinWidth;
  private final TextField txtBinShift;
  private final CheckBox cbExcludeSmallerNoise;
  private final Label lbStats;
  private final TextField txtRangeX;
  private final TextField txtRangeY;
  private final TextField txtRangeXEnd;
  private final TextField txtRangeYEnd;
  private final TextField txtGaussianLower;
  private final TextField txtGaussianUpper;
  private final TextField txtPrecision;
  private final CheckBox cbGaussianFit;
  private final CheckBox cbKeepSameXaxis;
  private final VBox boxSettings;
  private final VBox secondGaussian;
  private final HBox pnHistoSett;
  private final HBox xRanges;
  private final HBox yRanges;
  private final Executor exec;
  private EChartViewer pnHisto;
  private String xLabel;
  private HistogramData data;

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

      Label lblBinWidth = new Label("bin width");

      txtBinWidth = new TextField();
      txtBinWidth.setText("");

      Label lblBinShift = new Label("shift by");

      txtBinShift = new TextField();
      txtBinShift.setText("0");

      pnHistoSett = new HBox(5);
      pnHistoSett.getChildren()
          .addAll(cbExcludeSmallerNoise, lblBinWidth, txtBinWidth, lblBinShift, txtBinShift);
      pnHistoSett.setAlignment(Pos.CENTER);
    }
    {
      ToggleButton btnToggleLegend = new ToggleButton("Toggle legend");
      btnToggleLegend.setOnAction(e -> toggleLegends());
      btnToggleLegend.setTooltip(new Tooltip("Show/hide legend"));

      Button btnUpdateGaussian = new Button("Update");
      btnUpdateGaussian.setOnAction(e -> updateGaussian());
      btnUpdateGaussian.setTooltip(new Tooltip("Update Gaussian fit"));

      cbGaussianFit = new CheckBox("Gaussian fit");

      txtGaussianLower = new TextField();
      txtGaussianLower.setTooltip(
          new Tooltip("The lower bound (domain axis) for the Gaussian fit"));
      txtGaussianLower.setText("0");

      txtGaussianUpper = new TextField();
      txtGaussianUpper.setTooltip(
          new Tooltip("The upper bound (domain axis, x) for the Gaussian fit"));
      txtGaussianUpper.setText("0");

      txtPrecision = new TextField();
      txtPrecision.setTooltip(new Tooltip("Change number of significant figures and press update"));
      txtPrecision.setText("6");

      HBox gauss1 = new HBox(5, btnToggleLegend, btnUpdateGaussian, cbGaussianFit);
      gauss1.setAlignment(Pos.CENTER);
      HBox gauss2 = new HBox(5, new Label("Limits"), txtGaussianLower, new Label("-"),
          txtGaussianUpper);
      gauss2.setAlignment(Pos.CENTER);
      HBox gauss3 = new HBox(5, new Label("Significant figures"), txtPrecision);
      gauss3.setAlignment(Pos.CENTER);

      secondGaussian = new VBox(5);
      secondGaussian.setAlignment(Pos.CENTER);
//      secondGaussian.setPadding(new Insets(10));
      secondGaussian.getChildren().addAll(gauss1, gauss2, gauss3);

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

      xRanges = new HBox(5);
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

    VBox pnRanges = new VBox(5);
    pnRanges.setAlignment(Pos.CENTER);
//    pnRanges.setPadding(new Insets(10));

    // jump to next distribution
    cbKeepSameXaxis = new CheckBox("keep same x-axis length");

    Button btnPrevious = new Button("<");
    btnPrevious.setTooltip(new Tooltip("Jump to previous distribution (use left arrow"));
    btnPrevious.setOnAction(e -> jumpToPrevFeature());

    Button btnNext = new Button(">");
    btnNext.setTooltip(new Tooltip("Jump to previous distribution (use right arrow"));
    btnNext.setOnAction(e -> jumpToNextFeature());

    HBox pnJump = new HBox(5, cbKeepSameXaxis, btnPrevious, btnNext);
    pnJump.setOnKeyTyped(event -> {
      if (event.getCode() == KeyCode.RIGHT) {
        jumpToNextFeature();
      }
      if (event.getCode() == KeyCode.LEFT) {
        jumpToPrevFeature();
      }
//      event.consume();
    });
    pnRanges.getChildren().addAll(xRanges, yRanges, pnJump);

    secondGaussian.setPadding(new Insets(2));
    pnHistoSett.setPadding(new Insets(2));
    pnRanges.setPadding(new Insets(2));

    Accordion paramAccordion = new Accordion();
    paramAccordion.getPanes().add(new TitledPane("Histogram", pnHistoSett));
    paramAccordion.getPanes().add(new TitledPane("Gaussian fit", secondGaussian));
    paramAccordion.getPanes().add(new TitledPane("Ranges", pnRanges));

    boxSettings = new VBox(5);
    boxSettings.setAlignment(Pos.CENTER);
//    boxSettings.setPadding(new Insets(10));
    boxSettings.getChildren().addAll(lbStats, paramAccordion);
    contentPanel.setBottom(boxSettings);

    southwest = new BorderPane();
    contentPanel.setCenter(southwest);

    addListener();
    exec = Executors.newFixedThreadPool(2, runnable -> {
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

  public void setDomainLabel(final String xLabel) {
    this.xLabel = xLabel;
  }

  public void setData(final HistogramData data, final double binWidth) {
    setData(data, binWidth, false);
  }

  /**
   * set data and update histo
   *
   * @param data
   * @param binWidth zero (0) for auto detection, -1 to keep last binWidth
   * @param autoZoom reset zoom for both axes
   */
  public void setData(HistogramData data, double binWidth, boolean autoZoom) {
    this.data = data;
    if (data != null) {
      if (binWidth > 0) {
        txtBinWidth.setText(String.valueOf(binWidth));
      } else if (binWidth == 0 || txtBinWidth.getText().isEmpty()) {
        // set bin width
        int bin = (int) Math.sqrt(data.size());
        double l = data.getRange().getLength();
        double bw = l / bin;
        String bws = String.valueOf(bw);
        // round
        try {
          bws = Precision.toString(bw, 4);
        } catch (Exception e) {
        }
        txtBinWidth.setText(bws);
      }

      updateHistograms(autoZoom);

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
    txtRangeX.textProperty().addListener((o, ov, nv) -> applyXRange());
    txtRangeXEnd.textProperty().addListener((o, ov, nv) -> applyXRange());
    txtRangeY.textProperty().addListener((o, ov, nv) -> applyYRange());
    txtRangeYEnd.textProperty().addListener((o, ov, nv) -> applyYRange());

//    txtBinShift.textProperty().addListener((o, ov, nv) -> updateHistograms());
//    txtBinWidth.textProperty().addListener((o, ov, nv) -> updateHistograms());
//    cbExcludeSmallerNoise.setOnAction(e -> updateHistograms());

    PauseTransition pause = new PauseTransition(Duration.seconds(1));
    ChangeListener<String> listener = (observable, oldValue, newValue) -> {
      pause.setOnFinished(event -> HistogramPanel.this.updateHistograms(false));
      pause.playFromStart();
    };
    txtBinShift.textProperty().addListener(listener);
    txtBinWidth.textProperty().addListener(listener);
    cbExcludeSmallerNoise.setOnAction(e -> {
      pause.setOnFinished(event -> HistogramPanel.this.updateHistograms(false));
      pause.playFromStart();
    });

    // add gaussian?
    cbGaussianFit.setOnAction(e -> updateGaussian());
  }

  private void applyXRange() {
    if (!txtRangeX.getText().equals("") && !txtRangeXEnd.getText().equals("")) {
      try {
        double x = Double.parseDouble(txtRangeX.getText());
        double xe = Double.parseDouble(txtRangeXEnd.getText());
        if (x < xe) {
          XYPlot plot = getXYPlot();
          if (plot != null) {
            plot.getDomainAxis().setRange(x, xe);
          }
        }
      } catch (Exception e2) {
        logger.severe(e2.toString());
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
        logger.severe(e2.toString());
      }
    }
  }

  /**
   * Create new histograms
   */
  private void updateHistograms(boolean autoZoom) {
    if (data != null) {
      double binwidth2 = Double.NaN;
      double binShift2 = Double.NaN;
      try {
        binwidth2 = Double.parseDouble(txtBinWidth.getText());
        binShift2 = Double.parseDouble(txtBinShift.getText());
      } catch (Exception e) {
        logger.severe(e.toString());
      }
      if (!Double.isNaN(binShift2)) {
        try {

          lbStats.setText("UPDATING");
          lbStats.setTextFill(Color.RED);

          final double binwidth = binwidth2;
          final double binShift = Math.abs(binShift2);
          try {
            // set current update ID to prevent old updates to change the chart
            final long startID = currentUpdateID.incrementAndGet();

            exec.execute(() -> {
              logger.finest("Create histogram update thread " + startID);
              if (startID == currentUpdateID.get()) {
                JFreeChart chart = doInBackground(binShift, binwidth);
                Platform.runLater(() -> {
                  if (startID == currentUpdateID.get()) {
                    done(chart, autoZoom);
                    logger.info("Finished histogram update thread " + startID);
                  }
                });
              }
            });

          } catch (Exception e) {
            logger.severe(e.toString());
          }
        } catch (Exception e1) {
          logger.severe(e1.toString());
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

    JFreeChart chart = HistogramChartFactory.createHistogram(dat, xLabel, binwidth,
        r.getLowerBound() - binShift, r.getUpperBound());
    // add gaussian?
    if (cbGaussianFit.isSelected()) {
      addGaussianCurve(chart.getXYPlot());
    }
    return chart;
  }

  protected void done(JFreeChart chart, final boolean autoZoom) {
    JFreeChart histo;
    try {
      Range x = null, y = null;
      if (pnHisto != null) {
        x = pnHisto.getChart().getXYPlot().getDomainAxis().getRange();
        y = pnHisto.getChart().getXYPlot().getRangeAxis().getRange();
      }
      histo = chart;

      if (histo != null) {
        if (x != null && !autoZoom) {
          histo.getXYPlot().getDomainAxis().setRange(x);
        }
        if (y != null && !autoZoom) {
          histo.getXYPlot().getRangeAxis().setRange(y);
        }
        histo.getLegend().setVisible(true);
        if (pnHisto == null) {
          pnHisto = new EChartViewer(histo, true, true, true, true, true);
          southwest.setCenter(pnHisto);
        } else {
          pnHisto.setChart(histo);
          if (histo != null) {
            histo.getLegend().setVisible(false);
          }
        }

        lbStats.setText("DONE");
        lbStats.setTextFill(Color.GREEN);
        southwest.requestLayout();
      } else {
        lbStats.setText("ERROR");
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error during histogram panel set chart " + e.getMessage(), e);
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
      logger.severe(ex.toString());
    }
  }

  protected void hideGaussianCurves() {
    if (pnHisto != null) {
      hideGaussianCurve(pnHisto.getChart().getXYPlot());
    }
  }

  protected void hideGaussianCurve(XYPlot p) {
    if (JFreeChartUtils.getDatasetCountNullable(p) > 1) {
      p.setRenderer(JFreeChartUtils.getDatasetCountNullable(p) - 1, null);
      p.setDataset(JFreeChartUtils.getDatasetCountNullable(p) - 1, null);
    }
  }

  /**
   * Set zoom factor around peak at data point i
   *
   * @param i index
   */
  private void setZoomAroundFeatureAt(int i) {
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
    if (isGaussianFitEnabled()) {
      // find
      setGaussianFitRange(lower, upper);
    }

    // auto range y
    ChartLogicsFX.autoRangeAxis(getChartPanel());
  }


  public void actionPerformed(final ActionEvent event) {
    final String command = event.getActionCommand();
    if ("PREVIOUS_PEAK".equals(command)) {
      jumpToPrevFeature();
    } else if ("NEXT_PEAK".equals(command)) {
      jumpToNextFeature();
    }
  }

  /**
   * tries to find the next local maximum to jump to the prev peak
   */
  private void jumpToPrevFeature() {
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
            setZoomAroundFeatureAt(i);
            return;
          }
        }
      }
    }
  }

  private XYPlot getXYPlot() {
    return getChartPanel() != null ? getChartPanel().getChart().getXYPlot() : null;
  }

  /**
   * tries to find the next local maximum to jump to the prev peak
   */
  private void jumpToNextFeature() {
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
            setZoomAroundFeatureAt(i);
            return;
          }
        }
      }
    }
  }


  public EChartViewer getChartPanel() {
    return pnHisto;
  }

  public HistogramData getData() {
    return data;
  }

  public void setData(HistogramData data) {
    setData(data, -1);
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
   */
  public void setGaussianFitRange(double lower, double upper) {
    txtGaussianLower.setText(String.valueOf(lower));
    txtGaussianUpper.setText(String.valueOf(upper));
    updateGaussian();
  }

}
