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

package io.github.mzmine.modules.visualization.dash_lipidqc.quality;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.javafx.mvci.FxInteractor;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_lipidannotationcleanup.LipidAnnotationCleanupModule;
import io.github.mzmine.modules.dataprocessing.filter_lipidannotationcleanup.LipidAnnotationCleanupParameters;
import io.github.mzmine.modules.dataprocessing.filter_lipidpreferredlevel.SetLipidAnnotationLevelModule;
import io.github.mzmine.modules.dataprocessing.filter_lipidpreferredlevel.SetLipidAnnotationLevelParameters;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnalysisType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnnotationUtils;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickFalseNegativeCandidate;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interactor for the annotation quality MVCI component. Handles all annotation mutation operations
 * (delete, add false negative, multi-row cleanup, preferred level updates) and delegates GUI
 * refresh to the controller.
 */
class AnnotationQualityInteractor extends FxInteractor<AnnotationQualityModel> {

  private final AnnotationQualityController controller;

  AnnotationQualityInteractor(final @NotNull AnnotationQualityModel model,
      final @NotNull AnnotationQualityController controller) {
    super(model);
    this.controller = controller;
  }

  @Override
  public void updateModel() {
    // Not used — updates are triggered via QualityComputationTask
  }

  void deleteAnnotationWithConfirmation(final @NotNull MatchedLipid match) {
    final @Nullable FeatureListRow row = model.getRow();
    if (row == null) {
      return;
    }
    final String annotation = Objects.toString(match.getLipidAnnotation().getAnnotation(),
        "Unknown annotation");
    final String message =
        "Delete annotation \"" + annotation + "\" from selected row #" + row.getID() + "?";
    final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES,
        ButtonType.NO);
    alert.setTitle("Confirm annotation deletion");
    alert.setHeaderText("Delete annotation");
    final Optional<ButtonType> result = alert.showAndWait();
    if (result.isEmpty() || !ButtonType.YES.equals(result.get())) {
      return;
    }
    final List<MatchedLipid> remaining = row.getLipidMatches().stream()
        .filter(existingMatch -> !Objects.equals(existingMatch, match)).toList();
    row.setLipidAnnotations(remaining);
    controller.refreshAfterAnnotationDelete(row);
  }

  void addFalseNegativeCandidate(final @NotNull KendrickFalseNegativeCandidate candidate) {
    final @Nullable FeatureListRow row = model.getRow();
    if (row == null) {
      return;
    }
    final @NotNull ModularFeatureList featureList = model.getFeatureList();
    final Set<MatchedLipid> annotationsToAdd = new HashSet<>();
    annotationsToAdd.add(candidate.match());
    final LipidAnalysisType analysisType = Objects.requireNonNullElse(
        LipidQcScoringUtils.detectLipidAnalysisType(featureList),
        LipidAnalysisType.LC_REVERSED_PHASE);
    LipidAnnotationUtils.addAnnotationsToFeatureList(row, annotationsToAdd, analysisType, false, 0d,
        null, LipidQcScoringUtils.detectMs1Tolerance(featureList));
    controller.refreshAfterAnnotationDelete(row);
  }

  void removeMultiRowAnnotations() {
    final ParameterSet parameters = ConfigService.getConfiguration()
        .getModuleParameters(LipidAnnotationCleanupModule.class).cloneParameterSet();
    parameters.getParameter(LipidAnnotationCleanupParameters.featureLists)
        .setValue(new FeatureListsSelection(model.getFeatureList()));
    parameters.setParameter(LipidAnnotationCleanupParameters.lipidAnalysisType,
        Objects.requireNonNullElse(
            LipidQcScoringUtils.detectLipidAnalysisType(model.getFeatureList()),
            LipidAnalysisType.LC_REVERSED_PHASE));
    if (parameters.showSetupDialog(true) != ExitCode.OK) {
      return;
    }
    final FeatureListRow currentRow = model.getRow();
    final List<Task> tasks = MZmineCore.runMZmineModule(LipidAnnotationCleanupModule.class,
        parameters);
    tasks.forEach(task -> task.setOnFinished(
        () -> Platform.runLater(() -> controller.refreshAfterAnnotationDelete(currentRow))));
  }

  void setPreferredLipidAnnotationLevel() {
    final ParameterSet parameters = ConfigService.getConfiguration()
        .getModuleParameters(SetLipidAnnotationLevelModule.class).cloneParameterSet();
    parameters.getParameter(SetLipidAnnotationLevelParameters.flists)
        .setValue(new FeatureListsSelection(model.getFeatureList()));
    if (parameters.showSetupDialog(true) != ExitCode.OK) {
      return;
    }
    final @Nullable FeatureListRow currentRow = model.getRow();
    final List<Task> tasks = MZmineCore.runMZmineModule(SetLipidAnnotationLevelModule.class,
        parameters);
    tasks.forEach(task -> task.setOnFinished(
        () -> Platform.runLater(() -> controller.refreshAfterAnnotationDelete(currentRow))));
  }

  void removeCurrentRowAnnotations() {
    final @Nullable FeatureListRow currentRow = model.getRow();
    if (currentRow == null) {
      return;
    }
    currentRow.setLipidAnnotations(List.of());
    controller.refreshAfterAnnotationDelete(currentRow);
  }

  void removeOtherDuplicateRows(final @NotNull MatchedLipid selectedAnnotation) {
    final String annotation = Objects.toString(
        selectedAnnotation.getLipidAnnotation().getAnnotation(), "");
    final @Nullable FeatureListRow row = model.getRow();
    final int selectedRowId = row != null ? row.getID() : -1;
    final List<FeatureListRow> rowsToUpdate = QualityComputationTask.findDuplicateRowsExcludingSelected(
        model.getFeatureList(), selectedRowId, annotation);
    for (final FeatureListRow sameAnnotationRow : rowsToUpdate) {
      sameAnnotationRow.setLipidAnnotations(List.of());
    }
    controller.refreshAfterAnnotationDelete(row);
  }

  void navigateToRow(final @NotNull FeatureListRow row) {
    final @Nullable Consumer<@Nullable FeatureListRow> reselectCallback = model.getOnReselectRow();
    if (reselectCallback != null) {
      reselectCallback.accept(row);
    }
  }
}
