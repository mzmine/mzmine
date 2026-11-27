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
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.InterferenceMetrics;
import io.github.mzmine.modules.visualization.dash_lipidqc.kendrick.KendrickFalseNegativeCandidate;
import java.util.List;
import java.util.Objects;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * View builder for the annotation quality MVCI component. Subscribes to
 * {@link AnnotationQualityModel#qualityResultProperty()} and rebuilds quality cards on each
 * update.
 */
class AnnotationQualityViewBuilder extends FxViewBuilder<AnnotationQualityModel> {

  private static final double ISOMER_EXACT_MASS_TOLERANCE = 1e-6d;
  private static final double OVERALL_SCORE_TIE_TOLERANCE = 1e-6d;

  private final AnnotationQualityInteractor interactor;

  AnnotationQualityViewBuilder(final @NotNull AnnotationQualityModel model,
      final @NotNull AnnotationQualityInteractor interactor) {
    super(model);
    this.interactor = interactor;
  }

  @Override
  public Region build() {
    final VBox content = FxLayout.newVBox();
    content.getStyleClass().add("quality-content");
    content.setFillWidth(true);
    content.setMaxWidth(Double.MAX_VALUE);
    final ScrollPane scrollPane = new ScrollPane(content);
    scrollPane.setFitToWidth(true);

    final BorderPane rootPane = new BorderPane();
    final var placeholder = FxLabels.newLabel("Select a row with lipid annotations.");
    rootPane.setCenter(placeholder);
    BorderPane.setAlignment(placeholder, Pos.CENTER);

    final Button removeMultiRowAnnotationsButton = FxButtons.createButton(
        "Remove multi-row annotations", interactor::removeMultiRowAnnotations);
    final Button setPreferredAnnotationLevelButton = FxButtons.createButton(
        "Set preferred annotation level (global)", interactor::setPreferredLipidAnnotationLevel);
    final FlowPane actionBox = FxLayout.newFlowPane(Pos.TOP_LEFT, removeMultiRowAnnotationsButton,
        setPreferredAnnotationLevelButton);
    final TitledPane actionsPane = new TitledPane("Annotation actions", actionBox);
    actionsPane.setCollapsible(true);
    final Accordion actionsAccordion = new Accordion(actionsPane);
    actionsAccordion.setExpandedPane(null);
    rootPane.setBottom(actionsAccordion);

    model.qualityResultProperty().subscribe(
        result -> applyQualityResult(result, content, scrollPane, rootPane, placeholder));

    return rootPane;
  }

  private void applyQualityResult(final @Nullable QualityComputationResult result,
      final @NotNull VBox content, final @NotNull ScrollPane scrollPane,
      final @NotNull BorderPane rootPane, final @NotNull Label placeholder) {
    if (result == null || result.placeholderText() != null) {
      final String text =
          result != null ? result.placeholderText() : "Select a row with lipid annotations.";
      placeholder.setText(text);
      rootPane.setCenter(placeholder);
      BorderPane.setAlignment(placeholder, Pos.CENTER);
      return;
    }
    content.getChildren().clear();
    final InterferenceMetrics interferenceMetrics = result.interferenceMetrics();
    if (interferenceMetrics.totalPenaltyCount() > 0) {
      content.getChildren().add(warningLabel(
          "Potential interference: " + LipidQcScoringUtils.interferenceDetail(
              interferenceMetrics)));
    }
    if (result.falsePositiveReason() != null && !result.falsePositiveReason().isBlank()) {
      content.getChildren()
          .add(warningLabel("Potential false positive: " + result.falsePositiveReason()));
    }
    final @Nullable String isomerScoreTieWarning = detectIsomerScoreTieWarning(
        result.qualityCards());
    if (isomerScoreTieWarning != null) {
      content.getChildren().add(warningLabel(isomerScoreTieWarning));
    }
    if (result.falseNegativeCandidate() != null && result.falseNegativeQualityCard() != null) {
      content.getChildren().add(
          warningLabel("Potential false negative: " + result.falseNegativeCandidate().detail()));
      content.getChildren().add(createPotentialFalseNegativeCard(result.falseNegativeCandidate(),
          result.falseNegativeQualityCard()));
    }
    final @Nullable Region duplicateRowsAlert = result.selectedAnnotation() == null ? null
        : createDuplicateRowsAlert(result.selectedAnnotation(), result.duplicateRows(), content);
    if (duplicateRowsAlert != null) {
      content.getChildren().add(duplicateRowsAlert);
    }
    for (final QualityCardData qualityCardData : result.qualityCards()) {
      content.getChildren().add(createQualityCard(qualityCardData));
    }
    rootPane.setCenter(scrollPane);
  }

  private @NotNull Region createQualityCard(final @NotNull QualityCardData qualityCardData) {
    final MatchedLipid match = qualityCardData.match();
    final Button deleteButton = FxButtons.createButton("Delete annotation",
        () -> interactor.deleteAnnotationWithConfirmation(match));
    return LipidQualityCardUtils.createQualityCard(qualityCardData,
        model.isRetentionTimeAnalysisEnabled(),
        LipidQualityCardUtils.createCardHeader(LipidQualityCardUtils.formatCardTitle(match),
            deleteButton));
  }

  private @NotNull Region createPotentialFalseNegativeCard(
      final @NotNull KendrickFalseNegativeCandidate candidate,
      final @NotNull QualityCardData qualityCardData) {
    final Button convertButton = FxButtons.createButton("Add annotation",
        () -> interactor.addFalseNegativeCandidate(candidate));
    return LipidQualityCardUtils.createQualityCard(qualityCardData,
        model.isRetentionTimeAnalysisEnabled(), LipidQualityCardUtils.createCardHeader(
            "Potentially missed annotation: " + LipidQualityCardUtils.formatCardTitle(
                candidate.match()), convertButton));
  }

  private @Nullable Region createDuplicateRowsAlert(final @NotNull MatchedLipid selectedAnnotation,
      final @NotNull List<FeatureListRow> duplicateRows, final @NotNull VBox content) {
    if (duplicateRows.isEmpty()) {
      return null;
    }
    final FlowPane rowLinks = new FlowPane(4, 4);
    rowLinks.prefWrapLengthProperty().bind(content.widthProperty().subtract(36));
    rowLinks.getChildren().add(FxLabels.newLabel("Same annotation rows:"));
    for (final FeatureListRow duplicate : duplicateRows) {
      rowLinks.getChildren().add(FxLabels.newHyperlink(() -> interactor.navigateToRow(duplicate),
          "#" + duplicate.getID()));
    }
    final Button deleteSelectedRowButton = FxButtons.createButton("Delete selected",
        interactor::removeCurrentRowAnnotations);
    final Button deleteAllSameAnnotationRowsButton = FxButtons.createButton("Delete others",
        () -> interactor.removeOtherDuplicateRows(selectedAnnotation));
    final FlowPane actionButtons = new FlowPane(6, 6, deleteSelectedRowButton,
        deleteAllSameAnnotationRowsButton);
    actionButtons.prefWrapLengthProperty().bind(content.widthProperty().subtract(36));
    actionButtons.setAlignment(Pos.CENTER_LEFT);
    final VBox alertContainer = new VBox(6, rowLinks, actionButtons);
    alertContainer.setFillWidth(true);
    alertContainer.setMaxWidth(Double.MAX_VALUE);
    alertContainer.getStyleClass().add("quality-warning");
    return alertContainer;
  }

  private static @NotNull Region warningLabel(final @NotNull String text) {
    final var label = FxLabels.newLabel(text);
    label.setMaxWidth(Double.MAX_VALUE);
    HBox hBox = FxLayout.newHBox(label);
    hBox.getStyleClass().add("quality-warning");
    return hBox;
  }

  private static @Nullable String detectIsomerScoreTieWarning(
      final @NotNull List<QualityCardData> qualityCards) {
    if (qualityCards.size() < 2) {
      return null;
    }
    for (int i = 0; i < qualityCards.size() - 1; i++) {
      final QualityCardData left = qualityCards.get(i);
      for (int j = i + 1; j < qualityCards.size(); j++) {
        final QualityCardData right = qualityCards.get(j);
        if (!isIsomerScoreTie(left, right)) {
          continue;
        }
        return "Potential isomer tie: " + LipidQualityCardUtils.formatCardTitle(left.match())
            + " and " + LipidQualityCardUtils.formatCardTitle(
            right.match()) + " have the same overall quality score (" + String.format("%.1f%%",
            left.overall() * 100d) + ").";
      }
    }
    return null;
  }

  private static boolean isIsomerScoreTie(final @NotNull QualityCardData left,
      final @NotNull QualityCardData right) {
    if (Math.abs(left.overall() - right.overall()) > OVERALL_SCORE_TIE_TOLERANCE) {
      return false;
    }
    final String leftAnnotation = Objects.toString(
        left.match().getLipidAnnotation().getAnnotation(), "");
    final String rightAnnotation = Objects.toString(
        right.match().getLipidAnnotation().getAnnotation(), "");
    if (leftAnnotation.equals(rightAnnotation)) {
      return false;
    }
    final double leftExactMass = MatchedLipid.getExactMass(left.match());
    final double rightExactMass = MatchedLipid.getExactMass(right.match());
    return Double.isFinite(leftExactMass) && Double.isFinite(rightExactMass)
        && Math.abs(leftExactMass - rightExactMass) <= ISOMER_EXACT_MASS_TOLERANCE;
  }

}
