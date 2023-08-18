/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.gui.framework.fx.FilterableTreeItem;
import io.github.mzmine.gui.framework.fx.TreeItemPredicate;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineModuleCategory.MainCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javafx.beans.NamedArg;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class BatchModuleTreePane extends BorderPane {

  private static final Logger logger = Logger.getLogger(BatchModuleTreePane.class.getName());
  // by using linked hash map, the items will be added to the tree view as specified in the modules list
  private final Map<MainCategory, FilterableTreeItem<Object>> mainCategoryItems = new LinkedHashMap<>();
  private final Map<MZmineModuleCategory, FilterableTreeItem<Object>> categoryItems = new LinkedHashMap<>();
  private final TreeView<Object> treeView = new TreeView<>();
  private final TextField searchField = new TextField();

  private Runnable closeButtonEventHandler;
  private Consumer<MZmineRunnableModule> eventHandler;

  /**
   * @param includeTools can be used in fxml with includeTools="true"
   */
  public BatchModuleTreePane(@NamedArg("includeTools") boolean includeTools) {
    BorderPane bottom = new BorderPane(searchField);
    FontIcon icon = FxIconUtil.getFontIcon("bi-x-circle", 20);
    icon.setOnMouseClicked(event -> xButtonPressed());
    HBox box = new HBox(6, icon, new Label("Search"));
    box.setAlignment(Pos.CENTER_LEFT);
    box.setOpaqueInsets(new Insets(4, 5, 0, 5));
    bottom.setLeft(box);

    setCenter(treeView);
    setBottom(bottom);

    // set data to tree
    addModules((List<Class<? extends MZmineRunnableModule>>) (List) BatchModeModulesList.MODULES);
    if (includeTools) {
      addModules(BatchModeModulesList.TOOLS_AND_VISUALIZERS);
    }

    final FilterableTreeItem<Object> originalRoot = new FilterableTreeItem<>("Root");
    originalRoot.getSourceChildren().addAll(mainCategoryItems.values());
    treeView.setRoot(originalRoot);
    treeView.setShowRoot(false);

    // interactions
    searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      // filter, expand, and select
      TreeItemPredicate<Object> predicate = TreeItemPredicate.createSubStringPredicate(
          newValue.split(" "));

      var firstMatchingNode = originalRoot.expandAllMatches(predicate);
      treeView.getSelectionModel().select(firstMatchingNode);
    });

    treeView.setOnMouseClicked(e -> {
      if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
        e.consume();
        addSelectedModule();
      }
    });

    EventHandler<KeyEvent> keyHandler = event -> {
      if (event.getCode() == KeyCode.ENTER) {
        event.consume();
        addSelectedModule();
      }
      if (event.getCode() == KeyCode.ESCAPE) {
        event.consume();
        xButtonPressed();
      }
    };
    EventHandler<KeyEvent> arrowHandler = event -> {
      if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.PAGE_UP) {
        event.consume();
        shiftSelection(-1);
      }
      if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.PAGE_DOWN) {
        event.consume();
        shiftSelection(1);
      }
    };
    treeView.setOnKeyPressed(keyHandler);
    searchField.setOnKeyPressed(keyHandler);
    searchField.addEventHandler(KeyEvent.KEY_PRESSED, arrowHandler);
  }

  private void shiftSelection(final int add) {
    int newIndex = treeView.getSelectionModel().getSelectedIndex();
    while (true) {
      newIndex += add;
      TreeItem<Object> item = treeView.getTreeItem(newIndex);
      if (item == null) {
        return;
      } else if (item.getValue() instanceof BatchModuleWrapper) {
        treeView.getSelectionModel().select(newIndex);
        return;
      }
    }
  }

  public void addModules(final List<Class<? extends MZmineRunnableModule>> modules) {
    for (Class<? extends MZmineRunnableModule> moduleClass : modules) {
      final MZmineRunnableModule module = MZmineCore.getModuleInstance(moduleClass);
      final MZmineModuleCategory category = module.getModuleCategory();
      final FilterableTreeItem<Object> categoryItem = categoryItems.computeIfAbsent(category, c -> {
        final FilterableTreeItem<Object> item = new FilterableTreeItem<>(c);
        final FilterableTreeItem<Object> mainItem = mainCategoryItems.computeIfAbsent(
            c.getMainCategory(), FilterableTreeItem::new);
        mainItem.getSourceChildren().add(item);
        return item;
      });
      categoryItem.getSourceChildren()
          .add(new FilterableTreeItem<>(new BatchModuleWrapper(module)));
    }
  }

  public void setCloseButtonEventHandler(final Runnable closeButtonEventHandler) {
    this.closeButtonEventHandler = closeButtonEventHandler;
  }

  private void xButtonPressed() {
    searchField.setText("");
    if (closeButtonEventHandler != null) {
      closeButtonEventHandler.run();
    }
  }

  public void setOnAddModuleEventHandler(final Consumer<MZmineRunnableModule> eventHandler) {
    this.eventHandler = eventHandler;
  }

  /**
   * Module in tree was clicked or otherwise activated. Fire event to handler
   */
  public void addSelectedModule() {
    if (eventHandler != null) {
      // Processing module selected?
      TreeItem<Object> selectedNode = treeView.getSelectionModel().getSelectedItem();
      if (selectedNode == null || selectedNode.getValue() == null) {
        return;
      }
      final Object selectedItem = selectedNode.getValue();
      if (!(selectedItem instanceof BatchModuleWrapper wrappedModule)) {
        return;
      }

      if (!(wrappedModule.getModule() instanceof MZmineRunnableModule module)) {
        logger.finest(
            "Cannot add module that is not an MZmineRunnableModule " + wrappedModule.toString());
        return;
      }

      eventHandler.accept(module);
    }
  }


  public void focusSearchField() {
    searchField.requestFocus();
  }

  public void clearSearchText() {
    searchField.clear();
  }
}
