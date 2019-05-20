/*
 * Copyright 2006-2019 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.kendrickmassplot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.modules.visualization.kendrickmassplot.chartutils.XYBlockPixelSizeRenderer;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.dialogs.FeatureOverviewWindow;

/**
 * Window for Kendrick mass plots
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class KendrickMassPlotWindow extends JFrame implements ActionListener {

  private static final long serialVersionUID = 1L;
  private KendrickMassPlotToolBar toolBar;
  private JFreeChart chart;
  private PeakListRow selectedRows[];
  private String xAxisKMBase;
  private String zAxisKMBase;
  private String customYAxisKMBase;
  private String customXAxisKMBase;
  private String customZAxisKMBase;
  private boolean useCustomXAxisKMBase;
  private boolean useCustomZAxisKMBase;
  private double xAxisShift;
  private double yAxisShift;
  private double zAxisShift;
  private int yAxisCharge;
  private int xAxisCharge;
  private int zAxisCharge;
  private int yAxisDivisor;
  private int xAxisDivisor;
  private int zAxisDivisor;

  public KendrickMassPlotWindow(JFreeChart chart, ParameterSet parameters, EChartPanel chartPanel) {

    PeakList peakList = parameters.getParameter(KendrickMassPlotParameters.peakList).getValue()
        .getMatchingPeakLists()[0];

    this.selectedRows =
        parameters.getParameter(KendrickMassPlotParameters.selectedRows).getMatchingRows(peakList);

    this.customYAxisKMBase =
        parameters.getParameter(KendrickMassPlotParameters.yAxisCustomKendrickMassBase).getValue();

    this.useCustomXAxisKMBase =
        parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase).getValue();

    if (useCustomXAxisKMBase == true) {
      this.customXAxisKMBase =
          parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase)
              .getEmbeddedParameter().getValue();
    } else {
      this.xAxisKMBase = parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue();
    }

    this.useCustomZAxisKMBase =
        parameters.getParameter(KendrickMassPlotParameters.zAxisCustomKendrickMassBase).getValue();

    if (useCustomZAxisKMBase == true) {
      this.customZAxisKMBase =
          parameters.getParameter(KendrickMassPlotParameters.zAxisCustomKendrickMassBase)
              .getEmbeddedParameter().getValue();
    } else {
      this.zAxisKMBase = parameters.getParameter(KendrickMassPlotParameters.zAxisValues).getValue();
    }

    this.chart = chart;

    this.yAxisCharge = 1;

    this.xAxisCharge = 1;

    this.zAxisCharge = 1;

    this.yAxisDivisor = 1;

    this.xAxisDivisor = 1;

    this.zAxisDivisor = 1;

    this.xAxisShift = 0;

    this.yAxisShift = 0;

    this.zAxisShift = 0;

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setBackground(Color.white);

    // Add toolbar
    toolBar = new KendrickMassPlotToolBar(this, xAxisShift, yAxisShift, zAxisShift, xAxisCharge,
        yAxisCharge, zAxisCharge, xAxisDivisor, yAxisDivisor, zAxisDivisor, useCustomXAxisKMBase,
        useCustomZAxisKMBase);
    add(toolBar, BorderLayout.EAST);

    // Add the Windows menu
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(new WindowsMenu());
    setJMenuBar(menuBar);

    // mouse listener
    chartPanel.addChartMouseListener(new ChartMouseListener() {

      @Override
      public void chartMouseClicked(ChartMouseEvent event) {
        XYPlot plot = (XYPlot) chart.getPlot();
        double xValue = plot.getDomainCrosshairValue();
        double yValue = plot.getRangeCrosshairValue();

        if (plot.getDataset() instanceof KendrickMassPlotXYZDataset) {
          KendrickMassPlotXYZDataset dataset = (KendrickMassPlotXYZDataset) plot.getDataset();
          double[] xValues = new double[dataset.getItemCount(0)];
          for (int i = 0; i < xValues.length; i++) {
            if ((event.getTrigger().getButton() == MouseEvent.BUTTON1)
                && (event.getTrigger().getClickCount() == 2)) {
              if (dataset.getX(0, i).doubleValue() == xValue
                  && dataset.getY(0, i).doubleValue() == yValue) {
                new FeatureOverviewWindow(selectedRows[i]);
              }
            }
          }
        }
        if (plot.getDataset() instanceof KendrickMassPlotXYDataset) {
          KendrickMassPlotXYDataset dataset = (KendrickMassPlotXYDataset) plot.getDataset();
          double[] xValues = new double[dataset.getItemCount(0)];
          for (int i = 0; i < xValues.length; i++) {
            if ((event.getTrigger().getButton() == MouseEvent.BUTTON1)
                && (event.getTrigger().getClickCount() == 2)) {
              if (dataset.getX(0, i).doubleValue() == xValue
                  && dataset.getY(0, i).doubleValue() == yValue) {
                new FeatureOverviewWindow(selectedRows[i]);
              }
            }
          }
        }
      }

      @Override
      public void chartMouseMoved(ChartMouseEvent event) {}
    });
    pack();
  }

  @Override
  public void actionPerformed(ActionEvent event) {

    String command = event.getActionCommand();

    if (command.equals("TOGGLE_BLOCK_SIZE")) {
      XYPlot plot = chart.getXYPlot();
      XYBlockPixelSizeRenderer renderer = (XYBlockPixelSizeRenderer) plot.getRenderer();
      int height = (int) renderer.getBlockHeightPixel();

      if (height == 1) {
        height++;
      } else if (height == 5) {
        height = 1;
      } else if (height < 5 && height != 1) {
        height++;
      }
      renderer.setBlockHeightPixel(height);
      renderer.setBlockWidthPixel(height);
    }

    if (command.equals("TOGGLE_BACK_COLOR")) {
      XYPlot plot = chart.getXYPlot();
      if (plot.getBackgroundPaint() == Color.WHITE) {
        plot.setBackgroundPaint(Color.BLACK);
      } else {
        plot.setBackgroundPaint(Color.WHITE);
      }

    }

    if (command.equals("TOGGLE_GRID")) {
      XYPlot plot = chart.getXYPlot();
      if (plot.getDomainGridlinePaint() == Color.BLACK) {
        plot.setDomainGridlinePaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.WHITE);
      } else {
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinePaint(Color.BLACK);
      }

    }

    if (command.equals("TOGGLE_ANNOTATIONS")) {
      XYPlot plot = chart.getXYPlot();
      XYBlockPixelSizeRenderer renderer = (XYBlockPixelSizeRenderer) plot.getRenderer();
      Boolean itemNameVisible = renderer.getDefaultItemLabelsVisible();
      if (itemNameVisible == false) {
        renderer.setDefaultItemLabelsVisible(true);
      } else {
        renderer.setDefaultItemLabelsVisible(false);
      }
      if (plot.getBackgroundPaint() == Color.BLACK) {
        renderer.setDefaultItemLabelPaint(Color.WHITE);
      } else {
        renderer.setDefaultItemLabelPaint(Color.BLACK);
      }
    }

    // y axis commands
    if (command.equals("SHIFT_KMD_UP_Y")) {
      Double shiftValue = 0.01;
      yAxisShift = yAxisShift + shiftValue;
      XYPlot plot = chart.getXYPlot();
      shiftChanged(plot, shiftValue, command);
    }

    if (command.equals("SHIFT_KMD_DOWN_Y")) {
      Double shiftValue = -0.01;
      yAxisShift = yAxisShift + shiftValue;
      XYPlot plot = chart.getXYPlot();
      shiftChanged(plot, shiftValue, command);
    }

    if (command.equals("CHANGE_CHARGE_UP_Y")) {
      yAxisCharge = yAxisCharge + 1;
      XYPlot plot = chart.getXYPlot();
      chargeOrDivisorChanged(plot);
    }

    if (command.equals("CHANGE_CHARGE_DOWN_Y")) {
      if (yAxisCharge > 1) {
        yAxisCharge = yAxisCharge - 1;
      } else
        yAxisCharge = 1;
      XYPlot plot = chart.getXYPlot();
      chargeOrDivisorChanged(plot);
    }

    if (command.equals("CHANGE_DIVISOR_UP_Y")) {
      int minDivisor = getMinimumRecommendedDivisor(customYAxisKMBase);
      int maxDivisor = getMaximumRecommendedDivisor(customYAxisKMBase);
      if (yAxisDivisor == 1) {
        yAxisDivisor = minDivisor;
      } else if (yAxisDivisor >= minDivisor && yAxisDivisor < maxDivisor) {
        yAxisDivisor++;
      }
      XYPlot plot = chart.getXYPlot();
      chargeOrDivisorChanged(plot);
    }

    if (command.equals("CHANGE_DIVISOR_DOWN_Y")) {
      int minDivisor = getMinimumRecommendedDivisor(customYAxisKMBase);
      int maxDivisor = getMaximumRecommendedDivisor(customYAxisKMBase);
      if (yAxisDivisor > minDivisor && yAxisDivisor <= maxDivisor) {
        yAxisDivisor = yAxisDivisor - 1;
      } else if (yAxisDivisor == minDivisor) {
        yAxisDivisor = 1;
      }
      XYPlot plot = chart.getXYPlot();
      chargeOrDivisorChanged(plot);
    }

    // x axis commands
    if (command.equals("SHIFT_KMD_UP_X")) {
      Double shiftValue = 0.01;
      xAxisShift = xAxisShift + shiftValue;
      XYPlot plot = chart.getXYPlot();
      shiftChanged(plot, shiftValue, command);
    }

    if (command.equals("SHIFT_KMD_DOWN_X")) {
      Double shiftValue = -0.01;
      xAxisShift = xAxisShift + shiftValue;
      XYPlot plot = chart.getXYPlot();
      shiftChanged(plot, shiftValue, command);
    }

    if (command.equals("CHANGE_CHARGE_UP_X")) {
      xAxisCharge = xAxisCharge + 1;
      XYPlot plot = chart.getXYPlot();
      chargeOrDivisorChanged(plot);
    }

    if (command.equals("CHANGE_CHARGE_DOWN_X")) {
      if (xAxisCharge > 1) {
        xAxisCharge = xAxisCharge - 1;
      } else
        xAxisCharge = 1;
      XYPlot plot = chart.getXYPlot();
      chargeOrDivisorChanged(plot);
    }

    if (command.equals("CHANGE_DIVISOR_UP_X")) {
      int minDivisor = getMinimumRecommendedDivisor(customXAxisKMBase);
      int maxDivisor = getMaximumRecommendedDivisor(customXAxisKMBase);
      if (xAxisDivisor == 1) {
        xAxisDivisor = minDivisor;
      } else if (xAxisDivisor >= minDivisor && xAxisDivisor < maxDivisor) {
        xAxisDivisor++;
      }
      XYPlot plot = chart.getXYPlot();
      chargeOrDivisorChanged(plot);
    }

    if (command.equals("CHANGE_DIVISOR_DOWN_X")) {
      int minDivisor = getMinimumRecommendedDivisor(customXAxisKMBase);
      int maxDivisor = getMaximumRecommendedDivisor(customXAxisKMBase);
      if (xAxisDivisor > minDivisor && xAxisDivisor <= maxDivisor) {
        xAxisDivisor = xAxisDivisor - 1;
      } else if (xAxisDivisor == minDivisor) {
        xAxisDivisor = 1;
      }
      XYPlot plot = chart.getXYPlot();
      chargeOrDivisorChanged(plot);
    }

    // z axis commands
    if (command.equals("SHIFT_KMD_UP_Z")) {

      Double shiftValue = 0.01;
      zAxisShift = zAxisShift + shiftValue;
      XYPlot plot = chart.getXYPlot();
      shiftChanged(plot, shiftValue, command);
    }

    if (command.equals("SHIFT_KMD_DOWN_Z")) {

      Double shiftValue = -0.01;
      zAxisShift = zAxisShift + shiftValue;
      XYPlot plot = chart.getXYPlot();
      shiftChanged(plot, shiftValue, command);
    }

    if (command.equals("CHANGE_CHARGE_UP_Z")) {
      zAxisCharge = zAxisCharge + 1;
      XYPlot plot = chart.getXYPlot();
      chargeOrDivisorChanged(plot);
    }

    if (command.equals("CHANGE_CHARGE_DOWN_Z")) {
      if (zAxisCharge > 1) {
        zAxisCharge = zAxisCharge - 1;
      } else
        zAxisCharge = 1;
      XYPlot plot = chart.getXYPlot();
      chargeOrDivisorChanged(plot);
    }

    if (command.equals("CHANGE_DIVISOR_UP_Z")) {

      int minDivisor = getMinimumRecommendedDivisor(customZAxisKMBase);
      int maxDivisor = getMaximumRecommendedDivisor(customZAxisKMBase);
      if (zAxisDivisor == 1) {
        zAxisDivisor = minDivisor;
      } else if (zAxisDivisor >= minDivisor && zAxisDivisor < maxDivisor) {
        zAxisDivisor++;
      }
      XYPlot plot = chart.getXYPlot();
      chargeOrDivisorChanged(plot);
    }

    if (command.equals("CHANGE_DIVISOR_DOWN_Z")) {
      int minDivisor = getMinimumRecommendedDivisor(customZAxisKMBase);
      int maxDivisor = getMaximumRecommendedDivisor(customZAxisKMBase);
      if (zAxisDivisor > minDivisor && zAxisDivisor <= maxDivisor) {
        zAxisDivisor = zAxisDivisor - 1;
      } else if (zAxisDivisor == minDivisor) {
        zAxisDivisor = 1;
      }
      XYPlot plot = chart.getXYPlot();
      chargeOrDivisorChanged(plot);
    }
  }

  private void shiftChanged(XYPlot plot, double shiftValue, String command) {
    if (plot.getDataset() instanceof KendrickMassPlotXYDataset) {
      KendrickMassPlotXYDataset dataset = (KendrickMassPlotXYDataset) plot.getDataset();
      if (command.contains("_Y")) {
        double[] yValues = new double[dataset.getItemCount(0)];
        for (int i = 0; i < yValues.length; i++) {
          yValues[i] = dataset.getYValue(0, i) + shiftValue
              - Math.floor(dataset.getYValue(0, i) + shiftValue);
        }
        dataset.setyValues(yValues);
      } else if (command.contains("_X")) {
        double[] xValues = new double[dataset.getItemCount(0)];
        for (int i = 0; i < xValues.length; i++) {
          xValues[i] = dataset.getXValue(0, i) + shiftValue
              - Math.floor(dataset.getXValue(0, i) + shiftValue);
        }
        dataset.setxValues(xValues);
      }
    } else if (plot.getDataset() instanceof KendrickMassPlotXYZDataset) {
      KendrickMassPlotXYZDataset dataset = (KendrickMassPlotXYZDataset) plot.getDataset();
      if (command.contains("_Y")) {
        double[] yValues = new double[dataset.getItemCount(0)];
        for (int i = 0; i < yValues.length; i++) {
          yValues[i] = dataset.getYValue(0, i) + shiftValue
              - Math.floor(dataset.getYValue(0, i) + shiftValue);
        }
        dataset.setyValues(yValues);
      } else if (command.contains("_X")) {
        double[] xValues = new double[dataset.getItemCount(0)];
        for (int i = 0; i < xValues.length; i++) {
          xValues[i] = dataset.getXValue(0, i) + shiftValue
              - Math.floor(dataset.getXValue(0, i) + shiftValue);
        }
        dataset.setxValues(xValues);
      } else if (command.contains("_Z")) {
        double[] zValues = new double[dataset.getItemCount(0)];
        for (int i = 0; i < zValues.length; i++) {
          zValues[i] = dataset.getZValue(0, i) + shiftValue
              - Math.floor(dataset.getZValue(0, i) + shiftValue);
        }
        dataset.setzValues(zValues);
      }
    }
    chart.fireChartChanged();
    validate();

    // update toolbar
    this.remove(toolBar);
    toolBar = new KendrickMassPlotToolBar(this, xAxisShift, yAxisShift, zAxisShift, xAxisCharge,
        yAxisCharge, zAxisCharge, xAxisDivisor, yAxisDivisor, zAxisDivisor, useCustomXAxisKMBase,
        useCustomZAxisKMBase);
    this.add(toolBar, BorderLayout.EAST);
    this.revalidate();
  }

  private void chargeOrDivisorChanged(XYPlot plot) {

    if (plot.getDataset() instanceof KendrickMassPlotXYDataset) {
      KendrickMassPlotXYDataset dataset = (KendrickMassPlotXYDataset) plot.getDataset();
      double[] xValues = new double[dataset.getItemCount(0)];

      // Calc xValues
      xValues = new double[selectedRows.length];
      if (useCustomXAxisKMBase == true) {
        for (int i = 0; i < selectedRows.length; i++) {
          xValues[i] = Math
              .ceil(xAxisCharge * selectedRows[i].getAverageMZ()
                  * getKendrickMassFactor(customXAxisKMBase, xAxisDivisor))
              - xAxisCharge * selectedRows[i].getAverageMZ()
                  * getKendrickMassFactor(customXAxisKMBase, xAxisDivisor);
        }
      } else {
        for (int i = 0; i < selectedRows.length; i++) {

          // simply plot m/z values as x axis
          if (xAxisKMBase.equals("m/z")) {
            xValues[i] = selectedRows[i].getAverageMZ();
          }

          // plot Kendrick masses as x axis
          else if (xAxisKMBase.equals("KM")) {
            xValues[i] = selectedRows[i].getAverageMZ()
                * getKendrickMassFactor(customYAxisKMBase, yAxisDivisor);
          }
        }
      }

      // Calc yValues
      double[] yValues = new double[selectedRows.length];
      for (int i = 0; i < selectedRows.length; i++) {
        yValues[i] = Math
            .ceil(yAxisCharge * (selectedRows[i].getAverageMZ())
                * getKendrickMassFactor(customYAxisKMBase, yAxisDivisor))
            - yAxisCharge * (selectedRows[i].getAverageMZ())
                * getKendrickMassFactor(customYAxisKMBase, yAxisDivisor);
      }
      dataset.setyValues(yValues);
      dataset.setxValues(xValues);
      chart.fireChartChanged();
      validate();
    } else if (plot.getDataset() instanceof KendrickMassPlotXYZDataset) {
      KendrickMassPlotXYZDataset dataset = (KendrickMassPlotXYZDataset) plot.getDataset();
      double[] xValues = new double[dataset.getItemCount(0)];

      // Calc xValues
      xValues = new double[selectedRows.length];
      if (useCustomXAxisKMBase == true) {
        for (int i = 0; i < selectedRows.length; i++) {
          xValues[i] = Math
              .ceil(xAxisCharge * selectedRows[i].getAverageMZ()
                  * getKendrickMassFactor(customXAxisKMBase, xAxisDivisor))
              - xAxisCharge * selectedRows[i].getAverageMZ()
                  * getKendrickMassFactor(customXAxisKMBase, xAxisDivisor);
        }
      } else {
        for (int i = 0; i < selectedRows.length; i++) {

          // simply plot m/z values as x axis
          if (xAxisKMBase.equals("m/z")) {
            xValues[i] = selectedRows[i].getAverageMZ();
          }

          // plot Kendrick masses as x axis
          else if (xAxisKMBase.equals("KM")) {
            xValues[i] = selectedRows[i].getAverageMZ()
                * getKendrickMassFactor(customYAxisKMBase, yAxisDivisor);
          }
        }
      }

      // Calc yValues
      double[] yValues = new double[selectedRows.length];
      for (int i = 0; i < selectedRows.length; i++) {
        yValues[i] = Math
            .ceil(yAxisCharge * (selectedRows[i].getAverageMZ())
                * getKendrickMassFactor(customYAxisKMBase, yAxisDivisor))
            - yAxisCharge * (selectedRows[i].getAverageMZ())
                * getKendrickMassFactor(customYAxisKMBase, yAxisDivisor);
      }

      // Calc zValues
      double[] zValues = new double[selectedRows.length];
      if (useCustomZAxisKMBase == true) {
        for (int i = 0; i < selectedRows.length; i++) {
          zValues[i] = Math
              .ceil(zAxisCharge * (selectedRows[i].getAverageMZ())
                  * getKendrickMassFactor(customZAxisKMBase, zAxisDivisor))
              - zAxisCharge * (selectedRows[i].getAverageMZ())
                  * getKendrickMassFactor(customZAxisKMBase, zAxisDivisor);
        }
      } else
        for (int i = 0; i < selectedRows.length; i++) {

          // plot selected feature characteristic as z Axis
          if (zAxisKMBase.equals("Retention time")) {
            zValues[i] = selectedRows[i].getAverageRT();
          } else if (zAxisKMBase.equals("Intensity")) {
            zValues[i] = selectedRows[i].getAverageHeight();
          } else if (zAxisKMBase.equals("Area")) {
            zValues[i] = selectedRows[i].getAverageArea();
          } else if (zAxisKMBase.equals("Tailing factor")) {
            zValues[i] = selectedRows[i].getBestPeak().getTailingFactor();
          } else if (zAxisKMBase.equals("Asymmetry factor")) {
            zValues[i] = selectedRows[i].getBestPeak().getAsymmetryFactor();
          } else if (zAxisKMBase.equals("FWHM")) {
            zValues[i] = selectedRows[i].getBestPeak().getFWHM();
          } else if (zAxisKMBase.equals("m/z")) {
            zValues[i] = selectedRows[i].getBestPeak().getMZ();
          }
        }
      dataset.setyValues(yValues);
      dataset.setxValues(xValues);
      dataset.setzValues(zValues);
      chart.fireChartChanged();
      validate();
    }

    // update toolbar
    this.remove(toolBar);
    toolBar = new KendrickMassPlotToolBar(this, xAxisShift, yAxisShift, zAxisShift, xAxisCharge,
        yAxisCharge, zAxisCharge, xAxisDivisor, yAxisDivisor, zAxisDivisor, useCustomXAxisKMBase,
        useCustomZAxisKMBase);
    this.add(toolBar, BorderLayout.EAST);
    this.revalidate();
  }

  /*
   * Method to calculate the Kendrick mass factor for a give sum formula
   */
  private double getKendrickMassFactor(String formula, int divisor) {
    double exactMassFormula = FormulaUtils.calculateExactMass(formula);
    return ((int) ((exactMassFormula / divisor) + 0.5d)) / (exactMassFormula / divisor);
  }

  /*
   * Method to calculate the recommended minimum of a divisor for Kendrick mass defect analysis
   */
  private int getMinimumRecommendedDivisor(String formula) {
    double exactMass = FormulaUtils.calculateExactMass(formula);
    return (int) Math.round((2.0 / 3.0) * exactMass);
  }

  /*
   * Method to calculate the recommended maximum of a divisor for Kendrick mass defect analysis
   */
  private int getMaximumRecommendedDivisor(String formula) {
    double exactMass = FormulaUtils.calculateExactMass(formula);
    return (int) Math.round(2.0 * exactMass);
  }

}
