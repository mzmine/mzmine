/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.batchmode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class BatchTaskCancellationTest {

  @Test
  void cancellationWaitsForInterruptIgnoringChildToExit() throws Exception {
    MZmineCore.getConfiguration().getPreferences();
    final MZmineProject project = Mockito.mock(MZmineProject.class);
    Mockito.when(project.getCurrentFeatureLists()).thenReturn(List.of());
    Mockito.when(project.getCurrentRawDataFiles()).thenReturn(List.of());
    final InterruptIgnoringTask childTask = new InterruptIgnoringTask();
    final BatchTask batchTask = createBatchTask(project, childTask);
    final ExecutorService childExecutor = Executors.newSingleThreadExecutor();
    final TaskController taskController = mockTaskController();
    final AtomicReference<Throwable> workerFailure = new AtomicReference<>();
    final Thread batchWorker = new Thread(() -> {
      try (MockedStatic<ProjectService> projects = Mockito.mockStatic(ProjectService.class);
          MockedStatic<TaskService> taskService = Mockito.mockStatic(TaskService.class)) {
        projects.when(ProjectService::getProject).thenReturn(project);
        taskService.when(TaskService::getController).thenReturn(taskController);
        batchTask.run();
      } catch (Throwable t) {
        workerFailure.set(t);
      }
    });
    Mockito.when(taskController.addTasks(Mockito.any(Task[].class))).thenAnswer(invocation -> {
      final WrappedTask wrapped = new WrappedTask(childTask, TaskPriority.NORMAL);
      wrapped.setFuture(childExecutor.submit(wrapped::run));
      return new WrappedTask[]{wrapped};
    });
    try {
      batchWorker.start();
      assertTrue(childTask.started.await(2, TimeUnit.SECONDS));
      batchTask.cancel();
      assertTrue(childTask.canceled.await(2, TimeUnit.SECONDS));
      batchWorker.interrupt();
      batchWorker.join(100);
      assertTrue(batchWorker.isAlive(), "Batch returned before its child exited");
      childTask.release.countDown();
      batchWorker.join(2_000);
      assertFalse(batchWorker.isAlive());
      assertTrue(workerFailure.get() == null, () -> "Batch worker failed: " + workerFailure.get());
    } finally {
      childTask.release.countDown();
      batchTask.cancel();
      childExecutor.shutdownNow();
      assertTrue(childExecutor.awaitTermination(2, TimeUnit.SECONDS));
    }
  }

  private static @NotNull BatchTask createBatchTask(final @NotNull MZmineProject project,
      final @NotNull Task childTask) {
    final ParameterSet stepParameters = Mockito.mock(ParameterSet.class);
    Mockito.when(stepParameters.cloneParameterSet(Mockito.anyBoolean())).thenReturn(stepParameters);
    Mockito.when(stepParameters.getParameters()).thenReturn(new Parameter<?>[0]);
    Mockito.when(stepParameters.checkParameterValues(Mockito.any(Collection.class))).thenReturn(true);
    final BatchQueue queue = new BatchQueue();
    queue.add(new MZmineProcessingStepImpl<>(new BlockingModule(childTask), stepParameters));
    final BatchModeParameters parameters = new BatchModeParameters();
    parameters.getParameter(BatchModeParameters.batchQueue).setValue(queue);
    return new BatchTask(project, parameters, Instant.now());
  }

  private static @NotNull TaskController mockTaskController() {
    try {
      return (TaskController) Mockito.mock(Class.forName(
          "io.github.mzmine.taskcontrol.TaskControllerImpl"));
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  private record BlockingModule(@NotNull Task childTask) implements MZmineProcessingModule {
    @Override public @NotNull String getName() { return "Blocking test module"; }
    @Override public Class<? extends ParameterSet> getParameterSetClass() { return null; }
    @Override public @NotNull String getDescription() { return "Produces a blocking task"; }
    @Override public @NotNull MZmineModuleCategory getModuleCategory() {
      return MZmineModuleCategory.OTHER_DATA_PROCESSING;
    }
    @Override public @NotNull ExitCode runModule(final @NotNull MZmineProject project,
        final @NotNull ParameterSet parameters, final @NotNull Collection<Task> tasks,
        final @NotNull Instant moduleCallDate) {
      tasks.add(childTask);
      return ExitCode.OK;
    }
  }

  private static final class InterruptIgnoringTask extends AbstractTask {
    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch canceled = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private InterruptIgnoringTask() { super(null, Instant.now()); }
    @Override public void run() {
      setStatus(TaskStatus.PROCESSING);
      started.countDown();
      while (release.getCount() > 0) {
        try { release.await(); } catch (InterruptedException ignored) { }
      }
      if (!isCanceled()) setStatus(TaskStatus.FINISHED);
    }
    @Override public void cancel() { super.cancel(); canceled.countDown(); }
    @Override public String getTaskDescription() { return "Interrupt-ignoring batch child"; }
    @Override public double getFinishedPercentage() { return 0; }
  }
}
