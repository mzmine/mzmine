/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.batchmode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class BatchTaskFixedProjectTest {

  @ParameterizedTest
  @EnumSource(value = TaskStatus.class, names = {"FINISHED", "ERROR", "CANCELED"})
  void recordsOnlyOwnedResultsEvenWhenAStepDoesNotFinish(final @NotNull TaskStatus status) {
    MZmineCore.getConfiguration().getPreferences();
    final var project = new MZmineProjectImpl();
    final var existing = new ModularFeatureList("Existing analysis", null, List.of());
    final var partial = new ModularFeatureList("Partial trial output", null, List.of());
    final var unrelated = new ModularFeatureList("Unrelated analysis", null, List.of());
    project.addFeatureList(existing);
    final var module = Mockito.mock(MZmineProcessingModule.class);
    Mockito.when(module.getName()).thenReturn("Trial step");
    Mockito.when(module.getModuleCategory()).thenReturn(MZmineModuleCategory.OTHER_DATA_PROCESSING);
    Mockito.when(module.runModule(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenAnswer(invocation -> {
          final ParameterSet stepParameters = invocation.getArgument(1);
          final Collection<Task> tasks = invocation.getArgument(2);
          final Instant callDate = invocation.getArgument(3);
          tasks.add(new AbstractTask(null, callDate) {
            @Override
            public void run() {
              partial.addDescriptionOfAppliedTask(
                  new SimpleFeatureListAppliedMethod(module, stepParameters, callDate));
              project.addFeatureList(partial);
              project.addFeatureList(unrelated);
              if (status == TaskStatus.ERROR) {
                error("Expected failure after publishing a partial result");
              } else {
                setStatus(status);
              }
            }

            @Override
            public @NotNull String getTaskDescription() {
              return "Trial producing a partial result";
            }

            @Override
            public double getFinishedPercentage() {
              return 1;
            }
          });
          return ExitCode.OK;
        });
    final var parameters = new BatchModeParameters();
    parameters.setParameter(BatchModeParameters.batchQueue, queue(module));
    final var batch = BatchTask.forFixedProject(project, parameters, Instant.now());
    final TaskController controller = Mockito.mock(MZmineCore.getTaskController().getClass());
    Mockito.when(controller.addTasks(Mockito.any(Task[].class))).thenAnswer(invocation ->
        Arrays.stream(invocation.<Task[]>getArgument(0)).map(task -> {
          final var wrapped = new WrappedTask(task, TaskPriority.NORMAL);
          wrapped.run();
          return wrapped;
        }).toArray(WrappedTask[]::new));
    try (final var projects = Mockito.mockStatic(ProjectService.class);
        final var taskService = Mockito.mockStatic(TaskService.class)) {
      projects.when(ProjectService::getProject).thenReturn(project);
      taskService.when(TaskService::getController).thenReturn(controller);
      batch.run();
    }

    assertEquals(status, batch.getStatus());
    assertEquals(List.of(partial), batch.getResultFeatureLists());
    project.removeFeatureLists(batch.getResultFeatureLists());
    assertEquals(List.of(existing, unrelated), project.getCurrentFeatureLists());
  }

  @Test
  void fixedProjectTaskCancelsBeforeStartingAgainstAnotherProject() {
    final MZmineProject reviewedProject = Mockito.mock(MZmineProject.class);
    final BatchModeParameters parameters = new BatchModeParameters();
    parameters.getParameter(BatchModeParameters.batchQueue).setValue(new BatchQueue());
    final BatchTask task = BatchTask.forFixedProject(reviewedProject, parameters, Instant.now());

    task.run();

    assertEquals(TaskStatus.CANCELED, task.getStatus());
    assertEquals("The reviewed project changed before the batch could continue.",
        task.getErrorMessage());
    assertTrue(task.getResultFeatureLists().isEmpty());
  }

  @Test
  void isolatedProjectTaskDoesNotReadOrReplaceTheCurrentProject() {
    final MZmineProject isolatedProject = Mockito.mock(MZmineProject.class);
    final BatchTask task = BatchTask.forIsolatedProject(isolatedProject, new BatchQueue(),
        Instant.now());

    try (MockedStatic<ProjectService> projects = Mockito.mockStatic(ProjectService.class)) {
      task.run();
      projects.verifyNoInteractions();
    }

    assertEquals(TaskStatus.FINISHED, task.getStatus());
  }

  @Test
  void isolatedProjectTaskDispatchesRepeatedMutationsOnlyToItsPrivateProject() {
    final MZmineProjectImpl sourceProject = new MZmineProjectImpl();
    sourceProject.addFile(rawFile("source.mzML"));
    final MZmineProjectImpl isolatedProject = new MZmineProjectImpl();
    final AtomicInteger mutations = new AtomicInteger();
    final MutationModule module = new MutationModule(isolatedProject, mutations);

    try (MockedStatic<ProjectService> projects = Mockito.mockStatic(ProjectService.class)) {
      projects.when(ProjectService::getProject).thenReturn(sourceProject);
      BatchTask.forIsolatedProject(isolatedProject, queue(module), Instant.now()).run();
      BatchTask.forIsolatedProject(isolatedProject, queue(module), Instant.now()).run();
      projects.verify(ProjectService::getProject, Mockito.never());
    }

    assertEquals(2, mutations.get());
    assertEquals(2, isolatedProject.getNumberOfDataFiles());
    assertEquals(1, sourceProject.getNumberOfDataFiles());
  }

  private static @NotNull BatchQueue queue(final @NotNull MZmineProcessingModule module) {
    final ParameterSet parameters = Mockito.mock(ParameterSet.class);
    Mockito.when(parameters.getParameters()).thenReturn(new io.github.mzmine.parameters.Parameter<?>[0]);
    Mockito.when(parameters.checkParameterValues(Mockito.any())).thenReturn(true);
    Mockito.when(parameters.cloneParameterSet(Mockito.anyBoolean())).thenReturn(parameters);
    final BatchQueue queue = new BatchQueue();
    queue.add(new MZmineProcessingStepImpl<>(module, parameters));
    return queue;
  }

  private static @NotNull RawDataFile rawFile(final @NotNull String name) {
    final RawDataFile file = Mockito.mock(RawDataFile.class);
    Mockito.when(file.getName()).thenReturn(name);
    Mockito.when(file.getAbsoluteFilePath()).thenReturn(new java.io.File("/tmp/" + name));
    return file;
  }

  private record MutationModule(@NotNull MZmineProject expectedProject,
                                @NotNull AtomicInteger mutations) implements MZmineProcessingModule {

    @Override
    public @NotNull String getName() {
      return "Private project mutation";
    }

    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {
      return null;
    }

    @Override
    public @NotNull String getDescription() {
      return "Adds a raw data file to the supplied project.";
    }

    @Override
    public @NotNull ExitCode runModule(@NotNull MZmineProject project,
        @NotNull ParameterSet parameters, @NotNull Collection<io.github.mzmine.taskcontrol.Task> tasks,
        @NotNull Instant moduleCallDate) {
      assertEquals(expectedProject, project);
      project.addFile(rawFile("isolated-%d.mzML".formatted(mutations.incrementAndGet())));
      return ExitCode.OK;
    }

    @Override
    public @NotNull MZmineModuleCategory getModuleCategory() {
      return MZmineModuleCategory.OTHER_DATA_PROCESSING;
    }
  }
}
