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
