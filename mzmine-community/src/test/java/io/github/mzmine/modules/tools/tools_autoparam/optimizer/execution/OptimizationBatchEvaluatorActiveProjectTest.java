/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.modules.batchmode.BatchUtils;
import io.github.mzmine.modules.batchmode.timing.StepTimeMeasurement;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RTCorrectionParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ApplicationScope;
import io.github.mzmine.modules.tools.batchwizard.subparameters.CustomizationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ParameterOverride;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.MetricContext;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.moeaframework.core.Solution;
import testutils.MZmineTestUtil;

class OptimizationBatchEvaluatorActiveProjectTest {

  @ParameterizedTest
  @EnumSource(value = TaskStatus.class, names = {"FINISHED", "ERROR", "CANCELED"})
  void defaultUsesLiveQcMetadataAndCleansOnlyTrialLists(final @NotNull TaskStatus status) {
    MZmineTestUtil.startMzmineCore();
    final var manager = ProjectService.getProjectManager();
    final var previousProject = manager.getCurrentProject();
    final var project = new MZmineProjectImpl();
    final File path = new File("/tmp/171103_PMA_TK_QC_02.mzML");
    final RawDataFile raw = mock(RawDataFile.class);
    when(raw.getName()).thenReturn(path.getName());
    when(raw.getAbsoluteFilePath()).thenReturn(path);
    project.addFile(raw);
    final var existing = new ModularFeatureList("Existing analysis", null, raw);
    final var trial = new ModularFeatureList("Trial", null, raw);
    final var unrelated = new ModularFeatureList("Unrelated analysis", null, raw);
    project.addFeatureList(existing);
    final var sampleType = new StringMetadataColumn("mzmine_sample_type", "");
    project.getProjectMetadata().setValue(sampleType, raw, "QC");
    manager.setCurrentProject(project);

    final var importParameters = new AllSpectralDataImportParameters();
    importParameters.setParameter(AllSpectralDataImportParameters.extractMetadata, true);
    importParameters.setParameter(AllSpectralDataImportParameters.sortAndRecolor, true);
    importParameters.setParameter(AllSpectralDataImportParameters.metadataFile, true,
        new File("/tmp/stale-metadata.tsv"));
    final var queue = new BatchQueue();
    queue.add(new MZmineProcessingStepImpl<>(new AllSpectralDataImportModule(), importParameters));
    final var batch = mock(BatchTask.class);
    when(batch.getStatus()).thenReturn(status);
    when(batch.isCanceled()).thenReturn(status != TaskStatus.FINISHED);
    when(batch.getErrorMessage()).thenReturn("Original batch failure");
    when(batch.getLatestCreatedFeatureLists()).thenReturn(List.of(trial));
    when(batch.getResultFeatureLists()).thenReturn(List.of(trial));
    when(batch.getStepTimes()).thenReturn(List.of(new StepTimeMeasurement(0, 1d, "Batch", null)));
    final var selection = mock(FeatureListsSelection.class);
    when(selection.getMatchingFeatureLists()).thenReturn(new ModularFeatureList[]{trial});
    final var parameters = new RTCorrectionParameters();
    parameters.setParameter(RTCorrectionParameters.featureLists, selection);
    parameters.setParameter(RTCorrectionParameters.sampleTypes, SampleTypeFilter.qc());
    final TaskController controller = mock(MZmineCore.getTaskController().getClass());
    when(controller.runTaskOnThisThreadBlocking(batch)).thenAnswer(_ -> {
      project.addFeatureList(trial);
      project.addFeatureList(unrelated);
      // Exercise the global metadata lookup that failed for reimported private raw-file objects.
      final var errors = new ArrayList<String>();
      assertTrue(parameters.checkParameterValues(errors), errors.toString());
      return mock(WrappedTask.class);
    });

    try {
      try (final var batches = mockStatic(BatchModeModule.class);
          final var core = mockStatic(MZmineCore.class);
          final var evaluator = new OptimizationBatchEvaluator(project, new File[]{path}, List.of(),
              new MetricContext(List.of()), List.of(), new AtomicReference<>(TaskStatus.PROCESSING))) {
        core.when(MZmineCore::getTaskController).thenReturn(controller);
        core.when(() -> MZmineCore.getModuleInstance(AllSpectralDataImportModule.class))
            .thenReturn(new AllSpectralDataImportModule());
        batches.when(() -> BatchModeModule.prepareBatchTask(same(project), any(), any()))
            .thenAnswer(invocation -> {
              final BatchQueue prepared = invocation.getArgument(1);
              final var inputs = prepared.getFirst().getParameterSet();
              assertArrayEquals(new File[]{path},
                  inputs.getValue(AllSpectralDataImportParameters.fileNames),
                  "Analysis files must win over the wizard's import-file override");
              assertFalse(inputs.getValue(AllSpectralDataImportParameters.metadataFile),
                  "Trials must use the metadata already imported during preparation");
              return batch;
            });
        final var sequence = sequence(queue);
        final var customization = new CustomizationWizardParameters();
        final File[] otherFiles = {new File("/tmp/different-sample.mzML")};
        final var fileOverride = new ParameterOverride(
            AllSpectralDataImportModule.class.getName(), "Import",
            AllSpectralDataImportParameters.fileNames, otherFiles, ApplicationScope.ALL);
        customization.setParameter(CustomizationWizardParameters.enabled, true);
        customization.setParameter(CustomizationWizardParameters.overrides, List.of(fileOverride));
        when(sequence.get(WizardPart.CUSTOMIZATION)).thenReturn(Optional.of(customization));
        if (status == TaskStatus.FINISHED) {
          assertEquals(1, evaluator.evaluate(sequence, new Solution(0, 0), false, () -> 1));
        } else {
          final var failure = assertThrows(RuntimeException.class,
              () -> evaluator.evaluate(sequence, new Solution(0, 0), false, () -> 1));
          assertEquals(status == TaskStatus.ERROR
              ? "Batch optimization task failed: Original batch failure"
              : "Batch optimization task was canceled", failure.getMessage());
        }
        assertEquals(List.of(existing, unrelated), project.getCurrentFeatureLists());
        batches.verify(() -> BatchModeModule.prepareBatchTask(same(project), any(), any()));
        batches.verify(() -> BatchModeModule.prepareIsolatedBatchTask(any(), any(), any()), never());
        assertTrue(importParameters.getParameter(AllSpectralDataImportParameters.extractMetadata).getValue());
        assertTrue(importParameters.getValue(AllSpectralDataImportParameters.sortAndRecolor));
        assertArrayEquals(otherFiles, (File[]) fileOverride.parameterWithValue().getValue(),
            "Evaluation must not change the stored wizard override");
      }
      assertSame(project, ProjectService.getProject());
      assertEquals(List.of(existing, unrelated), project.getCurrentFeatureLists());
      assertEquals(List.of(raw), project.getCurrentRawDataFiles());
      assertEquals("QC", project.getProjectMetadata().getValue(sampleType, raw));
      verify(raw, never()).close();
    } finally {
      manager.setCurrentProject(previousProject);
    }
  }

