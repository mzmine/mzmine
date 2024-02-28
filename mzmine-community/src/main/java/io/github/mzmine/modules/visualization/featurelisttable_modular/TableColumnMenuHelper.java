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

package io.github.mzmine.modules.visualization.featurelisttable_modular;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.BooleanProperty;
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
import org.jetbrains.annotations.NotNull;

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
public class TableColumnMenuHelper {

  protected final Control tableView;
  protected final List<MenuItem> additionalMenuItems = new ArrayList<>();
  protected ContextMenu columnPopupMenu;
  protected boolean showAllColumnsOperators = true;
  // Default key to show menu: Shortcut (CTRL on windows) + Shift + Space
  protected Function<KeyEvent, Boolean> showMenuByKeyboardCheck = ke ->
      ke.getCode().equals(KeyCode.SPACE) && ke.isShortcutDown() && ke.isShiftDown();


  public TableColumnMenuHelper(TableView tableView) {
    this((Control) tableView);
  }

  public TableColumnMenuHelper(TreeTableView tableView) {
    this((Control) tableView);
  }

  private TableColumnMenuHelper(Control tableView) {
    super();
    this.tableView = tableView;

    if (tableView.getSkin() != null) {
      registerListeners();
      return;
    }

    // listen to skin change - this should happen once the table is shown
    tableView.skinProperty().addListener((a, b, newSkin) -> {
      final BooleanProperty tableMenuButtonVisibleProperty = getTableMenuButtonVisibleProperty(
          tableView);
      tableMenuButtonVisibleProperty.addListener((ob, o, n) -> {
        if (n) {
          registerListeners();
        }
      });
      if (tableMenuButtonVisibleProperty.get()) {
        registerListeners();
      }
    });
  }

  /**
   * @return property that controls the menu button in the corner of the table
   */
  private BooleanProperty getTableMenuButtonVisibleProperty(@NotNull Control tableView) {

    if (tableView instanceof TableView tab) {
      return tab.tableMenuButtonVisibleProperty();
    }
    if (tableView instanceof TreeTableView tree) {
      return tree.tableMenuButtonVisibleProperty();
    }
    throw new IllegalArgumentException(
        "Argument is no TableView or TreeTableView. Actual class: " + tableView.getClass()
            .getName());
  }

  /**
   * Get columns of the table or treetable
   *
   * @return list of columns
   */
  public List<? extends TableColumnBase> getColumns() {
    if (tableView instanceof TableView tab) {
      return tab.getColumns();
    } else if (tableView instanceof TreeTableView tree) {
      return tree.getColumns();
    } else {
      throw new IllegalArgumentException(
          "Table argument is no TreeTableView or TableView. Actual class: " + tableView.getClass()
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
  protected ContextMenu createContextMenu() {

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
      cm.getItems().addAll(additionalMenuItems);
      cm.getItems().add(new SeparatorMenuItem());
    }

    // menu item for each of the available columns
    for (TableColumnBase col : getColumns()) {

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
    for (TableColumnBase col : getColumns()) {
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

  /**
   * Creates the base context menu with the select all and custom menu items but without checkboxes
   * for the buttons
   *
   * @return a new context menu
   */
  @NotNull
  protected ContextMenu createBaseMenu() {
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
      cm.getItems().addAll(additionalMenuItems);
      cm.getItems().add(new SeparatorMenuItem());
    }
    return cm;
  }
}