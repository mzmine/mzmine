/*
 * Copyright 2006-2021 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.featurelisttable_modular;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableViewSkinBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * Helper class to replace default column selection popup for TableView.
 *
 * <p>
 * The original idea credeted to Roland and was found on https://stackoverflow.com/questions/27739833/adapt-tableview-menu-button
 * </p>
 * <p>
 * This improved version targets to solve several problems:
 * <ul>
 * <li>avoid to have to assign the TableView with the new context menu after the
 * window shown (it could cause difficulty when showAndWait() should be used. It
 * solves the problem by registering the onShown event of the containing Window.
 * </li>
 * <li>corrects the mispositioning bug when clicking the + button while the menu
 * is already on.</li>
 * <li>works using keyboard</li>
 * <li>possibility to add additional menu items</li>
 * </ul>
 * </p>
 * <p>
 * Usage from your code:
 *
 * <pre>
 * contextMenuHelper = new TableViewContextMenuHelper(this);
 * // Adding additional menu items
 * MenuItem exportMenuItem = new MenuItem("Export...");
 * contextMenuHelper.getAdditionalMenuItems().add(exportMenuItem);
 * </pre>
 * </p>
 * <p>
 * https://stackoverflow.com/questions/27739833/adapt-tableview-menu-button
 *
 * @author Roland
 * @author bvissy
 */
public class TreeColumnMenuHelper {

  private final TreeTableView tableView;
  private final List<MenuItem> additionalMenuItems = new ArrayList<>();
  private ContextMenu columnPopupMenu;
  private boolean showAllColumnsOperators = true;
  // Default key to show menu: Shortcut (CTRL on windows) + Shift + Space
  private Function<KeyEvent, Boolean> showMenuByKeyboardCheck = ke ->
      ke.getCode().equals(KeyCode.SPACE) && ke.isShortcutDown() && ke.isShiftDown();


  public TreeColumnMenuHelper(TreeTableView tableView) {
    super();
    this.tableView = tableView;

    if (tableView.getSkin() != null) {
      registerListeners();
      return;
    }

    // listen to skin change - this should happen once the table is shown
    tableView.skinProperty().addListener((a, b, newSkin) -> {
      tableView.tableMenuButtonVisibleProperty().addListener((ob, o, n) -> {
        if (n == true) {
          registerListeners();
        }
      });
      if (tableView.isTableMenuButtonVisible()) {
        registerListeners();
      }
    });
  }

  private static List<? extends TableColumnBase> getColumns(Control table) {
    if (table instanceof TableView tab) {
      return tab.getColumns();
    } else if (table instanceof TreeTableView tree) {
      return tree.getColumns();
    } else {
      throw new IllegalArgumentException(
          "Table argument is no TreeTableView or TableView. Actual class: " + table.getClass()
              .getName());
    }
  }

  /**
   * Registers the listeners.
   */
  private void registerListeners() {
    final Node buttonNode = findButtonNode();

    // Keyboard listener on the table
    tableView.addEventHandler(KeyEvent.KEY_PRESSED, ke -> {
      if (showMenuByKeyboardCheck.apply(ke)) {
        showContextMenu();
        ke.consume();
      }
    });

    // replace mouse listener on "+" node
    assert buttonNode != null;
    buttonNode.setOnMousePressed(me -> {
      showContextMenu();
      me.consume();

    });

  }

  protected void showContextMenu() {
    final Node buttonNode = findButtonNode();

    setFixedHeader();

    // When the menu is already shown clicking the + button hides it.
    if (columnPopupMenu != null) {
      columnPopupMenu.hide();
    } else {
      // Show the menu
      final ContextMenu newColumnPopupMenu = createContextMenu();
      newColumnPopupMenu.setOnHidden(ev -> columnPopupMenu = null);
      columnPopupMenu = newColumnPopupMenu;
      columnPopupMenu.show(buttonNode, Side.BOTTOM, 0, 0);
      // Repositioning the menu to be aligned by its right side (keeping inside the table view)
      columnPopupMenu.setX(
          buttonNode.localToScreen(buttonNode.getBoundsInLocal()).getMaxX() - columnPopupMenu
              .getWidth());
    }
  }

