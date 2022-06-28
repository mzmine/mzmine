/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.util.javafx;

import com.google.common.collect.ImmutableList;
import io.github.mzmine.main.MZmineCore;
import java.util.logging.Logger;
import javafx.beans.property.SimpleListProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Dynamically-built Windows menu.
 */
public class WindowsMenu extends Menu {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final MenuItem closeAllMenuItem;
  private final SimpleListProperty<MenuItem> itemsProperty;

  /**
   * Create the "Windows" menu for a MDI view
   */
  public WindowsMenu() {

    super("Windows");

    closeAllMenuItem = new MenuItem("Close all windows");
    closeAllMenuItem.setOnAction(e -> closeAllWindows());

    // If the menu has <= 3 items, it means only the main MZmine window is showing
    itemsProperty = new SimpleListProperty<>(getItems());
    closeAllMenuItem.disableProperty().bind(itemsProperty.sizeProperty().lessThanOrEqualTo(5));

    getItems().addAll(closeAllMenuItem, new SeparatorMenuItem());

    this.setOnShowing(e -> fillWindowsMenu());

  }

  /**
   * Add the Windows menu
   */
  public static void addWindowsMenu(final Scene scene) {
    Parent rootNode = scene.getRoot();
    if (rootNode instanceof Pane) {
      Pane rootPane = (Pane) rootNode;
      MenuBar menuBar = new MenuBar();
      menuBar.setUseSystemMenuBar(true);
      menuBar.getMenus().add(new WindowsMenu());
      rootPane.getChildren().add(menuBar);
    }
  }

  private void fillWindowsMenu() {
    while (getItems().size() > 4) {
      getItems().remove(4);
    }
    for (Window win : Window.getWindows()) {
      if (win instanceof Stage) {
        Stage stage = (Stage) win;
        final MenuItem item = new MenuItem(stage.getTitle());
        item.setOnAction(e -> {
          stage.toFront();
        });
        getItems().add(item);
      }
    }
  }

  private void closeAllWindows() {
    // Close all JavaFX Windows
    final var allWindows = ImmutableList.copyOf(Stage.getWindows());
    for (Window window : allWindows) {
      if (window == MZmineCore.getDesktop().getMainWindow()) {
        continue;
      }
      logger.finest("Closing window " + window);
      window.hide();
    }
  }

}
