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

package io.github.mzmine.javafx.components.factories;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.components.factories.FxLabels.Styles;
import io.github.mzmine.javafx.util.FxColorUtil;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.jetbrains.annotations.NotNull;

public class FxTexts {

  public static Text text(String content) {
    return new Text(content);
  }

  public static Text underlined(String content) {
    final Text text = text(content);
    text.setUnderline(true);
    return text;
  }

  public static Text boldText(String content) {
    return styledText(content, Styles.BOLD.getStyleClass());
  }

  public static Text italicText(String content) {
    return styledText(content, Styles.ITALIC.getStyleClass());
  }

  public static Text styledText(String content, Styles style) {
    return styledText(content, style.getStyleClass());
  }

  public static Text styledText(String content, String styleClass) {
    return styledText(text(content), styleClass);
  }

  public static @NotNull Text styledText(Text text, String styleClass) {
    text.getStyleClass().add(styleClass);
    return text;
  }

  public static Text hyperlinkText(String link) {
    return hyperlinkText(link, link);
  }

  public static Text hyperlinkText(String content, String link) {
    final Text text = text(content);
    return hyperlinkText(text, link);
  }

  public static Text hyperlinkText(Text text, String link) {
    text.getStyleClass().add("hyperlink");
    text.setOnMouseReleased(_ -> DesktopService.getDesktop().openWebPage(link));
    return text;
  }

  public static Text linebreak() {
    return text("\n");
  }

  public static Text styledText(ObservableValue<String> text, Styles styleClass) {
    return styledText(text, styleClass.getStyleClass());
  }

  public static Text styledText(ObservableValue<String> text, String styleClass) {
    return styledText(text(text), styleClass);
  }

  public static @NotNull Text text(ObservableValue<String> text) {
    final Text node = text(text.getValue());
    node.textProperty().bind(text);
    return node;
  }

  public static Text colored(Text text, Color color) {
    // text.setFill does not work - overwritten by css?
    text.setStyle("-fx-fill: " + FxColorUtil.colorToHex(color));
    return text;
  }

  public static Label colored(Label text, Color color) {
    text.setStyle("-fx-text-fill: " + FxColorUtil.colorToHex(color));
    return text;
  }
}
