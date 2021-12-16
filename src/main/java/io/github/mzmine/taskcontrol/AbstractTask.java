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
  private StringProperty name = new SimpleStringProperty("Task name");

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
   *  @param storage The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                RawDataFiles, MassLists, FeatureLists). May be null if results shall be stored
   *                in ram. For now, one storage should be created per module call in {@link
   *                io.github.mzmine.modules.MZmineRunnableModule#runModule(MZmineProject, ParameterSet, Collection, Instant)}.
   * @param moduleCallDate
   */
  protected AbstractTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    this.storage = storage;
    this.moduleCallDate = moduleCallDate;
  }

  /**
   *
   * @return The {@link MemoryMapStorage} used to store results of this task (e.g. RawDataFiles,
   * MassLists, FeatureLists). May be null if results shall be stored in ram.
   */
  @Nullable
  public MemoryMapStorage getMemoryMapStorage() {
    return storage;
  }

  /**
   */
  public final void setStatus(TaskStatus newStatus) {
    TaskStatus old = status;
    this.status = newStatus;
    if (listener != null && !status.equals(old))
      for (int i = 0; i < listener.size(); i++)
        listener.get(i).taskStatusChanged(this, status, old);
  }

  /**
   * Convenience method for determining if this task has been canceled. Also returns true if the
   * task encountered an error.
   *
   * @return true if this task has been canceled or stopped due to an error
   */
  public final boolean isCanceled() {
    return (status == TaskStatus.CANCELED) || (status == TaskStatus.ERROR);
  }

  /**
   * Convenience method for determining if this task has been completed
   *
   * @return true if this task is finished
   */
  public final boolean isFinished() {
    return status == TaskStatus.FINISHED;
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

  public void addTaskStatusListener(TaskStatusListener list) {
    if (listener == null)
      listener = new ArrayList<>();
    listener.add(list);
  }

  public boolean removeTaskStatusListener(TaskStatusListener list) {
    if (listener != null)
      return listener.remove(list);
    else
      return false;
  }

  public void clearTaskStatusListener() {
    if (listener != null)
      listener.clear();
  }

  public Instant getModuleCallDate() {
    return moduleCallDate;
  }
}
