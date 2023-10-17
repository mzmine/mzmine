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
import java.util.List;
import org.jfree.data.xy.AbstractXYZDataset;

/**
 * XYZDataset for Kendrick mass plots
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class KendrickMassPlotXYZDataset extends AbstractXYZDataset {

  private final FeatureListRow[] selectedRows;
  private double[] xValues;
  private double[] yValues;
  private double[] colorScaleValues;
  private double[] bubbleSizeValues;
  private KendrickPlotDataTypes xKendrickDataType;
  private KendrickPlotDataTypes yKendrickDataType;
  private KendrickPlotDataTypes colorKendrickDataType;
  private KendrickPlotDataTypes bubbleKendrickDataType;
  private ParameterSet parameters;
  private Integer xDivisor;
  private int xCharge;
  private Integer yDivisor;
  private int yCharge;

  public KendrickMassPlotXYZDataset(ParameterSet parameters, int xCharge,
      int yCharge) {
    FeatureList featureList = parameters.getParameter(KendrickMassPlotParameters.featureList)
        .getValue().getMatchingFeatureLists()[0];
    this.parameters = parameters.cloneParameterSet();
    this.selectedRows = featureList.getRows().toArray(new FeatureListRow[0]);
    this.xCharge = xCharge;
    this.yCharge = yCharge;
    xValues = new double[selectedRows.length];
    yValues = new double[selectedRows.length];
    colorScaleValues = new double[selectedRows.length];
    bubbleSizeValues = new double[selectedRows.length];
    init();
  }

  public KendrickMassPlotXYZDataset(ParameterSet parameters, int xDivisor, int xCharge,
      int yDivisor, int yCharge) {
    FeatureList featureList = parameters.getParameter(KendrickMassPlotParameters.featureList)
        .getValue().getMatchingFeatureLists()[0];
    this.parameters = parameters.cloneParameterSet();
    this.selectedRows = featureList.getRows().toArray(new FeatureListRow[0]);
    this.xDivisor = xDivisor;
    this.xCharge = xCharge;
    this.yDivisor = yDivisor;
    this.yCharge = yCharge;
    xValues = new double[selectedRows.length];
    yValues = new double[selectedRows.length];
    colorScaleValues = new double[selectedRows.length];
    bubbleSizeValues = new double[selectedRows.length];
    init();
  }

  public KendrickMassPlotXYZDataset(ParameterSet parameters, List<FeatureListRow> rows) {
    this.parameters = parameters.cloneParameterSet();
    this.selectedRows = rows.toArray(new FeatureListRow[0]);

    xValues = new double[selectedRows.length];
    yValues = new double[selectedRows.length];
    colorScaleValues = new double[selectedRows.length];
    bubbleSizeValues = new double[selectedRows.length];
    init();
  }

  private void init() {
    xKendrickDataType = parameters.getParameter(KendrickMassPlotParameters.xAxisValues)
        .getValue();
    if (xDivisor == null && xKendrickDataType.isKendrickType()) {
      this.xDivisor = calculateDivisorKM(
          parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase)
              .getValue());
      if (xKendrickDataType.equals(KendrickPlotDataTypes.REMAINDER_OF_KENDRICK_MASS)) {
        xDivisor++;
      }
    } else if (xDivisor == null) {
      xDivisor = 1;
    }
    initDimensionValues(xValues,
        parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase).getValue(),
        xKendrickDataType, xDivisor, xCharge);
    yKendrickDataType = parameters.getParameter(
        KendrickMassPlotParameters.yAxisValues).getValue();
    if (yDivisor == null && yKendrickDataType.isKendrickType()) {
      this.yDivisor = calculateDivisorKM(
          parameters.getParameter(KendrickMassPlotParameters.yAxisCustomKendrickMassBase)
              .getValue());
      if (yKendrickDataType.equals(KendrickPlotDataTypes.REMAINDER_OF_KENDRICK_MASS)) {
        yDivisor++;
      }
    } else if (yDivisor == null) {
      yDivisor = 1;
    }
    initDimensionValues(yValues,
        parameters.getParameter(KendrickMassPlotParameters.yAxisCustomKendrickMassBase).getValue(),
        yKendrickDataType, yDivisor, yCharge);
    colorKendrickDataType = parameters.getParameter(
        KendrickMassPlotParameters.colorScaleValues).getValue();
    initDimensionValues(colorScaleValues,
        parameters.getParameter(KendrickMassPlotParameters.colorScaleCustomKendrickMassBase)
            .getValue(),
        colorKendrickDataType, 1, 1);
    bubbleKendrickDataType = parameters.getParameter(
        KendrickMassPlotParameters.bubbleSizeValues).getValue();
    initDimensionValues(bubbleSizeValues,
        parameters.getParameter(KendrickMassPlotParameters.bubbleSizeCustomKendrickMassBase)
            .getValue(),
        bubbleKendrickDataType, 1, 1);
  }

  private void initDimensionValues(double[] values, String kendrickMassBase,
      KendrickPlotDataTypes kendrickPlotDataType, int divisor, int charge) {
    boolean isKendrickType = kendrickPlotDataType.isKendrickType();
    if (isKendrickType) {
      switch (kendrickPlotDataType) {
        case KENDRICK_MASS -> calculateKMs(values, kendrickMassBase, divisor, charge);
        case KENDRICK_MASS_DEFECT -> calculateKMDs(values, kendrickMassBase, divisor, charge);
        case REMAINDER_OF_KENDRICK_MASS -> calculateRKMs(values, kendrickMassBase, divisor, charge);
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

  private void calculateKMDs(double[] values, String kendrickMassBase, int divisor, int charge) {
    for (int i = 0; i < selectedRows.length; i++) {
      values[i] = calculateKendrickMassDefectChargeAndDivisorDependent(
          selectedRows[i].getAverageMZ(), kendrickMassBase, charge, divisor);
    }
  }

  private void calculateRKMs(double[] values, String kendrickMassBase, int divisor, int charge) {
    for (int i = 0; i < selectedRows.length; i++) {
      values[i] = calculateRemainderOfKendrickMassChargeAndDivisorDependent(
          selectedRows[i].getAverageMZ(), kendrickMassBase, charge, divisor);
    }
  }

  private void calculateKMs(double[] values, String kendrickMassBase, int divisor, int charge) {
    for (int i = 0; i < selectedRows.length; i++) {
      values[i] = calculateKendrickMassChargeAndDivisorDependent(selectedRows[i].getAverageMZ(),
          kendrickMassBase, charge, divisor);
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
    return xValues.length;
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

  public void setBubbleSize(int item, double newValue) {
    bubbleSizeValues[item] = newValue;
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

  public void setBubbleSizeValues(double[] values) {
    bubbleSizeValues = values;
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

  public KendrickPlotDataTypes getxKendrickDataType() {
    return xKendrickDataType;
  }

  public KendrickPlotDataTypes getyKendrickDataType() {
    return yKendrickDataType;
  }

  public KendrickPlotDataTypes getColorKendrickDataType() {
    return colorKendrickDataType;
  }

  public KendrickPlotDataTypes getBubbleKendrickDataType() {
    return bubbleKendrickDataType;
  }

  public int getxDivisor() {
    return xDivisor;
  }

  public int getxCharge() {
    return xCharge;
  }

  public int getyDivisor() {
    return yDivisor;
  }

  public int getyCharge() {
    return yCharge;
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

  private double calculateKendrickMassFactorDivisorDependent(String kendrickMassBase, int divisor) {
    double exactMassFormula = FormulaUtils.calculateExactMass(kendrickMassBase);
    return Math.round(exactMassFormula / divisor) / (exactMassFormula / divisor);
  }

  private double calculateKendrickMassChargeAndDivisorDependent(double mz, String kendrickMassBase,
      int charge, int divisor) {
    return charge * mz * calculateKendrickMassFactorDivisorDependent(kendrickMassBase, divisor);
  }

  private double calculateKendrickMassDefectChargeAndDivisorDependent(double mz,
      String kendrickMassBase, int charge, int divisor) {
    double kendrickMassChargeAndDivisorDependent = calculateKendrickMassChargeAndDivisorDependent(
        mz, kendrickMassBase, charge,
        divisor);
    return Math.round(
        kendrickMassChargeAndDivisorDependent)
        - kendrickMassChargeAndDivisorDependent;
  }

  private double calculateRemainderOfKendrickMassChargeAndDivisorDependent(double mz,
      String kendrickMassBase, int charge, int divisor) {
    double repeatingUnitMass = FormulaUtils.calculateExactMass(kendrickMassBase);
    double fractionalUnit =
        (charge * (divisor - Math.round(repeatingUnitMass)) * mz) / repeatingUnitMass;
    return fractionalUnit - Math.round(fractionalUnit);
  }

  /*
   * Method to calculate the divisor for Kendrick mass defect analysis
   */
  private int calculateDivisorKM(String formula) {
    double exactMass = FormulaUtils.calculateExactMass(formula);
    return (int) Math.round(exactMass);
  }

}
