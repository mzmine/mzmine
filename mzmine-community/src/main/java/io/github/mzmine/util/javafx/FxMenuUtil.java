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

package io.github.mzmine.util.javafx;

import io.github.mzmine.modules.MZmineRunnableModule;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination.Modifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FxMenuUtil {

  public static MenuItem newMenuItem(@NotNull String text, @Nullable Runnable onClick,
      @Nullable KeyCodeCombination accelerator) {
    MenuItem menuItem = new MenuItem(text);
    if (onClick != null) {
      menuItem.setOnAction(_ -> onClick.run());
    }
    menuItem.setAccelerator(accelerator);
    return menuItem;
  }

  public static MenuItem addMenuItem(Menu menu, @NotNull String text, @Nullable Runnable onClick) {
    return addMenuItem(menu, text, onClick, null);
  }

  public static MenuItem addMenuItem(Menu menu, @NotNull String text, @Nullable Runnable onClick,
      @Nullable KeyCode mainKey, @Nullable Modifier... modifers) {
    final MenuItem item = newMenuItem(text, onClick, null);

    if (mainKey != null && modifers != null) {
      item.setAccelerator(new KeyCodeCombination(mainKey, modifers));
    } else if (mainKey != null) {
      item.setAccelerator(new KeyCodeCombination(mainKey));
    }

    menu.getItems().add(item);
    return item;
  }

  public static RadioMenuItem addRadioMenuItem(@NotNull Menu menu, @NotNull ToggleGroup grp,
      @NotNull String text, @Nullable Runnable onClick) {
    final RadioMenuItem item = new RadioMenuItem(text);
    item.setToggleGroup(grp);
    if(onClick != null) {
      item.setOnAction(_ -> onClick.run());
    }
    menu.getItems().add(item);
    return item;
  }

  public static ModuleMenuItem addModuleMenuItem(Menu menu,
      @NotNull Class<? extends MZmineRunnableModule> module, KeyCode mainKey,
      Modifier... modifers) {
    KeyCodeCombination accelerator;
    if (mainKey != null && modifers != null) {
      accelerator = new KeyCodeCombination(mainKey, modifers);
    } else if (mainKey != null && modifers == null) {
      accelerator = new KeyCodeCombination(mainKey);
    } else {
      accelerator = null;
    }

    final ModuleMenuItem item = new ModuleMenuItem(module, accelerator);
    menu.getItems().add(item);
    return item;
  }

  public static Menu addModuleMenuItems(Menu menu,
      @NotNull Class<? extends MZmineRunnableModule>... modules) {
    for (@NotNull Class<? extends MZmineRunnableModule> module : modules) {
      addModuleMenuItem(menu, module, null);
    }
    return menu;
  }

  public static Menu addModuleMenuItems(String title,
      @NotNull Class<? extends MZmineRunnableModule>... modules) {
    return addModuleMenuItems(new Menu(title), modules);
  }

  /**
   * Creates a sub menu with the name {@code subMenuTitle}, adds all modules to the sub menu and
   * adds the sub menu to the {@code menu}.
   *
   * @return The created subMenu.
   */
  public static Menu addModuleMenuItems(Menu menu, String subMenuTitle,
      @NotNull Class<? extends MZmineRunnableModule>... modules) {
    final Menu subMenu = new Menu(subMenuTitle);
    addModuleMenuItems(subMenu, modules);
    menu.getItems().add(subMenu);
    return subMenu;
  }


  public static void addSeparator(Menu menu) {
    menu.getItems().add(new SeparatorMenuItem());
  }
}
