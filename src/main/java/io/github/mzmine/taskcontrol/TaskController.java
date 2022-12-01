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

import io.github.mzmine.taskcontrol.impl.TaskQueue;
import io.github.mzmine.taskcontrol.impl.WrappedTask;

/**
 * 
 */
public interface TaskController {

  public void addTask(Task task);

  public WrappedTask[] addTasks(Task tasks[]);

  public void addTask(Task task, TaskPriority priority);

  public WrappedTask[] addTasks(Task tasks[], TaskPriority[] priority);

  public void setTaskPriority(Task task, TaskPriority priority);

  public void addTaskControlListener(TaskControlListener listener);

  public TaskQueue getTaskQueue();

  public boolean isTaskInstanceRunningOrQueued(Class<? extends AbstractTask> clazz);

}
