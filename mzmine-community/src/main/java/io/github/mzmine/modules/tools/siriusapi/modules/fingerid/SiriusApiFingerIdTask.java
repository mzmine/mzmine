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

package io.github.mzmine.modules.tools.siriusapi.modules.fingerid;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.tools.siriusapi.SiriusJobWaiterTask;
import io.github.mzmine.modules.tools.siriusapi.MzmineToSirius;
import io.github.mzmine.modules.tools.siriusapi.Sirius;
import io.github.mzmine.modules.tools.siriusapi.SiriusToMzmine;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.FeatureTableFXUtil;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import io.mzio.users.user.CurrentUserService;
import io.sirius.ms.sdk.model.Job;
import io.sirius.ms.sdk.model.JobOptField;
import io.sirius.ms.sdk.model.JobSubmission;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.scene.control.ButtonType;
import javafx.scene.text.TextFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SiriusApiFingerIdTask extends AbstractFeatureListTask {

  public static final int FEATURE_COUNT_THRESHOLD = 50;
  private static final CloseableReentrantReadWriteLock optOutLock = new CloseableReentrantReadWriteLock();
  private final ModularFeatureList flist;
  private final String idStr;
  private SiriusJobWaiterTask jobWaiterTask = null;

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   * @param parameters
   * @param moduleClass
   */
  protected SiriusApiFingerIdTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    idStr = parameters.getOptionalValue(SiriusApiFingerIdParameters.rowIds).orElse("");
    flist = parameters.getValue(SiriusApiFingerIdParameters.flist).getMatchingFeatureLists()[0];
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  protected void process() {

    final List<FeatureListRow> rows =
        idStr.isBlank() ? flist.getRows() : FeatureUtils.idStringToRows(flist, idStr);

    if (!checkFeatureCount(rows)) {
      cancel();
      return;
    }

    try (Sirius sirius = new Sirius()) {
      final Map<Integer, String> idsMap = MzmineToSirius.exportToSiriusUnique(sirius, rows);

      final JobSubmission submission = sirius.api().jobs().getDefaultJobConfig(false, false, true);

      // polarity for fallback adducts
      // todo: set in a sirius preferences or so? Or just keep these as default adducts
      final PolarityType polarity = rows.stream()
          .map(r -> FeatureUtils.extractBestPolarity(r).orElse(null)).filter(Objects::nonNull)
          .findFirst().orElse(PolarityType.POSITIVE);
      switch (polarity) {
        case NEGATIVE -> submission.setFallbackAdducts(List.of("[M-H]-"));
        default -> submission.setFallbackAdducts(List.of("[M+H]+", "[M+NH4]+", "[M+Na]+"));
      }

      submission.setAlignedFeatureIds(idsMap.values().stream().toList());
      final Job job = sirius.api().jobs()
          .startJob(sirius.getProject().getProjectId(), submission, List.of(JobOptField.PROGRESS));

      jobWaiterTask = new SiriusJobWaiterTask(this, getModuleClass(), moduleCallDate, parameters,
          () -> sirius.api().jobs()
              .getJob(sirius.getProject().getProjectId(), job.getId(), List.of()),
          () -> SiriusToMzmine.importResultsForRows(sirius, rows));
      jobWaiterTask.run();

      FeatureTableFXUtil.updateCellsForFeatureList(flist);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean checkFeatureCount(List<FeatureListRow> rows) {

    if (rows.size() <= FEATURE_COUNT_THRESHOLD) {
      return true;
    }

    final HiddenParameter<Map<String, Boolean>> optOutParam = ConfigService.getConfiguration()
        .getPreferences().getParameter(MZminePreferences.siriusCountWarningOptOut);
    final String userHash = String.valueOf(CurrentUserService.getUserName().hashCode());

    try (var _ = optOutLock.lockRead()) {
      if (Boolean.TRUE.equals(optOutParam.getValue().get(userHash))) {
        return true;
      }
    }

    try (var _ = optOutLock.lockWrite()) {
      // might have changed on another thread
      if (Boolean.TRUE.equals(optOutParam.getValue().get(userHash))) {
        return true;
      }

      final TextFlow message = FxTextFlows.newTextFlow(FxTexts.text(
          "You are trying to compute %d features in Sirius. This may take a considerable amount of time. Do you want to continue?".formatted(
              rows.size())));
      final ButtonType result = DialogLoggerUtil.createAlertWithOptOutBlocking("Warning",
          "Confirmation required", message, "Do not show again", optedOut -> {
            optOutParam.getValue().put(userHash, optedOut);
          });

      if (result == ButtonType.YES) {
        return true;
      }

    }
    return false;
  }

  @Override
  public String getTaskDescription() {
    return jobWaiterTask == null ? "Waiting for Sirius Launch..."
        : "Running Sirius on feature list %s".formatted(flist.getName());
  }

  @Override
  public double getFinishedPercentage() {
    return jobWaiterTask == null ? 0 : jobWaiterTask.getFinishedPercentage();
  }

}
