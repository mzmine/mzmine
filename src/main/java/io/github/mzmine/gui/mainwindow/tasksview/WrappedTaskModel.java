/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.gui.mainwindow.tasksview;

import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * An observable wrapper for each task to be added to a TableView in {@link TasksView}
 */
public class WrappedTaskModel {

  private final WrappedTask task;
  private final StringProperty name = new SimpleStringProperty();
  private final DoubleProperty progress = new SimpleDoubleProperty();
  private final ObjectProperty<TaskPriority> priority = new SimpleObjectProperty<>();
  private final ObjectProperty<TaskStatus> status = new SimpleObjectProperty<>();

  public WrappedTaskModel(final WrappedTask task) {
    this.task = task;
  }

  public WrappedTask getTask() {
    return task;
  }

  public String getName() {
    return name.get();
  }

  public void setName(final String name) {
    this.name.set(name);
  }

  public StringProperty nameProperty() {
    return name;
  }

  public double getProgress() {
    return progress.get();
  }

  public void setProgress(final double progress) {
    this.progress.set(progress);
  }

  public DoubleProperty progressProperty() {
    return progress;
  }

  public TaskPriority getPriority() {
    return priority.get();
  }

  public void setPriority(final TaskPriority priority) {
    this.priority.set(priority);
  }

  public ObjectProperty<TaskPriority> priorityProperty() {
    return priority;
  }

  public TaskStatus getStatus() {
    return status.get();
  }

  public void setStatus(final TaskStatus status) {
    this.status.set(status);
  }

  public ObjectProperty<TaskStatus> statusProperty() {
    return status;
  }

  /**
   * Update properties based on task
   */
  public void updateProperties() {
    setName(task.getTaskDescription());
    setStatus(task.getStatus());
    setPriority(task.getTaskPriority());
    setProgress(task.getFinishedPercentage());
  }
}