  @ParameterizedTest
  @EnumSource(OptimizationBatchEvaluator.ProjectMode.class)
  void declinedOrderWarningDoesNotReserveOrLaunch(
      final @NotNull OptimizationBatchEvaluator.ProjectMode projectMode) {
    MZmineTestUtil.startMzmineCore();
    final var queue = importQueue();
    final var reserved = new AtomicInteger();
    try (final var warnings = mockStatic(BatchUtils.class);
        final var batches = mockStatic(BatchModeModule.class);
        final var evaluator = new OptimizationBatchEvaluator(new MZmineProjectImpl(),
            new File[]{new File("/tmp/selected.mzML")}, List.of(), new MetricContext(List.of()),
            List.of(), new AtomicReference<>(TaskStatus.PROCESSING), projectMode)) {
      warnings.when(() -> BatchUtils.confirmModuleOrderWarnings(queue)).thenReturn(false);

      final var failure = assertThrows(IllegalStateException.class,
          () -> evaluator.evaluate(sequence(queue), new Solution(0, 0), false,
              reserved::incrementAndGet));

      assertTrue(failure.getMessage().contains("processing-order warnings were declined"));
      assertEquals(0, reserved.get());
      warnings.verify(() -> BatchUtils.confirmModuleOrderWarnings(queue));
      batches.verifyNoInteractions();
    }
  }

  @ParameterizedTest
  @EnumSource(OptimizationBatchEvaluator.ProjectMode.class)
  void invalidImportLayoutDoesNotReserveOrLaunch(
      final @NotNull OptimizationBatchEvaluator.ProjectMode projectMode) {
    MZmineTestUtil.startMzmineCore();
    final var queue = importQueue();
    queue.addAll(importQueue());
    final var reserved = new AtomicInteger();
    try (final var warnings = mockStatic(BatchUtils.class);
        final var batches = mockStatic(BatchModeModule.class);
        final var evaluator = new OptimizationBatchEvaluator(new MZmineProjectImpl(),
            new File[]{new File("/tmp/selected.mzML")}, List.of(), new MetricContext(List.of()),
            List.of(), new AtomicReference<>(TaskStatus.PROCESSING), projectMode)) {
      final var failure = assertThrows(IllegalStateException.class,
          () -> evaluator.evaluate(sequence(queue), new Solution(0, 0), false,
              reserved::incrementAndGet));

      assertEquals("Could not set the optimization batch input files", failure.getMessage());
      assertEquals(0, reserved.get());
      warnings.verifyNoInteractions();
      batches.verifyNoInteractions();
    }
  }

  private static @NotNull BatchQueue importQueue() {
    final var queue = new BatchQueue();
    queue.add(new MZmineProcessingStepImpl<>(new AllSpectralDataImportModule(),
        new AllSpectralDataImportParameters()));
    return queue;
  }

  private static @NotNull WizardSequence sequence(final @NotNull BatchQueue queue) {
    final var sequence = mock(WizardSequence.class);
    final var workflow = mock(WorkflowDdaWizardParameters.class);
    final var factory = mock(WorkflowWizardParameterFactory.class);
    final var builder = new WizardBatchBuilder(sequence) {
      @Override
      protected @NotNull BatchQueue createQueueInternal() {
        return queue;
      }
    };
    when(sequence.get(WizardPart.WORKFLOW)).thenReturn(Optional.of(workflow));
    when(sequence.get(WizardPart.DATA_IMPORT)).thenReturn(Optional.of(mock(DataImportWizardParameters.class)));
    when(workflow.getFactory()).thenReturn(factory);
    when(factory.getBatchBuilder(sequence)).thenReturn(builder);
    return sequence;
  }
}
