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

import java.awt.Desktop;
import java.net.URI;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * The controller class for HelpWindow.fxml
 */
public class HelpWindowController {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  @FXML
  private WebView helpWebView;

  @FXML
  public void initialize() {

    helpWebView.getEngine().locationProperty().addListener((observable, oldValue, newValue) -> {

      // Open external links in system web browser
      if (newValue.startsWith("http://") || newValue.startsWith("https://")) {
        try {

          // Open system browser
          Desktop.getDesktop().browse(new URI(newValue));

          // Stay on the page that is already open
          helpWebView.getEngine().load(oldValue);

        } catch (Exception e) {
          e.printStackTrace();
        }
      }

    });

  }

  @FXML
  protected void handleClose(ActionEvent event) {
    logger.finest("Closing help window");
    helpWebView.getScene().getWindow().hide();
  }

  WebEngine getEngine() {
    return helpWebView.getEngine();
  }

}
