/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.io.import_rawdata_all;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class AllSpectralDataImportMainTaskCancellationTest {

  @Test
  void fixedProjectCancellationWaitsForTheRunningImportToExit() throws InterruptedException {
    MZmineCore.getConfiguration().getPreferences();
    final MZmineProject reviewedProject = Mockito.mock(MZmineProject.class);
    final InterruptIgnoringTask importTask = new InterruptIgnoringTask();
    final ExecutorService importExecutor = Executors.newSingleThreadExecutor();
    final TaskController taskController = mockTaskController();
    final AllSpectralDataImportMainTask mainTask =
        AllSpectralDataImportMainTask.forFixedProject(List.of(importTask), List.of(importTask),
            new AllSpectralDataImportParameters(), reviewedProject);
    final AtomicReference<Throwable> workerFailure = new AtomicReference<>();
    final Thread mainWorker = new Thread(() -> {
      try (MockedStatic<MZmineCore> core = Mockito.mockStatic(MZmineCore.class);
          MockedStatic<ProjectService> projectService = Mockito.mockStatic(ProjectService.class)) {
        core.when(MZmineCore::getTaskController).thenReturn(taskController);
        projectService.when(ProjectService::getProject).thenReturn(reviewedProject);
        mainTask.run();
      } catch (Throwable t) {
        workerFailure.set(t);
      }
    });

    Mockito.when(taskController.addTasks(Mockito.any(Task[].class))).thenAnswer(invocation -> {
      final WrappedTask wrapped = new WrappedTask(importTask, TaskPriority.NORMAL);
      wrapped.setFuture(importExecutor.submit(wrapped::run));
      return new WrappedTask[]{wrapped};
    });

    try {
      mainWorker.start();
      assertTrue(importTask.started.await(2, TimeUnit.SECONDS));

      mainTask.cancel();
      assertTrue(importTask.canceled.await(2, TimeUnit.SECONDS));
      assertTrue(importTask.interrupted.await(2, TimeUnit.SECONDS));
      assertTrue(mainWorker.isAlive(), "Main task returned before its child exited");

      importTask.release.countDown();
      mainWorker.join(2_000);
      assertFalse(mainWorker.isAlive());
      assertTrue(workerFailure.get() == null, () -> "Main worker failed: " + workerFailure.get());
      assertTrue(mainTask.isCanceled());
    } finally {
      importTask.release.countDown();
      mainTask.cancel();
      importExecutor.shutdownNow();
      assertTrue(importExecutor.awaitTermination(2, TimeUnit.SECONDS));
    }
  }

  private static TaskController mockTaskController() {
    try {
      return (TaskController) Mockito.mock(
          Class.forName("io.github.mzmine.taskcontrol.TaskControllerImpl"));
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  private static final class InterruptIgnoringTask extends AbstractTask {

    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch canceled = new CountDownLatch(1);
    private final CountDownLatch interrupted = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);

    private InterruptIgnoringTask() {
      super(null, Instant.now());
    }

    @Override
    public void run() {
      setStatus(TaskStatus.PROCESSING);
      started.countDown();
      while (release.getCount() > 0) {
        try {
          release.await();
        } catch (InterruptedException e) {
          interrupted.countDown();
        }
      }
      if (!isCanceled()) {
        setStatus(TaskStatus.FINISHED);
      }
    }

    @Override
    public void cancel() {
      super.cancel();
      canceled.countDown();
    }

    @Override
    public String getTaskDescription() {
      return "Interrupt-ignoring import task";
    }

    @Override
    public double getFinishedPercentage() {
      return 0;
    }
  }
}
