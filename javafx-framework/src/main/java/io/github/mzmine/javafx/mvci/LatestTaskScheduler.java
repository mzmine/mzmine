package io.github.mzmine.javafx.mvci;

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.taskcontrol.utils.TaskUtils;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

public class LatestTaskScheduler {

  private static final Logger logger = Logger.getLogger(LatestTaskScheduler.class.getName());

  public static final Duration DEFAULT_DELAY = Duration.millis(250);
  private final CloseableReentrantReadWriteLock taskLock = new CloseableReentrantReadWriteLock();
  private Map<String, Task> runningTasks;
  private Map<String, PauseTransition> delayedTasks;


  /**
   * Run task on GUI thread
   */
  public void onGuiThread(Runnable task) {
    FxThread.runLater(task);
  }


  /**
   * Run a task on a separate thread - for GUI updates after completion use {@link FxUpdateTask}
   *
   * @param task primary task run on separate thread
   */
  public void onTaskThread(final @NotNull Runnable task) {
    onTaskThread(task, TaskPriority.NORMAL);
  }


  /**
   * Stop old tasks with this unique name, restart a timer that calls the task on finish. This
   * accumulates multiple calls into a single task run. Run a task on a separate thread - for GUI
   * updates after completion use {@link FxUpdateTask}. Default delay
   *
   * @param task primary task run on separate thread. Update GUI after complete
   */
  public void onTaskThreadDelayed(final @NotNull FxUpdateTask<?> task) {
    onTaskThreadDelayed(task, DEFAULT_DELAY);
  }

  /**
   * Stop old tasks with this unique name, restart a timer that calls the task on finish. This
   * accumulates multiple calls into a single task run. Run a task on a separate thread - for GUI
   * updates after completion use {@link FxUpdateTask}.
   *
   * @param task  primary task run on separate thread. Update GUI after complete
   * @param delay the delay for call accumulation, the timer is always reset after each call.
   */
  public void onTaskThreadDelayed(final @NotNull FxUpdateTask<?> task,
      final @NotNull Duration delay) {
    onTaskThreadDelayed(task, task.getTaskPriority(), task.getName(), delay);
  }

  /**
   * Stop old tasks with this unique name, restart a timer that calls the task on finish. This
   * accumulates multiple calls into a single task run. Run a task on a separate thread - for GUI
   * updates after completion use {@link FxUpdateTask} and
   * {@link #onTaskThreadDelayed(FxUpdateTask, Duration)}.
   *
   * @param task           primary task run on separate thread
   * @param uniqueTaskName the unique task name is used to accumulate update calls and stop older
   *                       tasks
   */
  public void onTaskThreadDelayed(final @NotNull Runnable task, final String uniqueTaskName) {
    onTaskThreadDelayed(task, uniqueTaskName, DEFAULT_DELAY);
  }

  /**
   * Stop old tasks with this unique name, restart a timer that calls the task on finish. This
   * accumulates multiple calls into a single task run. Run a task on a separate thread - for GUI
   * updates after completion use {@link FxUpdateTask} and
   * {@link #onTaskThreadDelayed(FxUpdateTask, Duration)}.
   *
   * @param task           primary task run on separate thread
   * @param uniqueTaskName the unique task name is used to accumulate update calls and stop older
   *                       tasks
   * @param delay          the delay for call accumulation, the timer is always reset after each
   *                       call.
   */
  public void onTaskThreadDelayed(final @NotNull Runnable task, final String uniqueTaskName,
      final @NotNull Duration delay) {
    onTaskThreadDelayed(task, TaskPriority.NORMAL, uniqueTaskName, delay);
  }

  /**
   * Stop old tasks with this unique name, restart a timer that calls the task on finish. This
   * accumulates multiple calls into a single task run. Run a task on a separate thread - for GUI
   * updates after completion use {@link FxUpdateTask} and
   * {@link #onTaskThreadDelayed(FxUpdateTask, Duration)}.
   *
   * @param task           primary task run on separate thread
   * @param uniqueTaskName the unique task name is used to accumulate update calls and stop older
   *                       tasks
   * @param delay          the delay for call accumulation, the timer is always reset after each
   *                       call.
   */
  public void onTaskThreadDelayed(final @NotNull Runnable task,
      final @NotNull TaskPriority priority, final String uniqueTaskName,
      final @NotNull Duration delay) {
    if ((task instanceof FxUpdateTask<?> fxTask) && !fxTask.checkPreConditions()) {
      return;
    }

    try (var _ = taskLock.lockWrite()) {
      cancelOldTask(uniqueTaskName);

      if (delayedTasks == null) {
        delayedTasks = new HashMap<>();
      }
      // create or reuse pause transition to accumulate update calls
      // restart the timer
      PauseTransition updateAccumulator = delayedTasks.computeIfAbsent(uniqueTaskName,
          _ -> new PauseTransition(delay));
      updateAccumulator.setOnFinished(_ -> {
        logger.fine(STR."Scheduling task \{uniqueTaskName}");
        onTaskThread(task, priority);
      });
      updateAccumulator.setDuration(delay);
      updateAccumulator.playFromStart();
      logger.fine(STR."Delaying task \{uniqueTaskName} because of new update trigger");
    }
  }

  /**
   * Cancel old task if still active
   *
   * @param uniqueTaskName the unique task name that was used to start the task. also see
   *                       {@link FxUpdateTask#getName()}
   */
  public void cancelOldTask(final String uniqueTaskName) {
    var oldTask = removeOldTask(uniqueTaskName);
    if (oldTask != null && !oldTask.isFinished()) {
      oldTask.cancel();
      logger.finest(STR."Cancelled old task \{uniqueTaskName} because of new task trigger");
    }
  }

  /**
   * Run a task on a separate thread - for GUI updates after completion use {@link FxUpdateTask}
   *
   * @param task primary task run on separate thread
   */
  public void onTaskThread(final @NotNull Runnable task, final @NotNull TaskPriority priority) {
    final Task runningTask = TaskUtils.wrapTask(task);

    // if gui is updated after - add checks for latest task completion
    if (task instanceof FxUpdateTask<?> fxUpdateTask) {
      if (!fxUpdateTask.checkPreConditions()) {
        return;
      }

      String uniqueTaskName = fxUpdateTask.getName();

      try (var _ = taskLock.lockWrite()) {
        if (runningTasks == null) {
          runningTasks = new HashMap<>();
        }
        cancelOldTask(uniqueTaskName);
        runningTasks.put(uniqueTaskName, runningTask);
      }

      runningTask.setOnFinished(() -> {
        // remove the old task from map and compare with the running task if equal
        final Task oldTask = removeOldTask(uniqueTaskName);
        if (oldTask != null && oldTask.isFinished() && Objects.equals(oldTask, runningTask)) {
          // only update gui if still latest task
          FxThread.runLater(fxUpdateTask::updateGuiModel);
        }
      });
    }
    // schedule
    TaskService.getController().addTask(runningTask, priority);
  }


  /**
   * removes the task with a write lock
   *
   * @param taskName the name of the task in map
   * @return the old task in map
   */
  private Task removeOldTask(final @NotNull String taskName) {
    if (runningTasks == null) {
      return null;
    }
    try (var _ = taskLock.lockWrite()) {
      return runningTasks.remove(taskName);
    }
  }
}
