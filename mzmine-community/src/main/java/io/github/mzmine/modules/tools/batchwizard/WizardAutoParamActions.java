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

package io.github.mzmine.modules.tools.batchwizard;

import static io.github.mzmine.modules.tools.batchwizard.WizardPart.DATA_IMPORT;

import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.batchwizard.WizardParameterChanges.Source;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.DataFileStatisticsDashboardPane;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.WizardParameterEstimationResult;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.WizardParameterEstimationTask;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.BatchOptimizationMainTask;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.OptimizerModule;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.OptimizerParameters;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.SimpleOptimizerModule;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.SimpleOptimizerParameters;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.Preclassification;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationParameters;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationResult;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.RawDataPreclassificationTask;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.RawDataPreparation;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import io.mzio.users.user.CurrentUserService;
import java.io.File;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.ButtonType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parameter estimation and optimization started from the {@link BatchWizardTab}: pre-classifies the
 * representative files, runs the background tasks, and applies the results to the wizard.
 */
final class WizardAutoParamActions {

  private static final String ESTIMATE_ERROR = "Cannot estimate parameters";
  private static final String OPTIMIZE_ERROR = "Cannot optimize parameters";
  /**
   * The optimization compares feature lists across files.
   */
  private static final int MIN_OPTIMIZER_FILES = 3;

  private final @NotNull BatchWizardTab wizard;
  /**
   * Number of running pre-classification, estimation, and optimization tasks, see
   * {@link #startTask(AbstractTask, String)}.
   */
  private final SimpleIntegerProperty runningTasks = new SimpleIntegerProperty(0);

  WizardAutoParamActions(@NotNull BatchWizardTab wizard) {
    this.wizard = wizard;
  }

  /**
   * Disables estimation and optimization while a task runs. The parameter estimation needs
   * chromatographic traces, so spatial imaging (MALDI, LDI, DESI, SIMS) is not supported.
   */
  @NotNull BooleanBinding createDisabledBinding(
      @NotNull ReadOnlyObjectProperty<WizardStepParameters> selectedIonInterface) {
    final BooleanBinding imagingSelected = Bindings.createBooleanBinding(
        () -> selectedIonInterface.get() != null && selectedIonInterface.get()
            .getFactory() instanceof IonInterfaceWizardParameterFactory ionInterface
            && ionInterface.isImaging(), selectedIonInterface);
    return runningTasks.greaterThan(0).or(imagingSelected);
  }

  /**
   * @param showStatistics opens the data file statistics dashboard after applying the estimates
   */
  void estimate(final boolean showStatistics) {
    final String noWarningKey =
        CurrentUserService.getUserName().orElse("no-user") + "-no-estimate-warning";
    final boolean optedOut = Objects.requireNonNullElse(
        ConfigService.getPreference(MZminePreferences.otherOptOutWarnings).get(noWarningKey),
        false);
    if (!optedOut) {
      ButtonType clicked = DialogLoggerUtil.createAlertWithOptOutBlocking("Information",
          "Estimation and optimization will apply processing and alter wizard settings.",
          FxTextFlows.newTextFlow(FxTexts.text("""
              Running parameter estimation or optimization will apply multiple mass detection steps to imported and already imported raw data.
              
              - Present mass detection results will be overridden.
              - Current wizard settings will be altered as a result of estimation
              - Present parameter customisation in the advanced mode will be dropped""")),
          "Don't show again",
          optOutSelected -> ConfigService.getPreference(MZminePreferences.otherOptOutWarnings)
              .put(noWarningKey, optOutSelected));
      if (clicked == ButtonType.NO) {
        return;
      }
    }

    if (runningTasks.get() > 0) {
      return;
    }
    final InputFiles input = selectInputFiles(ESTIMATE_ERROR, 1);
    if (input == null) {
      return;
    }
    preclassify(input, ESTIMATE_ERROR,
        preclassification -> startEstimation(input, preclassification, showStatistics));
  }

