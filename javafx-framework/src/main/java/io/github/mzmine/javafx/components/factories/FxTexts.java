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
import io.github.mzmine.javafx.components.factories.FxLabels.Styles;
import javafx.scene.text.Text;

public class FxTexts {

  public static Text text(String content) {
    return new Text(content);
  }

  public static Text underlined(String content) {
    final Text text = new Text(content);
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
    final Text text = new Text(content);
    text.getStyleClass().add(styleClass);

    return text;
  }

  public static Text hyperlinkText(String link) {
    final Text text = new Text(link);
    text.getStyleClass().add("hyperlink");
    text.setOnMouseReleased(_ -> DesktopService.getDesktop().openWebPage(link));
    return text;
  }

  public static Text hyperlinkText(String content, String link) {
    final Text text = new Text(content);
    text.getStyleClass().add("hyperlink");
    text.setOnMouseReleased(_ -> DesktopService.getDesktop().openWebPage(link));
    return text;
  }

  public static Text hyperlinkText(Text text, String link) {
    text.getStyleClass().add("hyperlink");
    text.setOnMouseReleased(_ -> DesktopService.getDesktop().openWebPage(link));
    return text;
  }

  public static Text linebreak() {
    return text("\n");
  }

}
