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

import java.io.IOException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public HelpWindow(String helpFileURL) {

        // Title
        setTitle("Loading help...");

        logger.debug("Loading help file " + helpFileURL);

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
