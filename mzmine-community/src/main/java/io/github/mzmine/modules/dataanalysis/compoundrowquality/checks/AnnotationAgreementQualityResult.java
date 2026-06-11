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

package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import io.github.mzmine.datamodel.features.types.annotations.AnnotationSummaryChart;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckEvent;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckEvent.AnnotationStructureSelectedEvent;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckEvent.AnnotationSummaryActivated;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import io.github.mzmine.modules.visualization.molstructure.Structure2DComponent;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

/// Custom {@link QualityCheckResult} for the annotation-agreement check.
///
/// **Main pane** lists the aggregate verdicts: structure (InChIKey first block), formula, optional
/// lipid conflict, and — when structures differ — the mean pairwise Tanimoto structural similarity
/// formatted with the configured score number format. The similarity icon is color-scaled from the
/// {@code MZMINE_ORANGE_GRAY_BLUE_TWO_SIDED_NARROW} palette (orange → gray → blue = bad → ok →
/// good). The lipid conflict row is hidden when no conflict was found; the similarity row is hidden
/// when structures already match.
///
/// **Sub pane** shows the selected {@link AnnotationAgreementCheckType} above a dense grid of
/// unique agreeing groups (sorted by descending score, deduplicated by InChIKey first block). Each
/// group renders as a 4-column row: {@code [score chart] [structure] [structure] [score chart]},
/// with the two structures in the middle and each annotation's {@link AnnotationSummaryChart}
/// flanking them. Below each structure: "N row(s) agree". A tooltip on each structure lists the
/// agreeing rows (sorted by m/z, colored by the host dashboard's per-row coloring, bold for the
/// currently selected row). Double-click on a structure fires an
/// {@link AnnotationStructureSelectedEvent} so the host can promote the source row to its
/// selection. A copy icon button next to the source label puts the unique isomeric SMILES of the
/// rendered structures on the clipboard.
public final class AnnotationAgreementQualityResult extends QualityCheckResult {

  private static final Logger logger = Logger.getLogger(
      AnnotationAgreementQualityResult.class.getName());

  /// Max width of the tooltips so a long explanation wraps instead of stretching across the
  /// screen.
  private static final double TOOLTIP_MAX_WIDTH = 420d;
  /// Fixed size of the structure preview in each grid cell. Tight enough that two charts + two
  /// structures fit in one grid row inside the {@code CompoundRowQualityViewBuilder.MAIN_WIDTH}
  /// viewport minus the icon-column indent.
  private static final double STRUCTURE_WIDTH = 110d;
  private static final double STRUCTURE_HEIGHT = 100d;
  /// Side annotation-summary chart width. Height stretches to fill the row (chart fills the same
  /// vertical span as {@code [structure + count label]}) so the chart top + bottom align with the
  /// structure column.
  private static final double CHART_WIDTH = 45d;
  /// Extra horizontal gap inserted between the two structure columns (cols 1 and 2 in the grid) —
  /// additional to the grid's normal hgap so the two structures don't visually touch.
  private static final double STRUCTURE_GAP = 12d;

  /// Color-scaled paint scale for the similarity icon. Reads orange (similarity 0, "bad") → gray
  /// (mid, "ok") → blue (1, "good"). Created once and reused so the linear-interpolation LUT inside
  /// {@code PaintScale} is shared across instances.
  private static final PaintScale SIMILARITY_PAINT_SCALE = SimpleColorPalette.MZMINE_ORANGE_GRAY_BLUE_TWO_SIDED_NARROW.toPaintScale(
      PaintScaleTransform.LINEAR, Range.closed(0d, 1d));
  /// Similarity score below this threshold → similarity row shows the X (fail) icon in red.
  private static final double SIMILARITY_FAIL_THRESHOLD = 0.35;
  /// Similarity score above this threshold → similarity row shows the check (pass) icon in green.
  private static final double SIMILARITY_PASS_THRESHOLD = 0.9;

