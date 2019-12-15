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

package io.github.mzmine.gui.mainwindow;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.NewVersionCheck;
import io.github.mzmine.gui.NewVersionCheck.CheckType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * The controller class for MainMenu.fxml
 * 
 */
public class MainMenuController {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    @FXML
    private Menu windowsMenu;

    public void closeProject(ActionEvent event) {
        MZmineGUI.requestCloseProject();
    }

    public void exitApplication(ActionEvent event) {
        MZmineGUI.requestQuit();
    }

    public void openLink(ActionEvent event) {
        String url = "";

        // Link for menu item
        MenuItem item = (MenuItem) event.getSource();
        switch (item.getText()) {
        case "Tutorials":
            url = "http://mzmine.github.io/documentation.html";
            break;
        case "Support":
            url = "http://mzmine.github.io/support.html";
            break;
        case "Report Problem":
            url = "https://github.com/mzmine/mzmine3/issues";
            break;
        }

        // Open link in browser
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec("xdg-open " + url);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void versionCheck(ActionEvent event) {
        // Check for new version of MZmine
        logger.info("Checking for new MZmine version");
        NewVersionCheck NVC = new NewVersionCheck(CheckType.MENU);
        Thread nvcThread = new Thread(NVC);
        nvcThread.setPriority(Thread.MIN_PRIORITY);
        nvcThread.start();
    }

    public void setPreferences(ActionEvent event) {
        // Show the Preferences dialog
        logger.info("Showing the Preferences dialog");
        // MZmineCore.getConfiguration().getPreferences().showSetupDialog(null);
    }

    public void showAbout(ActionEvent event) {
        MZmineGUI.showAboutWindow();
    }

    public void fillWindowsMenu(ActionEvent event) {
        windowsMenu.getItems().clear();
        for (Window win : Window.getWindows()) {
            if (win instanceof Stage) {
                Stage stage = (Stage) win;
                MenuItem item = new MenuItem(stage.getTitle());
                windowsMenu.getItems().add(item);
            }
        }
    }

    public void closeAllWindows(ActionEvent event) {
        for (Window win : Window.getWindows()) {
            if (win == MZmineCore.getDesktop().getMainWindow())
                continue;
            win.hide();
        }

    }

    @SuppressWarnings("unchecked")
    public void runModule(ActionEvent event) {
        assert event.getSource() instanceof MenuItem;
        final MenuItem menuItem = (MenuItem) event.getSource();
        assert menuItem.getUserData() instanceof String;
        final String moduleClass = (String) menuItem.getUserData();

        logger.info("Menu item activated for module " + moduleClass);
        Class<? extends MZmineRunnableModule> moduleJavaClass;
        try {
            moduleJavaClass = (Class<? extends MZmineRunnableModule>) Class
                    .forName(moduleClass);
        } catch (Exception e) {
            MZmineGUI.displayMessage("Cannot load module class " + moduleClass);
            return;
        }

        MZmineModule module = MZmineCore.getModuleInstance(moduleJavaClass);

        if (module == null) {
            MZmineGUI.displayMessage(
                    "Cannot find module of class " + moduleClass);
            return;
        }

        ParameterSet moduleParameters = MZmineCore.getConfiguration()
                .getModuleParameters(moduleJavaClass);

        logger.info("Setting parameters for module " + module.getName());

        SwingUtilities.invokeLater(() -> {
            ExitCode exitCode = moduleParameters.showSetupDialog(null, true);
            if (exitCode != ExitCode.OK)
                return;

            ParameterSet parametersCopy = moduleParameters.cloneParameterSet();
            logger.finest("Starting module " + module.getName()
                    + " with parameters " + parametersCopy);
            MZmineCore.runMZmineModule(moduleJavaClass, parametersCopy);
        });

    }
}
