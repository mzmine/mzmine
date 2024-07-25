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

  public static final Text mzminePaper = hyperlinkText(
      "Schmid R, Heuckeroth S, Korf A et al. Nat Biotechnol 41, 447–449 (2023).",
      "https://www.nature.com/articles/s41587-023-01690-2");
  public static final Text gnpsPaper = hyperlinkText(
      "Wang M et al. Nat Biotechnol 34.8 (2016): 828-837",
      "https://www.nature.com/nbt/journal/v34/n8/full/nbt.3597.html");

  public static final Text fbmnPaper = hyperlinkText(
      "Nothias LF, Petras D, Schmid R et al. Nat Meth 17, 905–908 (2020)",
      "https://www.nature.com/articles/s41592-020-0933-6");

  public static final Text iimnPaper = hyperlinkText(
      "Schmid R, Petras D, Nothias LF et al. Nat Comm 12, 3832 (2021)",
      "https://www.nature.com/articles/s41467-021-23953-9");

  public static final Text masstPaper = hyperlinkText(
      "Wang M, Jarmusch A, Vargas F et al. Nat Biotechnol 38, 23–26 (2020)",
      "https://doi.org/10.1038/s41587-019-0375-9");

  public static final Text sirius4Paper = hyperlinkText(
      "Dührkop K, Fleischauer M, Ludwig M et al. Nat Methods 16, 299–302 (2019)",
      "http://dx.doi.org/10.1038/s41592-019-0344-8");

  public static final Text ms2deepscorePaper = hyperlinkText(
      "de Jonge N, Joas D, Truong LJ, van der Hooft JJJ, Huber F bioRxiv 2024.03.25.586580",
      "https://doi.org/10.1101/2024.03.25.586580");

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
