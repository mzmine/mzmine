/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.equivalentcarbonnumberplot;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.data.xy.AbstractXYDataset;

public class EquivalentCarbonNumberDataset extends AbstractXYDataset implements Task {

  // TODO replace with internal getTask method or with AbstractTaskXYDataset
  private static final Logger logger = Logger.getLogger(
      EquivalentCarbonNumberDataset.class.getName());
  protected final @NotNull Property<TaskStatus> status = new SimpleObjectProperty<>(
      TaskStatus.WAITING);
  protected String errorMessage = null;
  private List<TaskStatusListener> listener;
  private double finishedSteps;

  private double[] xValues;
  private double[] yValues;
  private final FeatureListRow[] lipidRows;
  private final List<FeatureListRow> selectedRows;
  private final ILipidClass selectedLipidClass;
  private final int selectedDBENumber;
  private List<MatchedLipid> lipidsForDBE;


  public EquivalentCarbonNumberDataset(List<FeatureListRow> selectedRows,
      FeatureListRow[] lipidRows, ILipidClass selectedLipidClass, int selectedDBENumber) {
    this.selectedRows = selectedRows;
    this.lipidRows = lipidRows;
    this.selectedLipidClass = selectedLipidClass;
    this.selectedDBENumber = selectedDBENumber;
    MZmineCore.getTaskController().addTask(this);
  }

  @Override
  public void run() {
    finishedSteps = 0;
    setStatus(TaskStatus.PROCESSING);
    if (isCanceled()) {
      setStatus(TaskStatus.CANCELED);
      return;
    }

    final List<MatchedLipid> selectedLipids = new ArrayList<>();
    final List<Double> xValueList = new ArrayList<>();
    final List<Double> yValueList = new ArrayList<>();
    for (final FeatureListRow lipidRow : lipidRows) {
      final Float rowRt = lipidRow.getAverageRT();
      if (rowRt == null || !Float.isFinite(rowRt)) {
        continue;
      }

      final List<MatchedLipid> featureLipids = lipidRow.get(LipidMatchListType.class);
      if (featureLipids == null || featureLipids.isEmpty()) {
        continue;
      }

      final Set<Integer> addedCarbonsForRow = new HashSet<>();
      for (final MatchedLipid featureMatchedLipid : featureLipids) {
        if (!featureMatchedLipid.getLipidAnnotation().getLipidClass().equals(selectedLipidClass)) {
          continue;
        }

        final int dbe = featureMatchedLipid.getLipidAnnotation().getChainsDoubleBondCount();
        if (dbe != selectedDBENumber) {
          continue;
        }

        final int carbons = featureMatchedLipid.getLipidAnnotation().getChainsCarbonCount();
        if (carbons < 0 || !addedCarbonsForRow.add(carbons)) {
          continue;
        }

        selectedLipids.add(featureMatchedLipid);
        xValueList.add(rowRt.doubleValue());
        yValueList.add((double) carbons);
      }
    }
    lipidsForDBE = selectedLipids;
    xValues = xValueList.stream().mapToDouble(Double::doubleValue).toArray();
    yValues = yValueList.stream().mapToDouble(Double::doubleValue).toArray();
    finishedSteps = 1;
    setStatus(TaskStatus.FINISHED);
  }


  @Override
  public int getSeriesCount() {
    return 1;
  }

  public Comparable<?> getRowKey(int row) {
    return selectedRows.get(row).toString();
  }

  @Override
  public Comparable getSeriesKey(int series) {
    return getRowKey(series);
  }


  public double[] getXValues() {
    return xValues;
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

  public MatchedLipid getMatchedLipid(int item) {
    return lipidsForDBE.get(item);
  }

  @Override
  public String getTaskDescription() {
    return "Computing ECN model for " + selectedLipidClass.getAbbr() + " with " + selectedDBENumber
        + " DBEs";
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
