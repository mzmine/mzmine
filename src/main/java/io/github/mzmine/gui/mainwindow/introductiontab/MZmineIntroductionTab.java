/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.gui.mainwindow.introductiontab;

import io.github.mzmine.gui.SnowFallPane;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.javafx.HtmlLinkOpenExternalBrowserListener;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class MZmineIntroductionTab extends SimpleTab {

  public MZmineIntroductionTab() {
    super("Welcome to MZmine 3");

    final WebView browser = new WebView();
    final WebEngine engine = browser.getEngine();

    if (MZmineCore.getConfiguration().isDarkMode()) {
      engine.load(getClass().getResource("MZmineIntroduction_darkmode.html").toString());
    } else {
      engine.load(getClass().getResource("MZmineIntroduction.html").toString());
    }
    engine.getLoadWorker().stateProperty()
        .addListener(new HtmlLinkOpenExternalBrowserListener(browser));

    StackPane stack = new StackPane(browser);
    HBox main = new HBox(stack);

    SnowFallPane snow = new SnowFallPane(main, 1500);
    snow.setMouseTransparent(true);
    stack.getChildren().add(snow);

    snow.setMaxSize(400, 400);
    snow.setPrefSize(400, 400);
//    snow.prefWidthProperty().bind(main.widthProperty());
//    snow.prefHeightProperty().bind(main.heightProperty());
//    snow.maxWidthProperty().bind(main.widthProperty());
//    snow.maxHeightProperty().bind(main.heightProperty());

    super.setContent(main);
  }
}
