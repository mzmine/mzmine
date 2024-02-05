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

package io.github.mzmine.taskcontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tasks will most likely want to extend either {@link AbstractTask} or more specified classes
 * {@link AbstractFeatureListTask} {@link AbstractRawDataFileTask}
 */
public abstract class AbstractModifiableTask implements Task {

  private static final Logger logger = Logger.getLogger(AbstractModifiableTask.class.getName());
  private final StringProperty name = new SimpleStringProperty("Task name");
  private TaskStatus status = TaskStatus.WAITING;
  private String errorMessage = null;
  // listener to control status changes
  private List<TaskStatusListener> listener;

  public final String getName() {
    return name.get();
  }

  public final void setName(String value) {
    name.set(value);
  }

  public StringProperty nameProperty() {
    return name;
  }

  @Override
  public void cancel() {
    if (!isFinished()) {
      setStatus(TaskStatus.CANCELED);
    }
  }

  @Override
  public void error(@NotNull String message, @Nullable Exception exceptionToLog) {
    if (exceptionToLog != null) {
      logger.log(Level.SEVERE, message, exceptionToLog);
    }
    setErrorMessage(message);
    setStatus(TaskStatus.ERROR);
  }

  @Override
  public final String getErrorMessage() {
    return errorMessage;
  }

  public final void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.NORMAL;
  }

  /**
   * Returns the TaskStatus of this Task
   *
   * @return The current status of this task
   */
  @Override
  public final TaskStatus getStatus() {
    return this.status;
  }

  /**
   *
   */
  public final void setStatus(TaskStatus newStatus) {
    TaskStatus old = status;
    this.status = newStatus;
    if (listener != null && !status.equals(old)) {
      for (int i = 0; i < listener.size(); i++) {
        listener.get(i).taskStatusChanged(this, status, old);
      }
    }
  }

  @Override
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
}
