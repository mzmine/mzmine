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

package io.github.mzmine.gui.mainwindow;

import io.github.mzmine.main.MZmineCore;
import io.mzio.users.gui.fx.UsersController;
import io.mzio.users.gui.fx.UsersViewState;

/**
 * Options for the user to login, register and control local users
 */
public class UsersTab extends SimpleTab {

  private volatile static UsersTab instance;

  private final UsersController controller;

  private UsersTab() {
    this(UsersViewState.LOCAL_USERS);
  }

  public UsersTab(UsersViewState state) {
    super("Users");
    controller = UsersController.getInstance(state);
    setContent(controller.buildView());

    setOnClosed(_ -> {
      controller.close();
      if (instance != null) {
        synchronized (UsersTab.class) {
          instance = null;
        }
      }
    });
  }

  public static UsersTab showTab() {
    return showTab(UsersViewState.LOCAL_USERS);
  }

  public static UsersTab showTab(UsersViewState state) {
    if (instance == null) {
      synchronized (UsersTab.class) {
        if (instance == null) {
          instance = new UsersTab(state);
          MZmineCore.getDesktop().addTab(instance);
        }
      }
    }
    UsersController.getInstance(state);
    var tabPane = instance.getTabPane();
    if (tabPane != null) {
      // show tab
      tabPane.getSelectionModel().select(instance);
    }
    return instance;
  }
}
