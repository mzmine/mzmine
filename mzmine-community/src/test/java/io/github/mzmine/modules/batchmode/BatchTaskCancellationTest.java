/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.batchmode;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Arrays;
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
  void fixedProjectCancellationWaitsForInterruptIgnoringChildToExit() throws Exception {
    MZmineCore.getConfiguration().getPreferences();
    final MZmineProject project = Mockito.mock(MZmineProject.class);
    Mockito.when(project.getCurrentFeatureLists()).thenReturn(List.of());
    Mockito.when(project.getCurrentRawDataFiles()).thenReturn(List.of());
    final InterruptIgnoringTask childTask = new InterruptIgnoringTask();
    final BatchTask batchTask = createFixedProjectBatchTask(project, childTask);
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
      assertTrue(childTask.interrupted.await(2, TimeUnit.SECONDS));
      batchWorker.interrupt();
      batchWorker.join(100);
      assertTrue(batchWorker.isAlive(), "Batch returned before its child exited");

      childTask.release.countDown();
      batchWorker.join(2_000);
      assertFalse(batchWorker.isAlive());
      assertTrue(workerFailure.get() == null, () -> "Batch worker failed: " + workerFailure.get());
      assertTrue(batchTask.isCanceled());
    } finally {
      childTask.release.countDown();
      batchTask.cancel();
      childExecutor.shutdownNow();
      assertTrue(childExecutor.awaitTermination(2, TimeUnit.SECONDS));
    }
  }

  @Test
  void fixedProjectChangeCancelsAnActiveChildWithoutClientPolling() throws Exception {
    MZmineCore.getConfiguration().getPreferences();
    final MZmineProject reviewedProject = Mockito.mock(MZmineProject.class);
    Mockito.when(reviewedProject.getCurrentFeatureLists()).thenReturn(List.of());
    Mockito.when(reviewedProject.getCurrentRawDataFiles()).thenReturn(List.of());
    final MZmineProject replacementProject = Mockito.mock(MZmineProject.class);
    final AtomicReference<MZmineProject> activeProject = new AtomicReference<>(reviewedProject);
    final InterruptIgnoringTask childTask = new InterruptIgnoringTask();
    final BatchTask batchTask = createFixedProjectBatchTask(reviewedProject, childTask);
    final ExecutorService childExecutor = Executors.newSingleThreadExecutor();
    final TaskController taskController = mockTaskController();
    final AtomicReference<Throwable> workerFailure = new AtomicReference<>();
    final Thread batchWorker = new Thread(() -> {
      try (MockedStatic<ProjectService> projects = Mockito.mockStatic(ProjectService.class);
          MockedStatic<TaskService> taskService = Mockito.mockStatic(TaskService.class)) {
        projects.when(ProjectService::getProject).thenAnswer(_ -> activeProject.get());
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

      activeProject.set(replacementProject);
      assertTrue(childTask.canceled.await(2, TimeUnit.SECONDS));
      assertTrue(childTask.interrupted.await(2, TimeUnit.SECONDS));
      batchWorker.join(100);
      assertTrue(batchWorker.isAlive(), "Batch returned before its child exited");

      childTask.release.countDown();
      batchWorker.join(2_000);
      assertFalse(batchWorker.isAlive());
      assertTrue(workerFailure.get() == null, () -> "Batch worker failed: " + workerFailure.get());
      assertEquals(TaskStatus.CANCELED, batchTask.getStatus());
      assertEquals("The reviewed project changed before the batch could continue.",
          batchTask.getErrorMessage());
    } finally {
      childTask.release.countDown();
      batchTask.cancel();
      childExecutor.shutdownNow();
      assertTrue(childExecutor.awaitTermination(2, TimeUnit.SECONDS));
    }
  }

  @Test
  void fixedProjectChildFailureCancelsActiveSiblingAndWaitsForItsExit() throws Exception {
    MZmineCore.getConfiguration().getPreferences();
    final MZmineProject project = Mockito.mock(MZmineProject.class);
    Mockito.when(project.getCurrentFeatureLists()).thenReturn(List.of());
    Mockito.when(project.getCurrentRawDataFiles()).thenReturn(List.of());
    final FailingTask failingChild = new FailingTask();
    final InterruptIgnoringTask siblingChild = new InterruptIgnoringTask();
    final BatchTask batchTask = createFixedProjectBatchTask(project, failingChild, siblingChild);
    final ExecutorService childExecutor = Executors.newFixedThreadPool(2);
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

    Mockito.when(taskController.addTasks(Mockito.any(Task[].class))).thenAnswer(invocation ->
        Arrays.stream(invocation.<Task[]>getArgument(0)).map(task -> {
          final WrappedTask wrapped = new WrappedTask(task, TaskPriority.NORMAL);
          wrapped.setFuture(childExecutor.submit(wrapped::run));
          return wrapped;
        }).toArray(WrappedTask[]::new));

    try {
      batchWorker.start();
      assertTrue(failingChild.started.await(2, TimeUnit.SECONDS));
      assertTrue(siblingChild.started.await(2, TimeUnit.SECONDS));

      failingChild.release.countDown();
      assertTrue(siblingChild.canceled.await(2, TimeUnit.SECONDS));
      assertTrue(siblingChild.interrupted.await(2, TimeUnit.SECONDS));
      batchWorker.join(100);
      assertTrue(batchWorker.isAlive(), "Batch returned before its sibling exited");

      siblingChild.release.countDown();
      batchWorker.join(2_000);
      assertFalse(batchWorker.isAlive());
      assertTrue(workerFailure.get() == null, () -> "Batch worker failed: " + workerFailure.get());
      assertEquals(TaskStatus.ERROR, batchTask.getStatus());
      assertEquals("Expected test failure", batchTask.getErrorMessage());
    } finally {
      failingChild.release.countDown();
      siblingChild.release.countDown();
      batchTask.cancel();
      childExecutor.shutdownNow();
      assertTrue(childExecutor.awaitTermination(2, TimeUnit.SECONDS));
    }
  }

  @Test
  void fixedProjectChildCancellationCancelsActiveSiblingAndWaitsForItsExit() throws Exception {
    MZmineCore.getConfiguration().getPreferences();
    final MZmineProject project = Mockito.mock(MZmineProject.class);
    Mockito.when(project.getCurrentFeatureLists()).thenReturn(List.of());
    Mockito.when(project.getCurrentRawDataFiles()).thenReturn(List.of());
    final SelfCancelingTask cancelingChild = new SelfCancelingTask();
    final InterruptIgnoringTask siblingChild = new InterruptIgnoringTask();
    final BatchTask batchTask = createFixedProjectBatchTask(project, cancelingChild, siblingChild);
    final ExecutorService childExecutor = Executors.newFixedThreadPool(2);
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

    Mockito.when(taskController.addTasks(Mockito.any(Task[].class))).thenAnswer(invocation ->
        Arrays.stream(invocation.<Task[]>getArgument(0)).map(task -> {
          final WrappedTask wrapped = new WrappedTask(task, TaskPriority.NORMAL);
          wrapped.setFuture(childExecutor.submit(wrapped::run));
          return wrapped;
        }).toArray(WrappedTask[]::new));

    try {
      batchWorker.start();
      assertTrue(cancelingChild.started.await(2, TimeUnit.SECONDS));
      assertTrue(siblingChild.started.await(2, TimeUnit.SECONDS));

      cancelingChild.release.countDown();
      assertTrue(siblingChild.canceled.await(2, TimeUnit.SECONDS));
      assertTrue(siblingChild.interrupted.await(2, TimeUnit.SECONDS));
      batchWorker.join(100);
      assertTrue(batchWorker.isAlive(), "Batch returned before its sibling exited");

      siblingChild.release.countDown();
      batchWorker.join(2_000);
      assertFalse(batchWorker.isAlive());
      assertTrue(workerFailure.get() == null, () -> "Batch worker failed: " + workerFailure.get());
      assertEquals(TaskStatus.CANCELED, batchTask.getStatus());
    } finally {
      cancelingChild.release.countDown();
      siblingChild.release.countDown();
      batchTask.cancel();
      childExecutor.shutdownNow();
      assertTrue(childExecutor.awaitTermination(2, TimeUnit.SECONDS));
    }
  }

  @Test
  void fixedProjectReconcilesChildFailureBeforeListenerRegistration() throws Exception {
    MZmineCore.getConfiguration().getPreferences();
    final MZmineProject project = Mockito.mock(MZmineProject.class);
    Mockito.when(project.getCurrentFeatureLists()).thenReturn(List.of());
    Mockito.when(project.getCurrentRawDataFiles()).thenReturn(List.of());
    final FailingTask failedChild = new FailingTask();
    final InterruptIgnoringTask siblingChild = new InterruptIgnoringTask();
    final BatchTask batchTask = createFixedProjectBatchTask(project, failedChild, siblingChild);
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
      final WrappedTask failedWrapped = new WrappedTask(failedChild, TaskPriority.NORMAL);
      final WrappedTask siblingWrapped = new WrappedTask(siblingChild, TaskPriority.NORMAL);
      siblingWrapped.setFuture(childExecutor.submit(siblingWrapped::run));
      if (!siblingChild.started.await(2, TimeUnit.SECONDS)) {
        throw new AssertionError("Sibling did not start before the pre-existing failure");
      }
      failedChild.error("Pre-existing test failure");
      return new WrappedTask[]{failedWrapped, siblingWrapped};
    });

    try {
      batchWorker.start();
      assertTrue(siblingChild.started.await(2, TimeUnit.SECONDS));
      assertTrue(siblingChild.canceled.await(2, TimeUnit.SECONDS));
      assertTrue(siblingChild.interrupted.await(2, TimeUnit.SECONDS));
      batchWorker.join(100);
      assertTrue(batchWorker.isAlive(), "Batch returned before its sibling exited");

      siblingChild.release.countDown();
      batchWorker.join(2_000);
      assertFalse(batchWorker.isAlive());
      assertTrue(workerFailure.get() == null, () -> "Batch worker failed: " + workerFailure.get());
      assertEquals(TaskStatus.ERROR, batchTask.getStatus());
      assertEquals("Pre-existing test failure", batchTask.getErrorMessage());
    } finally {
      siblingChild.release.countDown();
      batchTask.cancel();
      childExecutor.shutdownNow();
      assertTrue(childExecutor.awaitTermination(2, TimeUnit.SECONDS));
    }
  }

  private static @NotNull BatchTask createFixedProjectBatchTask(final @NotNull MZmineProject project,
      final @NotNull Task... childTasks) {
    final ParameterSet stepParameters = Mockito.mock(ParameterSet.class);
    Mockito.when(stepParameters.cloneParameterSet(Mockito.anyBoolean())).thenReturn(stepParameters);
    Mockito.when(stepParameters.getParameters()).thenReturn(new Parameter<?>[0]);
    Mockito.when(stepParameters.checkParameterValues(Mockito.any(Collection.class))).thenReturn(true);
    final BatchQueue queue = new BatchQueue();
    queue.add(new MZmineProcessingStepImpl<>(new BlockingModule(List.of(childTasks)), stepParameters));
    final BatchModeParameters parameters = new BatchModeParameters();
    parameters.getParameter(BatchModeParameters.batchQueue).setValue(queue);
    return BatchTask.forFixedProject(project, parameters, Instant.now());
  }

  private static @NotNull TaskController mockTaskController() {
    try {
      return (TaskController) Mockito.mock(
          Class.forName("io.github.mzmine.taskcontrol.TaskControllerImpl"));
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  private record BlockingModule(@NotNull List<Task> childTasks) implements MZmineProcessingModule {

    @Override
    public @NotNull String getName() {
      return "Blocking test module";
    }

    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {
      return null;
    }

    @Override
    public @NotNull String getDescription() {
      return "Produces one interrupt-ignoring task";
    }

    @Override
    public @NotNull ExitCode runModule(final @NotNull MZmineProject project,
        final @NotNull ParameterSet parameters, final @NotNull Collection<Task> tasks,
        final @NotNull Instant moduleCallDate) {
      tasks.addAll(childTasks);
      return ExitCode.OK;
    }

    @Override
    public @NotNull MZmineModuleCategory getModuleCategory() {
      return MZmineModuleCategory.OTHER_DATA_PROCESSING;
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
      return "Interrupt-ignoring batch child";
    }

    @Override
    public double getFinishedPercentage() {
      return 0;
    }
  }

  private static final class FailingTask extends AbstractTask {

    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);

    private FailingTask() {
      super(null, Instant.now());
    }

    @Override
    public void run() {
      setStatus(TaskStatus.PROCESSING);
      started.countDown();
      try {
        release.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        cancel();
        return;
      }
      error("Expected test failure");
    }

    @Override
    public String getTaskDescription() {
      return "Failing batch child";
    }

    @Override
    public double getFinishedPercentage() {
      return 0;
    }
  }

  private static final class SelfCancelingTask extends AbstractTask {

    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);

    private SelfCancelingTask() {
      super(null, Instant.now());
    }

    @Override
    public void run() {
      setStatus(TaskStatus.PROCESSING);
      started.countDown();
      try {
        release.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      cancel();
    }

    @Override
    public String getTaskDescription() {
      return "Canceling batch child";
    }

    @Override
    public double getFinishedPercentage() {
      return 0;
    }
  }
}
