/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.Solution;

class OptimizationBatchEvaluatorLifecycleTest {

  @org.junit.jupiter.api.BeforeAll static void initializeConfiguration() {
    io.github.mzmine.main.MZmineCore.getConfiguration().getPreferences();
  }

  @Test void closeDuringQueueConstructionPreventsAnyImportLaunch() throws Exception {
    final var evaluator = evaluator();
    final var builder = mock(WizardBatchBuilder.class);
    final var sequence = sequence(builder);
    final var building = new CountDownLatch(1);
    final var release = new CountDownLatch(1);
    final var outcome = new AtomicReference<Throwable>();
    final var factoryCalled = new AtomicBoolean();
    when(builder.createQueue()).thenAnswer(_ -> {
      building.countDown();
      assertTrue(release.await(5, TimeUnit.SECONDS));
      return importQueue();
    });
    final Thread worker = new Thread(() -> {
      try (final var batches = mockStatic(BatchModeModule.class)) {
        batches.when(() -> BatchModeModule.prepareIsolatedBatchTask(any(), any(), any()))
            .thenAnswer(_ -> { factoryCalled.set(true); return mock(BatchTask.class); });
        evaluator.evaluate(sequence, new Solution(0, 0), false, () -> 1);
      } catch (Throwable failure) {
        outcome.set(failure);
      }
    });
    try {
      worker.start();
      assertTrue(building.await(5, TimeUnit.SECONDS));
      evaluator.close();
      release.countDown();
      worker.join(5_000);
      assertFalse(worker.isAlive());
      assertInstanceOf(IllegalStateException.class, outcome.get());
      assertFalse(factoryCalled.get(), "Closing before task registration must prevent launch");
    } finally {
      release.countDown();
      worker.join(5_000);
      evaluator.close();
    }
  }

  @Test void cancellationReturnsBeforeChildExitAndCloseWaitsForTheChild() throws Exception {
    final var evaluator = evaluator();
    final var builder = mock(WizardBatchBuilder.class);
    final var sequence = sequence(builder);
    when(builder.createQueue()).thenAnswer(_ -> importQueue());
    final var started = new CountDownLatch(1);
    final var release = new CountDownLatch(1);
    final var canceled = new AtomicBoolean();
    final var outcome = new AtomicReference<Throwable>();
    final BatchTask child = mock(BatchTask.class);
    when(child.isCanceled()).thenAnswer(_ -> canceled.get());
    doAnswer(_ -> { canceled.set(true); return null; }).when(child).cancel();
    doAnswer(_ -> {
      started.countDown();
      assertTrue(release.await(5, TimeUnit.SECONDS));
      return null;
    }).when(child).run();
    final TaskController controller = mock(MZmineCore.getTaskController().getClass());
    when(controller.runTaskOnThisThreadBlocking(child)).thenAnswer(_ -> {
      child.run();
      return mock(WrappedTask.class);
    });
    final Thread worker = new Thread(() -> {
      try (final var batches = mockStatic(BatchModeModule.class);
          final var core = mockStatic(MZmineCore.class)) {
        core.when(MZmineCore::getTaskController).thenReturn(controller);
        batches.when(() -> BatchModeModule.prepareIsolatedBatchTask(any(), any(), any()))
            .thenReturn(child);
        evaluator.evaluate(sequence, new Solution(0, 0), false, () -> 1);
      } catch (Throwable failure) {
        outcome.set(failure);
      }
    });
    try (final var cleanup = Executors.newSingleThreadExecutor()) {
      try {
        worker.start();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        evaluator.cancel();
        assertTrue(canceled.get());
        assertTrue(worker.isAlive(), "Cancellation must return while the child is still draining");
        final var closeStarted = new CountDownLatch(1);
        final var close = cleanup.submit(() -> { closeStarted.countDown(); evaluator.close(); });
        assertTrue(closeStarted.await(5, TimeUnit.SECONDS));
        assertThrows(java.util.concurrent.TimeoutException.class,
            () -> close.get(100, TimeUnit.MILLISECONDS));
        release.countDown();
        worker.join(5_000);
        close.get(5, TimeUnit.SECONDS);
        assertFalse(worker.isAlive());
        assertInstanceOf(RuntimeException.class, outcome.get());
      } finally {
        release.countDown();
        worker.join(5_000);
        evaluator.close();
      }
    }
  }

  private static @NotNull OptimizationBatchEvaluator evaluator() {
    return new OptimizationBatchEvaluator(new MZmineProjectImpl(), new File[0], List.of(),
        new io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.MetricContext(List.of()), List.of(), new AtomicReference<>(TaskStatus.PROCESSING));
  }

  private static @NotNull WizardSequence sequence(final @NotNull WizardBatchBuilder builder) {
    final var sequence = mock(WizardSequence.class);
    final var workflow = mock(WorkflowDdaWizardParameters.class);
    final var factory = mock(WorkflowWizardParameterFactory.class);
    when(sequence.get(WizardPart.WORKFLOW)).thenReturn(Optional.of(workflow));
    when(sequence.get(WizardPart.DATA_IMPORT)).thenReturn(Optional.of(mock(DataImportWizardParameters.class)));
    when(workflow.getFactory()).thenReturn(factory);
    when(factory.getBatchBuilder(sequence)).thenReturn(builder);
    return sequence;
  }

  private static @NotNull BatchQueue importQueue() {
    final var queue = new BatchQueue();
    queue.add(new MZmineProcessingStepImpl<>(new AllSpectralDataImportModule(),
        new AllSpectralDataImportParameters()));
    return queue;
  }
}
