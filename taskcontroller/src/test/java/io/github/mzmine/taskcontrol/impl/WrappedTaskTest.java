/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.taskcontrol.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class WrappedTaskTest {

  @Test
  void canceledQueuedTaskNeverStartsAndIsExecutionComplete() {
    final BlockingTask task = new BlockingTask();
    final WrappedTask wrappedTask = new WrappedTask(task, TaskPriority.NORMAL);

    wrappedTask.cancel();
    wrappedTask.run();

    assertTrue(wrappedTask.isExecutionComplete());
    assertFalse(task.wasRun.get());
  }

  @Test
  void statusCancellationStillInvokesNativeCancellationExactlyOnce() {
    final BlockingTask task = new BlockingTask();
    final WrappedTask wrappedTask = new WrappedTask(task, TaskPriority.NORMAL);
    task.setStatus(TaskStatus.CANCELED);
    wrappedTask.cancel();
    assertEquals(1, task.cancelCalls);
    assertTrue(wrappedTask.isExecutionComplete());
  }

  @Test
  void preexistingTerminalStatusCannotLeaveQueuedWorkIncomplete() {
    for (final TaskStatus status : new TaskStatus[]{TaskStatus.CANCELED, TaskStatus.ERROR}) {
      final BlockingTask task = new BlockingTask();
      task.setStatus(status);
      final WrappedTask wrappedTask = new WrappedTask(task, TaskPriority.NORMAL);
      wrappedTask.setFuture(new java.util.concurrent.FutureTask<>(wrappedTask::run, null));
      wrappedTask.run();
      assertTrue(wrappedTask.isExecutionComplete());
      assertFalse(task.wasRun.get());
    }
  }

  @Test
  void runningTaskIsIncompleteUntilRunHasExited() throws InterruptedException {
    final BlockingTask task = new BlockingTask();
    final WrappedTask wrappedTask = new WrappedTask(task, TaskPriority.NORMAL);
    final Thread worker = new Thread(wrappedTask::run);

    worker.start();
    assertTrue(task.started.await(2, TimeUnit.SECONDS));
    wrappedTask.cancel();

    assertFalse(wrappedTask.isExecutionComplete());
    task.release.countDown();
    worker.join(2_000);

    assertFalse(worker.isAlive());
    assertTrue(wrappedTask.isExecutionComplete());
  }

  private static final class BlockingTask extends AbstractTask {

    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private final AtomicBoolean wasRun = new AtomicBoolean();
    private int cancelCalls;

    private BlockingTask() {
      super(null, Instant.now());
    }

    @Override
    public void cancel() {
      cancelCalls++;
      super.cancel();
    }

    @Override
    public void run() {
      wasRun.set(true);
      setStatus(TaskStatus.PROCESSING);
      started.countDown();
      try {
        release.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      if (!isCanceled()) {
        setStatus(TaskStatus.FINISHED);
      }
    }

    @Override
    public String getTaskDescription() {
      return "Blocking test task";
    }

    @Override
    public double getFinishedPercentage() {
      return 0;
    }
  }
}