  /**
   * @param advancedSettings shows the full optimizer setup instead of the simple one that only
   *                         selects the parameters to optimize
   */
  void optimize(final boolean advancedSettings) {
    if (runningTasks.get() > 0) {
      return;
    }
    final InputFiles input = selectInputFiles(OPTIMIZE_ERROR, MIN_OPTIMIZER_FILES);
    if (input == null) {
      return;
    }
    // decision: set up before the pre-classification, so cancelling does not import the files.
    // The setup only depends on the wizard presets, which the pre-classification does not change.
    final OptimizerParameters optimizerParam = setupOptimizer(advancedSettings);
    if (optimizerParam == null) {
      return;
    }
    preclassify(input, OPTIMIZE_ERROR,
        preclassification -> startOptimizer(input, preclassification, optimizerParam));
  }

  /**
   * The representative files of the data import step, see
   * {@link RawDataPreparation#selectOptimizerInputFiles(File[])}. Selected once, so the random
   * selection does not change between pre-classification and run.
   *
   * @return the files, null after showing an error if there are too few
   */
  private @Nullable InputFiles selectInputFiles(@NotNull String errorTitle, int minFiles) {
    wizard.updateAllParametersFromUi();
    final WizardStepParameters importStep = wizard.getSequence().get(DATA_IMPORT).orElse(null);
    if (importStep == null) {
      DialogLoggerUtil.showErrorDialog(errorTitle, "The wizard has no data import step.");
      return null;
    }
    final File[] allFiles = importStep.getValue(DataImportWizardParameters.fileNames);
    if (allFiles == null || allFiles.length == 0) {
      DialogLoggerUtil.showErrorDialog(errorTitle,
          "Select at least one raw data file in the Data Import step first.");
      return null;
    }
    final File[] files = RawDataPreparation.selectOptimizerInputFiles(allFiles);
    if (files.length < minFiles) {
      DialogLoggerUtil.showErrorDialog(errorTitle, """
          This requires at least %d raw data files. Those data files need to be comparable (same \
          instrument, same method, similar or same sample).""".formatted(minFiles));
      return null;
    }
    final File metadataFile = importStep.getOptionalValue(DataImportWizardParameters.metadataFile)
        .orElse(null);
    return new InputFiles(files, metadataFile);
  }

  /**
   * Imports and pre-classifies the files in a background task, see {@link Preclassification}.
   * Conflicts are shown as an error and stop the run, choices are asked in a dialog. Then continues
   * on the JavaFX thread with the decided settings.
   *
   * @param errorTitle   title of error dialogs
   * @param continuation receives the settings fixed by the pre-classification, see
   *                     {@link PreclassificationParameters}
   */
  private void preclassify(@NotNull InputFiles input, @NotNull String errorTitle,
      @NotNull Consumer<@NotNull ParameterSet> continuation) {
    // a copy, so the task never reads the live wizard off the JavaFX thread
    final RawDataPreclassificationTask task = new RawDataPreclassificationTask(
        MemoryMapStorage.forRawDataFile(), Instant.now(), input.files(), input.metadataFile(),
        wizard.getSequence().copy(), resolution -> {
      final ParameterSet preclassification = resolvePreclassification(resolution, errorTitle);
      if (preclassification != null) {
        continuation.accept(preclassification);
      }
    });
    startTask(task, errorTitle);
  }

  /**
   * @return the decided settings, or null if there was a conflict or the user cancelled
   */
  private @Nullable ParameterSet resolvePreclassification(@NotNull PreclassificationResult resolved,
      @NotNull String errorTitle) {
    if (resolved.hasConflicts()) {
      DialogLoggerUtil.showErrorDialog(errorTitle, resolved.describeConflicts());
      return null;
    }
    if (!resolved.needsUserChoice()) {
      return resolved.parameters();
    }
    // decision: the dialog only shows the parameters that need a choice. They are the same
    // instances as in the decided settings, so the dialog changes those
    final ParameterSet choices = new SimpleParameterSet(
        resolved.choiceParameters().toArray(new Parameter<?>[0]));
    final ParameterSetupDialog dialog = new ParameterSetupDialog(true, choices,
        FxTextFlows.newTextFlow(FxTexts.text(String.join("\n\n", resolved.choiceMessages()))));
    dialog.showAndWait();
    return dialog.getExitCode() == ExitCode.OK ? resolved.parameters() : null;
  }