  private void setFixedHeader() {
    // setting the preferred height for the table header row
    // if the preferred height isn't set, then the table header would disappear if there are no visible columns
    // and with it the table menu button
    // by setting the preferred height the header will always be visible
    // note: this may need adjustments in case you have different heights in columns (eg when you use grouping)
    Region tableHeaderRow = getTableHeaderRow();
    double defaultHeight = tableHeaderRow.getHeight();
    tableHeaderRow.setPrefHeight(defaultHeight);
  }

  private Node findButtonNode() {
    TableHeaderRow tableHeaderRow = getTableHeaderRow();
    if (tableHeaderRow == null) {
      return null;
    }

    for (Node child : tableHeaderRow.getChildren()) {

      // child identified as cornerRegion in TableHeaderRow.java
      if (child.getStyleClass().contains("show-hide-columns-button")) {
        return child;
      }
    }
    return null;
  }

  private TableHeaderRow getTableHeaderRow() {
    TableViewSkinBase tableSkin = (TableViewSkinBase) tableView.getSkin();
    if (tableSkin == null) {
      return null;
    }

    // get all children of the skin
    ObservableList<Node> children = tableSkin.getChildren();

    // find the TableHeaderRow child
    for (Node node : children) {

      if (node instanceof TableHeaderRow header) {
        return header;
      }
    }
    return null;
  }

  /**
   * Create a menu with custom items. The important thing is that the menu remains open while you
   * click on the menu items.
   */
  private ContextMenu createContextMenu() {

    ContextMenu cm = new ContextMenu();

    // create new context menu
    CustomMenuItem cmi;

    if (showAllColumnsOperators) {
      // select all item
      Label selectAll = new Label("Select all");
      selectAll.addEventHandler(MouseEvent.MOUSE_CLICKED, this::doSelectAll);

      cmi = new CustomMenuItem(selectAll);
      cmi.setOnAction(this::doSelectAll);
      cmi.setHideOnClick(false);
      cm.getItems().add(cmi);

      // deselect all item
      Label deselectAll = new Label("Deselect all");
      deselectAll.addEventHandler(MouseEvent.MOUSE_CLICKED, this::doDeselectAll);

      cmi = new CustomMenuItem(deselectAll);
      cmi.setOnAction(this::doDeselectAll);
      cmi.setHideOnClick(false);
      cm.getItems().add(cmi);

      // separator
      cm.getItems().add(new SeparatorMenuItem());
    }

    if (!additionalMenuItems.isEmpty()) {
      cm.getItems().add(new SeparatorMenuItem());
      cm.getItems().addAll(additionalMenuItems);
    }

    // menu item for each of the available columns
    for (TableColumnBase col : getColumns(tableView)) {

      CheckBox cb = new CheckBox(col.getText());
      cb.selectedProperty().bindBidirectional(col.visibleProperty());

      cmi = new CustomMenuItem(cb);
      cmi.setOnAction(e -> {
        cb.setSelected(!cb.isSelected());
        e.consume();
      });
      cmi.setHideOnClick(false);

      cm.getItems().add(cmi);
    }

    return cm;
  }

  protected void setAllVisible(boolean visible) {
    for (TableColumnBase col : getColumns(tableView)) {
      col.setVisible(visible);
    }
  }

  protected void doDeselectAll(Event e) {
    setAllVisible(false);
    e.consume();
  }

  protected void doSelectAll(Event e) {
    setAllVisible(true);
    e.consume();
  }

  public boolean isShowAllColumnsOperators() {
    return showAllColumnsOperators;
  }

  /**
   * Sets whether the Select all/Deselect all buttons are visible
   *
   * @param showAllColumnsOperators
   */
  public void setShowAllColumnsOperators(boolean showAllColumnsOperators) {
    this.showAllColumnsOperators = showAllColumnsOperators;
  }

  public List<MenuItem> getAdditionalMenuItems() {
    return additionalMenuItems;
  }

  public Function<KeyEvent, Boolean> getShowMenuByKeyboardCheck() {
    return showMenuByKeyboardCheck;
  }

  /**
   * Overrides the keypress check to show the menu. Default is Shortcut + Shift + Space.
   *
   * <p>
   * To disable keyboard shortcut use the <code>e -> false</code> function.
   * </p>
   *
   * @param showMenuByKeyboardCheck
   */
  public void setShowMenuByKeyboardCheck(Function<KeyEvent, Boolean> showMenuByKeyboardCheck) {
    this.showMenuByKeyboardCheck = showMenuByKeyboardCheck;
  }

}