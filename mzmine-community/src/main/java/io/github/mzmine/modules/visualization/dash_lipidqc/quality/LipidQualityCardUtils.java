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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnalysisType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.ElutionOrderMetrics;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils.InterferenceMetrics;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
import java.awt.Paint;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.renderer.PaintScale;

/**
 * Computes and creates the lipid annotation quality cards (MS1, MS2, adduct, isotope, elution
 * order, interference and overall score) of the lipid annotation QC dashboard, so they can also be
 * shown in other views.
 */
public final class LipidQualityCardUtils {

  private static final Logger logger = Logger.getLogger(LipidQualityCardUtils.class.getName());
  private static final double METRIC_BAR_WIDTH = 230d;
  private static final double METRIC_BAR_HEIGHT = 16d;
  /**
   * Computes the cards of {@link #createQualityCard(FeatureListRow, MatchedLipid)} one after the
   * other. Each card iterates the feature list for the elution order. The thread ends when idle.
   */
  private static final ThreadPoolExecutor cardExecutor = createCardExecutor();

  private LipidQualityCardUtils() {
  }

  private static @NotNull ThreadPoolExecutor createCardExecutor() {
    final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 100, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(), runnable -> {
      final Thread thread = new Thread(runnable, "Lipid quality cards");
      thread.setDaemon(true);
      return thread;
    });
    executor.allowCoreThreadTimeOut(true);
    return executor;
  }

  /**
   * Creates the quality card of a lipid annotation, without actions in the header. Returns a
   * placeholder right away, the metrics are computed in the background and then replace the
   * placeholder.
   *
   * @param row   the annotated row
   * @param match the lipid annotation of the row
   * @return the container of the quality card
   */
  public static @NotNull Region createQualityCard(@NotNull FeatureListRow row,
      @NotNull MatchedLipid match) {
    final Label placeholder = FxLabels.newLabel("Computing annotation quality...");
    final VBox container = new VBox(placeholder);
    container.setFillWidth(true);

    cardExecutor.execute(() -> {
      try {
        final ModularFeatureList featureList =
            row.getFeatureList() instanceof ModularFeatureList flist ? flist : null;
        final boolean includeRetentionTime = includeRetentionTimeAnalysis(featureList);
        final QualityCardData data = computeQualityCardData(row, featureList, match,
            LipidQcScoringUtils.computeInterferenceMetrics(row), includeRetentionTime);
        FxThread.runLater(() -> container.getChildren().setAll(
            createQualityCard(data, includeRetentionTime,
                createCardHeader(formatCardTitle(match), null))));
      } catch (Exception e) {
        logger.log(Level.WARNING, "Could not compute lipid annotation quality", e);
        FxThread.runLater(() -> placeholder.setText("Could not compute annotation quality"));
      }
    });
    return container;
  }

  /**
   * @param featureList the feature list, null if unknown
   * @return true if the last lipid annotation step used an analysis type with a retention time
   * pattern, or if this is unknown
   */
  public static boolean includeRetentionTimeAnalysis(@Nullable ModularFeatureList featureList) {
    final LipidAnalysisType analysisType =
        featureList == null ? null : LipidQcScoringUtils.detectLipidAnalysisType(featureList);
    return analysisType == null || analysisType.hasRetentionTimePattern();
  }

  /**
   * @param row                  the annotated row
   * @param featureList          the feature list for MS1 tolerance and elution order context
   * @param match                the lipid annotation of the row
   * @param interferenceMetrics  interference metrics of the row
   * @param includeRetentionTime true to score the elution order
   * @return the quality metrics of the annotation
   */
  static @NotNull QualityCardData computeQualityCardData(@NotNull FeatureListRow row,
      @Nullable ModularFeatureList featureList, @NotNull MatchedLipid match,
      @NotNull InterferenceMetrics interferenceMetrics, boolean includeRetentionTime) {
    final QualityMetric ms1 = evaluateMs1(row, featureList, match);
    final QualityMetric ms2 = evaluateMs2(match);
    final QualityMetric adduct = evaluateAdduct(row, match);
    final QualityMetric isotope = evaluateIsotope(row, match);
    final @Nullable ElutionOrderMetrics elutionMetrics = includeRetentionTime && featureList != null
        ? LipidQcScoringUtils.computeElutionOrderMetrics(featureList, row, match) : null;
    final QualityMetric elutionOrder =
        includeRetentionTime ? elutionMetrics != null ? new QualityMetric(
            elutionMetrics.combinedScore(),
            LipidQcScoringUtils.formatElutionOrderDetail(elutionMetrics))
            : new QualityMetric(0.4d, "Missing RT context")
            : new QualityMetric(0d, "Disabled for current lipid analysis mode");
    final QualityMetric interference = new QualityMetric(
        LipidQcScoringUtils.computeInterferenceScore(interferenceMetrics.totalPenaltyCount()),
        LipidQcScoringUtils.interferenceDetail(interferenceMetrics));
    final double adductWeight = LipidQcScoringUtils.computeAdductWeight(row, match);
    final double elutionOrderWeight =
        includeRetentionTime ? LipidQcScoringUtils.computeElutionOrderWeight(elutionMetrics) : 0d;
    final double overall = LipidQcScoringUtils.computeWeightedQualityScore(ms1.score(), ms2.score(),
        adduct.score(), isotope.score(), interference.score(), elutionOrder.score(), true,
        includeRetentionTime, adductWeight, elutionOrderWeight);
    return new QualityCardData(match, ms1, ms2, adduct, isotope, elutionOrder, interference,
        overall);
  }

  /**
   * @param data                 the quality metrics
   * @param includeRetentionTime true to show the elution order score
   * @param header               the header on top of the card, see
   *                             {@link #createCardHeader(String, Node)}
   * @return the quality card with one bar per metric
   */
  static @NotNull Region createQualityCard(@NotNull QualityCardData data,
      boolean includeRetentionTime, @NotNull Node header) {
    final VBox card = new VBox(6);
    card.getStyleClass().add("quality-card");
    card.setFillWidth(true);
    card.setMaxWidth(Double.MAX_VALUE);
    card.getChildren().add(header);
    card.getChildren().add(createMetricRow("Overall quality", data.overall(),
        data.overall() >= 0.75 ? "High confidence"
            : data.overall() >= 0.5 ? "Moderate confidence" : "Low confidence"));
    card.getChildren()
        .add(createMetricRow("MS1 mass accuracy", data.ms1().score(), data.ms1().detail()));
    card.getChildren()
        .add(createMetricRow("MS2 diagnostics", data.ms2().score(), data.ms2().detail()));
    card.getChildren().add(createMetricRow("Lipid Ion vs Ion Identity", data.adduct().score(),
        data.adduct().detail()));
    card.getChildren()
        .add(createMetricRow("Isotope pattern", data.isotope().score(), data.isotope().detail()));
    if (includeRetentionTime) {
      card.getChildren().add(createMetricRow("Elution order score", data.elutionOrder().score(),
          data.elutionOrder().detail()));
    }
    card.getChildren().add(createMetricRow("Interference risk", data.interference().score(),
        data.interference().detail()));
    return card;
  }

  /**
   * @param title  the bold title
   * @param action an action on the right, e.g., a button, null for none
   * @return the card header
   */
  static @NotNull Region createCardHeader(@NotNull String title, @Nullable Node action) {
    final Label label = FxLabels.newBoldLabel(title);
    label.setWrapText(true);
    if (action == null) {
      return label;
    }
    final Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    final HBox header = new HBox(8, label, spacer, action);
    header.setAlignment(Pos.CENTER_LEFT);
    return header;
  }

  /**
   * @param match the lipid annotation
   * @return the annotation and adduct
   */
  static @NotNull String formatCardTitle(@NotNull MatchedLipid match) {
    final String annotation = Objects.toString(match.getLipidAnnotation().getAnnotation(),
        "Unknown annotation");
    final @Nullable String adductName =
        match.getIonizationType() != null ? match.getIonizationType().getAdductName() : null;
    if (adductName == null || adductName.isBlank()) {
      return annotation;
    }
    return annotation + " " + adductName;
  }

  private static @NotNull Region createMetricRow(@NotNull String name, double score,
      @NotNull String detail) {
    final double clipped = Math.max(0d, Math.min(1d, score));
    final javafx.scene.paint.Color scoreColor = qualityScoreColor(clipped);
    final StackPane barPane = createMetricBar(name, clipped, scoreColor);
    final var detailLabel = FxLabels.newSmallLabel(detail);
    return new VBox(3, barPane, detailLabel);
  }

  private static @NotNull StackPane createMetricBar(@NotNull String name, double score,
      @NotNull javafx.scene.paint.Color scoreColor) {
    final Region barTrack = new Region();
    barTrack.setMinWidth(METRIC_BAR_WIDTH);
    barTrack.setPrefWidth(METRIC_BAR_WIDTH);
    barTrack.setMaxWidth(METRIC_BAR_WIDTH);
    barTrack.setMinHeight(METRIC_BAR_HEIGHT);
    barTrack.setPrefHeight(METRIC_BAR_HEIGHT);
    barTrack.setMaxHeight(METRIC_BAR_HEIGHT);
    barTrack.getStyleClass().add("metric-bar-track");
    final Region barFill = new Region();
    final double fillWidth = METRIC_BAR_WIDTH * score;
    barFill.setMinWidth(fillWidth);
    barFill.setPrefWidth(fillWidth);
    barFill.setMaxWidth(fillWidth);
    barFill.setMinHeight(METRIC_BAR_HEIGHT);
    barFill.setPrefHeight(METRIC_BAR_HEIGHT);
    barFill.setMaxHeight(METRIC_BAR_HEIGHT);
    barFill.setStyle("-fx-background-color: " + FxColorUtil.colorToHex(scoreColor)
        + "; -fx-background-radius: 3;");
    final var barLabel = FxLabels.newLabel(name + "  " + String.format("%.0f%%", score * 100d));
    final boolean whiteLabelText = ConfigService.getConfiguration().isDarkMode() || score >= 0.5d;
    barLabel.getStyleClass().addAll("metric-bar-label",
        whiteLabelText ? "metric-bar-label-white-text" : "metric-bar-label-dark-text");
    barLabel.setMouseTransparent(true);
    final StackPane barPane = new StackPane(barTrack, barFill, barLabel);
    StackPane.setAlignment(barTrack, Pos.CENTER_LEFT);
    StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
    StackPane.setAlignment(barLabel, Pos.CENTER_LEFT);
    barPane.setAlignment(Pos.CENTER_LEFT);
    barPane.setMinWidth(METRIC_BAR_WIDTH);
    barPane.setPrefWidth(METRIC_BAR_WIDTH);
    barPane.setMaxWidth(METRIC_BAR_WIDTH);
    return barPane;
  }

  private static @NotNull javafx.scene.paint.Color qualityScoreColor(double score) {
    final SimpleColorPalette defaultPalette = ConfigService.getDefaultColorPalette();
    final PaintScale scoreScale = new SimpleColorPalette(defaultPalette.getNegativeColor(),
        defaultPalette.getNeutralColor(), defaultPalette.getPositiveColor()).toPaintScale(
        PaintScaleTransform.LINEAR, Range.closed(0d, 1d));
    final Paint awtPaint = scoreScale.getPaint(LipidQcScoringUtils.clampToUnit(score));
    if (awtPaint instanceof Color awtColor) {
      return FxColorUtil.awtColorToFX(awtColor);
    }
    return javafx.scene.paint.Color.GRAY;
  }

  private static @NotNull QualityMetric evaluateMs1(@NotNull FeatureListRow row,
      @Nullable ModularFeatureList featureList, @NotNull MatchedLipid match) {
    final double exactMz = MatchedLipid.getExactMass(match);
    final double observedMz =
        match.getAccurateMz() != null ? match.getAccurateMz() : row.getAverageMZ();
    final double ppm = (observedMz - exactMz) / exactMz * 1e6;
    final @Nullable MZTolerance ms1Tolerance =
        featureList == null ? null : LipidQcScoringUtils.detectMs1Tolerance(featureList);
    final double score = LipidQcScoringUtils.computeMs1Score(row, match, ms1Tolerance);
    return new QualityMetric(score, String.format("%.2f ppm", ppm));
  }

  private static @NotNull QualityMetric evaluateMs2(@NotNull MatchedLipid match) {
    final double explained = LipidQcScoringUtils.clampToUnit(
        match.getMsMsScore() == null ? 0d : match.getMsMsScore());
    return new QualityMetric(explained,
        String.format("%.1f", explained * 100d) + "% explained intensity");
  }

  private static @NotNull QualityMetric evaluateAdduct(@NotNull FeatureListRow row,
      @NotNull MatchedLipid match) {
    if (row.getBestIonIdentity() == null) {
      return new QualityMetric(0d, "No ion identity available for cross-check");
    }
    final String featureAdduct = normalizeAdduct(row.getBestIonIdentity().toString());
    final String lipidAdduct = normalizeAdduct(match.getIonizationType().getAdductName());
    final boolean matchFound = featureAdduct.equals(lipidAdduct);
    final String detail = "Feature: " + row.getBestIonIdentity().toString() + " vs Lipid: "
        + match.getIonizationType().getAdductName();
    return new QualityMetric(matchFound ? 1d : 0d, detail);
  }

  private static @NotNull QualityMetric evaluateIsotope(@NotNull FeatureListRow row,
      @NotNull MatchedLipid match) {
    if (row.getBestIsotopePattern() == null || match.getIsotopePattern() == null) {
      return new QualityMetric(0d, "Missing measured or theoretical isotope pattern");
    }
    final float score = IsotopePatternScoreCalculator.getSimilarityScore(
        row.getBestIsotopePattern(), match.getIsotopePattern(), new MZTolerance(0.003, 10d), 0d);
    return new QualityMetric(score, "Similarity score " + String.format("%.2f", score));
  }

  private static @NotNull String normalizeAdduct(@Nullable String adduct) {
    return adduct == null ? "" : adduct.replaceAll("\\s+", "").toLowerCase();
  }
}
