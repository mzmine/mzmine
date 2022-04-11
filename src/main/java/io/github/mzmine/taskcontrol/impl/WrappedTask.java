/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
