/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.javafx.components.factories;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.util.FxColorUtil;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import org.jetbrains.annotations.Nullable;

public class FxLabels {

  public enum Styles {
    REGULAR, BOLD_TITLE, BOLD, ITALIC, // colored
    WARNING, ERROR;

    public void addStyleClass(Label label) {
      var style = getStyleClass();
      if (style != null) {
        label.getStyleClass().add(style);
      }
    }

    @Nullable
    public String getStyleClass() {
      return switch (this) {
        case WARNING -> "warning-label";
        case ERROR -> "error-label";
        case REGULAR -> null;
        case BOLD_TITLE -> "bold-title-label";
        case BOLD -> "bold-label";
        case ITALIC -> "italic-label";
      };
    }
  }

  public static Label styled(String name, String styleClass) {
    final Label label = new Label(name);
    label.getStyleClass().add(styleClass);
    return label;
  }

  public static Label newBoldLabel(String name) {
    return styled(name, Styles.BOLD.getStyleClass());
  }

  public static Label newItalicLabel(String name) {
    return styled(name, Styles.ITALIC.getStyleClass());
  }

  public static Label underlined(String name) {
    final Label label = new Label(name);
    label.setUnderline(true);
    return label;
  }

  public static Label newLabel(Styles style, String text) {
    return newLabel(style, null, text);
  }

  public static Label newLabel(Styles style, @Nullable Color color) {
    return newLabel(style, color, "");
  }

  public static Label newLabel(Styles style, @Nullable Color color, @Nullable String text) {
    return newLabel(style, color, TextAlignment.CENTER, text);
  }

  public static Label newLabel(Styles style, @Nullable Color color,
      @Nullable TextAlignment textAlignment, @Nullable String text) {
    Label label = new Label(text);
    if (color != null) {
      label.setStyle("-fx-text-fill: " + FxColorUtil.colorToHex(color));
    }
    style.addStyleClass(label);
    if (textAlignment != null) {
      label.setTextAlignment(textAlignment);
    }
    label.setWrapText(true);
    return label;
  }

  // direct bindings
  public static Label newLabel(Styles style, ObservableValue<? extends String> binding) {
    return newLabel(style, null, binding);
  }

  public static Label newLabel(Styles style, @Nullable Color color,
      ObservableValue<? extends String> binding) {
    return newLabel(style, color, null, binding);
  }

  public static Label newLabel(Styles style, @Nullable Color color,
      @Nullable TextAlignment textAlignment, ObservableValue<? extends String> binding) {
    Label label = newLabel(style, color, textAlignment, "");
    label.textProperty().bind(binding);
    return label;
  }

  public static Label newBoldTitle(String text) {
    return newLabel(Styles.BOLD_TITLE, text);
  }

  public static Label newBoldTitle(ObservableValue<? extends String> binding) {
    return newLabel(Styles.BOLD_TITLE, binding);
  }

  public static Label newLabel(String text) {
    return newLabel(Styles.REGULAR, text);
  }

  public static Label newLabelNoWrap(String text) {
    final Label label = newLabel(Styles.REGULAR, text);
    label.setWrapText(false);
    label.setMinWidth(Region.USE_PREF_SIZE);
    return label;
  }

  public static Label newLabel(ObservableValue<? extends String> binding) {
    return newLabel(Styles.REGULAR, binding);
  }

  public static Hyperlink newHyperlink(Runnable onClick, String text) {
    var hyperlink = new Hyperlink(text);
    hyperlink.setOnAction(_ -> onClick.run());
    return hyperlink;
  }

  public static Hyperlink newWebHyperlink(String link) {
    var hyperlink = new Hyperlink(link);
    hyperlink.setOnAction(_ -> DesktopService.getDesktop().openWebPage(hyperlink.getText()));
    return hyperlink;
  }
}
