/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.io.import_rawdata_all;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

/**
 * Opens the native data-import setup dialog and retains an accepted selection without submitting
 * it. Callers can show their own review before creating and submitting the returned tasks.
 */
public final class NativeImportSelection {

  private NativeImportSelection() {
  }

  /**
   * Opens the standard import dialog. This method must be called on the JavaFX application thread.
   * Cancellation returns an empty result and never creates tasks.
   */
  public static @NotNull Optional<PreparedImport> showDialog() {
    final ParameterSet initial = ConfigService.getConfiguration()
        .getModuleParameters(AllSpectralDataImportModule.class).cloneParameterSet();
    return showDialog(initial);
  }

  /**
   * Opens the standard import dialog with a detached initial selection. This is useful for native
   * folder discovery while still letting the user review and edit every import setting.
   */
  public static @NotNull Optional<PreparedImport> showDialog(final @NotNull ParameterSet initial) {
    return showDialog(initial, _ -> {});
  }

  /** Retains the native import editor for a caller-controlled review handoff, without submitting. */
  public static @NotNull Optional<PreparedImport> showDialog(final @NotNull ParameterSet initial,
      final @NotNull java.util.function.Consumer<io.github.mzmine.parameters.dialogs.ParameterSetupDialog> reviewOpened) {
    if (!Platform.isFxApplicationThread()) {
      throw new IllegalStateException(
          "Native import selection must run on the JavaFX application thread.");
    }
    final ParameterSet dialogParameters = initial.cloneParameterSet();
    final ExitCode exitCode = dialogParameters instanceof AllSpectralDataImportParameters parameters
        ? parameters.showSetupDialog(true, reviewOpened) : dialogParameters.showSetupDialog(true);
    if (exitCode != ExitCode.OK) {
      return Optional.empty();
    }
    return Optional.of(new PreparedImport(dialogParameters));
  }

  /** A detached accepted import selection. It does not expose or submit tasks until requested. */
  public static final class PreparedImport {

    private final @NotNull ParameterSet parameters;

    private PreparedImport(final @NotNull ParameterSet parameters) {
      this.parameters = parameters.cloneParameterSet();
    }

    /** Returns a detached parameter copy for local review or persistence by the caller. */
    public @NotNull ParameterSet parameters() {
      return parameters.cloneParameterSet();
    }

    /**
     * Validates this selection and creates the regular import task(s), but does not submit them to
     * the task controller. The caller owns the explicit submission decision.
     */
    public @NotNull ImportTaskPreparation prepareTasks(final @NotNull MZmineProject project,
        final @NotNull Instant moduleCallDate) {
      return AllSpectralDataImportModule.prepareTasks(project, parameters, moduleCallDate);
    }
  }

  /** Result of creating import tasks without task-controller submission. */
  public record ImportTaskPreparation(@NotNull ExitCode exitCode, @NotNull List<Task> tasks) {

    public ImportTaskPreparation {
      tasks = List.copyOf(tasks);
    }

    /** True only when the standard import module accepted the selected input. */
    public boolean isReadyToSubmit() {
      return exitCode == ExitCode.OK && !tasks.isEmpty();
    }

    /** The project-bound main task, present only for a successful prepared native import. */
    public @NotNull Optional<AllSpectralDataImportMainTask> mainTask() {
      return tasks.stream().filter(AllSpectralDataImportMainTask.class::isInstance)
          .map(AllSpectralDataImportMainTask.class::cast).findFirst();
    }

    /** Actual raw files added by this prepared import, never files from another import or project. */
    public @NotNull List<RawDataFile> getImportedRawDataFiles() {
      return mainTask().map(AllSpectralDataImportMainTask::getImportedRawDataFiles)
          .orElse(List.of());
    }

    /** Measured native import counts, if this preparation created the project-bound main task. */
    public @NotNull Optional<AllSpectralDataImportMainTask.ImportOutcome> getImportOutcome() {
      return mainTask().map(AllSpectralDataImportMainTask::getImportOutcome);
    }
  }
}
