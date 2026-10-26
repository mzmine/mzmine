/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.tools_autoparam.estimation;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.Preclassification;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationResult;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.RawDataPreparation;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Computes raw-file statistics and wizard estimates without running an optimization batch.
 */
public final class WizardParameterEstimationTask extends AbstractTask {

  private final File @NotNull [] files;
  private final @Nullable File metadataFile;
  private final @NotNull WizardSequence sequence;
  private final @Nullable Function<@NotNull PreclassificationResult, @Nullable ParameterSet>
      preclassificationResolution;
  private final @NotNull Predicate<@NotNull PresetSelection> presetConfirmation;
  private final @NotNull Consumer<WizardParameterEstimationResult> onFinished;
  private volatile double progress;
  private volatile @Nullable WizardParameterEstimationResult result;

  public @Nullable WizardParameterEstimationResult getResult() {
    return result;
  }

  /**
   * @param files              the files to import and pre-classify
   * @param preclassificationResolution resolves remaining user choices on the JavaFX thread, or
   *                                     null to reject unresolved choices
   * @param presetConfirmation called on the JavaFX thread with the presets that fit the raw data,
   *                           returns true to estimate for them, see
   *                           {@link ParameterEstimationContext#withFittingPresets}
   */
  public WizardParameterEstimationTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, File @NotNull [] files, @Nullable File metadataFile,
      @NotNull WizardSequence sequence,
      @Nullable Function<@NotNull PreclassificationResult, @Nullable ParameterSet>
          preclassificationResolution,
      @NotNull Predicate<@NotNull PresetSelection> presetConfirmation,
      @NotNull Consumer<WizardParameterEstimationResult> onFinished) {
    super(storage, moduleCallDate, "Estimate wizard parameters");
    this.files = files.clone();
    this.metadataFile = metadataFile;
    this.sequence = sequence;
    this.preclassificationResolution = preclassificationResolution;
    this.presetConfirmation = presetConfirmation;
    this.onFinished = onFinished;
  }

  /** Computes a detached proposal and rejects pre-classification choices without a resolver. */
  public WizardParameterEstimationTask(final @Nullable MemoryMapStorage storage,
      final @NotNull Instant moduleCallDate, final File @NotNull [] files,
      final @Nullable File metadataFile, final @NotNull WizardSequence sequence,
      final @NotNull Consumer<WizardParameterEstimationResult> onFinished) {
    this(storage, moduleCallDate, files, metadataFile, sequence, null, _ -> true, onFinished);
  }

  @Override
  public @NotNull String getTaskDescription() {
    return "Estimating wizard parameters from %d raw data file(s)".formatted(files.length);
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    if (isCanceled()) {
      return;
    }
    setStatus(TaskStatus.PROCESSING);
    if (TaskService.getController().isTaskInstanceRunningOrQueued(BatchTask.class)) {
      error("Cannot start estimation or optimization while another Batch is running.");
      return;
    }
    try {
      WizardParameterEstimationResult.requireSupportedWorkflow(sequence);
      final List<RawDataFile> importedFiles = RawDataPreparation.importFilesBlocking(files,
          metadataFile);
      progress = 0.35;
      if (isCanceled()) {
        return;
      }
      if (importedFiles.isEmpty()) {
        error("None of the selected wizard files could be imported.");
        return;
      }

      final ParameterSet resolved = resolvePreclassification(importedFiles);
      if (resolved == null || isCanceled()) {
        return;
      }
      final WizardParameterEstimationResult result = WizardParameterEstimationResult.estimate(
          importedFiles, null, sequence, resolved, this::confirmPresetsOnFxThread,
          getMemoryMapStorage(), this::isCanceled);
      if (result == null) {
        if (!isCanceled()) {
          cancel();
        }
        return;
      }
      if (isCanceled()) {
        return;
      }
      this.result = result;
      progress = 1d;
      FxThread.runLater(() -> {
        if (!isCanceled()) {
          onFinished.accept(result);
        }
      });
      setStatus(TaskStatus.FINISHED);
    } catch (Exception e) {
      error("Could not estimate wizard parameters: " + e.getMessage(), e);
    }
  }

  /**
   * Resolves classification on the task thread and dispatches only a remaining user choice to the
   * JavaFX thread. A null result means that the user cancelled the choice dialog.
   */
  private @Nullable ParameterSet resolvePreclassification(
      @NotNull List<@NotNull RawDataFile> importedFiles) {
    final PreclassificationResult classification = Preclassification.resolve(importedFiles,
        sequence);
    if (classification.hasConflicts()) {
      error("Cannot estimate parameters because acquisition settings conflict:\n"
          + classification.describeConflicts());
      return null;
    }
    if (!classification.needsUserChoice()) {
      return classification.parameters();
    }
    if (preclassificationResolution == null) {
      error("Cannot estimate parameters because acquisition settings require a user choice:\n"
          + String.join("\n", classification.choiceMessages()));
      return null;
    }

    final AtomicReference<@Nullable ParameterSet> resolved = new AtomicReference<>();
    FxThread.runOnFxThreadAndWait(() -> {
      if (!isCanceled()) {
        resolved.set(preclassificationResolution.apply(classification));
      }
    });
    if (resolved.get() == null && !isCanceled()) {
      cancel();
    }
    return resolved.get();
  }

  /**
   * Runs the confirmation on the JavaFX thread and waits on the task thread for the answer.
   */
  private boolean confirmPresetsOnFxThread(@NotNull PresetSelection presets) {
    final AtomicBoolean confirmed = new AtomicBoolean(false);
    FxThread.runOnFxThreadAndWait(() -> {
      if (!isCanceled()) {
        confirmed.set(presetConfirmation.test(presets));
      }
    });
    return confirmed.get();
  }

}
