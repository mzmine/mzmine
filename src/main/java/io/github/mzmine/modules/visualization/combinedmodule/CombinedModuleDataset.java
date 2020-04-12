package io.github.mzmine.modules.visualization.combinedmodule;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import org.jfree.data.xy.AbstractXYDataset;

public class CombinedModuleDataset extends AbstractXYDataset implements Task {

  private RawDataFile rawDataFile;
  private Range<Double> totalRTRange, totalMZRange;
  private CombinedModuleVisualizerWindowController visualizer;
  private TaskStatus status = TaskStatus.WAITING;

  public CombinedModuleDataset(RawDataFile dataFile, Range<Double> rtRange, Range<Double> mzRange,
      CombinedModuleVisualizerWindowController visualizer) {
    this.rawDataFile = dataFile;
    this.totalMZRange = mzRange;
    this.totalRTRange = rtRange;
    this.visualizer = visualizer;

    MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);
  }

  @Override
  public String getTaskDescription() {
    return "Updating MS/MS visualizer of " + rawDataFile;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public TaskStatus getStatus() {
    return status;
  }

  @Override
  public String getErrorMessage() {
    return null;
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.NORMAL;
  }

  @Override
  public void cancel() {
    status = TaskStatus.CANCELED;
  }

  @Override
  public void run() {
    status = TaskStatus.PROCESSING;

    status = TaskStatus.FINISHED;
  }

  @Override
  public int getSeriesCount() {
    return 0;
  }

  @Override
  public Comparable<?> getSeriesKey(int series) {
    return rawDataFile.getName();
  }

  @Override
  public int getItemCount(int series) {
    return 0;
  }

  @Override
  public Number getX(int series, int item) {
    return null;
  }

  @Override
  public Number getY(int series, int item) {
    return null;
  }
}
