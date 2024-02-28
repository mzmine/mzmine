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

package io.github.mzmine.gui.helpwindow;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import com.google.common.base.Strings;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;

/**
 * Simple help window
 */
public class HelpWindow extends Stage {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  public HelpWindow(String helpFileURL) {

    // Title
    setTitle("Loading help...");

    logger.finest("Loading help file " + helpFileURL);

    try {
      // Load the window FXML
      URL mainFXML = getClass().getResource("HelpWindow.fxml");
      FXMLLoader loader = new FXMLLoader(mainFXML);
      BorderPane rootPane = (BorderPane) loader.load();
      Scene scene = new Scene(rootPane, 800, 600, Color.WHITE);
      setScene(scene);

      // Load the requested page
      HelpWindowController controller = loader.getController();
      WebEngine webEngine = controller.getEngine();
      webEngine.load(helpFileURL);

      // Update title based on loaded page
      webEngine.titleProperty().addListener(e -> {
        final String title = webEngine.getTitle();
        if (!Strings.isNullOrEmpty(title))
          setTitle("MZmine help: " + title);
      });

    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}
