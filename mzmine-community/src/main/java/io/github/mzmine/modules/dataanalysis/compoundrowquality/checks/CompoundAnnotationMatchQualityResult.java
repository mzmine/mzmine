package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckEvent;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckEvent.AnnotationDetailRequestedEvent;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import io.github.mzmine.modules.visualization.molstructure.Structure2DComponent;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Custom {@link QualityCheckResult} for the compound-annotation check. Main pane shows the title +
 * summary on the left and a fixed-size 2D structure preview on the right. Sub pane lists the
 * annotation's defining identifiers (score, name, formula, ion type, SMILES, InChI, InChI key) in a
 * two-column grid; clicking a value copies it to the system clipboard.
 */
public final class CompoundAnnotationMatchQualityResult extends QualityCheckResult {

  private static final Logger logger = Logger.getLogger(
      CompoundAnnotationMatchQualityResult.class.getName());

  // Fixed size of the structure preview in the main pane. Width is intentionally smaller than the
  // pane width (CompoundRowQualityViewBuilder.MAIN_WIDTH = 350) so the title + summary keep room.
  private static final double STRUCTURE_WIDTH = 140d;
  private static final double STRUCTURE_HEIGHT = 110d;

  private static final @NotNull String VALUE_UNAVAILABLE = "—";

  private final @NotNull FeatureAnnotation annotation;
  private final @NotNull String summary;
  // The member row that owns {@link #annotation}. Passed alongside the annotation in any
  // {@link AnnotationDetailRequestedEvent} fired from this card so listeners can target the right
  // place (e.g. open the IIMN network centered on this row).
  private final @NotNull FeatureListRow annotationRow;
  private final @Nullable Consumer<@NotNull QualityCheckEvent> onEvent;

  public CompoundAnnotationMatchQualityResult(@NotNull QualityCheckStatus status,
      @NotNull String summary, @NotNull FeatureAnnotation annotation,
      @NotNull FeatureListRow annotationRow, @NotNull List<@NotNull FeatureListRow> involvedRows,
      @Nullable Consumer<@NotNull QualityCheckEvent> onEvent) {
    super(QualityCheckType.COMPOUND_ANNOTATION, status, involvedRows);
    this.annotation = annotation;
    this.summary = summary;
    this.annotationRow = annotationRow;
    this.onEvent = onEvent;
  }

  @Override
  public @NotNull Region buildMainPane() {
    final Label title = configureWrap(FxLabels.newBoldLabel(type.getLabel()));
    final Label summaryLabel = configureWrap(FxLabels.newLabel(summary));
    final VBox text = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, title, summaryLabel);
    text.setMinWidth(0);
    HBox.setHgrow(text, Priority.ALWAYS);

    final HBox row = FxLayout.newHBox(Pos.TOP_LEFT, Insets.EMPTY, text);
    row.setMinWidth(0);

    final Region structure = buildStructure();
    if (structure != null) {
      row.getChildren().add(structure);
    }
    return row;
  }

  /// Build a fixed-size {@link Structure2DComponent} for the annotation. Returns {@code null} when
  /// the annotation has no structure (no SMILES/InChI), so the main pane shows just the text.
  private @Nullable Region buildStructure() {
    final MolecularStructure mol = annotation.getStructure();
    if (mol == null) {
      return null;
    }
    try {
      final Structure2DComponent component = new Structure2DComponent(mol.structure());
      // Seed canvas size; BorderPane resizes it to fill the fixed-size wrapper.
      component.setWidth(STRUCTURE_WIDTH);
      component.setHeight(STRUCTURE_HEIGHT);
      // Wrap in a fixed-size Region so the surrounding HBox lays out a stable footprint.
      // Structure2DComponent.isResizable() is true, so BorderPane.center will resize the canvas to
      // exactly the wrapper's bounds.
      final BorderPane wrapper = new BorderPane(component);
      wrapper.setMinSize(STRUCTURE_WIDTH, STRUCTURE_HEIGHT);
      wrapper.setPrefSize(STRUCTURE_WIDTH, STRUCTURE_HEIGHT);
      wrapper.setMaxSize(STRUCTURE_WIDTH, STRUCTURE_HEIGHT);
      // Double click on the structure preview requests the full detail view for this annotation.
      // The single-click handler is intentionally left untouched so the surrounding card's
      // expand/collapse toggle keeps working when the user single-clicks anywhere in the card.
      wrapper.setOnMouseClicked(event -> {
        if (event.getClickCount() >= 2) {
          fireDetailRequested();
          event.consume();
        }
      });
      Tooltip.install(wrapper, new Tooltip("Double-click to open details"));
      return wrapper;
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to render 2D structure for compound annotation", e);
      return null;
    }
  }

  @Override
  public @Nullable Region buildSubPane() {
    final IonType ion = annotation.getAdductType();
    final String ionStr = ion == null ? null : ion.toString();

    final GridPane grid = FxLayout.newGrid2Col(Insets.EMPTY, //
        label("Score:"), copyableValue(annotation.getScoreString()), //
        label("Name:"), copyableValue(annotation.getCompoundName()), //
        label("Formula:"), copyableValue(annotation.getFormula()), //
        label("Ion type:"), copyableValue(ionStr), //
        label("SMILES:"), copyableValue(annotation.getSmiles()), //
        label("InChI:"), copyableValue(annotation.getInChI()), //
        label("InChI key:"), copyableValue(annotation.getInChIKey()));
    grid.setMinWidth(0);

    // Top-right icon button: fires the same AnnotationDetailRequestedEvent the structure
    // double-click does. HBox + spacer keeps the button hard-right regardless of pane width;
    final ButtonBase detailButton = FxIconUtil.newIconButton(FxIcons.LINK,
        "Open network details for this annotation", this::fireDetailRequested);

    final var box = FxLayout.newStackPane(Insets.EMPTY, grid, detailButton);
    StackPane.setAlignment(detailButton, Pos.TOP_RIGHT);
    box.setMinWidth(0);
    return box;
  }

  /// Dispatches an {@link AnnotationDetailRequestedEvent} to {@link #onEvent} when wired. No-op
  /// when the result was built without a listener (e.g. standalone use of the quality pane).
  private void fireDetailRequested() {
    if (onEvent != null) {
      onEvent.accept(new AnnotationDetailRequestedEvent(annotation, annotationRow));
    }
  }

  private static @NotNull Label label(@NotNull String text) {
    final Label label = FxLabels.newBoldLabel(text);
    label.setMinWidth(Region.USE_PREF_SIZE);
    return label;
  }

  /// Wrapping value label whose click copies the underlying value (not the user-facing text — they
  /// are identical here, but the contract is explicit) to the system clipboard. Disabled when the
  /// value is null/blank so a user click on an empty cell is a no-op.
  private static @NotNull Label copyableValue(@Nullable String value) {
    final boolean hasValue = value != null && !value.isBlank();
    final Label label = configureWrap(FxLabels.newLabel(hasValue ? value : VALUE_UNAVAILABLE));
    if (!hasValue) {
      return label;
    }
    label.setCursor(Cursor.HAND);
    label.setTooltip(new Tooltip("Click to copy"));
    label.setOnMouseClicked(_ -> {
      final ClipboardContent content = new ClipboardContent();
      content.putString(value);
      Clipboard.getSystemClipboard().setContent(content);
      DialogLoggerUtil.showPlainNotification("Copied to clipboard", "Value copied");
    });
    return label;
  }

  private static @NotNull Label configureWrap(@NotNull Label label) {
    label.setWrapText(true);
    label.setMinWidth(0);
    label.setMaxWidth(Double.MAX_VALUE);
    return label;
  }
}
