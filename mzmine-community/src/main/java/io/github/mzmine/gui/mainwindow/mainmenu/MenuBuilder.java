/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.gui.mainwindow.mainmenu;

import io.github.mzmine.gui.mainwindow.mainmenu.impl.ProjectMenuBuilder;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public abstract class MenuBuilder {

  public static MenuBuilder forCategory(final MainMenuEntries category) {
    return switch (category) {
      case PROJECT -> new ProjectMenuBuilder();
      case RAW_DATA_METHODS -> null;
      case FEATURE_DETECTION -> null;
      case FEATURE_LIST_METHODS -> null;
      case VISUALISATION -> null;
      case TOOLS -> null;
      case MZWIZARD -> null;
      case WINDOWS -> null;
      case USERS -> null;
      case HELP -> null;
    };
  }

  public abstract Menu build(Workspace workspace);

  /**
   * Removes all {@link WorkspaceMenuItem}s that do not belong in the given workspace. It's the
   * implementing classes' responsibility to call this method, as some menus may be more specific
   * than just filtering.
   *
   * @param menu      the menu to filter. this instance is mutated.
   * @param workspace the workspace to filter for.
   * @return the filtered menu. the same instance as the parameter.
   */
  public static Menu filterMenu(final Menu menu, Workspace workspace) {

    List<MenuItem> itemsToRemove = new ArrayList<>();
    for (MenuItem item : menu.getItems()) {
      if (item instanceof WorkspaceMenuItem wi) {
        if (!wi.contains(workspace)) {
          itemsToRemove.add(item);
        }
      }

      // check sub menus recursively
      if (item instanceof Menu m) {
        filterMenu(m, workspace);
        if (m.getItems().isEmpty()) {
          // remove empty sub menus
          menu.getItems().remove(m);
        }
      }
    }

    menu.getItems().removeAll(itemsToRemove);
  }
}
