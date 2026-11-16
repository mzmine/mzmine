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

package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxLabels.Styles;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.javafx.util.color.ColorsFX;
import io.github.mzmine.main.ConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import org.jetbrains.annotations.NotNull;

/**
 * Banner with the verdict of the resource estimation: a colored box with an icon, the verdict and
 * its reasons. Additional runs are controlled beside the file-count input in the result tab.
 */
final class EstimationOutcomePane extends HBox {

  private static final int ICON_SIZE = 40;

  private final Label title = FxLabels.newLabel(Styles.BOLD_TITLE, "");
  private final VBox details = new VBox(2);

  EstimationOutcomePane() {
    setSpacing(14);
    setAlignment(Pos.CENTER_LEFT);
    setPadding(new Insets(10, 14, 10, 14));
    title.setTextAlignment(TextAlignment.LEFT);
    title.setWrapText(true);
    title.setMinWidth(0);
    title.setMaxWidth(Double.MAX_VALUE);
  }

  void setOutcome(@NotNull final EstimationOutcome outcome) {
    final EstimationVerdict verdict = outcome.verdict();
    final Color color = color(verdict);
    final FxIcons icon = switch (verdict) {
      case ENOUGH_RESOURCES -> FxIcons.CHECK_CIRCLE_FILL;
      case ENOUGH_RESOURCES_UNCERTAIN -> FxIcons.CHECK_CIRCLE;
      case RAM_REQUIREMENT_UNCERTAIN, LARGER_RUN_NEEDED -> FxIcons.RELOAD;
      case NOT_ENOUGH_RESOURCES -> FxIcons.X_CIRCLE_FILL;
    };

    title.setText(verdict.title());
    details.getChildren().setAll(outcome.details().stream().map(text -> {
      final Label label = FxLabels.newLabel(Styles.REGULAR, text);
      label.setTextAlignment(TextAlignment.LEFT);
      label.setWrapText(true);
      label.setMinWidth(0);
      label.setMaxWidth(Double.MAX_VALUE);
      return label;
    }).toList());
    final VBox text = new VBox(6, title, details);
    text.setMinWidth(0);
    HBox.setHgrow(text, Priority.ALWAYS);
    getChildren().setAll(FxIconUtil.getFontIcon(icon, ICON_SIZE, color), text);
    // tinted background with a strong left border in the color of the verdict
    setStyle("""
        -fx-background-color: #%s;
        -fx-border-color: %s;
        -fx-border-width: 0 0 0 6;
        -fx-background-radius: 6;
        -fx-border-radius: 6;""".formatted(
        ColorsFX.toHexOpacityString(Color.color(color.getRed(), color.getGreen(), color.getBlue(),
            0.15)), ColorsFX.toHexString(color)));
  }

  /**
   * Uses the positive and negative colors of the palette that respect the color vision setting.
   */
  private static @NotNull Color color(@NotNull final EstimationVerdict verdict) {
    return switch (verdict) {
      case ENOUGH_RESOURCES, ENOUGH_RESOURCES_UNCERTAIN ->
          ConfigService.getDefaultColorPalette().getPositiveColor();
      case RAM_REQUIREMENT_UNCERTAIN, LARGER_RUN_NEEDED -> ColorsFX.YELLOW_WARN;
      case NOT_ENOUGH_RESOURCES -> ConfigService.getDefaultColorPalette().getNegativeColor();
    };
  }
}
