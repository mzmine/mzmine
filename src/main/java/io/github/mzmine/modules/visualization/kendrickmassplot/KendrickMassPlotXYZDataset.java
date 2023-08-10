/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.kendrickmassplot;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.FormulaUtils;
import org.jfree.data.xy.AbstractXYZDataset;

/**
 * XYZDataset for Kendrick mass plots
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class KendrickMassPlotXYZDataset extends AbstractXYZDataset {

  private FeatureListRow[] selectedRows;
  private double[] xValues;
  private double[] yValues;
  private double[] colorScaleValues;
  private final double[] bubbleSizeValues;
  private ParameterSet parameters;

  public KendrickMassPlotXYZDataset(double[] xValues, double[] yValues, double[] colorScaleValues,
      double[] bubbleSizeValues) {
    super();
    this.xValues = xValues;
    this.yValues = yValues;
    this.colorScaleValues = colorScaleValues;
    this.bubbleSizeValues = bubbleSizeValues;
  }

  public KendrickMassPlotXYZDataset(ParameterSet parameters) {

    FeatureList featureList = parameters.getParameter(KendrickMassPlotParameters.featureList)
        .getValue().getMatchingFeatureLists()[0];

    this.parameters = parameters;

    this.selectedRows = featureList.getRows().toArray(new FeatureListRow[0]);

    xValues = new double[selectedRows.length];
    yValues = new double[selectedRows.length];
    colorScaleValues = new double[selectedRows.length];
    bubbleSizeValues = new double[selectedRows.length];
    initDimensionValues(xValues,
        parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase).getValue(),
        parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue());
    initDimensionValues(yValues,
        parameters.getParameter(KendrickMassPlotParameters.yAxisCustomKendrickMassBase).getValue(),
        parameters.getParameter(KendrickMassPlotParameters.yAxisValues).getValue());
    initDimensionValues(colorScaleValues,
        parameters.getParameter(KendrickMassPlotParameters.colorScaleCustomKendrickMassBase)
            .getValue(),
        parameters.getParameter(KendrickMassPlotParameters.colorScaleValues).getValue());
    initDimensionValues(bubbleSizeValues,
        parameters.getParameter(KendrickMassPlotParameters.bubbleSizeCustomKendrickMassBase)
            .getValue(),
        parameters.getParameter(KendrickMassPlotParameters.bubbleSizeValues).getValue());
  }

  private void initDimensionValues(double[] values, String kendrickMassBase,
      KendrickPlotDataTypes kendrickPlotDataType) {
    boolean isKendrickType = kendrickPlotDataType.isKendrickType();
    if (isKendrickType) {
      switch (kendrickPlotDataType) {
        case KENDRICK_MASS -> calculateKMs(values, kendrickMassBase);
        case KENDRICK_MASS_DEFECT -> calculateKMDs(values, kendrickMassBase);
      }
    } else {
      switch (kendrickPlotDataType) {
        case M_OVER_Z -> {
          useMZ(values);
        }
        case RETENTION_TIME -> {
          useRT(values);
        }
        case MOBILITY -> {
          useMobility(values);
        }
        case INTENSITY -> {
          useIntensity(values);
        }
        case AREA -> {
          useArea(values);
        }
        case TAILING_FACTOR -> {
          useTailingFactor(values);
        }
        case ASYMMETRY_FACTOR -> {
          useAsymmetryFactor(values);
        }
        case FWHM -> {
          useFwhm(values);
        }
      }
    }
  }

  private void useFwhm(double[] values) {
    for (int i = 0; i < selectedRows.length; i++) {
      values[i] = selectedRows[i].getBestFeature().getFWHM();
    }
  }

  private void useAsymmetryFactor(double[] values) {
    for (int i = 0; i < selectedRows.length; i++) {
      values[i] = selectedRows[i].getBestFeature().getAsymmetryFactor();
    }
  }

  private void useTailingFactor(double[] values) {
    for (int i = 0; i < selectedRows.length; i++) {
      values[i] = selectedRows[i].getBestFeature().getTailingFactor();
    }
  }

  private void useArea(double[] values) {
    for (int i = 0; i < selectedRows.length; i++) {
      values[i] = selectedRows[i].getAverageArea();
    }
  }

  private void useIntensity(double[] values) {
    for (int i = 0; i < selectedRows.length; i++) {
      values[i] = selectedRows[i].getAverageHeight();
    }
  }

  private void useMobility(double[] values) {
    for (int i = 0; i < selectedRows.length; i++) {
      if (selectedRows[i].getAverageMobility() != null) {
        values[i] = selectedRows[i].getAverageMobility();
      } else {
        values[i] = 0;
      }
    }
  }

  private void useRT(double[] values) {
    for (int i = 0; i < selectedRows.length; i++) {
      values[i] = selectedRows[i].getAverageRT();
    }
  }

  private void useMZ(double[] values) {
    for (int i = 0; i < selectedRows.length; i++) {
      values[i] = selectedRows[i].getAverageMZ();
    }
  }

  private void calculateKMDs(double[] values, String kendrickMassBase) {
    for (int i = 0; i < selectedRows.length; i++) {
      values[i] = calculateKendrickMassDefect(selectedRows[i].getAverageMZ(), kendrickMassBase);
    }
  }

  private void calculateKMs(double[] values, String kendrickMassBase) {
    for (int i = 0; i < selectedRows.length; i++) {
      values[i] = calculateKendrickMass(selectedRows[i].getAverageMZ(), kendrickMassBase);
    }
  }

  public ParameterSet getParameters() {
    return parameters;
  }

  public void setParameters(ParameterSet parameters) {
    this.parameters = parameters;
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
  public Number getZ(int series, int item) {
    return colorScaleValues[item];
  }

  public double getBubbleSize(int series, int item) {
    return bubbleSizeValues[item];
  }

  public void setxValues(double[] values) {
    xValues = values;
  }

  public void setyValues(double[] values) {
    yValues = values;
  }

  public void setColorScaleValues(double[] values) {
    colorScaleValues = values;
  }

  public double[] getxValues() {
    return xValues;
  }

  public double[] getyValues() {
    return yValues;
  }

  public double[] getColorScaleValues() {
    return colorScaleValues;
  }

  public double[] getBubbleSizeValues() {
    return bubbleSizeValues;
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

  private double getKendrickMassFactor(String formula) {
    double exactMassFormula = FormulaUtils.calculateExactMass(formula);
    return ((int) ((exactMassFormula) + 0.5d)) / (exactMassFormula);
  }

  private double calculateKendrickMass(double mz, String kendrickMassBase) {
    return mz * getKendrickMassFactor(kendrickMassBase);
  }

  private double calculateKendrickMassDefect(double mz, String kendrickMassBase) {
    return Math.ceil(mz * getKendrickMassFactor(kendrickMassBase)) - mz * getKendrickMassFactor(
        kendrickMassBase);
  }

}
