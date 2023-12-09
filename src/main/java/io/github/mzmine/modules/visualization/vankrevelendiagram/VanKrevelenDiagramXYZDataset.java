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

package io.github.mzmine.modules.visualization.vankrevelendiagram;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotation;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.XYZBubbleDataset;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.FormulaUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jfree.data.xy.AbstractXYZDataset;
import org.openscience.cdk.config.Elements;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/*
 * XYZDataset for Van Krevelen diagram
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
class VanKrevelenDiagramXYZDataset extends AbstractXYZDataset implements XYZBubbleDataset {

  private final ParameterSet parameters;
  private final List<FeatureListRow> filteredRows;
  private final double[] xValues;
  private final double[] yValues;
  private final double[] colorScaleValues;
  private final double[] bubbleSizeValues;
  private VanKrevelenDiagramDataTypes bubbleVanKrevelenDataType;


  public VanKrevelenDiagramXYZDataset(ParameterSet parameters) {
    FeatureList featureList = parameters.getParameter(VanKrevelenDiagramParameters.featureList)
        .getValue().getMatchingFeatureLists()[0];
    this.parameters = parameters.cloneParameterSet();
    FeatureListRow[] selectedRows = featureList.getRows().toArray(new FeatureListRow[0]);
    filteredRows = Arrays.stream(selectedRows)
        .filter(featureListRow -> featureListRow.getPreferredAnnotation() != null).toList();
    xValues = new double[filteredRows.size()];
    yValues = new double[filteredRows.size()];
    colorScaleValues = new double[filteredRows.size()];
    bubbleSizeValues = new double[filteredRows.size()];
    init();
  }

  private void init() {
    initElementRatioValues(Elements.ofString("O").toIElement(), Elements.ofString("C").toIElement(),
        xValues);
    initElementRatioValues(Elements.ofString("H").toIElement(), Elements.ofString("C").toIElement(),
        yValues);
    initDimensionValues(colorScaleValues,
        parameters.getParameter(VanKrevelenDiagramParameters.colorScaleValues).getValue());
    initDimensionValues(bubbleSizeValues,
        parameters.getParameter(VanKrevelenDiagramParameters.bubbleSizeValues).getValue());
    bubbleVanKrevelenDataType = parameters.getParameter(
        VanKrevelenDiagramParameters.bubbleSizeValues).getValue();
  }

  private void initElementRatioValues(IElement elementOne, IElement elementTwo, double[] values) {
    List<Object> annotations = filteredRows.stream().map(FeatureListRow::getPreferredAnnotation)
        .toList();
    for (int i = 0; i < annotations.size(); i++) {
      switch (annotations.get(i)) {
        case MatchedLipid lipid -> {
          int elementOneCount = MolecularFormulaManipulator.getElementCount(
              lipid.getLipidAnnotation().getMolecularFormula(), elementOne);
          int elementTwoCount = MolecularFormulaManipulator.getElementCount(
              lipid.getLipidAnnotation().getMolecularFormula(), elementTwo);
          if (elementOneCount > 0 && elementTwoCount > 0) {
            values[i] = (double) elementOneCount / elementTwoCount;
          } else {
            values[i] = 0.0;
          }
        }
        case FeatureAnnotation ann -> {
          int elementOneCount = MolecularFormulaManipulator.getElementCount(
              Objects.requireNonNull(FormulaUtils.createMajorIsotopeMolFormula(ann.getFormula())),
              elementOne);
          int elementTwoCount = MolecularFormulaManipulator.getElementCount(
              Objects.requireNonNull(FormulaUtils.createMajorIsotopeMolFormula(ann.getFormula())),
              elementTwo);
          if (elementOneCount > 0 && elementTwoCount > 0) {
            values[i] = (double) elementOneCount / elementTwoCount;
          } else {
            values[i] = 0.0;
          }
        }
        case ManualAnnotation ann -> {
          int elementOneCount = MolecularFormulaManipulator.getElementCount(
              Objects.requireNonNull(FormulaUtils.createMajorIsotopeMolFormula(ann.getFormula())),
              elementOne);
          int elementTwoCount = MolecularFormulaManipulator.getElementCount(
              Objects.requireNonNull(FormulaUtils.createMajorIsotopeMolFormula(ann.getFormula())),
              elementTwo);
          if (elementOneCount > 0 && elementTwoCount > 0) {
            values[i] = (double) elementOneCount / elementTwoCount;
          } else {
            values[i] = 0.0;
          }
        }
        case MolecularFormulaIdentity ann -> {
          int elementOneCount = MolecularFormulaManipulator.getElementCount(Objects.requireNonNull(
              FormulaUtils.createMajorIsotopeMolFormula(ann.getFormulaAsString())), elementOne);
          int elementTwoCount = MolecularFormulaManipulator.getElementCount(Objects.requireNonNull(
              FormulaUtils.createMajorIsotopeMolFormula(ann.getFormulaAsString())), elementTwo);
          if (elementOneCount > 0 && elementTwoCount > 0) {
            values[i] = (double) elementOneCount / elementTwoCount;
          } else {
            values[i] = 0.0;
          }
        }
        default -> throw new IllegalStateException("Unexpected value: " + annotations.get(i));
      }
    }
  }

  private void initDimensionValues(double[] values,
      VanKrevelenDiagramDataTypes vanKrevelenDiagramDataType) {
    switch (vanKrevelenDiagramDataType) {
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

  private void useFwhm(double[] values) {
    for (int i = 0; i < filteredRows.size(); i++) {
      values[i] = filteredRows.get(i).getBestFeature().getFWHM();
    }
  }

  private void useAsymmetryFactor(double[] values) {
    for (int i = 0; i < filteredRows.size(); i++) {
      values[i] = filteredRows.get(i).getBestFeature().getAsymmetryFactor();
    }
  }

  private void useTailingFactor(double[] values) {
    for (int i = 0; i < filteredRows.size(); i++) {
      values[i] = filteredRows.get(i).getBestFeature().getTailingFactor();
    }
  }

  private void useArea(double[] values) {
    for (int i = 0; i < filteredRows.size(); i++) {
      values[i] = filteredRows.get(i).getAverageArea();
    }
  }

  private void useIntensity(double[] values) {
    for (int i = 0; i < filteredRows.size(); i++) {
      values[i] = filteredRows.get(i).getAverageHeight();
    }
  }

  private void useMobility(double[] values) {
    for (int i = 0; i < filteredRows.size(); i++) {
      if (filteredRows.get(i).getAverageMobility() != null) {
        values[i] = filteredRows.get(i).getAverageMobility();
      } else {
        values[i] = 0;
      }
    }
  }

  private void useRT(double[] values) {
    for (int i = 0; i < filteredRows.size(); i++) {
      values[i] = filteredRows.get(i).getAverageRT();
    }
  }

  private void useMZ(double[] values) {
    for (int i = 0; i < filteredRows.size(); i++) {
      values[i] = filteredRows.get(i).getAverageMZ();
    }
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

  @Override
  public Number getBubbleSize(int series, int item) {
    return bubbleSizeValues[item];
  }

  @Override
  public double getBubbleSizeValue(int series, int item) {
    return bubbleSizeValues[item];
  }


  @Override
  public int getSeriesCount() {
    return 1;
  }

  public Comparable<?> getRowKey(int row) {
    return filteredRows.get(row).toString();
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

  public double[] getColorScaleValues() {
    return colorScaleValues;
  }

  public double[] getBubbleSizeValues() {
    return bubbleSizeValues;
  }

  public VanKrevelenDiagramDataTypes getBubbleVanKrevelenDataType() {
    return bubbleVanKrevelenDataType;
  }
}