  private final @NotNull AnnotationAgreementCheckType checkType;
  // When true the card collapses to a single "Single annotation" label and exposes no sub pane.
  private final boolean singleAnnotation;
  private final boolean structuresEqual;
  private final boolean formulasEqual;
  private final boolean lipidConflict;
  // null when there is fewer than 2 comparable structures (single annotation or no IAtomContainers).
  private final @Nullable Double similarity;
  // Unique agreeing groups across all member rows (sorted by descending score, deduplicated by
  // InChIKey first block; only groups with a structure-bearing representative are kept).
  private final @NotNull List<@NotNull AnnotationAgreementGroup> groups;
  // Optional host wiring — null when the pane runs standalone without a dashboard.
  private final @Nullable ColorAssignment colorAssignment;
  private final @Nullable ObjectProperty<@Nullable FeatureListRow> selectedMemberRow;
  private final @Nullable Consumer<@NotNull QualityCheckEvent> onEvent;
  // Callback fired when the user picks a different option in the in-pane "Source" ComboBox. Null
  // when no parameter-update sink is wired (standalone use); the combo then becomes read-only.
  private final @Nullable Consumer<@NotNull AnnotationAgreementCheckType> onCheckTypeChange;

  public AnnotationAgreementQualityResult(@NotNull QualityCheckStatus status,
      @NotNull AnnotationAgreementCheckType checkType, boolean structuresEqual,
      boolean formulasEqual, boolean lipidConflict, @Nullable Double similarity,
      @NotNull List<@NotNull AnnotationAgreementGroup> groups,
      @NotNull List<@NotNull FeatureListRow> involvedRows,
      @Nullable ColorAssignment colorAssignment,
      @Nullable ObjectProperty<@Nullable FeatureListRow> selectedMemberRow,
      @Nullable Consumer<@NotNull QualityCheckEvent> onEvent,
      @Nullable Consumer<@NotNull AnnotationAgreementCheckType> onCheckTypeChange) {
    super(QualityCheckType.ANNOTATION_AGREEMENT, status, involvedRows);
    this.checkType = checkType;
    this.singleAnnotation = false;
    this.structuresEqual = structuresEqual;
    this.formulasEqual = formulasEqual;
    this.lipidConflict = lipidConflict;
    this.similarity = similarity;
    this.groups = List.copyOf(groups);
    this.colorAssignment = colorAssignment;
    this.selectedMemberRow = selectedMemberRow;
    this.onEvent = onEvent;
    this.onCheckTypeChange = onCheckTypeChange;
  }

  private AnnotationAgreementQualityResult(@NotNull AnnotationAgreementCheckType checkType,
      @NotNull List<@NotNull FeatureListRow> involvedRows) {
    super(QualityCheckType.ANNOTATION_AGREEMENT, QualityCheckStatus.PASS, involvedRows);
    this.checkType = checkType;
    this.singleAnnotation = true;
    this.structuresEqual = true;
    this.formulasEqual = true;
    this.lipidConflict = false;
    this.similarity = null;
    this.groups = List.of();
    this.colorAssignment = null;
    this.selectedMemberRow = null;
    this.onEvent = null;
    this.onCheckTypeChange = null;
  }

  /// Factory for the trivial "only one distinct annotation across members" case. The card collapses
  /// to a single label and has no sub pane.
  public static @NotNull AnnotationAgreementQualityResult singleAnnotation(
      @NotNull AnnotationAgreementCheckType checkType,
      @NotNull List<@NotNull FeatureListRow> involvedRows) {
    return new AnnotationAgreementQualityResult(checkType, involvedRows);
  }

  /// Claim the full card width for the structure grid — its 4-column layout needs every pixel.
  /// Other checks keep the default indented sub pane.
  @Override
  public boolean wantsFullWidthSubPane() {
    return !singleAnnotation;
  }

  @Override
  public @NotNull Region buildMainPane() {
    final Label title = configureWrap(FxLabels.newBoldLabel(type.getLabel()));

    if (singleAnnotation) {
      final Label label = configureWrap(FxLabels.newLabel("Single annotation"));
      final VBox box = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, title, label);
      box.setMinWidth(0);
      return box;
    }

    final SimpleColorPalette palette = ConfigService.getDefaultColorPalette();

    final HBox structureRow = makeRow(structuresEqual ? FxIcons.CHECK_CIRCLE : FxIcons.X_CIRCLE,
        structuresEqual ? palette.getPositiveColor() : palette.getNegativeColor(),
        structuresEqual ? "Structure equals" : "Structure mismatch",
        "Comparison of the InChIKey first block (connectivity layer). "
            + "Ignores stereochemistry and isotopes — only the skeletal structure is compared.");

