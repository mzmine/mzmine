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

package io.github.mzmine.taskcontrol.impl;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Wrapper class for Tasks that stores additional information
 */
public class WrappedTask {

  private StringProperty name = new SimpleStringProperty("");

  public final String getName() {
    return name.get();
  }

  public final void setName(String value) {
    name.set(value);
  }

  public StringProperty nameProperty() {
    return name;
  }

  private Task task;
  private Property<TaskPriority> priority;
  private WorkerThread assignedTo;

  public WrappedTask(Task task, TaskPriority priority) {
    this.task = task;
    this.priority = new SimpleObjectProperty<>(priority);
  }

  /**
   * @return Returns the priority.
   */
  TaskPriority getPriority() {
    return priority.getValue();
  }

  /**
   * @param priority The priority to set.
   */
  void setPriority(TaskPriority priority) {
    MZmineCore.runLater(() -> this.priority.setValue(priority));
    if (assignedTo != null) {
      switch (priority) {
        case HIGH -> assignedTo.setPriority(Thread.MAX_PRIORITY);
        case NORMAL -> assignedTo.setPriority(Thread.NORM_PRIORITY);
      }
    }
  }

  public Property<TaskPriority> priorityProperty() {
    return priority;
  }

  /**
   * @return Returns the assigned.
   */
  boolean isAssigned() {
    return assignedTo != null;
  }

  void assignTo(WorkerThread thread) {
    assignedTo = thread;
  }

  /**
   * @return Returns the task.
   */
  public synchronized Task getActualTask() {
    return task;
  }

  public synchronized String toString() {
    return task.getTaskDescription();
  }

  synchronized void removeTaskReference() {
    task = new FinishedTask(task);
  }

}
