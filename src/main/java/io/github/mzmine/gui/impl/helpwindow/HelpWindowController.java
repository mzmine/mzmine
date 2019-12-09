/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 3.
 * 
 * MZmine 3 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 3 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 3; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.gui.impl.helpwindow;

import java.awt.Desktop;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * The controller class for HelpWindow.fxml
 */
public class HelpWindowController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @FXML
    private WebView helpWebView;

    @FXML
    public void initialize() {

        helpWebView.getEngine().locationProperty()
                .addListener((observable, oldValue, newValue) -> {

                    // Open external links in system web browser
                    if (newValue.startsWith("http://")
                            || newValue.startsWith("https://")) {
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
        logger.debug("Closing help window");
        helpWebView.getScene().getWindow().hide();
    }

    WebEngine getEngine() {
        return helpWebView.getEngine();
    }

}