    final HBox formulaRow = makeRow(formulasEqual ? FxIcons.CHECK_CIRCLE : FxIcons.X_CIRCLE,
        formulasEqual ? palette.getPositiveColor() : palette.getNegativeColor(),
        formulasEqual ? "Formula equals" : "Formula mismatch", null);

    final VBox box = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, title, structureRow,
        formulaRow);
    box.setMinWidth(0);

    if (lipidConflict) {
      box.getChildren().add(makeRow(FxIcons.X_CIRCLE, palette.getNegativeColor(), "Lipid conflicts",
          "Multiple MatchedLipid annotations disagree on the lipid label "
              + "(ILipidAnnotation.getAnnotation())."));
    }

    if (!structuresEqual && similarity != null) {
      final String scoreString = ConfigService.getGuiFormats().score(similarity);
      // Thresholded icon: < 0.35 fails, > 0.9 passes, the middle band stays informational with the
      // color-scaled palette so the user still gets a sense of "where in the middle".
      final FxIcons similarityIcon;
      final Color similarityColor;
      if (similarity < SIMILARITY_FAIL_THRESHOLD) {
        similarityIcon = FxIcons.X_CIRCLE;
        similarityColor = palette.getNegativeColor();
      } else if (similarity > SIMILARITY_PASS_THRESHOLD) {
        similarityIcon = FxIcons.CHECK_CIRCLE;
        similarityColor = palette.getPositiveColor();
      } else {
        similarityIcon = FxIcons.INFO_CIRCLE;
        similarityColor = FxColorUtil.awtColorToFX(SIMILARITY_PAINT_SCALE.getPaint(similarity));
      }
      box.getChildren().add(
          makeRow(similarityIcon, similarityColor, "Structure similarity: " + scoreString,
              "Mean pairwise Tanimoto similarity over CDK molecular fingerprints "
                  + "(default path-based Fingerprinter). 1.0 = identical fingerprints, 0.0 = no shared "
                  + "bits. Icon: < %.2f fails, > %.2f passes; the middle band keeps the orange → gray → "
                  + "blue scaled icon color.".formatted(SIMILARITY_FAIL_THRESHOLD,
                  SIMILARITY_PASS_THRESHOLD)));
    }
    return box;
  }

  @Override
  public @Nullable Region buildSubPane() {
    if (singleAnnotation) {
      return null;
    }
    if (groups.isEmpty() && involvedRows.isEmpty()) {
      return null;
    }
    final VBox body = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true);
    body.setMinWidth(0);

    // Header line: "Source:" prefix + ComboBox<AnnotationAgreementCheckType> + spacer +
    // copy-isomeric-SMILES button.
    final Label sourcePrefix = FxLabels.newLabel("Source:");
    final ComboBox<AnnotationAgreementCheckType> sourceCombo = buildSourceCombo();
    final Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    final ButtonBase copyButton = FxIconUtil.newIconButton(FxIcons.SAVE,
        "Copy unique isomeric SMILES to clipboard", this::copyUniqueIsomericSmiles);
    final HBox header = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, sourcePrefix, sourceCombo,
        spacer, copyButton);
    header.setMinWidth(0);
    body.getChildren().add(header);

    if (!groups.isEmpty()) {
      body.getChildren().add(buildGrid());
    }

    if (!involvedRows.isEmpty()) {
      body.getChildren().add(configureWrap(FxLabels.newItalicLabel(
          "Involves %d row%s".formatted(involvedRows.size(),
              involvedRows.size() == 1 ? "" : "s"))));
    }
    return body;
  }

  /// Build the source ComboBox. Pre-selected with the {@link AnnotationAgreementCheckType} that
  /// produced this card; on change to a different value, fires {@link #onCheckTypeChange}, which
  /// rewrites the controller's ParameterSet and triggers a full recompute. Disabled when no
  /// callback is wired (standalone pane).
  private @NotNull ComboBox<AnnotationAgreementCheckType> buildSourceCombo() {
    final ComboBox<AnnotationAgreementCheckType> combo = new ComboBox<>(
        FXCollections.observableArrayList(AnnotationAgreementCheckType.values()));
    combo.setValue(checkType);
    combo.setDisable(onCheckTypeChange == null);
    combo.valueProperty().addListener((obs, oldValue, newValue) -> {
      // Only fire when the selected value differs from the one that produced the rendered card —
      // a setValue() round-trip during the next recompute then becomes a no-op.
      if (newValue != null && newValue != checkType && onCheckTypeChange != null) {
        onCheckTypeChange.accept(newValue);
      }
    });
    return combo;
  }

  /// Build the 4-column grid: {@code [chart][structure][structure][chart]} per row, pairing two
  /// annotation groups left + right. Charts are pinned top + grow vertically so their top + bottom
  /// align with the {@code [structure + count label]} cell on the same row. An extra left margin on
  /// column 2 inserts a visual gap between the two structures.
  private @NotNull GridPane buildGrid() {
    final GridPane grid = new GridPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);
    grid.setMinWidth(0);
    for (int i = 0; i < groups.size(); i += 2) {
      final int rowIndex = i / 2;
      final AnnotationAgreementGroup left = groups.get(i);
      final Region leftChart = buildChartCell(left);
      grid.add(leftChart, 0, rowIndex);
      grid.add(buildStructureCell(left), 1, rowIndex);
      if (i + 1 < groups.size()) {
        final AnnotationAgreementGroup right = groups.get(i + 1);
        final Region rightStructure = buildStructureCell(right);
        // Extra horizontal gap between the two structure columns — added as a left margin on the
        // right structure cell so the left structure sits right next to its chart and the gap
        // appears between cols 1 and 2 only.
        if (rightStructure != null) {
          GridPane.setMargin(rightStructure, new Insets(0, 0, 0, STRUCTURE_GAP));
        }
        grid.add(rightStructure, 2, rowIndex);
        grid.add(buildChartCell(right), 3, rowIndex);
      }
    }
    return grid;
  }

  /// Render the {@link AnnotationSummaryChart} for one group's representative annotation. Height is
  /// unbounded + grows in the grid row so the chart fills the same vertical span as the
  /// neighbouring {@code [structure + count label]} cell — top and bottom edges line up.
  /// Double-clicking the chart fires an {@link AnnotationSummaryActivated} event so the host can
  /// open the matching detail view (compound DB tab, spectral library tab, lipid QC).
  private @NotNull Region buildChartCell(@NotNull AnnotationAgreementGroup group) {
    final AnnotationSummaryChart chart = new AnnotationSummaryChart();
    chart.setMinWidth(CHART_WIDTH);
    chart.setPrefWidth(CHART_WIDTH);
    chart.setMaxWidth(CHART_WIDTH);
    // Min height matches the structure preview so the chart never collapses below it. Max height
    // is unbounded so it stretches with the [structure + count] cell.
    chart.setMinHeight(STRUCTURE_HEIGHT);
    chart.setMaxHeight(Double.MAX_VALUE);
    final AnnotationSummary summary = AnnotationSummary.of(group.row(), group.annotation());
    chart.setAnnotation(summary);
    if (onEvent != null) {
      chart.setCursor(Cursor.HAND);
      chart.setOnMouseClicked(event -> {
        if (event.getClickCount() >= 2) {
          onEvent.accept(new AnnotationSummaryActivated(group.row(), summary));
          event.consume();
        }
      });
    }
    GridPane.setValignment(chart, VPos.TOP);
    GridPane.setVgrow(chart, Priority.ALWAYS);
    GridPane.setFillHeight(chart, true);
    return chart;
  }

  /// Render one group's structure with the agreeing-rows count below; wires a tooltip that lists
  /// the agreeing rows as colored chips (sorted by m/z, bold for the currently selected row) and a
  /// double-click handler that promotes the representative row via an
  /// {@link AnnotationStructureSelectedEvent}.
  private @Nullable Region buildStructureCell(@NotNull AnnotationAgreementGroup group) {
    final MolecularStructure mol = group.annotation().getStructure();
    if (mol == null) {
      return null;
    }
    try {
      final Structure2DComponent component = new Structure2DComponent(mol.structure());
      component.setWidth(STRUCTURE_WIDTH);
      component.setHeight(STRUCTURE_HEIGHT);
      final BorderPane structureWrapper = new BorderPane(component);
      structureWrapper.setMinSize(STRUCTURE_WIDTH, STRUCTURE_HEIGHT);
      structureWrapper.setPrefSize(STRUCTURE_WIDTH, STRUCTURE_HEIGHT);
      structureWrapper.setMaxSize(STRUCTURE_WIDTH, STRUCTURE_HEIGHT);
      installRowsTooltip(structureWrapper, group);
      structureWrapper.setCursor(Cursor.HAND);
      structureWrapper.setOnMouseClicked(event -> {
        if (onEvent != null && event.getButton() == MouseButton.PRIMARY) {
          onEvent.accept(new AnnotationStructureSelectedEvent(group.annotation(), group.row()));
          event.consume();
        }
      });

      final int rowCount = group.agreeingRows().size();
      final Label countLabel = configureWrap(
          FxLabels.newLabel("%d row%s agree".formatted(rowCount, rowCount == 1 ? "" : "s")));
      countLabel.setMaxWidth(STRUCTURE_WIDTH);

      final VBox cell = FxLayout.newVBox(Pos.TOP_CENTER, Insets.EMPTY, false, structureWrapper,
          countLabel);
      cell.setMinWidth(0);
      return cell;
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to render 2D structure for annotation agreement", e);
      return null;
    }
  }

  /// Install a tooltip on {@code target} whose graphic is a {@link FlowPane} of colored row chips —
  /// one per agreeing row, sorted by m/z, bold for the currently selected row. Reuses
  /// {@link FragmentParentsRendering#buildChip} so coloring + selection-bold styling match the
  /// other quality-pane chips.
  private void installRowsTooltip(@NotNull Region target, @NotNull AnnotationAgreementGroup group) {
    if (colorAssignment == null || group.agreeingRows().isEmpty()) {
      return;
    }
    final FlowPane chips = FxLayout.newFlowPane();
    chips.setPadding(Insets.EMPTY);
    chips.setMinWidth(0);
    chips.setMaxWidth(TOOLTIP_MAX_WIDTH);
    for (final FeatureListRow row : group.agreeingRows()) {
      chips.getChildren()
          .add(FragmentParentsRendering.buildChip(row, colorAssignment, selectedMemberRow));
    }
    final Tooltip tooltip = new Tooltip();
    tooltip.setGraphic(chips);
    tooltip.setMaxWidth(TOOLTIP_MAX_WIDTH);
    Tooltip.install(target, tooltip);
  }

  /// Copy the unique isomeric SMILES of every rendered group's representative structure to the
  /// system clipboard, one per line. Falls back to canonical SMILES when isomeric is missing.
  private void copyUniqueIsomericSmiles() {
    final String payload = groups.stream().map(g -> g.annotation().getStructure())
        .filter(java.util.Objects::nonNull).map(mol -> {
          final String iso = mol.isomericSmiles();
          if (iso != null && !iso.isBlank()) {
            return iso;
          }
          return mol.canonicalSmiles();
        }).filter(s -> s != null && !s.isBlank()).distinct().collect(Collectors.joining("\n"));
    if (payload.isBlank()) {
      DialogLoggerUtil.showPlainNotification("Nothing to copy",
          "No isomeric SMILES available for the unique structures");
      return;
    }
    final ClipboardContent content = new ClipboardContent();
    content.putString(payload);
    Clipboard.getSystemClipboard().setContent(content);
    DialogLoggerUtil.showPlainNotification("Copied to clipboard",
        "Unique isomeric SMILES copied (%d structure%s)".formatted(groups.size(),
            groups.size() == 1 ? "" : "s"));
  }

  /// Build a single {icon + label} row. When {@code tooltipText} is non-null, the same tooltip is
  /// installed on both the icon and the label so hovering either surface reveals the explanation.
  private static @NotNull HBox makeRow(@NotNull FxIcons icon, @NotNull Color color,
      @NotNull String text, @Nullable String tooltipText) {
    final FontIcon iconNode = FxIconUtil.getFontIcon(icon, FxIconUtil.DEFAULT_ICON_SIZE, color);
    final Label label = configureWrap(FxLabels.newLabel(text));
    if (tooltipText != null) {
      final Tooltip tooltip = new Tooltip(tooltipText);
      tooltip.setWrapText(true);
      tooltip.setMaxWidth(TOOLTIP_MAX_WIDTH);
      label.setTooltip(tooltip);
      Tooltip.install(iconNode, tooltip);
    }
    final HBox row = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, iconNode, label);
    row.setMinWidth(0);
    return row;
  }

  private static @NotNull Label configureWrap(@NotNull Label label) {
    label.setWrapText(true);
    label.setMinWidth(0);
    label.setMaxWidth(Double.MAX_VALUE);
    return label;
  }
}
