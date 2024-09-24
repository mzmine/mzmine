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

import io.github.mzmine.gui.mainwindow.mainmenu.impl.FeatureDetectionMenuBuilder;
import io.github.mzmine.gui.mainwindow.mainmenu.impl.ProjectMenuBuilder;
import io.github.mzmine.gui.mainwindow.mainmenu.impl.RawDataMenuBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

public abstract class MenuBuilder {

  public static MenuBuilder forCategory(final MainMenuEntries category) {
    return switch (category) {
      case PROJECT -> new ProjectMenuBuilder();
      case RAW_DATA_METHODS -> new RawDataMenuBuilder();
      case FEATURE_DETECTION -> new FeatureDetectionMenuBuilder();
      case FEATURE_LIST_METHODS -> null;
      case VISUALISATION -> null;
      case TOOLS -> null;
      case MZWIZARD -> null;
      case WINDOWS -> null;
      case USERS -> null;
      case HELP -> null;
    };
  }

  /**
   * Builds the menu for the respective workspaces. Multiple workspaces may be selected, e.g. LC-MS
   * and IMS-MS, so we don't need an enum value for LC-IMS-MS, MALDI-IMS-MS, and DI-IMS-MS or so,
   * but can just compose of the individual workspaces.
   *
   */
  public abstract Menu build(Collection<Workspace> workspaces);

  /**
   * Removes all {@link WorkspaceMenuItem}s that do not belong in the given workspace. It's the
   * implementing classes' responsibility to call this method, as some menus may be more specific
   * than just filtering.
   *
   * @param menu       the menu to filter. this instance is mutated.
   * @param workspaces the workspaces to filter for.
   * @return the filtered menu. the same instance as the parameter.
   */
  public static Menu filterMenu(final Menu menu, Collection<Workspace> workspaces) {

    List<MenuItem> itemsToRemove = new ArrayList<>();
    ObservableList<MenuItem> items = menu.getItems();
    for (MenuItem item : items) {
      if (item instanceof WorkspaceMenuItem wi) {
        if (workspaces.stream().noneMatch(wi::contains)) {
          itemsToRemove.add(item);
        }
      }

      // check sub menus recursively
      if (item instanceof Menu m) {
        filterMenu(m, workspaces);
        if (m.getItems().isEmpty()) {
          // remove empty sub menus
          menu.getItems().remove(m);
        }
      }
    }

    menu.getItems().removeAll(itemsToRemove);

    // remove duplicate separators
    for (int i = 0; i < items.size() - 1; i++) {
      MenuItem item = items.get(i);

      if(item instanceof SeparatorMenuItem si && items.get(i + 1) instanceof SeparatorMenuItem) {
        items.remove(si);
      }
    }

    return menu;
  }
}
