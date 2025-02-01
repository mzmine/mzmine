/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.XYZBubbleDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYItemObjectProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.data.xy.AbstractXYZDataset;

/**
 * XYZDataset for Kendrick mass plots
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class KendrickMassPlotXYZDataset extends AbstractXYZDataset implements Task,
    XYZBubbleDataset, XYItemObjectProvider<FeatureListRow> {
  // TODO replace with getTask method or AbstractTaskXYZDataset

  private static final Logger logger = Logger.getLogger(KendrickMassPlotXYZDataset.class.getName());
  protected final @NotNull Property<TaskStatus> status = new SimpleObjectProperty<>(
      TaskStatus.WAITING);
  protected String errorMessage = null;
  private List<TaskStatusListener> listener;
  private double finishedSteps;
  private final FeatureListRow[] selectedRows;
  private double[] xValues;
  private double[] yValues;
  private double[] colorScaleValues;
  private double[] bubbleSizeValues;
  private final boolean[] isAnnotated;
  private KendrickPlotDataTypes xKendrickDataType;
  private KendrickPlotDataTypes yKendrickDataType;
  private KendrickPlotDataTypes colorKendrickDataType;
  private KendrickPlotDataTypes bubbleKendrickDataType;
  private ParameterSet parameters;
  private Integer xDivisor;
  private int xCharge;
  private Integer yDivisor;
  private int yCharge;

  public KendrickMassPlotXYZDataset(ParameterSet parameters, int xCharge, int yCharge) {
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
    isAnnotated = new boolean[selectedRows.length];
    setStatus(TaskStatus.WAITING);
    MZmineCore.getTaskController().addTask(this);
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
    isAnnotated = new boolean[selectedRows.length];
    setStatus(TaskStatus.WAITING);
    MZmineCore.getTaskController().addTask(this);
  }

  @Override
  public void run() {
    finishedSteps = 0;
    setStatus(TaskStatus.PROCESSING);
    if (isCanceled()) {
      setStatus(TaskStatus.CANCELED);
    }
    xKendrickDataType = parameters.getParameter(KendrickMassPlotParameters.xAxisValues).getValue();
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
    yKendrickDataType = parameters.getParameter(KendrickMassPlotParameters.yAxisValues).getValue();
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
    colorKendrickDataType = parameters.getParameter(KendrickMassPlotParameters.colorScaleValues)
        .getValue();
    initDimensionValues(colorScaleValues,
        parameters.getParameter(KendrickMassPlotParameters.colorScaleCustomKendrickMassBase)
            .getValue(), colorKendrickDataType, 1, 1);
    bubbleKendrickDataType = parameters.getParameter(KendrickMassPlotParameters.bubbleSizeValues)
        .getValue();
    initDimensionValues(bubbleSizeValues,
        parameters.getParameter(KendrickMassPlotParameters.bubbleSizeCustomKendrickMassBase)
            .getValue(), bubbleKendrickDataType, 1, 1);
    for (int i = 0; i < selectedRows.length; i++) {
      isAnnotated[i] = selectedRows[i].isIdentified();
    }
    finishedSteps = 1;
    setStatus(TaskStatus.FINISHED);
  }

  private void initDimensionValues(double[] values, String kendrickMassBase,
      KendrickPlotDataTypes kendrickPlotDataType, int divisor, int charge) {
    for (int i = 0; i < selectedRows.length; i++) {
      FeatureListRow row = selectedRows[i];
      switch (kendrickPlotDataType) {
        case KENDRICK_MASS ->
            values[i] = calculateKendrickMassChargeAndDivisorDependent(row.getAverageMZ(),
                kendrickMassBase, charge, divisor);
        case KENDRICK_MASS_DEFECT ->
            values[i] = calculateKendrickMassDefectChargeAndDivisorDependent(row.getAverageMZ(),
                kendrickMassBase, charge, divisor);
        case REMAINDER_OF_KENDRICK_MASS ->
            values[i] = calculateRemainderOfKendrickMassChargeAndDivisorDependent(
                row.getAverageMZ(), kendrickMassBase, charge, divisor);
        case MZ -> values[i] = row.getAverageMZ();
        case RETENTION_TIME -> values[i] = row.getAverageRT();
        case MOBILITY ->
            values[i] = (row.getAverageMobility() != null) ? row.getAverageMobility() : 0.0;
        case INTENSITY -> values[i] = row.getMaxHeight();
        case AREA -> values[i] = row.getMaxArea();
        case TAILING_FACTOR -> values[i] =
            (row.getBestFeature().getTailingFactor() != null) ? row.getBestFeature()
                .getTailingFactor() : 0.0;
        case ASYMMETRY_FACTOR -> values[i] =
            (row.getBestFeature().getAsymmetryFactor() != null) ? row.getBestFeature()
                .getAsymmetryFactor() : 0.0;
        case FWHM -> values[i] =
            (row.getBestFeature().getFWHM() != null) ? row.getBestFeature().getFWHM() : 0.0;
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
    if (status.getValue().equals(TaskStatus.FINISHED)) {
      return xValues.length;
    } else {
      return 0;
    }
  }

  @Override
  public Number getX(int series, int item) {
    if (status.getValue().equals(TaskStatus.FINISHED)) {
      return xValues[item];
    } else {
      return 0;
    }
  }

  @Override
  public Number getY(int series, int item) {
    if (status.getValue().equals(TaskStatus.FINISHED)) {
      return yValues[item];
    } else {
      return 0;
    }
  }

  @Override
  public Number getZ(int series, int item) {
    if (status.getValue().equals(TaskStatus.FINISHED)) {
      return colorScaleValues[item];
    } else {
      return 0;
    }
  }

  @Override
  public double getBubbleSizeValue(int series, int item) {
    if (status.getValue().equals(TaskStatus.FINISHED)) {
      return bubbleSizeValues[item];
    } else {
      return 0;
    }
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
    if (status.getValue().equals(TaskStatus.FINISHED)) {
      return xValues;
    } else {
      return new double[]{0};
    }
  }

  public double[] getyValues() {
    if (status.getValue().equals(TaskStatus.FINISHED)) {
      return yValues;
    } else {
      return new double[]{0};
    }
  }

  public double[] getColorScaleValues() {
    if (status.getValue().equals(TaskStatus.FINISHED)) {
      return colorScaleValues;
    } else {
      return new double[]{0};
    }
  }

  public double[] getBubbleSizeValues() {
    if (status.getValue().equals(TaskStatus.FINISHED)) {
      return bubbleSizeValues;
    } else {
      return new double[]{0};
    }
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

  public FeatureListRow getSelectedRow(int row) {
    return selectedRows[row];
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
        mz, kendrickMassBase, charge, divisor);
    return Math.round(kendrickMassChargeAndDivisorDependent)
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

  @Override
  public String getTaskDescription() {
    return "Computing values for Kendrick plot dataset";
  }

  @Override
  public double getFinishedPercentage() {
    return finishedSteps;
  }

  @Override
  public TaskStatus getStatus() {
    return status.getValue();
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.NORMAL;
  }

  @Override
  public void cancel() {
    setStatus(TaskStatus.CANCELED);
  }

  public final void setStatus(TaskStatus newStatus) {
    TaskStatus old = getStatus();
    status.setValue(newStatus);
    if (listener != null && !newStatus.equals(old)) {
      for (TaskStatusListener listener : listener) {
        listener.taskStatusChanged(this, newStatus, old);
      }
    }
  }

  public void addTaskStatusListener(TaskStatusListener list) {
    if (listener == null) {
      listener = new ArrayList<>();
    }
    listener.add(list);
  }

  @Override
  public boolean removeTaskStatusListener(TaskStatusListener list) {
    if (listener != null) {
      return listener.remove(list);
    } else {
      return false;
    }
  }

  @Override
  public void clearTaskStatusListener() {
    if (listener != null) {
      listener.clear();
    }
  }

  @Override
  public void error(@Nullable String message, @Nullable Exception exceptionToLog) {
    message = requireNonNullElse(message, "");
    if (exceptionToLog != null) {
      logger.log(Level.SEVERE, message, exceptionToLog);
    }
    setStatus(TaskStatus.ERROR);
  }

  @Override
  public FeatureListRow getItemObject(int item) {
    if (item < selectedRows.length) {
      return selectedRows[item];
    }
    return null;
  }

  public boolean isAnnotated(int item) {
    return isAnnotated[item];
  }
}
