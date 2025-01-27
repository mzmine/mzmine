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

package io.github.mzmine.modules.visualization.vankrevelendiagram;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotation;
import io.github.mzmine.datamodel.identities.MolecularFormulaIdentity;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.XYZBubbleDataset;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.data.xy.AbstractXYZDataset;
import org.openscience.cdk.config.Elements;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/*
 * XYZDataset for Van Krevelen diagram
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
class VanKrevelenDiagramXYZDataset extends AbstractXYZDataset implements Task, XYZBubbleDataset {

  // TODO replace with getTask or AbstractTaskXYZDataset
  private static final Logger logger = Logger.getLogger(
      VanKrevelenDiagramXYZDataset.class.getName());
  protected final @NotNull Property<TaskStatus> status = new SimpleObjectProperty<>(
      TaskStatus.WAITING);
  protected String errorMessage = null;
  private List<TaskStatusListener> listener;
  private double finishedSteps;
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
    filteredRows = featureList.stream()
        .filter(featureListRow -> featureListRow.getPreferredAnnotation() != null).toList();
    xValues = new double[filteredRows.size()];
    yValues = new double[filteredRows.size()];
    colorScaleValues = new double[filteredRows.size()];
    bubbleSizeValues = new double[filteredRows.size()];
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
    finishedSteps = 1;
    setStatus(TaskStatus.FINISHED);
  }

  private void initElementRatioValues(IElement elementOne, IElement elementTwo, double[] values) {
    for (int i = 0; i < filteredRows.size(); i++) {
      Object preferredAnnotation = filteredRows.get(i).getPreferredAnnotation();
      if (preferredAnnotation != null) {
        String formula = getFormulaFromAnnotation(preferredAnnotation);
        if (formula != null) {
          int elementOneCount = MolecularFormulaManipulator.getElementCount(
              Objects.requireNonNull(FormulaUtils.createMajorIsotopeMolFormula(formula)),
              elementOne);
          int elementTwoCount = MolecularFormulaManipulator.getElementCount(
              Objects.requireNonNull(FormulaUtils.createMajorIsotopeMolFormula(formula)),
              elementTwo);
          values[i] = (elementOneCount > 0 && elementTwoCount > 0) ? (double) elementOneCount
                                                                     / elementTwoCount : 0.0;
        } else {
          values[i] = 0.0;
        }
      }
    }
  }

  private String getFormulaFromAnnotation(Object annotation) {
    return switch (annotation) {
      case MatchedLipid lipid ->
          MolecularFormulaManipulator.getString(lipid.getLipidAnnotation().getMolecularFormula());
      case FeatureAnnotation ann -> ann.getFormula();
      case ManualAnnotation ann -> ann.getFormula();
      case MolecularFormulaIdentity ann -> ann.getFormulaAsString();
      default -> null;
    };
  }

  private void initDimensionValues(double[] values,
      VanKrevelenDiagramDataTypes vanKrevelenDiagramDataType) {
    for (int i = 0; i < filteredRows.size(); i++) {
      FeatureListRow row = filteredRows.get(i);
      switch (vanKrevelenDiagramDataType) {
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
        default -> throw new IllegalStateException(
            "Unexpected VanKrevelenDiagramDataType: " + vanKrevelenDiagramDataType);
      }
    }
  }


  public int getItemCount(int series) {
    return isFinished() ? xValues.length : 0;
  }

  @Override
  public Number getX(int series, int item) {
    return isFinished() ? xValues[item] : 0;
  }

  @Override
  public Number getY(int series, int item) {
    return isFinished() ? yValues[item] : 0;
  }

  @Override
  public Number getZ(int series, int item) {
    return isFinished() ? colorScaleValues[item] : 0;
  }

  @Override
  public double getBubbleSizeValue(int series, int item) {
    return isFinished() ? bubbleSizeValues[item] : 0;
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

  public VanKrevelenDiagramDataTypes getBubbleVanKrevelenDataType() {
    return bubbleVanKrevelenDataType;
  }

  @Override
  public String getTaskDescription() {
    return "Computing values for Van Krevelen diagram dataset";
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
}
