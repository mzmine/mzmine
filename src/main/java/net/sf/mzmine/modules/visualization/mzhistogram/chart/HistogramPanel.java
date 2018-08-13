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

package net.sf.mzmine.modules.visualization.mzhistogram.chart;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.concurrent.ExecutionException;
import java.util.function.DoubleFunction;
import java.util.stream.DoubleStream;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.mzmine.chartbasics.EChartFactory;
import net.sf.mzmine.chartbasics.EChartPanel;
import net.sf.mzmine.framework.listener.DelayedDocumentListener;
import net.sf.mzmine.util.maths.Precision;

public class HistogramPanel extends JPanel {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final JPanel contentPanel;
  private DelayedDocumentListener ddlRepaint;
  private JPanel southwest;
  private JTextField txtBinWidth, txtBinShift;
  private JCheckBox cbExcludeSmallerNoise, cbThirdSQRT;
  private JLabel lbStats;
  private JTextField txtRangeX;
  private JTextField txtRangeY;
  private JTextField txtRangeXEnd;
  private JTextField txtRangeYEnd;
  private JTextField txtGaussianLower;
  private JTextField txtGaussianUpper;
  private JTextField txtPrecision;
  private JCheckBox cbGaussianFit;

  private EChartPanel pnHisto;
  private HistogramData data;
  private JPanel boxSettings;

  /**
   * Create the dialog.
   */
  public HistogramPanel() {
    setBounds(100, 100, 903, 952);
    setMinimumSize(new Dimension(600, 300));
    setLayout(new BorderLayout());
    contentPanel = new JPanel();
    add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new BorderLayout(0, 0));
    {
      JPanel west = new JPanel();
      contentPanel.add(west, BorderLayout.WEST);
      west.setLayout(new BorderLayout(0, 0));
      {
        JPanel panel = new JPanel();
        west.add(panel, BorderLayout.NORTH);
      }
    }
    {
      JPanel center1 = new JPanel();
      contentPanel.add(center1, BorderLayout.CENTER);
      center1.setLayout(new BorderLayout(0, 0));
      {
        boxSettings = new JPanel();
        center1.add(boxSettings, BorderLayout.SOUTH);
        boxSettings.setLayout(new BoxLayout(boxSettings, BoxLayout.Y_AXIS));
        {
          JPanel pnstats = new JPanel();
          boxSettings.add(pnstats);
          {
            lbStats = new JLabel("last stats:");
            pnstats.add(lbStats);
          }
        }
        {
          JPanel pnHistoSett = new JPanel();
          boxSettings.add(pnHistoSett);
          {
            cbExcludeSmallerNoise = new JCheckBox("exclude smallest");
            cbExcludeSmallerNoise.setSelected(true);
            pnHistoSett.add(cbExcludeSmallerNoise);
          }
          {
            cbThirdSQRT = new JCheckBox("cube root(I)");
            cbThirdSQRT.setSelected(false);
            pnHistoSett.add(cbThirdSQRT);
          }
          {
            Component horizontalStrut = Box.createHorizontalStrut(20);
            pnHistoSett.add(horizontalStrut);
          }
          {
            JLabel lblBinWidth = new JLabel("bin width");
            pnHistoSett.add(lblBinWidth);
          }
          {
            txtBinWidth = new JTextField();
            txtBinWidth.setText("");
            pnHistoSett.add(txtBinWidth);
            txtBinWidth.setColumns(7);
          }
          {
            Component horizontalStrut = Box.createHorizontalStrut(20);
            pnHistoSett.add(horizontalStrut);
          }
          {
            JLabel lblBinWidth = new JLabel("shift bins by");
            pnHistoSett.add(lblBinWidth);
          }
          {
            txtBinShift = new JTextField();
            txtBinShift.setText("0");
            pnHistoSett.add(txtBinShift);
            txtBinShift.setColumns(7);
          }
        }
        {
          JPanel secondGaussian = new JPanel();
          boxSettings.add(secondGaussian);
          {
            JButton btnToggleLegend = new JButton("Toggle legend");
            btnToggleLegend.addActionListener(e -> toggleLegends());
            btnToggleLegend.setToolTipText("Show/hide legend");
            secondGaussian.add(btnToggleLegend);
          }
          {
            JButton btnUpdateGaussian = new JButton("Update");
            btnUpdateGaussian.addActionListener(e -> updateGaussian());
            btnUpdateGaussian.setToolTipText("Update Gaussian fit");
            secondGaussian.add(btnUpdateGaussian);
          }
          {
            cbGaussianFit = new JCheckBox("Gaussian fit");
            secondGaussian.add(cbGaussianFit);
          }
          {
            JLabel lblFrom = new JLabel("from");
            secondGaussian.add(lblFrom);
          }
          {
            txtGaussianLower = new JTextField();
            txtGaussianLower.setToolTipText("The lower bound (domain axis) for the Gaussian fit");
            txtGaussianLower.setText("0");
            secondGaussian.add(txtGaussianLower);
            txtGaussianLower.setColumns(7);
          }
          {
            JLabel label = new JLabel("-");
            secondGaussian.add(label);
          }
          {
            txtGaussianUpper = new JTextField();
            txtGaussianUpper
                .setToolTipText("The upper bound (domain axis, x) for the Gaussian fit");
            txtGaussianUpper.setText("0");
            txtGaussianUpper.setColumns(7);
            secondGaussian.add(txtGaussianUpper);
          }
          {
            Component horizontalStrut = Box.createHorizontalStrut(20);
            secondGaussian.add(horizontalStrut);
          }
          {
            JLabel lblSignificantFigures = new JLabel("significant figures");
            secondGaussian.add(lblSignificantFigures);
          }
          {
            txtPrecision = new JTextField();
            txtPrecision.setText("4");
            secondGaussian.add(txtPrecision);
            txtPrecision.setColumns(3);
          }
        }
        {
          JPanel third = new JPanel();
          boxSettings.add(third);
          {
            JLabel lblRanges = new JLabel("x-range");
            third.add(lblRanges);
          }
          {
            txtRangeX = new JTextField();
            third.add(txtRangeX);
            txtRangeX.setToolTipText("Set the x-range for both histograms");
            txtRangeX.setText("0");
            txtRangeX.setColumns(6);
          }
          {
            JLabel label = new JLabel("-");
            third.add(label);
          }
          {
            txtRangeXEnd = new JTextField();
            txtRangeXEnd.setToolTipText("Set the x-range for both histograms");
            txtRangeXEnd.setText("0");
            txtRangeXEnd.setColumns(6);
            third.add(txtRangeXEnd);
          }
          {
            JButton btnApplyX = new JButton("Apply");
            btnApplyX.addActionListener(e -> applyXRange());
            third.add(btnApplyX);
          }
          {
            JPanel panel = new JPanel();
            boxSettings.add(panel);
            {
              JLabel label = new JLabel("y-range");
              panel.add(label);
            }
            {
              txtRangeY = new JTextField();
              panel.add(txtRangeY);
              txtRangeY.setText("0");
              txtRangeY.setToolTipText("Set the y-range for both histograms");
              txtRangeY.setColumns(6);
            }
            {
              JLabel label = new JLabel("-");
              panel.add(label);
            }
            {
              txtRangeYEnd = new JTextField();
              txtRangeYEnd.setToolTipText("Set the y-range for both histograms");
              txtRangeYEnd.setText("0");
              txtRangeYEnd.setColumns(6);
              panel.add(txtRangeYEnd);
            }
            {
              JButton btnApplyY = new JButton("Apply");
              btnApplyY.addActionListener(e -> applyYRange());
              panel.add(btnApplyY);
            }
          }
        }
      }
      {
        southwest = new JPanel();
        center1.add(southwest, BorderLayout.CENTER);
        southwest.setLayout(new BorderLayout(0, 0));
      }
    }

