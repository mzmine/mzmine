/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.gui;

import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.javafx.WindowsMenu;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * @author SteffenHeu https://github.com/SteffenHeu - steffen.heuckeroth@uni-muenster.de
 */
public class MZmineWindow extends Stage {

  protected final BorderPane mainPane;
  protected final TabPane tabPane;

  /**
   * If this flag is set to true, no tabs will be added to this window via {@link
   * MZmineGUI#addTab(MZmineTab)}. However, tabs can still be added by directly calling the {@link
   * MZmineWindow#addTab(MZmineTab)} method of this window.
   */
  private final boolean isExclusive;

  /**
   * Creates a new window.
   */
  public MZmineWindow() {
    this(false);
  }

  /**
   * @param isExclusive If this flag is set to true, no tabs will be added to this window via {@link
   *                    MZmineGUI#addTab(MZmineTab)}. However, tabs can still be added by directly
   *                    calling the {@link MZmineWindow#addTab(MZmineTab)} method of this window.
   *                    The default value is false.
   */
  public MZmineWindow(boolean isExclusive) {
    super();

    this.isExclusive = isExclusive;

    mainPane = new BorderPane();
    tabPane = new TabPane();
    Scene scene = new Scene(mainPane);
    scene.getStylesheets()
        .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    mainPane.setCenter(tabPane);
    tabPane.getSelectionModel().selectedItemProperty()
        .addListener((obs, old, newVal) -> setTitle(newVal.getText()));
    this.setScene(scene);
    WindowsMenu.addWindowsMenu(scene);
  }

  /**
   * Adds a {@link MZmineTab} to this window. Also invokes {@link Stage#show()} so it can be invoked
   * in the form of
   * <p>
   * {@code new MZmineWindow.addTab(tab);}
   *
   * @param tab The tab.
   * @return {@link java.util.List#add(Object)}
   */
  public boolean addTab(MZmineTab tab) {
    if (!isShowing()) {
      show();
    }
    return tabPane.getTabs().add(tab);
  }

  public ObservableList<Tab> getTabs() {
    return tabPane.getTabs();
  }

  public int getNumberOfTabs() {
    return tabPane.getTabs().size();
  }
}
