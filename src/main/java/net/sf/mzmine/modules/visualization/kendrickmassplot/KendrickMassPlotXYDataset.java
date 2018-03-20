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

package net.sf.mzmine.modules.visualization.kendrickmassplot;

import org.jfree.data.xy.AbstractXYDataset;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.FormulaUtils;

/**
 * XYDataset for Kendrick mass plots
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
class KendrickMassPlotXYDataset extends AbstractXYDataset {

  private static final long serialVersionUID = 1L;

  private PeakListRow selectedRows[];
  private String yAxisKMBase;
  private String xAxisKMBase;
  private String customYAxisKMBase;
  private String customXAxisKMBase;
  private double xAxisKMFactor = -1;
  private double yAxisKMFactor = -1;
  private double[] xValues;
  private double[] yValues;

  public KendrickMassPlotXYDataset(ParameterSet parameters) {

    PeakList peakList = parameters.getParameter(KendrickMassPlotParameters.peakList).getValue()
        .getMatchingPeakLists()[0];

    this.selectedRows =
        parameters.getParameter(KendrickMassPlotParameters.selectedRows).getMatchingRows(peakList);

    if (parameters.getParameter(KendrickMassPlotParameters.yAxisCustomKendrickMassBase)
        .getValue() == true) {
      this.customYAxisKMBase =
          parameters.getParameter(KendrickMassPlotParameters.yAxisCustomKendrickMassBase)
              .getEmbeddedParameter().getValue();
    } else {
      this.yAxisKMBase = parameters.getParameter(KendrickMassPlotParameters.yAxisValues).getValue();
    }

    if (parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase)
        .getValue() == true) {
      this.customXAxisKMBase =
          parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase)
              .getEmbeddedParameter().getValue();
    } else {
      this.xAxisKMBase = parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue();
    }

    System.out.println(
        parameters.getParameter(KendrickMassPlotParameters.yAxisCustomKendrickMassBase).getValue()
            + " XAxis: " + customXAxisKMBase + " YAxis: " + customYAxisKMBase);

    // Calc xValues
    xValues = new double[selectedRows.length];
    if (parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase)
        .getValue() == true) {
      for (int i = 0; i < selectedRows.length; i++) {
        xValues[i] =
            ((int) (selectedRows[i].getAverageMZ() * getKendrickMassFactor(customXAxisKMBase)) + 1)
                - selectedRows[i].getAverageMZ() * getKendrickMassFactor(customXAxisKMBase);
      }
    } else {
      for (int i = 0; i < selectedRows.length; i++) {
        // simply plot m/z values as x axis
        if (xAxisKMBase.equals("m/z")) {
          xValues[i] = selectedRows[i].getAverageMZ();
        }
        // plot Kendrick masses as x axis
        else if (xAxisKMBase.equals("KM")) {
          xValues[i] = selectedRows[i].getAverageMZ() * getxAxisKMFactor(xAxisKMBase);
        }
        // plot Kendrick mass defect (KMD) as x Axis to the base of H
        else if (xAxisKMBase.equals("KMD (H)")) {
          xValues[i] = ((int) (selectedRows[i].getAverageMZ() * getxAxisKMFactor(xAxisKMBase)) + 1)
              - selectedRows[i].getAverageMZ() * getxAxisKMFactor(xAxisKMBase);
        }
        // plot Kendrick mass defect (KMD) as x Axis to the base of CH2
        else if (xAxisKMBase.equals("KMD (CH2)")) {
          xValues[i] = ((int) (selectedRows[i].getAverageMZ() * getxAxisKMFactor(xAxisKMBase)) + 1)
              - selectedRows[i].getAverageMZ() * getxAxisKMFactor(xAxisKMBase);
        }
        // plot Kendrick mass defect (KMD) as x Axis to the base of O
        else if (xAxisKMBase.equals("KMD (O)")) {
          xValues[i] = ((int) (selectedRows[i].getAverageMZ() * getxAxisKMFactor(xAxisKMBase)) + 1)
              - selectedRows[i].getAverageMZ() * getxAxisKMFactor(xAxisKMBase);
        }
      }
    }

    // Calc yValues
    yValues = new double[selectedRows.length];
    if (parameters.getParameter(KendrickMassPlotParameters.yAxisCustomKendrickMassBase)
        .getValue() == true) {
      for (int i = 0; i < selectedRows.length; i++) {
        yValues[i] =
            ((int) (selectedRows[i].getAverageMZ() * getKendrickMassFactor(customYAxisKMBase)) + 1)
                - selectedRows[i].getAverageMZ() * getKendrickMassFactor(customYAxisKMBase);
      }
    } else {
      for (int i = 0; i < selectedRows.length; i++) {
        // plot Kendrick mass defect (KMD) as y Axis to the base of H
        if (yAxisKMBase.equals("KMD (H)")) {
          yValues[i] = ((int) (selectedRows[i].getAverageMZ() * getyAxisKMFactor(yAxisKMBase)) + 1)
              - selectedRows[i].getAverageMZ() * getyAxisKMFactor(yAxisKMBase);
        }
        // plot Kendrick mass defect (KMD) as y Axis to the base of CH2
        else if (yAxisKMBase.equals("KMD (CH2)")) {
          yValues[i] = ((int) (selectedRows[i].getAverageMZ() * getyAxisKMFactor(yAxisKMBase)) + 1)
              - selectedRows[i].getAverageMZ() * getyAxisKMFactor(yAxisKMBase);
        }
        // plot Kendrick mass defect (KMD) as y Axis to the base of O
        else if (yAxisKMBase.equals("KMD (O)")) {
          yValues[i] = ((int) (selectedRows[i].getAverageMZ() * getyAxisKMFactor(yAxisKMBase)) + 1)
              - selectedRows[i].getAverageMZ() * getyAxisKMFactor(yAxisKMBase);
        }
      }
    }
  }

  // Calculate xAxis Kendrick mass factor (KM factor)
  private double getxAxisKMFactor(String xAxisKMBase) {
    if (xAxisKMFactor == -1) {
      if (xAxisKMBase.equals("KMD (CH2)")) {
        xAxisKMFactor = (14 / 14.01565006);
      } else if (xAxisKMBase.equals("KMD (H)")) {
        xAxisKMFactor = (1 / 1.007825037);
      } else if (xAxisKMBase.equals("KMD (O)")) {
        xAxisKMFactor = (16 / 15.994915);
      } else {
        xAxisKMFactor = 0;
      }
    }
    return xAxisKMFactor;
  }

  // Calculate yAxis Kendrick mass factor (KM factor)
  private double getyAxisKMFactor(String yAxisKMBase) {
    if (yAxisKMFactor == -1) {
      if (yAxisKMBase.equals("KMD (CH2)")) {
        yAxisKMFactor = (14 / 14.01565006);
      } else if (yAxisKMBase.equals("KMD (H)")) {
        yAxisKMFactor = (1 / 1.007825037);
      } else if (yAxisKMBase.equals("KMD (O)")) {
        yAxisKMFactor = (16 / 15.994915);
      } else {
        yAxisKMFactor = 0;
      }
    }
    return yAxisKMFactor;
  }

  @Override
  public int getItemCount(int series) {
    return selectedRows.length;
  }

  @Override
  public Number getX(int series, int item) {
    return xValues[item];
  }

  @Override
  public Number getY(int series, int item) {
    return yValues[item];
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  public Comparable<?> getRowKey(int row) {
    return selectedRows[row].toString();
  }

  @Override
  public Comparable<?> getSeriesKey(int series) {
    return getRowKey(series);
  }

  public double[] getxValues() {
    return xValues;
  }

  public double[] getyValues() {
    return yValues;
  }

  private double getKendrickMassFactor(String formula) {
    double exactMassFormula = FormulaUtils.calculateExactMass(formula);
    double kendrickMassFactor = ((int) (exactMassFormula + 0.5d)) / exactMassFormula;
    System.out.println(
        "Formula: " + formula + " " + "exactMassFormula: " + exactMassFormula + "Kendrick mass "
            + ((int) (exactMassFormula + 0.5d) + "\n" + "KMD-Factor: " + kendrickMassFactor));
    return kendrickMassFactor;
  }
}