  /**
   * @param preclassification the settings fixed by the pre-classification, see
   *                          {@link PreclassificationParameters}
   */
  private void startEstimation(@NotNull InputFiles input, @NotNull ParameterSet preclassification,
      final boolean showStatistics) {
    // keep edits made while the pre-classification was running
    wizard.updateAllParametersFromUi();
    final WizardParameterEstimationTask task = new WizardParameterEstimationTask(
        MemoryMapStorage.forRawDataFile(), Instant.now(), input.files(), input.metadataFile(),
        wizard.getSequence().copy(), preclassification, wizard::confirmAndSwitchPresets,
        result -> applyEstimation(result, showStatistics));
    startTask(task, ESTIMATE_ERROR);
  }

  private void applyEstimation(@NotNull WizardParameterEstimationResult result,
      final boolean showStatistics) {
    wizard.applyParameterValues(sequence -> result.estimates().applyEstimates(sequence),
        Source.ESTIMATION);
    if (!showStatistics) {
      return;
    }
    MZmineCore.getDesktop().addTab(new SimpleTab("Data File Statistics",
        new DataFileStatisticsDashboardPane(result.statistics(), result.interSampleRtStatistics(),
            result.context().massDetectorType())));
  }

  /**
   * Starts the optimization after the pre-classification.
   *
   * @param preclassification the settings fixed by the pre-classification, see
   *                          {@link PreclassificationParameters}
   * @param optimizerParam    the optimizer settings, see {@link #setupOptimizer(boolean)}
   */
  private void startOptimizer(@NotNull InputFiles input, @NotNull ParameterSet preclassification,
      @NotNull OptimizerParameters optimizerParam) {
    // keep edits made while the pre-classification was running
    wizard.updateAllParametersFromUi();

    final BatchOptimizationMainTask optimizer = new BatchOptimizationMainTask(
        MemoryMapStorage.forRawDataFile(), Instant.now(), input.files(), input.metadataFile(),
        wizard, optimizerParam, preclassification);
    // tracked like estimation, so no second run writes into the wizard and errors are shown
    startTask(optimizer, OPTIMIZE_ERROR);
  }

  /**
   * Shows the optimizer setup. The wizard sequence limits the parameter checklist to the parameters
   * that apply to its presets.
   *
   * @param advancedSettings shows the full setup, otherwise the simple setup that only selects the
   *                         parameters to optimize and uses defaults for all other settings
   * @return the full parameters, independent of the module configuration, so later edits do not
   * change the running optimization. null if the user cancelled.
   */
  private @Nullable OptimizerParameters setupOptimizer(final boolean advancedSettings) {
    if (advancedSettings) {
      final OptimizerParameters fullParam = (OptimizerParameters) ConfigService.getConfiguration()
          .getModuleParameters(OptimizerModule.class);
      if (fullParam.showSetupDialog(true, wizard.getSequence()) != ExitCode.OK) {
        return null;
      }
      return (OptimizerParameters) fullParam.cloneParameterSet();
    }

    final SimpleOptimizerParameters simpleParam = (SimpleOptimizerParameters) ConfigService.getConfiguration()
        .getModuleParameters(SimpleOptimizerModule.class);
    if (simpleParam.showSetupDialog(true, wizard.getSequence()) != ExitCode.OK) {
      return null;
    }
    return OptimizerParameters.create(simpleParam);
  }

  /**
   * Starts a pre-classification, estimation, or optimization task. The estimate and optimize
   * buttons are disabled while any of them runs, errors are shown in a dialog.
   */
  private void startTask(@NotNull AbstractTask task, @NotNull String errorTitle) {
    // decision: a counter instead of a flag. A task hands its result to the JavaFX thread before
    // it finishes, so the next task may already run when the previous one reports its end
    runningTasks.set(runningTasks.get() + 1);
    task.addTaskStatusListener((_, newStatus, _) -> {
      if (!newStatus.isUnmodifiable()) {
        return;
      }
      Platform.runLater(() -> {
        runningTasks.set(runningTasks.get() - 1);
        if (newStatus == TaskStatus.ERROR) {
          DialogLoggerUtil.showErrorDialog(errorTitle, task.getErrorMessage());
        }
      });
    });
    TaskService.getController().addTask(task);
  }

  public IntegerProperty runningTasksProperty() {
    return runningTasks;
  }

  /**
   * @param files        the representative raw data files
   * @param metadataFile the metadata file of the data import step, or null
   */
  private record InputFiles(File @NotNull [] files, @Nullable File metadataFile) {

  }
}
