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

package io.github.mzmine.taskcontrol;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract implementation of task which defines common methods to make Task implementation
 * easier. Added task status listener
 */
public abstract class AbstractTask implements Task {

  protected final MemoryMapStorage storage;
  protected final Instant moduleCallDate;

  private TaskStatus status = TaskStatus.WAITING;

  private String errorMessage = null;
  // listener to control status changes
  private List<TaskStatusListener> listener;
  private final StringProperty name = new SimpleStringProperty("Task name");

  public final String getName() {
    return name.get();
  }

  public final void setName(String value) {
    name.set(value);
  }

  public StringProperty nameProperty() {
    return name;
  }

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   *                       {@link
   *                       io.github.mzmine.modules.MZmineRunnableModule#runModule(MZmineProject,
   *                       ParameterSet, Collection, Instant)}.
   * @param moduleCallDate
   */
  protected AbstractTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    this.storage = storage;
    this.moduleCallDate = moduleCallDate;
  }

  /**
   * @return The {@link MemoryMapStorage} used to store results of this task (e.g. RawDataFiles,
   * MassLists, FeatureLists). May be null if results shall be stored in ram.
   */
  @Nullable
  public MemoryMapStorage getMemoryMapStorage() {
    return storage;
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

  /**
   * @see io.github.mzmine.taskcontrol.Task#cancel()
   */
  @Override
  public void cancel() {
    setStatus(TaskStatus.CANCELED);
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getErrorMessage()
   */
  @Override
  public final String getErrorMessage() {
    return errorMessage;
  }

  /**
   *
   */
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

  public Instant getModuleCallDate() {
    return moduleCallDate;
  }
}