    addListener();
  }

  /**
   * 
   * @param data
   * @param binWidth zero (0) for auto detection, -1 to keep last binWidth
   */
  public HistogramPanel(HistogramData data, double binWidth) {
    this();
    setData(data, binWidth);
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

      //
      ddlRepaint.stop();
      updateHistograms();

      contentPanel.revalidate();
      contentPanel.repaint();
    }
  }

  /**
   * Toggles visibility of legends
   */
  private void toggleLegends() {
    if (pnHisto != null) {
      LegendTitle legend = pnHisto.getChart().getLegend();
      if (legend != null)
        legend.setVisible(!legend.isVisible());
    }
  }

  private void addListener() {
    ddlRepaint = new DelayedDocumentListener(e -> repaint());

    // ranges
    DelayedDocumentListener ddlx = new DelayedDocumentListener(e -> applyXRange());
    DelayedDocumentListener ddly = new DelayedDocumentListener(e -> applyYRange());

    txtRangeX.getDocument().addDocumentListener(ddlx);
    txtRangeXEnd.getDocument().addDocumentListener(ddlx);
    txtRangeY.getDocument().addDocumentListener(ddly);
    txtRangeYEnd.getDocument().addDocumentListener(ddly);
    cbThirdSQRT.addItemListener(e -> updateHistograms());
    cbExcludeSmallerNoise.addItemListener(e -> updateHistograms());
    txtBinWidth.getDocument()
        .addDocumentListener(new DelayedDocumentListener(e -> updateHistograms()));
    txtBinShift.getDocument()
        .addDocumentListener(new DelayedDocumentListener(e -> updateHistograms()));

    // add gaussian?
    cbGaussianFit.addItemListener(e -> updateGaussian());
  }


  private void applyXRange() {
    try {
      double x = Double.parseDouble(txtRangeX.getText());
      double xe = Double.parseDouble(txtRangeXEnd.getText());
      if (x < xe) {
        if (pnHisto != null)
          pnHisto.getChart().getXYPlot().getDomainAxis().setRange(x, xe);
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
        if (pnHisto != null)
          pnHisto.getChart().getXYPlot().getRangeAxis().setRange(y, ye);
      }
    } catch (Exception e2) {
      logger.error("", e2);
    }
  }


  /**
   * Create new histograms
   * 
   * @throws Exception
   */
  private void updateHistograms() {
    if (data != null) {
      double binwidth2 = Double.NaN;
      double binShift2 = Double.NaN;
      try {
        binwidth2 = Double.parseDouble(txtBinWidth.getText());
        binShift2 = Double.parseDouble(txtBinShift.getText());
      } catch (Exception e) {
      }
      if (!Double.isNaN(binShift2)) {
        try {
          final double binwidth = binwidth2;
          final double binShift = Math.abs(binShift2);
          new SwingWorker<JFreeChart, Void>() {
            @Override
            protected JFreeChart doInBackground() throws Exception {
              // create histogram
              double[] dat = data.getData();
              if (cbExcludeSmallerNoise.isSelected()) {
                double noise = data.getRange().getLowerBound();
                // get processed data from original image
                dat = DoubleStream.of(dat).filter(d -> d > noise).toArray();
              }

              Range r = EChartFactory.getBounds(dat);

              DoubleFunction<Double> f =
                  cbThirdSQRT.isSelected() ? val -> Math.cbrt(val) : val -> val;

              JFreeChart chart = EChartFactory.createHistogram(dat, "I", binwidth,
                  r.getLowerBound() - binShift, r.getUpperBound(), f);
              // add gaussian?
              if (cbGaussianFit.isSelected()) {
                addGaussianCurve(chart.getXYPlot());
              }
              return chart;
            }

            @Override
            protected void done() {
              JFreeChart histo;
              try {
                Range x = null, y = null;
                if (pnHisto != null) {
                  x = pnHisto.getChart().getXYPlot().getDomainAxis().getRange();
                  y = pnHisto.getChart().getXYPlot().getRangeAxis().getRange();
                }
                histo = get();
                if (x != null)
                  histo.getXYPlot().getDomainAxis().setRange(x);
                if (y != null)
                  histo.getXYPlot().getRangeAxis().setRange(y);
                pnHisto = new EChartPanel(histo, true, true, true, true, true);
                histo.getLegend().setVisible(true);

                southwest.removeAll();
                southwest.add(pnHisto, BorderLayout.CENTER);
                southwest.getParent().revalidate();
                southwest.getParent().repaint();
              } catch (InterruptedException e) {
                logger.error("", e);
              } catch (ExecutionException e) {
                logger.error("", e);
              }
            }
          }.execute();
        } catch (Exception e1) {
          logger.error("", e1);
        }

      }
    }
  }

  protected void updateGaussian() {
    if (cbGaussianFit.isSelected())
      addGaussianCurves();
    else
      hideGaussianCurves();
  }

  protected void addGaussianCurves() {
    if (pnHisto != null)
      addGaussianCurve(pnHisto.getChart().getXYPlot());
  }

  /**
   * Add Gaussian curve to the plot
   * 
   * @param p
   */
  protected void addGaussianCurve(XYPlot p) {
    try {
      double gMin = Double.valueOf(txtGaussianLower.getText());
      double gMax = Double.valueOf(txtGaussianUpper.getText());
      int sigDigits = Integer.valueOf(getTxtPrecision().getText());

      XYDataset data = p.getDataset(0);
      hideGaussianCurve(p);

      EChartFactory.addGaussianFit(p, data, 0, gMin, gMax, sigDigits, true);
    } catch (Exception ex) {
      logger.error("", ex);
    }
  }

  protected void hideGaussianCurves() {
    if (pnHisto != null)
      hideGaussianCurve(pnHisto.getChart().getXYPlot());
  }

  protected void hideGaussianCurve(XYPlot p) {
    if (p.getDatasetCount() > 1) {
      p.setRenderer(p.getDatasetCount() - 1, null);
      p.setDataset(p.getDatasetCount() - 1, null);
    }
  }

  public EChartPanel getChartPanel() {
    return pnHisto;
  }

  public HistogramData getData() {
    return data;
  }


  public JPanel getSouthwest() {
    return southwest;
  }

  public JTextField getTxtBinWidth() {
    return txtBinWidth;
  }

  public JCheckBox getCbExcludeSmallerNoise() {
    return cbExcludeSmallerNoise;
  }

  public JLabel getLbStats() {
    return lbStats;
  }

  public JTextField getTxtRangeX() {
    return txtRangeX;
  }

  public JTextField getTxtRangeY() {
    return txtRangeY;
  }

  public JTextField getTxtRangeYEnd() {
    return txtRangeYEnd;
  }

  public JTextField getTxtRangeXEnd() {
    return txtRangeXEnd;
  }

  public JCheckBox getCbGaussianFit() {
    return cbGaussianFit;
  }

  public JTextField getTxtGaussianLower() {
    return txtGaussianLower;
  }

  public JTextField getTxtGaussianUpper() {
    return txtGaussianUpper;
  }

  public JTextField getTxtPrecision() {
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

  public JPanel getBoxSettings() {
    return boxSettings;
  }
}
