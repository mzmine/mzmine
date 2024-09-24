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

import io.github.mzmine.gui.mainwindow.mainmenu.ModuleMenuItem;
import io.github.mzmine.modules.MZmineRunnableModule;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination.Modifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FxMenuUtil {

  public static MenuItem menuItem(@NotNull String text, @Nullable Runnable onClick,
      @Nullable KeyCodeCombination accelerator) {
    MenuItem menuItem = new MenuItem(text);
    if (onClick != null) {
      menuItem.setOnAction(_ -> onClick.run());
    }
    menuItem.setAccelerator(accelerator);
    return menuItem;
  }

  public static MenuItem addMenuItem(Menu menu, @NotNull String text, @NotNull Runnable onClick,
      @Nullable KeyCodeCombination accelerator) {
    final MenuItem item = menuItem(text, onClick, accelerator);
    menu.getItems().add(item);
    return item;
  }

  public static MenuItem addMenuItem(Menu menu, @NotNull String text, @NotNull Runnable onClick,
      KeyCode mainKey, Modifier... modifers) {
    final MenuItem item = menuItem(text, onClick, new KeyCodeCombination(mainKey, modifers));
    menu.getItems().add(item);
    return item;
  }

  public static ModuleMenuItem menuItem(@NotNull String text,
      @NotNull Class<? extends MZmineRunnableModule> module,
      @Nullable KeyCodeCombination accelerator) {
    final ModuleMenuItem item = new ModuleMenuItem(text, null, module, accelerator);
    return item;
  }

  public static ModuleMenuItem menuItem(@NotNull String text,
      @NotNull Class<? extends MZmineRunnableModule> module, KeyCode mainKey,
      Modifier... modifers) {
    return menuItem(text, module, new KeyCodeCombination(mainKey, modifers));
  }

  public static ModuleMenuItem addMenuItem(Menu menu, @NotNull String text,
      @NotNull Class<? extends MZmineRunnableModule> module,
      @Nullable KeyCodeCombination accelerator) {
    final ModuleMenuItem item = menuItem(text, module, accelerator);
    menu.getItems().add(item);
    return item;
  }

  public static ModuleMenuItem addMenuItem(Menu menu, @NotNull String text,
      @NotNull Class<? extends MZmineRunnableModule> module, KeyCode mainKey,
      Modifier... modifers) {
    final ModuleMenuItem item = menuItem(text, module, mainKey, modifers);
    menu.getItems().add(item);
    return item;
  }

  public static void addSeparator(Menu menu) {
    menu.getItems().add(new SeparatorMenuItem());
  }
}
