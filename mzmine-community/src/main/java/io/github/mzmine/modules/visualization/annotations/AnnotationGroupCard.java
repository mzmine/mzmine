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

package io.github.mzmine.modules.visualization.annotations;

import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.AnnotationSummaryChart;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.molstructure.Structure2DComponent;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

/**
 * Toggle card for annotations that describe the same compound (see
 * {@link io.github.mzmine.util.annotations.CompoundAnnotationUtils#uniqueAnnotationKey}). Shows the
 * best match score and the number of grouped annotations, the annotation summary chart of the best
 * annotation and the structure (or the name if there is no structure). Similar to the structure
 * cards of the annotation agreement check in the compound dashboard. The selected card gets an
 * accent colored border.
 */
class AnnotationGroupCard extends ToggleButton {

  private static final Logger logger = Logger.getLogger(AnnotationGroupCard.class.getName());
  private static final double STRUCTURE_WIDTH = 165d;
  private static final double STRUCTURE_HEIGHT = 90d;
  private static final double CHART_WIDTH = 40d;
  private static final double HEADER_HEIGHT = 22d;
  private static final double BORDER_WIDTH = 2d;
  /**
   * Height of a card with a {@link #STRUCTURE_HEIGHT} structure. Cards fill the height they get,
   * the structure and chart shrink when the card is smaller, e.g., when a scroll bar takes space.
   */
  static final double FULL_HEIGHT = HEADER_HEIGHT + STRUCTURE_HEIGHT + 2 * BORDER_WIDTH;
  // inline style, the theme stylesheet sets a toggle-button padding that overrides setPadding.
  // fixed border width so selecting does not shift the layout
  private static final String STYLE = "-fx-padding: 0; -fx-border-width: " + BORDER_WIDTH + "px;";
  private static final String SELECTED_STYLE = STYLE + " -fx-border-color: -jr-accent;";

  private final @NotNull List<AnnotationSummary> annotations;

  /**
   * @param annotations the grouped annotations, not empty
   * @param sorter      the annotation sort order of the feature list, best first
   */
  AnnotationGroupCard(@NotNull List<AnnotationSummary> annotations,
      @NotNull Comparator<AnnotationSummary> sorter) {
    this.annotations = annotations;
    final AnnotationSummary best = annotations.stream().min(sorter).orElseThrow();

    // best match score of the group, e.g., spectral similarity
    final String bestScore = annotations.stream().map(AnnotationSummary::annotation)
        .filter(Objects::nonNull).map(FeatureAnnotation::getScore).filter(Objects::nonNull)
        .max(Float::compare).map(ConfigService.getGuiFormats()::score).orElse("-");
    final int n = annotations.size();
    // no wrap: a wrapping label makes the card ~85 px taller in a toggle button
    final HBox header = FxLayout.newHBox(Pos.CENTER, Insets.EMPTY, 0,
        FxLabels.newBoldLabel(bestScore),
        FxLabels.newLabelNoWrap(", %d match%s".formatted(n, n == 1 ? "" : "es")));
    header.setMinHeight(HEADER_HEIGHT);
    header.setPrefHeight(HEADER_HEIGHT);
    header.setMaxHeight(HEADER_HEIGHT);

    // the button sizes its graphic to the graphic's pref size, so the chart and structure pref
    // heights follow the card height. Full height until the card is laid out
    final ObservableValue<Double> contentHeight = heightProperty().map(
        h -> h.doubleValue() <= 0 ? STRUCTURE_HEIGHT
            : Math.max(0, h.doubleValue() - (FULL_HEIGHT - STRUCTURE_HEIGHT)));

    final AnnotationSummaryChart chart = new AnnotationSummaryChart();
    chart.setMinSize(CHART_WIDTH, 0);
    chart.setPrefWidth(CHART_WIDTH);
    chart.prefHeightProperty().bind(contentHeight);
    chart.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    chart.setAnnotation(best);

    final Region structure = createStructure(best);
    structure.prefHeightProperty().bind(contentHeight);

    // fill the height given by the parent, width stays the pref width
    setMaxSize(Region.USE_PREF_SIZE, Double.MAX_VALUE);
    selectedProperty().subscribe(selected -> setStyle(selected ? SELECTED_STYLE : STYLE));
    final VBox box = new VBox(header,
        FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 0, chart, structure));
    box.setAlignment(Pos.TOP_CENTER);
    box.setFillWidth(false);
    setGraphic(box);
    // annotation is never null, see FeatureAnnotationsPane
    setTooltip(new Tooltip(best.annotation().getCompoundName()));
  }

  /**
   * @return the grouped annotations
   */
  @NotNull List<AnnotationSummary> getAnnotations() {
    return annotations;
  }

  private @NotNull Region createStructure(@NotNull AnnotationSummary best) {
    final MolecularStructure mol = annotations.stream().map(AnnotationSummary::annotation)
        .filter(Objects::nonNull).map(FeatureAnnotation::getStructure).filter(Objects::nonNull)
        .findFirst().orElse(null);

    Region content = null;
    if (mol != null) {
      try {
        final Structure2DComponent component = new Structure2DComponent(mol.structure());
        component.setWidth(STRUCTURE_WIDTH);
        component.setHeight(STRUCTURE_HEIGHT);
        content = new BorderPane(component);
      } catch (Exception e) {
        logger.log(Level.WARNING, "Failed to render 2D structure for annotation card", e);
      }
    }
    if (content == null) {
      // no structure: show the name, or the formula if there is no name
      final FeatureAnnotation a = best.annotation();
      final String text = a == null ? "" : Objects.requireNonNullElse(a.getCompoundName(),
          Objects.requireNonNullElse(a.getFormula(), ""));
      final Label label = FxLabels.newLabel(text);
      label.setWrapText(true);
      content = new BorderPane(label);
    }
    // pref height is bound by the caller
    content.setMinSize(STRUCTURE_WIDTH, 0);
    content.setPrefWidth(STRUCTURE_WIDTH);
    content.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    return content;
  }
}
