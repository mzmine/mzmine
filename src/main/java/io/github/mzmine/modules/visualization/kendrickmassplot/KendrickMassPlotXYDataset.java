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

import java.awt.Color;
import org.jfree.data.xy.AbstractXYDataset;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.FormulaUtils;

/**
 * XYDataset for Kendrick mass plots
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class KendrickMassPlotXYDataset extends AbstractXYDataset {

  private static final long serialVersionUID = 1L;

  private FeatureListRow[] selectedRows;
  private String xAxisKMBase;
  private String customYAxisKMBase;
  private String customXAxisKMBase;
  private String bubbleSizeLabel;
  private double[] xValues;
  private double[] yValues;
  private double[] bubbleSizeValues;
  private ParameterSet parameters;
  private String seriesKey;
  private int itemCount;
  private Color color;

  public KendrickMassPlotXYDataset(double[] xValues, double[] yValues, double[] bubbleSizeValues,
      String seriesKey, Color color) {
    this.xValues = xValues;
    this.yValues = yValues;
    this.bubbleSizeValues = bubbleSizeValues;
    this.seriesKey = seriesKey;
    this.color = color;
    itemCount = xValues.length;
  }


  public KendrickMassPlotXYDataset(ParameterSet parameters) {

    FeatureList featureList = parameters.getParameter(KendrickMassPlotParameters.featureList)
        .getValue().getMatchingFeatureLists()[0];

    this.seriesKey = "Kendrick plot";

    this.parameters = parameters;

    this.selectedRows = parameters.getParameter(KendrickMassPlotParameters.selectedRows)
        .getMatchingRows(featureList);

    this.customYAxisKMBase =
        parameters.getParameter(KendrickMassPlotParameters.yAxisCustomKendrickMassBase).getValue();

    this.bubbleSizeLabel =
        parameters.getParameter(KendrickMassPlotParameters.bubbleSize).getValue();

    if (parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase)
        .getValue() == true) {
      this.customXAxisKMBase =
          parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase)
              .getEmbeddedParameter().getValue();
    } else {
      this.xAxisKMBase = parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue();
    }

    itemCount = selectedRows.length;

    // Calc xValues
    xValues = new double[selectedRows.length];
    if (parameters.getParameter(KendrickMassPlotParameters.xAxisCustomKendrickMassBase)
        .getValue() == true) {
      for (int i = 0; i < selectedRows.length; i++) {
        xValues[i] =
            Math.ceil(selectedRows[i].getAverageMZ() * getKendrickMassFactor(customXAxisKMBase))
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
          xValues[i] = selectedRows[i].getAverageMZ() * getKendrickMassFactor(customYAxisKMBase);
        }
      }
    }

    // Calc yValues
    yValues = new double[selectedRows.length];
    for (int i = 0; i < selectedRows.length; i++) {
      yValues[i] =
          Math.ceil((selectedRows[i].getAverageMZ()) * getKendrickMassFactor(customYAxisKMBase))
              - (selectedRows[i].getAverageMZ()) * getKendrickMassFactor(customYAxisKMBase);
    }

    // Calc bubble size
    bubbleSizeValues = new double[selectedRows.length];
    for (int i = 0; i < selectedRows.length; i++) {
      if (bubbleSizeLabel.equals("Retention time")) {
        bubbleSizeValues[i] = selectedRows[i].getAverageRT();
      } else if (bubbleSizeLabel.equals("Intensity")) {
        bubbleSizeValues[i] = selectedRows[i].getAverageHeight();
      } else if (bubbleSizeLabel.equals("Area")) {
        bubbleSizeValues[i] = selectedRows[i].getAverageArea();
      } else if (bubbleSizeLabel.equals("Tailing factor")) {
        bubbleSizeValues[i] = selectedRows[i].getBestFeature().getTailingFactor();
      } else if (bubbleSizeLabel.equals("Asymmetry factor")) {
        bubbleSizeValues[i] = selectedRows[i].getBestFeature().getAsymmetryFactor();
      } else if (bubbleSizeLabel.equals("FWHM")) {
        bubbleSizeValues[i] = selectedRows[i].getBestFeature().getFWHM();
      } else if (bubbleSizeLabel.equals("m/z")) {
        bubbleSizeValues[i] = selectedRows[i].getBestFeature().getMZ();
      } else {
        bubbleSizeValues[i] = 5;
      }
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
    return itemCount;
  }

  @Override
  public Number getX(int series, int item) {
    return xValues[item];
  }

  @Override
  public Number getY(int series, int item) {
    return yValues[item];
  }

  public double getBubbleSize(int series, int item) {
    return bubbleSizeValues[item];
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Comparable<?> getSeriesKey(int series) {
    return seriesKey;
  }

  public double[] getxValues() {
    return xValues;
  }

  public double[] getyValues() {
    return yValues;
  }

  public double[] getBubbleSizeValues() {
    return bubbleSizeValues;
  }

  public void setBubbleSize(double[] bubbleSize) {
    this.bubbleSizeValues = bubbleSize;
  }

  public void setxValues(double[] values) {
    xValues = values;
  }

  public void setyValues(double[] values) {
    yValues = values;
  }

  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  private double getKendrickMassFactor(String formula) {
    double exactMassFormula = FormulaUtils.calculateExactMass(formula);
    return ((int) (exactMassFormula + 0.5d)) / exactMassFormula;
  }
}
