package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import io.github.mzmine.modules.visualization.molstructure.Structure2DComponent;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Custom {@link QualityCheckResult} for the annotation-agreement check. The main pane lists the
 * aggregate verdicts comparing the {@code getPreferredAnnotation} of every member row: structure
 * (InChIKey first block), formula, and — when structures differ — the mean pairwise Tanimoto
 * structural similarity formatted using the configured score number format.
 * <p>
 * Tooltips: the structure label explains the InChIKey first-block comparison, the similarity label
 * explains the Tanimoto computation. The similarity row is hidden when structures already match
 * (the score is redundant in that case).
 */
public final class AnnotationAgreementQualityResult extends QualityCheckResult {

  private static final Logger logger = Logger.getLogger(
      AnnotationAgreementQualityResult.class.getName());

  /// Max width of the tooltips so a long explanation wraps instead of stretching across the
  /// screen.
  private static final double TOOLTIP_MAX_WIDTH = 420d;
  /// Fixed size of each {@link Structure2DComponent} cell in the sub-pane grid. Two cells plus the
  /// grid gap fit inside the {@code CompoundRowQualityViewBuilder.MAIN_WIDTH} (350) viewport minus
  /// the icon-column indent.
  private static final double STRUCTURE_CELL_WIDTH = 150d;
  private static final double STRUCTURE_CELL_HEIGHT = 110d;

  // When true the card collapses to a single "Single annotation" label and exposes no sub pane —
  // there is only one distinct annotation across all members so the comparison rows / structure
  // grid would add no information.
  private final boolean singleAnnotation;
  private final boolean structuresEqual;
  private final boolean formulasEqual;
  // null when there is fewer than 2 comparable structures (single annotation or no IAtomContainers).
  private final @Nullable Double similarity;
  // Unique structures across all member annotations (already deduplicated by InChIKey / canonical
  // SMILES). Rendered as a 2-column dense grid of 2D structure previews in the sub pane.
  private final @NotNull List<@NotNull MolecularStructure> uniqueStructures;

  public AnnotationAgreementQualityResult(@NotNull QualityCheckStatus status,
      boolean structuresEqual, boolean formulasEqual, @Nullable Double similarity,
      @NotNull List<@NotNull MolecularStructure> uniqueStructures,
      @NotNull List<@NotNull FeatureListRow> involvedRows) {
    super(QualityCheckType.ANNOTATION_AGREEMENT, status, involvedRows);
    this.singleAnnotation = false;
    this.structuresEqual = structuresEqual;
    this.formulasEqual = formulasEqual;
    this.similarity = similarity;
    this.uniqueStructures = List.copyOf(uniqueStructures);
  }

  private AnnotationAgreementQualityResult(@NotNull List<@NotNull FeatureListRow> involvedRows) {
    super(QualityCheckType.ANNOTATION_AGREEMENT, QualityCheckStatus.PASS, involvedRows);
    this.singleAnnotation = true;
    this.structuresEqual = true;
    this.formulasEqual = true;
    this.similarity = null;
    this.uniqueStructures = List.of();
  }

  /// Factory for the trivial "only one distinct annotation across members" case. The card collapses
  /// to a single label and has no sub pane.
  public static @NotNull AnnotationAgreementQualityResult singleAnnotation(
      @NotNull List<@NotNull FeatureListRow> involvedRows) {
    return new AnnotationAgreementQualityResult(involvedRows);
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

    // Structure row — tooltip explains the InChIKey first-block comparison
    final HBox structureRow = makeRow(structuresEqual ? FxIcons.CHECK_CIRCLE : FxIcons.X_CIRCLE,
        structuresEqual ? palette.getPositiveColor() : palette.getNegativeColor(),
        structuresEqual ? "Structure equals" : "Structure mismatch",
        "Comparison of the InChIKey first block (connectivity layer). "
            + "Ignores stereochemistry and isotopes — only the skeletal structure is compared.");

    // Formula row — no tooltip requested
    final HBox formulaRow = makeRow(formulasEqual ? FxIcons.CHECK_CIRCLE : FxIcons.X_CIRCLE,
        formulasEqual ? palette.getPositiveColor() : palette.getNegativeColor(),
        formulasEqual ? "Formula equals" : "Formula mismatch", null);

    final VBox box = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, title, structureRow,
        formulaRow);
    box.setMinWidth(0);

    // Similarity row hidden when structures already match — it adds no information in that case.
    if (!structuresEqual && similarity != null) {
      final String scoreString = ConfigService.getGuiFormats().score(similarity);
      final HBox similarityRow = makeRow(FxIcons.INFO_CIRCLE, palette.getNeutralColor(),
          "Structure similarity: " + scoreString,
          "Mean pairwise Tanimoto similarity over CDK molecular fingerprints "
              + "(default path-based Fingerprinter). 1.0 = identical fingerprints, 0.0 = no shared bits.");
      box.getChildren().add(similarityRow);
    }
    return box;
  }

  @Override
  public @Nullable Region buildSubPane() {
    if (singleAnnotation) {
      return null;
    }
    if (uniqueStructures.isEmpty() && involvedRows.isEmpty()) {
      return null;
    }
    final VBox body = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true);
    body.setMinWidth(0);

    if (!uniqueStructures.isEmpty()) {
      // Dense 2-column grid of 2D structure previews, left-to-right top-to-bottom.
      final GridPane grid = new GridPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);
      grid.setMinWidth(0);
      for (int i = 0; i < uniqueStructures.size(); i++) {
        final Region cell = buildStructureCell(uniqueStructures.get(i));
        if (cell != null) {
          grid.add(cell, i % 2, i / 2);
        }
      }
      body.getChildren().add(grid);
    }

    if (!involvedRows.isEmpty()) {
      body.getChildren().add(configureWrap(FxLabels.newItalicLabel(
          "Involves %d row%s".formatted(involvedRows.size(),
              involvedRows.size() == 1 ? "" : "s"))));
    }
    return body;
  }

  /// Render a single structure preview into a fixed-size wrapper so the surrounding
  /// {@link GridPane} can lay out a stable footprint. Returns null when the
  /// {@link Structure2DComponent} cannot render the molecule (e.g. CDK throws on an unsupported
  /// atom container).
  private static @Nullable Region buildStructureCell(@NotNull MolecularStructure mol) {
    try {
      final Structure2DComponent component = new Structure2DComponent(mol.structure());
      component.setWidth(STRUCTURE_CELL_WIDTH);
      component.setHeight(STRUCTURE_CELL_HEIGHT);
      final BorderPane wrapper = new BorderPane(component);
      wrapper.setMinSize(STRUCTURE_CELL_WIDTH, STRUCTURE_CELL_HEIGHT);
      wrapper.setPrefSize(STRUCTURE_CELL_WIDTH, STRUCTURE_CELL_HEIGHT);
      wrapper.setMaxSize(STRUCTURE_CELL_WIDTH, STRUCTURE_CELL_HEIGHT);
      return wrapper;
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to render 2D structure for annotation agreement", e);
      return null;
    }
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
