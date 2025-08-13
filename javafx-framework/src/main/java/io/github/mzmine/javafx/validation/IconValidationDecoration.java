/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.javafx.validation;

import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import java.util.Collection;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import org.controlsfx.control.decoration.Decoration;
import org.controlsfx.control.decoration.GraphicDecoration;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationMessage;
import org.controlsfx.validation.decoration.AbstractValidationDecoration;
import org.jetbrains.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

public class IconValidationDecoration extends AbstractValidationDecoration {

  private static final Image REQUIRED_IMAGE = new Image(
      org.controlsfx.validation.decoration.GraphicValidationDecoration.class.getResource(
              "/impl/org/controlsfx/control/validation/required-indicator.png")
          .toExternalForm()); //$NON-NLS-1$

  public IconValidationDecoration() {

  }

  protected Node createDecorationNode(ValidationMessage message) {
    FontIcon graphic = getGraphicBySeverity(message.getSeverity());
    Label label = new Label();
    label.setPadding(new Insets(6, 6, 0, 0));
    label.setGraphic(graphic);
    label.setTooltip(createTooltip(message));
    label.setAlignment(Pos.CENTER);
    return label;
  }

  protected FontIcon getGraphicBySeverity(Severity severity) {
    int size = 12;
    return switch (severity) {
      case ERROR -> FxIconUtil.getFontIcon(FxIcons.X_CIRCLE_FILL, size, Color.RED);
      case WARNING -> FxIconUtil.getFontIcon(FxIcons.EXCLAMATION_CIRCLE_FILL, size, Color.GOLD);
      default -> FxIconUtil.getFontIcon(FxIcons.INFO_CIRCLE_FILL, size, Color.LIGHTSTEELBLUE);
    };
  }

  protected Tooltip createTooltip(ValidationMessage message) {
    Tooltip tooltip = new Tooltip(message.getText());
    tooltip.setOpacity(.9);
    tooltip.setAutoFix(true);
    final String style = getStyleBySeverity(message.getSeverity());
    if (style != null) {
      tooltip.getStyleClass().add(style);
    }
    return tooltip;
  }

  @Nullable
  protected String getStyleBySeverity(Severity severity) {
    return switch (severity) {
      case ERROR -> "tooltip-error";
      case WARNING -> "tooltip-warning";
      default -> null;
    };
  }

  @Override
  protected Collection<Decoration> createValidationDecorations(ValidationMessage message) {
    return List.of(new TooltipFixGraphicDecoration(createDecorationNode(message), Pos.TOP_RIGHT));
  }

  @Override
  protected Collection<Decoration> createRequiredDecorations(Control target) {
    return List.of(new GraphicDecoration(new ImageView(REQUIRED_IMAGE), Pos.TOP_LEFT,
        REQUIRED_IMAGE.getWidth() / 2, REQUIRED_IMAGE.getHeight() / 2));
  }

}
