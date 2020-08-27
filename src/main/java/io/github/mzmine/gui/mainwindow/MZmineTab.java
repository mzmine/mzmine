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

package io.github.mzmine.gui.mainwindow;

import com.google.errorprone.annotations.ForOverride;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.MZmineWindow;
import io.github.mzmine.main.MZmineCore;
import java.util.Collection;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javax.annotation.Nonnull;


/**
 * This is a wrapper class to wrap any visualisation component in a pane to add to the main window.
 * Upon selection of files or feature lists in the main window, these tabs can be updated to
 * visualise the current selection. Tabs are not updated if they are not selected. However, they
 * will be updated, if a tab gets selected after a change of file or feature list selection.
 *
 * @author SteffenHeu - https://github.com/SteffenHeu - steffen.heuckeroth@uni-muenster.de
 */
public abstract class MZmineTab extends Tab {

  public static final Logger logger = Logger.getLogger(MZmineTab.class.getName());

  /**
   * If checked/unchecked the tab is/is not updated according to the selection of files/lists in the
   * main window.
   */
  private final CheckBox cbUpdateOnSelection;

  /**
   * If true this tab cannot be moved between windows. Default value is true.
   */
  private final BooleanProperty windowChangeAllowed;

  private final ContextMenu contextMenu;

  public MZmineTab(String title, boolean showBinding, boolean defaultBindingState) {
    super(title);

    cbUpdateOnSelection = new CheckBox("");
    cbUpdateOnSelection.setTooltip(new Tooltip(
        "If selected this tab is updated according to the current selection of raw files or feature lists."));
    cbUpdateOnSelection.setSelected(defaultBindingState);

    windowChangeAllowed = new SimpleBooleanProperty(true);

    if (showBinding) {
      setGraphic(cbUpdateOnSelection);
    }

    contextMenu = new ContextMenu();
    contextMenu.setOnShowing(e -> updateContextMenu());
    updateContextMenu();
    setContextMenu(contextMenu);

  }

  public MZmineTab(String title) {
    this(title, false, false);
  }

  @Nonnull
  public abstract Collection<? extends RawDataFile> getRawDataFiles();

  @Nonnull
  public abstract Collection<? extends ModularFeatureList> getFeatureLists();

  @Nonnull
  public abstract Collection<? extends ModularFeatureList> getAlignedFeatureLists();

  public abstract void onRawDataFileSelectionChanged(
      Collection<? extends RawDataFile> rawDataFiles);

  public abstract void onFeatureListSelectionChanged(
      Collection<? extends ModularFeatureList> featureLists);

  public abstract void onAlignedFeatureListSelectionChanged(
      Collection<? extends ModularFeatureList> featurelists);

  public boolean isUpdateOnSelection() {
    return cbUpdateOnSelection.isSelected();
  }

  public BooleanProperty updateOnSelectionProperty() {
    return cbUpdateOnSelection.selectedProperty();
  }

  public void setUpdateOnSelection(boolean updateOnSelection) {
    this.cbUpdateOnSelection.setSelected(updateOnSelection);
  }

  /**
   * @return true or false. If true this tab can be moved between windows.
   * @see MZmineTab#windowChangeAllowed
   */
  public boolean isWindowChangeAllowed() {
    return windowChangeAllowed.get();
  }

  public BooleanProperty windowChangeAllowedProperty() {
    return windowChangeAllowed;
  }

  /**
   * @param windowChangeAllowed true or false. If true this tab can be moved between windows.
   * @see MZmineTab#windowChangeAllowed
   */
  public void setWindowChangeAllowed(boolean windowChangeAllowed) {
    this.windowChangeAllowed.set(windowChangeAllowed);
  }

  private void updateContextMenu() {
    MenuItem bind = new MenuItem("(Un)bind to selection");
    MenuItem openInNewWindow = new MenuItem("Open in new window");
    Menu moveToWindow = new Menu("Move to window...");
    MenuItem closeTab = new MenuItem("Close");

    bind.setOnAction(e -> {
      cbUpdateOnSelection.setSelected(!cbUpdateOnSelection.isSelected());
      e.consume();
    });

    closeTab.setOnAction(e -> {
      if (this.isClosable()) {
        getTabPane().getTabs().remove(this);
      }
      e.consume();
    });

    openInNewWindow.setOnAction(e -> {
      if (!isWindowChangeAllowed()) {
        logger.info("Window change not allowed for tab " + this.getText());
        e.consume();
        return;
      }
      getTabPane().getTabs().remove(this);
      new MZmineWindow().addTab(this);
      e.consume();
    });

    MenuItem moveToMainWindow = new MenuItem("Main window");
    moveToMainWindow.setOnAction(e -> {
      if (MZmineCore.getDesktop().getTabsInMainWindow().size() < MZmineGUI.MAX_TABS
          && isWindowChangeAllowed()) {
        getTabPane().getTabs().remove(this);
        MZmineCore.getDesktop().addTab(this);
        e.consume();
        return;
      }
      logger.info(
          "Maximum number of tabs in main window reached or tab cannot be moved. Cannot move tab to main window.");
      e.consume();
    });

    moveToWindow.getItems().add(moveToMainWindow);

    for (MZmineWindow window : MZmineCore.getDesktop().getWindows()) {
      MenuItem wi = new MenuItem(window.getTitle());

      if (window.isExclusive()) {
        wi.setDisable(true);
      }

      wi.setOnAction(e -> {
        if (window.getNumberOfTabs() >= MZmineGUI.MAX_TABS || !isWindowChangeAllowed()) {
          logger.info("Maximum number of tabs in " + window.getTitle()
              + " window reached or tab cannot be moved. Cannot move tab to main window.");
          e.consume();
          return;
        }
        getTabPane().getTabs().remove(this);
        window.addTab(this);
        e.consume();
      });
      moveToWindow.getItems().add(wi);
    }
    contextMenu.getItems().clear();
    contextMenu.getItems().addAll(bind, new SeparatorMenuItem(), openInNewWindow, moveToWindow,
        new SeparatorMenuItem(), closeTab);
  }
}
